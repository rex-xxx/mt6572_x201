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

#define P_MODULE  P_MODULE_DEC( 7816SM )

#include "wme_context.h"

#if ( (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) ) && (defined P_INCLUDE_SE_SECURITY)

#define P_SEHALSM_NULL_IDENTIFIER ((uint32_t)(-1))
#define P_SEHALSM_RAW_IDENTIFIER  ((uint32_t)0x80808080)

static void static_PSeHalSmTraceBytes(const char* pszLabel, const uint8_t* pData, uint32_t nData)
{
   static const char sHex[]="0123456789ABCDEF";
   char sData[260*3], *psData = sData;
   uint32_t i;
   if (nData > 260) { nData = 260; }
   for(i = 0; i < nData; i++)
   {
      *psData++ = sHex[ (pData[i] >> 4) & 0x0F ];
      *psData++ = sHex[ (pData[i]     ) & 0x0F ];
      *psData++ = ' ';
   }
   *psData = '\0';

   PDebugTrace("%s = %s", pszLabel, sData);
}

/* ----------------------------------------------------------------------------

      HAL SE Access Functions

   ---------------------------------------------------------------------------- */

static void static_PSeHalSmRawOpenChannel(
                  tContext* pContext,
                  tSESlot* pSlot,
                  tPBasicGenericDataCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  uint32_t nType,
                  const uint8_t* pAID,
                  uint32_t nAIDLength)
{
   tCSePorting* pSePorting = PContextGetSeHalInstance(pContext)->pSePorting;

   PDFCFillCallbackContext(
      pContext, (tDFCCallback*)pCallback, pCallbackParameter,
      &pSlot->sExchange.sCallbackContext);

   CSeOpenChannel(pSePorting, pSlot->nHalSlotIdentifier, pSlot->nHalSessionIdentifier,
      nType, pAID, nAIDLength);
}

static void static_PSeHalSmRawExchangeApdu(
                  tContext* pContext,
                  tSESlot* pSlot,
                  uint32_t nChannelIdentifier,
                  tPBasicGenericDataCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  const uint8_t* pSendApduBuffer,
                  uint32_t nSendApduBufferLength,
                  uint8_t* pReceivedApduBuffer,
                  uint32_t nReceivedApduBufferMaxLength)
{
   tCSePorting* pSePorting = PContextGetSeHalInstance(pContext)->pSePorting;

   PDFCFillCallbackContext(
      pContext, (tDFCCallback*)pCallback, pCallbackParameter,
      &pSlot->sExchange.sCallbackContext);

   CSeExchangeApdu(pSePorting, pSlot->nHalSlotIdentifier, pSlot->nHalSessionIdentifier, nChannelIdentifier,
      pSendApduBuffer, nSendApduBufferLength, pReceivedApduBuffer, nReceivedApduBufferMaxLength);
}

static void static_PSeHalSmRawGetResponseApdu(
                  tContext* pContext,
                  tSESlot* pSlot,
                  uint32_t nChannelIdentifier,
                  uint8_t* pResponseApduBuffer,
                  uint32_t nResponseApduBufferLength,
                  uint32_t* pnResponseApduActualSize)
{
   tCSePorting* pSePorting = PContextGetSeHalInstance(pContext)->pSePorting;

   CSeGetResponseApdu(pSePorting, pSlot->nHalSlotIdentifier, pSlot->nHalSessionIdentifier, nChannelIdentifier,
      pResponseApduBuffer,
      nResponseApduBufferLength,
      pnResponseApduActualSize);
}

static void static_PSeHalSmRawCloseChannel(
                  tContext* pContext,
                  tSESlot* pSlot,
                  uint32_t nChannelIdentifier,
                  tPBasicGenericCallbackFunction* pCallback,
                  void* pCallbackParameter)
{
   tCSePorting* pSePorting = PContextGetSeHalInstance(pContext)->pSePorting;

   PDFCFillCallbackContext(
      pContext, (tDFCCallback*)pCallback, pCallbackParameter,
      &pSlot->sExchange.sCallbackContext);

   CSeCloseChannel(pSePorting, pSlot->nHalSlotIdentifier, pSlot->nHalSessionIdentifier, nChannelIdentifier);
}

/* ----------------------------------------------------------------------------

      Logical Channel Interface

   ---------------------------------------------------------------------------- */

/** The state indicating that there is no current operation */
#define P_SEHALSM_STATE_IDLE                 0
/** The state indicating that a channel opening is ongoing */
#define P_SEHALSM_STATE_OPEN_PENDING         1
/** The state indicating that an APDU exchange command is ongoing */
#define P_SEHALSM_STATE_EXCHANGE_PENDING     2
/** The state indicating that a MANAGE CHANNEL [close] APDU command is ongoing (normal) */
#define P_SEHALSM_STATE_CLOSE_PENDING        3
/** The state indicating that a MANAGE CHANNEL [close] APDU command is ongoing (error condition) */
#define P_SEHALSM_STATE_CLOSE_ERROR_PENDING  4
/** The state indicating that MANAGE CHANNEL [close] APDU commands are ongoing */
#define P_SEHALSM_STATE_CLOSE_ALL_PENDING    5


typedef struct __tPSeHalSmChannel
{
   /* Whether teh channel is opened or not */
   bool_t bIsOpened;

   /** The channel identifier (may be P_SEHALSM_NULL_IDENTIFIER) */
   uint32_t nChannelIdentifier;

   /* The corresponding instance */
   tPSeHalSmInstance* pInstance;

   /* Whether an empty AID was used to select the application */
   bool_t bIsDefaultSelected;

   /** AID length */
   uint32_t nAIDLength;

   /** AID value (of the currently selected application) */
   uint8_t aAID[P_7816SM_MAX_AID_LENGTH];

   /** Buffer accumulating the response to the last APDU command */
   uint8_t* pAllocatedReceivedAPDUBuffer;

   /** The size of the pAllocatedReceivedAPDUBuffer buffer */
   uint32_t nAllocatedReceivedAPDUBufferMaxLength;

   /** The offset to the next available byte within pAllocatedReceivedAPDUBuffer */
   uint32_t nResponseIndex;
} tPSeHalSmChannel;


struct __tPSeHalSmInstance
{
   /** The current context */
   tContext* pContext;

   /** The slot instance */
   tSESlot* pSlot;

   /** The raw channel */
   tPSeHalSmChannel sRawChannel;

   /** The logical channels */
   tPSeHalSmChannel sLogicalChannel[P_7816SM_MAX_LOGICAL_CHANNEL];

   /** The next channel reference */
   /*uint32_t nNextChannelReference; @todo */

   /** Data associated with the current operation */
   struct
   {
      /** The current operation state */
      uint32_t nState;

      /** The error to be returned on error-processing completion */
      uint32_t nCloseResult;

      /** The reader-to-card communication buffer (Command APDU) */
      uint8_t aReaderToCard[5+255];

      /** The card-to-reader communication buffer (Response APDU) */
      uint8_t aCardToReader[256+2];

      /** AID length */
      uint32_t nAIDLength;

      /** AID value (used in the next SELECT command) */
      uint8_t aAID[P_7816SM_MAX_AID_LENGTH];

      struct
      {
        /** The buffer provided by the caller to store the APDU response */
        uint8_t* pReceivedApduBuffer;
        /** The length of the buffer provided by the caller to store the APDU response */
        uint32_t nReceivedApduBufferMaxLength;
      } result;

      /** Callback information */
      struct
      {
         union
         {
            tPBasicGenericDataCallbackFunction* pSmChannelCallback;
            tPBasicGenericDataCallbackFunction* pSmDataCallback;
            tPBasicGenericCallbackFunction* pSmCallback;
         } u;
         void* pCallbackParameter;
      } callback;

      /** The channel of the current operation */
      tPSeHalSmChannel* pChannel;
      /** The channel type of the current operation */
      uint32_t nChannelType;
   } current;
};


/* ----------------------------------------------------------------------------
      Internal functions
   ---------------------------------------------------------------------------- */

/**
 * @brief Frees the internal buffer used to accumulate the last command APDU response.
 *
 * @param[in]  pChannel  The channel.
 **/
static void static_PSeHalSmFreeReceptionBuffer(
            tPSeHalSmChannel* pChannel)
{
   CDebugAssert(pChannel != null);

   if (pChannel->pAllocatedReceivedAPDUBuffer != null)
   {
      CMemoryFree(pChannel->pAllocatedReceivedAPDUBuffer);
      pChannel->pAllocatedReceivedAPDUBuffer = (uint8_t*)null;
   }

   pChannel->nAllocatedReceivedAPDUBufferMaxLength = 0;
   pChannel->nResponseIndex = 0;
}

/* forward declaration (see below) */
static void static_PSeHalSmExchangeApduCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  uint32_t nDataLength,
                  W_ERROR nResult );

/**
 * @brief Exchanges an APDU command with the ISO-7816 card.
 *
 * The APDU command is already available in internal buffer pInstance->current.aReaderToCard.
 * The class byte is then updated to contain the correct logical channel bits.
 *
 * @param[in]  pInstance  The instance of the ISO-7816 State Machine.
 *
 * @param[in]  pChannel  The channel to be used to send the APDU command.
 *
 * @param[in]  nReaderToCardLength  The length of the APDU command.
 */
static void static_PSeHalSmSendApdu(
                  tPSeHalSmInstance* pInstance,
                  tPSeHalSmChannel* pChannel,
                  uint32_t nReaderToCardLength)
{
   CDebugAssert(pInstance != null);
   CDebugAssert(pChannel != null);
   CDebugAssert(nReaderToCardLength < sizeof(pInstance->current.aReaderToCard));

   static_PSeHalSmTraceBytes("[SEHAL-SM] C-APDU", pInstance->current.aReaderToCard,
      nReaderToCardLength);

   pInstance->current.pChannel = pChannel;

   static_PSeHalSmRawExchangeApdu(
      pInstance->pContext,
      pInstance->pSlot,
      pChannel->nChannelIdentifier,
      static_PSeHalSmExchangeApduCompleted,
      pChannel,  /* pCallbackParameter */
      pInstance->current.aReaderToCard,
      nReaderToCardLength,
      pInstance->current.aCardToReader,
      sizeof(pInstance->current.aCardToReader));
}

/**
 * @brief Sends a GET RESPONSE command.
 *
 * @param[in]  pInstance  The instance of the ISO-7816 State Machine.
 *
 * @param[in]  pChannel  The channel to be used to send the APDU command.
 *
 * @param[in]  nLe  The Le value to be passed in P3.
 **/
static void static_PSeHalSmGetResponse(
                  tPSeHalSmInstance* pInstance,
                  tPSeHalSmChannel* pChannel,
                  uint8_t nLe)
{
   CDebugAssert(pInstance != null);
   CDebugAssert(pChannel != null);

   pInstance->current.aReaderToCard[0] = P_7816SM_CLA; /* CLA byte is set later */
   pInstance->current.aReaderToCard[1] = P_7816SM_INS_GET_RESPONSE;
   pInstance->current.aReaderToCard[2] = 0x00;
   pInstance->current.aReaderToCard[3] = 0x00;
   pInstance->current.aReaderToCard[4] = nLe;

   static_PSeHalSmSendApdu(pInstance, pChannel, 5);
}

/**
 * @brief Reissues the last APDU command with the right Le.
 *
 * @param[in]  pInstance  The instance of the ISO-7816 State Machine.
 *
 * @param[in]  pChannel  The channel to be used to send the APDU command.
 *
 * @param[in]  nLe  The Le value to be passed in P3.
 **/
static void static_PSeHalSmReissueCommand(
                  tPSeHalSmInstance* pInstance,
                  tPSeHalSmChannel* pChannel,
                  uint8_t nLe)
{
   CDebugAssert(pInstance != null);
   CDebugAssert(pChannel != null);

   /* Free reception buffer */
   static_PSeHalSmFreeReceptionBuffer(pChannel);

   /* The command is assumed to be an outgoing command! */
   pInstance->current.aReaderToCard[4] = nLe;

   static_PSeHalSmSendApdu(pInstance, pChannel, 5);
}

/** Callback used when the static_PSeHalSmSendOpenLogicalChannelApdu function completes */
static void static_PSeHalSmSendOpenLogicalChannelApduCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  uint32_t nDataLength,
                  W_ERROR nResult)
{
   tPSeHalSmInstance* pInstance = (tPSeHalSmInstance*)pCallbackParameter;

   if (nResult != W_SUCCESS)
   {
      pInstance->current.nState = P_SEHALSM_STATE_IDLE;

      /* Notify caller of error */
      pInstance->current.callback.u.pSmChannelCallback(
         pContext,
         pInstance->current.callback.pCallbackParameter,
         P_7816SM_NULL_CHANNEL, nResult);
      return;
   }

   CDebugAssert(pInstance->current.pChannel != null);
   CDebugAssert(pInstance->current.pChannel->bIsOpened == W_FALSE);
   CDebugAssert(pInstance->current.pChannel->nChannelIdentifier != P_SEHALSM_NULL_IDENTIFIER);

   static_PSeHalSmExchangeApduCompleted(pContext,
      pInstance->current.pChannel,
      nDataLength,
      nResult);
}

/**
 * Extracts the AID from the FCI returned by the SELECT [AID] command.
 *
 * The ISO/GP SE returns the following answer to SELECT.
 * 6F L              FCI template
 *    84 L  xx-xx    Application AID
 *    A5 L           Proprietary data
 **/
static W_ERROR static_PSeHalSmExtractAidFromFci(
   const uint8_t* pBuffer,
   uint32_t nBufferLength,
   const uint8_t** ppAID,
   uint32_t* pnAIDLength)
{
   uint32_t nError;
   tAsn1Parser sParser;
   tAsn1Parser sIncludedParser;
   /*
    * The ISO/GP SE returns the following answer to SELECT.
    * 6F L              FCI template
    *    84 L  xx-xx    Application AID
    *    A5 L           Proprietary data
    */

   nError = PAsn1InitializeParser( &sParser, pBuffer, nBufferLength );
   if (nError != W_SUCCESS)
   {
      PDebugWarning("static_PSeHalSmExtractAidFromFci: Bad TLV format");
      return nError;
   }

   if(PAsn1GetTagValue(&sParser) != 0x6F)
   {
      PDebugWarning("static_PSeHalSmExtractAidFromFci: Bad '6F' tag");
      return W_ERROR_BAD_PARAMETER;
   }

   nError = PAsn1ParseIncludedTlv( &sParser, &sIncludedParser);
   if (nError != W_SUCCESS)
   {
      PDebugWarning("static_PSeHalSmExtractAidFromFci: Bad included TLV format");
      return nError;
   }

   do
   {
      if(PAsn1GetTagValue(&sIncludedParser) == 0x84)
      {
         PAsn1GetPointerOnBinaryContent(&sIncludedParser, ppAID, pnAIDLength);
         return W_SUCCESS;
      }
   } while(PAsn1MoveToNextTlv(&sIncludedParser) == W_SUCCESS);

   *ppAID = null;
   *pnAIDLength = 0;

   PDebugWarning("static_PSeHalSmExtractAidFromFci: The AID tag '84' is not found");
   return W_ERROR_BAD_PARAMETER;
}

/** Callback used when the static_PSeHalSmSendCloseChannelApdu function completes */
void static_PSeHalSmSendCloseChannelApduCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  W_ERROR nResult)
{
   tPSeHalSmInstance* pInstance = (tPSeHalSmInstance*)pCallbackParameter;
   uint32_t nDataLength = 0;

   CDebugAssert(pInstance->current.pChannel->nChannelIdentifier != P_SEHALSM_NULL_IDENTIFIER);

   if (nResult == W_SUCCESS)
   {
      /* Simulate success */
      pInstance->current.aCardToReader[0] = 0x90;
      pInstance->current.aCardToReader[1] = 0x00;
      nDataLength = 2;
   }

   static_PSeHalSmExchangeApduCompleted(pContext,
      pInstance->current.pChannel,
      nDataLength,
      nResult);
}

/**
 * @brief Closes a logical channel.
 *
 * This function sends a MANAGE CHANNEL [close] command.
 *
 * @param[in]  pInstance  The instance of the ISO-7816 State Machine.
 *
 * @param[in]  pChannel  The channel to be closed.
 *
 **/
static void static_PSeHalSmSendCloseChannelApdu(
                  tContext* pContext,
                  tPSeHalSmInstance* pInstance,
                  tPSeHalSmChannel* pChannel)
{
   CDebugAssert(pContext != null);
   CDebugAssert(pInstance != null);
   CDebugAssert(pChannel != null);

   /* Free reception buffer */
   static_PSeHalSmFreeReceptionBuffer(pChannel);

   pInstance->current.pChannel = pChannel;

   static_PSeHalSmRawCloseChannel(pContext,
      pInstance->pSlot,
      pChannel->nChannelIdentifier,
      static_PSeHalSmSendCloseChannelApduCompleted,
      pInstance);
}

/**
 * @brief Closes a logical channel that is still opened.
 *
 * @param[in]  pInstance  The instance of the ISO-7816 State Machine.
 *
 * @result  An error code, among which:
 *          - W_SUCCESS, if all logical channels are already closed
 *          - W_ERROR_OPERATION_PENDING, if a logical channel closing operation
 *               has been started.
 **/
static W_ERROR static_PSeHalSmCloseNextLogicalChannel(
                  tContext* pContext,
                  tPSeHalSmInstance* pInstance)
{
   uint32_t nChannelId;

   CDebugAssert(pInstance->current.nState == P_SEHALSM_STATE_IDLE);

   for(nChannelId = P_7816SM_MAX_LOGICAL_CHANNEL - 1; nChannelId >= 1; nChannelId--)
   {
      if (pInstance->sLogicalChannel[nChannelId].bIsOpened != W_FALSE)
      {
         /* Force closed state (in case something wrong occurs later, to prevent endless loop) */
         pInstance->sLogicalChannel[nChannelId].bIsOpened = W_FALSE;

         pInstance->current.nState = P_SEHALSM_STATE_CLOSE_ALL_PENDING;
         static_PSeHalSmSendCloseChannelApdu(pContext, pInstance, &pInstance->sLogicalChannel[nChannelId]);

         return W_ERROR_OPERATION_PENDING;
      }
   }

   /* Force closing of the raw channel and the basic logical channel */
   pInstance->sRawChannel.bIsOpened = W_FALSE;
   pInstance->sRawChannel.nChannelIdentifier = P_SEHALSM_NULL_IDENTIFIER;

   pInstance->sLogicalChannel[0].bIsOpened = W_FALSE;
   pInstance->sLogicalChannel[0].nChannelIdentifier = P_SEHALSM_NULL_IDENTIFIER;

   /* All channels are already closed */
   return W_SUCCESS;
}

/**
 * @brief Accumulates response APDU bytes into an internal buffer.
 *
 * @param[in]  pChannel  The channel that received the bytes.
 *
 * @param[in]  pCardToReaderBuffer  The buffer containing the bytes to be accumulated.
 *
 * @param[in]  nDataLength  The number of bytes to be accumulated.
 *
 * @param[in]  nExtraDataLength  The number of extra bytes to be allocated in case the
 *                internal buffer is expanded.
 *
 * @result     An error code, among which:
 *             - W_SUCCESS, in case of success.
 *             - W_ERROR_OUT_OF_RESOURCE, in case of an out-of-memory condition.
 **/
static W_ERROR static_PSeHalSmSaveResponseApdu(
            tPSeHalSmChannel* pChannel,
            const uint8_t* pCardToReaderBuffer,
            uint32_t nDataLength,
            uint32_t nExtraDataLength)
{
   uint8_t* pNewBuffer;
   uint32_t nNewLength;

   CDebugAssert(pChannel != null);

   if ((pCardToReaderBuffer == null) && (nDataLength != 0))
   {
      CDebugAssert(W_FALSE);
      return W_ERROR_BAD_PARAMETER;
   }

   if ((nDataLength == 0) && (nExtraDataLength == 0))
   {
      return W_SUCCESS;
   }

   nNewLength = pChannel->nResponseIndex + nDataLength + nExtraDataLength;

   if (nNewLength > pChannel->nAllocatedReceivedAPDUBufferMaxLength)
   {
      pNewBuffer = (uint8_t*)CMemoryAlloc(nNewLength);
      if (pNewBuffer == null)
      {
         PDebugError("static_PSeHalSmSaveResponseApdu: Failed to allocate %d bytes", nNewLength);
         return W_ERROR_OUT_OF_RESOURCE;
      }

      if (pChannel->pAllocatedReceivedAPDUBuffer != null)
      {
         CMemoryCopy(pNewBuffer, pChannel->pAllocatedReceivedAPDUBuffer, pChannel->nResponseIndex);
         CMemoryFree(pChannel->pAllocatedReceivedAPDUBuffer);
      }

      pChannel->pAllocatedReceivedAPDUBuffer = pNewBuffer;
      pChannel->nAllocatedReceivedAPDUBufferMaxLength = nNewLength;
   }

   /* Copy the response */
   if (pCardToReaderBuffer != null)
   {
      CMemoryCopy( pChannel->pAllocatedReceivedAPDUBuffer + pChannel->nResponseIndex, pCardToReaderBuffer, nDataLength );
      pChannel->nResponseIndex += nDataLength;
   }

   return W_SUCCESS;
}

/**
 * @brief Callback called when a Raw APDU exchange completes.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pCallbackParameter  The callback parameter.
 *
 * @param[in]  nDataLength  The length of the returned data.
 *
 * @param[in]  nResult  The error code, among which:
 *             - W_SUCCESS to indicate that nDataLength bytes are returned.
 *             - W_ERROR_BUFFER_TOO_SHORT to indicate that the provided buffer for
 *                 the reponse is too short. The number of available bytes is given
 *                 in nDataLength.
 *             - W_ERROR_UICC_COMMUNICATION to indicate a communication error with
 *                  the ISO-7816 card. In this case, nDataLength is 0.
 **/
static void static_PSeHalSmExchangeApduCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  uint32_t nDataLength,
                  W_ERROR nResult )
{
   tPSeHalSmChannel* pChannel = (tPSeHalSmChannel*)pCallbackParameter;
   tPSeHalSmInstance* pInstance = pChannel->pInstance;

   const uint8_t* pCardToReader = pInstance->current.aCardToReader;

   CDebugAssert(pInstance != null);
   CDebugAssert(pChannel != null);

   if (pInstance->current.nState == P_SEHALSM_STATE_EXCHANGE_PENDING)
   {
      if (nResult == W_SUCCESS)
      {
         static_PSeHalSmTraceBytes("[SEHAL-SM] R-APDU", pInstance->current.aCardToReader,
            nDataLength);
      }
      else
      {
         PDebugTrace("[SEHAL-SM] R-APDU = error 0x%02X (%s)", nResult, PUtilTraceError(nResult));
      }
   }

   /* Check the error code return */
   if (nResult == W_SUCCESS) for(;(pInstance->current.nState == P_SEHALSM_STATE_EXCHANGE_PENDING) || (pInstance->current.nChannelType != W_7816_CHANNEL_TYPE_RAW);)
   {
      /* The response APDU must at least contain a 2-byte status word */
      if (nDataLength < 2)
      {
         nResult = W_ERROR_UICC_COMMUNICATION;
         break;
      }

      /* Process 61xx status words: Retrieve data and send a GET RESPONSE command */
      if (pCardToReader[nDataLength - 2] == P_7816SM_SW1_61)
      {
         nResult = static_PSeHalSmSaveResponseApdu(pChannel, pCardToReader, nDataLength - 2, pCardToReader[nDataLength - 1] + 2);
         if (nResult != W_SUCCESS)
         {
            break;
         }

         static_PSeHalSmGetResponse(pInstance, pChannel, pCardToReader[nDataLength - 1]);
         return;
      }

      /* Process 6Cxx status words (T=0): Reissue the same command with the right P3=Le */
      if (pCardToReader[nDataLength - 2] == P_7816SM_SW1_6C)
      {
         static_PSeHalSmReissueCommand(pInstance, pChannel, pCardToReader[nDataLength - 1]);
         return;
      }

      break;
   }

   switch(pInstance->current.nState)
   {
      case P_SEHALSM_STATE_OPEN_PENDING:
      {
         /* This is the answer to the SELECT [AID] command */

         if (nResult == W_SUCCESS)
         {
            if (pInstance->current.nChannelType == W_7816_CHANNEL_TYPE_RAW)
            {
               CDebugAssert(nDataLength == 0);

               /* The channel is now considered as opened */
               pChannel->bIsOpened = W_TRUE;
               CDebugAssert(pChannel->nChannelIdentifier != P_SEHALSM_NULL_IDENTIFIER);

               CMemoryFill(pChannel->aAID, 0, sizeof(pChannel->aAID));
               pChannel->nAIDLength = 0;

               pInstance->current.nState = P_SEHALSM_STATE_IDLE;

               /* Notify caller of successful completion */
               pInstance->current.callback.u.pSmChannelCallback(
                  pContext,
                  pInstance->current.callback.pCallbackParameter,
                  pChannel->nChannelIdentifier, W_SUCCESS);

               return;
            }

            /* The answer must end with status word '9000', or '62xx' or '63xx' */
            if (((pCardToReader[nDataLength - 2] == 0x90) && (pCardToReader[nDataLength - 1] == 0x00))
               ||(pCardToReader[nDataLength - 2] == 0x62)
               ||(pCardToReader[nDataLength - 2] == 0x63))
            {
               uint32_t nAIDLength;
               const uint8_t* pAID;

               PDebugTrace("static_PSeHalSmExchangeApduCompleted: Application selected on channel %d", pChannel->nChannelIdentifier);

               /* Copy answer to extensible internal buffer - ignore error */
               nResult = static_PSeHalSmSaveResponseApdu(pChannel, pCardToReader, nDataLength, 0);

               /* The channel is now considered as opened */
               pChannel->bIsOpened = W_TRUE;
               CDebugAssert(pChannel->nChannelIdentifier != P_SEHALSM_NULL_IDENTIFIER);

               /* Remember the AID of the currently selected application */
               pChannel->nAIDLength = 0;
               CMemoryFill(pChannel->aAID, 0x00, sizeof(pChannel->aAID));

               /* First try to extract the AID from the returned FCI (if any) */
               if ((nDataLength > 2) && (static_PSeHalSmExtractAidFromFci(pCardToReader, nDataLength - 2, &pAID, &nAIDLength) == W_SUCCESS))
               {
                  if (nAIDLength > P_7816SM_MAX_AID_LENGTH)
                  {
                     PDebugWarning("static_PSeHalSmExchangeApduCompleted: The AID extracted from the FCI is too long (%d), truncated", nAIDLength);
                     nAIDLength = P_7816SM_MAX_AID_LENGTH;
                  }
               }
               else
               {
                  /* Use the AID passed to the Open[Basic/Logical]Channel function */
                  pAID = pInstance->current.aAID;
                  nAIDLength = pInstance->current.nAIDLength;
               }

               if (nAIDLength == 0)
               {
                  PDebugWarning("static_PSeHalSmExchangeApduCompleted: The AID of the selected application is unknown");
               }
               else
               {
                  static_PSeHalSmTraceBytes("[SEHAL-SM] Selected application AID", pAID, nAIDLength);
               }

               pChannel->bIsDefaultSelected = (pInstance->current.nAIDLength != 0) ? W_FALSE : W_TRUE;

               CMemoryCopy(pChannel->aAID, pAID, nAIDLength);
               pChannel->nAIDLength = nAIDLength;

               pInstance->current.nState = P_SEHALSM_STATE_IDLE;

               /* Notify caller of successful completion */
               pInstance->current.callback.u.pSmChannelCallback(
                  pContext,
                  pInstance->current.callback.pCallbackParameter,
                  pChannel->nChannelIdentifier, W_SUCCESS);

               return;
            }

            nResult = W_ERROR_UICC_COMMUNICATION;
            /* FALL THROUGH */
         }

         PDebugTrace("static_PSeHalSmExchangeApduCompleted: failed to select application on channel id=%d", pChannel->nChannelIdentifier);

         if (pInstance->current.nChannelType == W_7816_CHANNEL_TYPE_BASIC)
         {
            /* The application was selected on the basic logical channel */

            /* The channel is now considered as closed */
            pChannel->bIsOpened = W_FALSE;
            pChannel->nChannelIdentifier = P_SEHALSM_NULL_IDENTIFIER;

            /* Note: There is no need to close the basic logical channel with MANAGE CHANNEL */
            pInstance->current.nState = P_SEHALSM_STATE_IDLE;

            /* Notify caller of failure */
            pInstance->current.callback.u.pSmChannelCallback(
               pContext,
               pInstance->current.callback.pCallbackParameter,
               P_7816SM_NULL_CHANNEL,
               W_ERROR_ITEM_NOT_FOUND);

            return;
         }
         else
         {
            /* The application was selected on another logical channel (not the basic logical channel) */

            /* Close the newly opened logical channel */
            pInstance->current.nState = P_SEHALSM_STATE_CLOSE_ERROR_PENDING;
            pInstance->current.nCloseResult = W_ERROR_ITEM_NOT_FOUND;
            static_PSeHalSmSendCloseChannelApdu(pContext, pInstance, pChannel);
            return;
         }
      }

      case P_SEHALSM_STATE_EXCHANGE_PENDING:
      {
         /* This is the answer to an APDU command */

         if (nResult == W_SUCCESS)
         {
            /* Copy answer to extensible internal buffer - ignore error */
            (void)static_PSeHalSmSaveResponseApdu(pChannel, pCardToReader, nDataLength, 0);
         }
         else if (nResult == W_ERROR_BUFFER_TOO_SHORT)
         {
            uint32_t nActualLength = (uint32_t)(-1);

            /* Get the R-APDU length */
            static_PSeHalSmRawGetResponseApdu(pContext,
               pInstance->pSlot,
               pChannel->nChannelIdentifier,
               (uint8_t*)null, 0,
               &nActualLength);

            if (nActualLength == (uint32_t)(-1))
            {
               /* The static_PSeHalSmRawGetResponseApdu() function did nothing ?!? */
               nActualLength = 0;
               nResult = W_ERROR_BAD_STATE;
            }
            else
            {
               /* Allocate required bytes */
               nResult = static_PSeHalSmSaveResponseApdu(pChannel, (const uint8_t*)null, 0, nActualLength);
               if (nResult == W_SUCCESS)
               {
                  /* Get the R-APDU bytes */
                  static_PSeHalSmRawGetResponseApdu(pContext,
                     pInstance->pSlot,
                     pChannel->nChannelIdentifier,
                     pChannel->pAllocatedReceivedAPDUBuffer + pChannel->nResponseIndex,
                     nActualLength, &nActualLength);
                  pChannel->nResponseIndex += nActualLength;
               }
            }
         }

         if (nResult == W_SUCCESS)
         {
            nDataLength = pChannel->nResponseIndex;

            if (pChannel->nResponseIndex <= pInstance->current.result.nReceivedApduBufferMaxLength)
            {
               CDebugAssert(pChannel->pAllocatedReceivedAPDUBuffer != null);

               CMemoryCopy(pInstance->current.result.pReceivedApduBuffer,
                  pChannel->pAllocatedReceivedAPDUBuffer,
                  pChannel->nResponseIndex);

               static_PSeHalSmFreeReceptionBuffer(pChannel);

               nResult = W_SUCCESS;
            }
            else
            {
               PDebugTrace("static_PSeHalSmExchangeApduCompleted: APDU buffer is too short");

               nResult = W_ERROR_BUFFER_TOO_SHORT;
            }
         }
         else
         {
            nDataLength = 0;
         }

         pInstance->current.nState = P_SEHALSM_STATE_IDLE;

         /* Notify caller of completion */
         pInstance->current.callback.u.pSmDataCallback(
            pContext,
            pInstance->current.callback.pCallbackParameter,
            nDataLength, nResult);

         return;
      }

      case P_SEHALSM_STATE_CLOSE_PENDING:
      {
         /* This is the answer to the MANAGE CHANNEL [close] command (normal processing) */

         if (nResult == W_SUCCESS)
         {
            /* The answer must be 0x90, 0x00 */
            if ((nDataLength != 2) || (pCardToReader[0] != 0x90) || (pCardToReader[1] != 0x00))
            {
               PDebugError("static_PSeHalSmExchangeApduCompleted: MANAGE CHANNEL [close] returned a bad response (length=%d, SW=0x%02X%02X)",
                  nDataLength, pCardToReader[nDataLength - 2], pCardToReader[nDataLength - 1]);

               nResult = W_ERROR_UICC_COMMUNICATION;
            }
         }

         PDebugTrace("static_PSeHalSmExchangeApduCompleted: Channel id=%d closed", pChannel->nChannelIdentifier);

         /* The channel is now considered as closed (whatever result) */
         pChannel->bIsOpened = W_FALSE;
         pChannel->nChannelIdentifier = P_SEHALSM_NULL_IDENTIFIER;

         pInstance->current.nState = P_SEHALSM_STATE_IDLE;

         /* Notify caller of completion */
         pInstance->current.callback.u.pSmChannelCallback(
            pContext,
            pInstance->current.callback.pCallbackParameter,
            P_7816SM_NULL_CHANNEL,
            nResult);

         return;
      }

      case P_SEHALSM_STATE_CLOSE_ERROR_PENDING:
      {
         /* This is the answer to the MANAGE CHANNEL [close] command (error-condition processing) */

         if (nResult == W_SUCCESS)
         {
            /* The answer must be 0x90, 0x00 */
            if ((nDataLength != 2) || (pCardToReader[0] != 0x90) || (pCardToReader[1] != 0x00))
            {
               PDebugError("static_PSeHalSmExchangeApduCompleted: MANAGE CHANNEL [close] returned a bad response (length=%d, SW=0x%02X%02X)",
                  nDataLength, pCardToReader[nDataLength - 2], pCardToReader[nDataLength - 1]);

               nResult = W_ERROR_UICC_COMMUNICATION;
            }
         }

         PDebugTrace("static_PSeHalSmExchangeApduCompleted: Channel id=%d closed", pChannel->nChannelIdentifier);

         /* The channel is now considered as closed (whatever result) */
         pChannel->bIsOpened = W_FALSE;
         pChannel->nChannelIdentifier = P_SEHALSM_NULL_IDENTIFIER;

         pInstance->current.nState = P_SEHALSM_STATE_IDLE;

         /* Notify caller of error */
         pInstance->current.callback.u.pSmChannelCallback(
            pContext,
            pInstance->current.callback.pCallbackParameter,
            P_7816SM_NULL_CHANNEL,
            pInstance->current.nCloseResult);

         return;
      }

      case P_SEHALSM_STATE_CLOSE_ALL_PENDING:
      {
         /* This is the answer to the MANAGE CHANNEL [close] command (normal close-all processing) */

         if (nResult == W_SUCCESS)
         {
            /* The answer must be 0x90, 0x00 */
            if ((nDataLength != 2) || (pCardToReader[0] != 0x90) || (pCardToReader[1] != 0x00))
            {
               PDebugError("static_PSeHalSmExchangeApduCompleted: MANAGE CHANNEL [close] returned a bad response (length=%d, SW=0x%02X%02X)",
                  nDataLength, pCardToReader[nDataLength - 2], pCardToReader[nDataLength - 1]);

               nResult = W_ERROR_UICC_COMMUNICATION;
            }
         }

         /* Remember the first error code that occurred */
         if ((nResult != W_SUCCESS) && (pInstance->current.nCloseResult == W_SUCCESS))
         {
            pInstance->current.nCloseResult = nResult;
         }

         PDebugTrace("static_PSeHalSmExchangeApduCompleted: Channel id=%d closed", pChannel->nChannelIdentifier);

         /* The channel is now considered as closed (whatever result) */
         pChannel->bIsOpened = W_FALSE;
         pChannel->nChannelIdentifier = P_SEHALSM_NULL_IDENTIFIER;

         pInstance->current.nState = P_SEHALSM_STATE_IDLE;

         if (static_PSeHalSmCloseNextLogicalChannel(pContext, pInstance) == W_SUCCESS)
         {
            /* Notify caller of completion */
            pInstance->current.callback.u.pSmCallback(
               pContext,
               pInstance->current.callback.pCallbackParameter,
               pInstance->current.nCloseResult);
         }

         return;
      }
   }

   return;
}

/* ----------------------------------------------------------------------------
      External functions
   ---------------------------------------------------------------------------- */

/* See Header file */
W_ERROR PSeHalSmCreateInstance(
                  tContext* pContext,
                  tSESlot* pSlot,
                  tPSeHalSmInstance** ppInstance)
{
   tPSeHalSmInstance* pInstance;
   uint32_t nChannelId;

   PDebugTrace("PSeHalSmCreateInstance");

   if (pSlot == null)
   {
      PDebugError("PSeHalSmCreateInstance: pSlot == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (ppInstance == null)
   {
      PDebugError("PSeHalSmCreateInstance: ppInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   pInstance = (tPSeHalSmInstance*)CMemoryAlloc(sizeof(tPSeHalSmInstance));
   if ( pInstance == null )
   {
      PDebugError("PSeHalSmCreateInstance: Failed to allocate tPSeHalSmInstance");
      return W_ERROR_OUT_OF_RESOURCE;
   }
   CMemoryFill(pInstance, 0, sizeof(*pInstance));

   pInstance->pSlot = pSlot;
   pInstance->pContext = pContext;

   /* Initialize the raw channel */
   pInstance->sRawChannel.bIsOpened = W_FALSE;
   pInstance->sRawChannel.nChannelIdentifier = P_SEHALSM_NULL_IDENTIFIER;
   pInstance->sRawChannel.pInstance = pInstance;

   /* Initialize logical channels */
   for(nChannelId = 0; nChannelId < P_7816SM_MAX_LOGICAL_CHANNEL; nChannelId++)
   {
      pInstance->sLogicalChannel[nChannelId].bIsOpened = W_FALSE;
      pInstance->sLogicalChannel[nChannelId].nChannelIdentifier = P_SEHALSM_NULL_IDENTIFIER;
      pInstance->sLogicalChannel[nChannelId].pInstance = pInstance;
   }

   *ppInstance = pInstance;

   return W_SUCCESS;
}

/* See Header file */
void PSeHalSmDestroyInstance(
                  tContext* pContext,
                  tPSeHalSmInstance* pInstance)
{
   uint32_t nChannelId;

   PDebugTrace("PSeHalSmDestroyInstance");

   if (pInstance == null)
   {
      PDebugError("PSeHalSmDestroyInstance: pInstance == null");
      return;
   }

   if (pInstance->sRawChannel.bIsOpened != W_FALSE)
   {
      static_PSeHalSmFreeReceptionBuffer(&pInstance->sRawChannel);

      PDebugWarning("PSeHalSmDestroyInstance: Raw channel has not been closed");
   }

   for(nChannelId = 0; nChannelId < P_7816SM_MAX_LOGICAL_CHANNEL; nChannelId++)
   {
      if (pInstance->sLogicalChannel[nChannelId].bIsOpened != W_FALSE)
      {
         static_PSeHalSmFreeReceptionBuffer(&pInstance->sLogicalChannel[nChannelId]);

         PDebugWarning("PSeHalSmDestroyInstance: Logical channel id=%d has not been closed", pInstance->sLogicalChannel[nChannelId].nChannelIdentifier);
      }
   }

   CMemoryFree(pInstance);
}

/* See the definition of tP7816SmOpenChannel in header file */
static W_ERROR static_PSeHalSmOpenChannel(
                  tContext* pContext,
                  tPSeHalSmInstance* pInstance,
                  tPBasicGenericDataCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  uint32_t nType,
                  const uint8_t* pAID,
                  uint32_t nAIDLength)
{
   PDebugTrace("static_PSeHalSmOpenChannel (type=%d)", nType);

   if (pInstance == null)
   {
      PDebugError("static_PSeHalSmOpenChannel: pInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((pCallback == null) && (nType != W_7816_CHANNEL_TYPE_RAW))
   {
      PDebugError("static_PSeHalSmOpenChannel: pCallback == null");
      return W_ERROR_BAD_PARAMETER;
   }

   switch(nType)
   {
      default:
      {
         PDebugError("static_PSeHalSmOpenChannel: nType (%u) is incorrect", nType);
         return W_ERROR_BAD_PARAMETER;
      }

      case W_7816_CHANNEL_TYPE_RAW:
      {
         if ((pAID != null) || (nAIDLength != 0))
         {
            PDebugError("static_PSeHalSmOpenChannel: pAID != null || nAIDLength != 0");
            return W_ERROR_BAD_PARAMETER;
         }

         break;
      }

      case W_7816_CHANNEL_TYPE_BASIC:
      case W_7816_CHANNEL_TYPE_LOGICAL:
      {
         if ((pAID == null) && (nAIDLength != 0))
         {
            PDebugError("static_PSeHalSmOpenChannel: pAID == null && nAIDLength != 0");
            return W_ERROR_BAD_PARAMETER;
         }

         if (pAID != null)
         {
            if (nAIDLength == g_n7816SmMfAidSize)
            {
               if (CMemoryCompare(pAID, g_a7816SmMfAid, nAIDLength) != 0)
               {
                  PDebugError("static_PSeHalSmOpenChannel: pAID != null && nAIDLength == 2 but pAID does not contain 0x3F, 0x00");
                  return W_ERROR_BAD_PARAMETER;
               }
            }
            else if ((nAIDLength < P_7816SM_MIN_AID_LENGTH) || (nAIDLength > P_7816SM_MAX_AID_LENGTH))
            {
               PDebugError("static_PSeHalSmOpenChannel: pAID != null && nAIDLength not in range 5..16");
               return W_ERROR_BAD_PARAMETER;
            }
         }

         break;
      }
   }

   if (pInstance->current.nState != P_SEHALSM_STATE_IDLE)
   {
      PDebugError("static_PSeHalSmOpenChannel: A communication is currently ongoing");
      return W_ERROR_BAD_STATE;
   }

   if (nType == W_7816_CHANNEL_TYPE_RAW)
   {
      uint32_t nChannelId;

      if (pInstance->sRawChannel.bIsOpened != W_FALSE)
         {
            PDebugError("static_PSeHalSmOpenRawChannel: The raw channel is already opened");
            return W_ERROR_EXCLUSIVE_REJECTED;
         }

      for(nChannelId = 0; nChannelId < P_7816SM_MAX_LOGICAL_CHANNEL; nChannelId++)
      {
         if (pInstance->sLogicalChannel[nChannelId].bIsOpened != W_FALSE)
         {
            PDebugError("static_PSeHalSmOpenRawChannel: The basic channel or a logical channel is already opened");
            return W_ERROR_EXCLUSIVE_REJECTED;
         }
      }

      if ((pCallback == null) && (pCallbackParameter == null))
      {
         PDebugError("static_PSeHalSmOpenChannel: pCallback == null && pCallbackParameter == null");
         return W_ERROR_BAD_PARAMETER;
      }

      if (pCallback == null)
      {
         pInstance->sRawChannel.bIsOpened = W_TRUE;
         pInstance->sRawChannel.nChannelIdentifier = P_SEHALSM_RAW_IDENTIFIER;

         *(uint32_t*)pCallbackParameter = pInstance->sRawChannel.nChannelIdentifier;
         return W_SUCCESS;
      }

      static_PSeHalSmFreeReceptionBuffer(&pInstance->sRawChannel);
      goto open_channel;
   }

   if (pInstance->sRawChannel.bIsOpened != W_FALSE)
   {
      PDebugError("static_PSeHalSmOpenChannel: The raw channel is already opened");
      return W_ERROR_EXCLUSIVE_REJECTED;
   }

   if ((nType == W_7816_CHANNEL_TYPE_BASIC)
   && (pInstance->sLogicalChannel[0].bIsOpened != W_FALSE))
   {
      PDebugError("PSeHalSmOpenLogicalChannel: The basic logical channel is already opened");
      return W_ERROR_EXCLUSIVE_REJECTED;
   }

   /* Free reception buffer (basic logical channel) */
   static_PSeHalSmFreeReceptionBuffer(&pInstance->sLogicalChannel[0]);

open_channel:

   /* Remember callback parameters */
   pInstance->current.callback.u.pSmChannelCallback = pCallback;
   pInstance->current.callback.pCallbackParameter = pCallbackParameter;

   /* Copy the AID for further use (in the SELECT [AID] command) */
   pInstance->current.nAIDLength = nAIDLength;
   if (pAID != null)
   {
      CMemoryCopy(pInstance->current.aAID, pAID, nAIDLength);
   }

   pInstance->current.pChannel = (tPSeHalSmChannel*)null;
   pInstance->current.nChannelType = nType;

   /* Set current operation */
   pInstance->current.nState = P_SEHALSM_STATE_OPEN_PENDING;

   static_PSeHalSmRawOpenChannel(pContext,
      pInstance->pSlot,
      static_PSeHalSmSendOpenLogicalChannelApduCompleted,
      pInstance,
      nType,
      pAID, nAIDLength);

   return W_ERROR_OPERATION_PENDING;
}

/**
 * @brief Checks whether a channel is associated with an instance.
 *
 * @param[in]  pInstance  The instance of the ISO-7816 State Machine.
 *
 * @param[in]  nChannelIdentifier  The identifier of the channel to be tested.
 *
 * @param[in]  bAll  false to indicate that only the supplementary logical channels
 *                are to be tested, true to indicate that the raw channel and the
 *                basic logical channel are to be tested as well.
 *
  * @result    The pointer on the channel or null if an erro is detected
 **/
static tPSeHalSmChannel* static_PSeHalSmCheckLogicalChannel(
                  tPSeHalSmInstance* pInstance,
                  uint32_t nChannelIdentifier,
                  bool_t bAll)
{
   uint32_t nChannelId;
   tPSeHalSmChannel* pChannel = (tPSeHalSmChannel*)null;

   CDebugAssert(pInstance != null);

   if (bAll != W_FALSE)
   {
      if(nChannelIdentifier == pInstance->sRawChannel.nChannelIdentifier)
      {
         pChannel = &pInstance->sRawChannel;
      }
      else if(nChannelIdentifier == pInstance->sLogicalChannel[0].nChannelIdentifier)
      {
         pChannel = &pInstance->sLogicalChannel[0];
      }
   }

   if(pChannel == null)
   {
      /* Ignore the raw channel and the basic logical channel */
      for(nChannelId = 1; nChannelId < P_7816SM_MAX_LOGICAL_CHANNEL; nChannelId++)
      {
         if (nChannelIdentifier == pInstance->sLogicalChannel[nChannelId].nChannelIdentifier)
         {
            pChannel = &pInstance->sLogicalChannel[nChannelId];
            break;
         }
      }
   }

   return pChannel;
}

/* See the definition of tP7816SmCloseChannel in header file */
static W_ERROR static_PSeHalSmCloseChannel(
                  tContext* pContext,
                  tPSeHalSmInstance* pInstance,
                  uint32_t nChannelIdentifier,
                  tPBasicGenericCallbackFunction* pCallback,
                  void* pCallbackParameter)
{
   tPSeHalSmChannel* pChannel = null;

   PDebugTrace("PSeHalSmCloseChannel");

   if (pInstance == null)
   {
      PDebugError("PSeHalSmCloseChannel: pInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pInstance->current.nState != P_SEHALSM_STATE_IDLE)
   {
      PDebugError("PSeHalSmCloseChannel: A communication is currently ongoing");
      return W_ERROR_BAD_STATE;
   }


   if (nChannelIdentifier == P_SEHALSM_NULL_IDENTIFIER)
   {
      if (pCallback == null)
      {
         PDebugError("PSeHalSmCloseChannel: pCallback == null");
         return W_ERROR_BAD_PARAMETER;
      }

      /* Remember callback parameters */
      pInstance->current.callback.u.pSmCallback = pCallback;
      pInstance->current.callback.pCallbackParameter = pCallbackParameter;

      /* Assign the error code that shall be returned by default */
      pInstance->current.nCloseResult = W_SUCCESS;

      /* Process MANAGE CHANNEL [close] commands */
      return static_PSeHalSmCloseNextLogicalChannel(pContext, pInstance);
   }

   if ((nChannelIdentifier != pInstance->sRawChannel.nChannelIdentifier)
   && (nChannelIdentifier != pInstance->sLogicalChannel[0].nChannelIdentifier)
   && (pCallback == null))
   {
      PDebugError("PSeHalSmCloseChannel: pCallback == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((pChannel = static_PSeHalSmCheckLogicalChannel(pInstance, nChannelIdentifier, W_TRUE)) == null)
   {
      PDebugError("PSeHalSmCloseChannel: This channel is not associated with this instance");
      return W_ERROR_ITEM_NOT_FOUND;
   }

   /* Remember callback parameters */
   pInstance->current.callback.u.pSmCallback = pCallback;
   pInstance->current.callback.pCallbackParameter = pCallbackParameter;

   /* Process the MANAGE CHANNEL [close] command */
   pInstance->current.nState = P_SEHALSM_STATE_CLOSE_PENDING;
   static_PSeHalSmSendCloseChannelApdu(pContext, pInstance, pChannel);

   return W_ERROR_OPERATION_PENDING;
}

/* See the definition of tP7816SmExchangeApdu in header file */
static W_ERROR static_PSeHalSmExchangeApdu(
                  tContext* pContext,
                  tPSeHalSmInstance* pInstance,
                  uint32_t nChannelReference,
                  tPBasicGenericDataCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  const uint8_t* pSendApduBuffer,
                  uint32_t nSendApduBufferLength,
                  uint8_t* pReceivedApduBuffer,
                  uint32_t nReceivedApduBufferMaxLength)
{
   tPSeHalSmChannel* pChannel = (tPSeHalSmChannel*)null;

   PDebugTrace("PSeHalSmExchangeApdu");

   if (pInstance == null)
   {
      PDebugError("PSeHalSmExchangeApdu: pInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (nChannelReference == P_7816SM_NULL_CHANNEL)
   {
      PDebugError("PSeHalSmExchangeApdu: nChannelReference == P_7816SM_NULL_CHANNEL");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pCallback == null)
   {
      PDebugError("PSeHalSmExchangeApdu: pCallback == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pSendApduBuffer == null)
   {
      PDebugError("PSeHalSmExchangeApdu: pSendApduBuffer == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (nSendApduBufferLength > sizeof(pInstance->current.aReaderToCard))
   {
      PDebugError("PSeHalSmExchangeApdu: nSendApduBufferLength is greater than %d",
         (uint32_t) sizeof(pInstance->current.aReaderToCard));
      return W_ERROR_BAD_PARAMETER;
   }

   if (pReceivedApduBuffer == null)
   {
      PDebugError("PSeHalSmExchangeApdu: pReceivedApduBuffer == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pInstance->current.nState != P_SEHALSM_STATE_IDLE)
   {
      PDebugError("PSeHalSmExchangeApdu: A communication is currently ongoing");
      return W_ERROR_BAD_STATE;
   }

   if ((pChannel = static_PSeHalSmCheckLogicalChannel(pInstance, nChannelReference, W_TRUE)) == null)
   {
      PDebugError("PSeHalSmExchangeApdu: This channel is not associated with this instance");
      return W_ERROR_ITEM_NOT_FOUND;
   }

   /* Remember callback parameters */
   pInstance->current.callback.u.pSmDataCallback = pCallback;
   pInstance->current.callback.pCallbackParameter = pCallbackParameter;

   /* Remember answer buffer */
   pInstance->current.result.pReceivedApduBuffer = pReceivedApduBuffer;
   pInstance->current.result.nReceivedApduBufferMaxLength = nReceivedApduBufferMaxLength;

   /* Filter APDU commands sent on the basic channel or a supplementary logical channel */
   if (pChannel != &pInstance->sRawChannel)
   {
      /* Note - The CLA byte is not checked */

      /* Check MANAGE CHANNEL */
      if ((nSendApduBufferLength >= 2) && (pSendApduBuffer[1] == P_7816SM_INS_MANAGE_CHANNEL))
      {
         PDebugError("PSeHalSmExchangeApdu: Forbidden to send the MANAGE CHANNEL command");
         return W_ERROR_SECURITY;
      }

      /* Check SELECT [AID] */
      if ((nSendApduBufferLength >= 3) && (pSendApduBuffer[1] == P_7816SM_INS_SELECT) && (pSendApduBuffer[2]== P_7816SM_P1_SELECT_AID))
      {
         PDebugError("PSeHalSmExchangeApdu: Forbidden to send the SELECT [AID] command");
         return W_ERROR_SECURITY;
      }
   }

   /* Free reception buffer */
   static_PSeHalSmFreeReceptionBuffer(pChannel);

   /* Copy APDU command to internal buffer */
   CMemoryCopy(pInstance->current.aReaderToCard, pSendApduBuffer, nSendApduBufferLength);

   /* Process the APDU command */
   pInstance->current.nState = P_SEHALSM_STATE_EXCHANGE_PENDING;
   static_PSeHalSmSendApdu(pInstance, pChannel, nSendApduBufferLength);

   return W_ERROR_OPERATION_PENDING;
}

static W_ERROR static_PSeHalSmGetData(
                  tContext* pContext,
                  tPSeHalSmInstance* pInstance,
                  uint32_t nChannelReference,
                  uint32_t nType,
                  uint8_t* pBuffer,
                  uint32_t nBufferMaxLength,
                  uint32_t* pnActualLength)
{
   tPSeHalSmChannel* pChannel = null;
   uint8_t* pSourceBuffer = null;
   uint32_t nActualLength = 0;

   PDebugTrace("static_PSeHalSmGetData");

   if(pnActualLength != null)
   {
      *pnActualLength = 0;
   }

   if (pInstance == null)
   {
      PDebugError("static_PSeHalSmGetData: pInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (nChannelReference == P_7816SM_NULL_CHANNEL)
   {
      PDebugError("static_PSeHalSmGetData: nChannelReference == P_7816SM_NULL_CHANNEL");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((nType != P_7816SM_DATA_TYPE_IS_RAW_CHANNEL) && (nType != P_7816SM_DATA_TYPE_IS_BASIC_CHANNEL))
   {
      if ((pBuffer == null) && (nBufferMaxLength != 0))
      {
         PDebugError("static_PSeHalSmGetData: pBuffer == null  && nBufferMaxLength != 0");
         return W_ERROR_BAD_PARAMETER;
      }

      if (pnActualLength == null)
      {
         PDebugError("static_PSeHalSmGetData: pnActualLength == null");
         return W_ERROR_BAD_PARAMETER;
      }
   }

   if ((pChannel = static_PSeHalSmCheckLogicalChannel(pInstance, nChannelReference, W_TRUE)) == null)
   {
      PDebugError("static_PSeHalSmGetData: This channel is not associated with this instance");
      return W_ERROR_ITEM_NOT_FOUND;
   }

   switch(nType)
   {
      case P_7816SM_DATA_TYPE_IS_RAW_CHANNEL:
      {
         return (pChannel == &pInstance->sRawChannel) ? W_SUCCESS : W_ERROR_BAD_STATE;
      }

      case P_7816SM_DATA_TYPE_IS_BASIC_CHANNEL:
      {
         return (pChannel == &pInstance->sLogicalChannel[0]) ? W_SUCCESS : W_ERROR_BAD_STATE;
      }

      case P_7816SM_DATA_TYPE_IS_DEFAULT_SELECTED:
      {
         return (pChannel->bIsDefaultSelected != W_FALSE) ? W_SUCCESS : W_ERROR_BAD_STATE;
      }

      case P_7816SM_DATA_TYPE_AID:
      {
         nActualLength = pChannel->nAIDLength;
         pSourceBuffer = pChannel->aAID;
         break;
      }

      case P_7816SM_DATA_TYPE_LAST_RESPONSE_APDU:
      {
         nActualLength = pChannel->nResponseIndex;
         pSourceBuffer = pChannel->pAllocatedReceivedAPDUBuffer;
         break;
      }

      default:
      {
         PDebugError("static_PSeHalSmGetData: nType is unknown");
         return W_ERROR_BAD_PARAMETER;
      }
   }

   if(pnActualLength != null)
   {
      *pnActualLength = nActualLength;
   }

   if (nActualLength != 0)
   {
      if (pBuffer == null)
      {
         CDebugAssert(nBufferMaxLength == 0);
         return W_SUCCESS;
      }
      else if (nBufferMaxLength < nActualLength)
      {
         PDebugTrace("static_PSeHalSmGetData: nBufferMaxLength is too short");
         return W_ERROR_BUFFER_TOO_SHORT;
      }
      else
      {
         CDebugAssert(pSourceBuffer != null);
         CMemoryCopy(pBuffer, pSourceBuffer, nActualLength);
      }
   }

   if(nType == P_7816SM_DATA_TYPE_LAST_RESPONSE_APDU)
   {
      static_PSeHalSmFreeReceptionBuffer(pChannel);
   }

   return W_SUCCESS;
}

/* See header file */
tP7816SmInterface g_sPSeHalSmInterface =
{
   (tP7816SmOpenChannel*)static_PSeHalSmOpenChannel,
   (tP7816SmCloseChannel*)static_PSeHalSmCloseChannel,
   (tP7816SmExchangeApdu*)static_PSeHalSmExchangeApdu,
   (tP7816SmGetData*)static_PSeHalSmGetData
};

void PSeHalSmNotifyOperationCompletion(
                  tContext* pContext,
                  uint32_t nHalSlotIdentifier,
                  uint32_t nOperation,
                  bool_t bSuccess,
                  uint32_t nParam1,
                  uint32_t nParam2)
{
   tSESlot* pSlot = PSEDriverGetSlotFromHalIdentifier(pContext, nHalSlotIdentifier);
   tPSeHalSmInstance* pSeHalSmInstance;

   if (pSlot == null)
   {
      PDebugError("PSeHalSmNotifyOperationCompletion: Invalid nHalSlotIdentifier parameter");
      return;
   }

   pSeHalSmInstance = (tPSeHalSmInstance*)pSlot->s7816Sm.pLowLevelSmInstance;
   CDebugAssert(pSeHalSmInstance != null);

   switch(nOperation)
   {
      default:
      {
         PDebugError("PSeHalSmNotifyOperationCompletion: Invalid nOperation parameter");
         return;
      }

      case C_SE_OPERATION_OPEN:
      {
         CDebugAssert(pSeHalSmInstance->current.pChannel == null);

         if (bSuccess != W_FALSE)
         {
            if (pSeHalSmInstance->current.nChannelType == W_7816_CHANNEL_TYPE_RAW)
            {
               CDebugAssert(pSeHalSmInstance->sRawChannel.bIsOpened == W_FALSE);
               pSeHalSmInstance->current.pChannel = &pSeHalSmInstance->sRawChannel;
               pSeHalSmInstance->sRawChannel.nChannelIdentifier = nParam1;
            }
            else if (pSeHalSmInstance->current.nChannelType == W_7816_CHANNEL_TYPE_BASIC)
            {
               CDebugAssert(pSeHalSmInstance->sLogicalChannel[0].bIsOpened == W_FALSE);
               pSeHalSmInstance->current.pChannel = &pSeHalSmInstance->sLogicalChannel[0];
               pSeHalSmInstance->current.pChannel->nChannelIdentifier = nParam1;
            }
            else
            {
               uint32_t nIndex;

               CDebugAssert(pSeHalSmInstance->current.nChannelType == W_7816_CHANNEL_TYPE_LOGICAL);

               /* Find an available Channel entry */
               for(nIndex = 1; nIndex < P_7816SM_MAX_LOGICAL_CHANNEL; nIndex++)
               {
                  if (pSeHalSmInstance->sLogicalChannel[nIndex].bIsOpened == W_FALSE)
                  {
                     /* This entry is free */
                     pSeHalSmInstance->current.pChannel = &pSeHalSmInstance->sLogicalChannel[nIndex];
                     pSeHalSmInstance->current.pChannel->nChannelIdentifier = nParam1;
                     break;
                  }
               }

               if (pSeHalSmInstance->current.pChannel == null)
               {
                  PDebugError("static_PSeHalSmSendOpenLogicalChannelApduCompleted: Too many channels");
                  bSuccess = W_FALSE;
                  /* @todo Close channel - for now, we fall through */
               }
            }
         }

         if (pSeHalSmInstance->current.pChannel != null)
         {
            uint32_t nActualResponseLength = 0;

            static_PSeHalSmRawGetResponseApdu(pContext,
               pSeHalSmInstance->pSlot,
               nParam1, /* nChannelIdentifier */
               pSeHalSmInstance->current.aCardToReader,
               sizeof(pSeHalSmInstance->current.aCardToReader),
               &nActualResponseLength);

            CDebugAssert(nActualResponseLength == nParam2);
         }

         /*
          * @todo - It's not possible to change bSuccess into an accurate error code!!!
          * Change into W_ERROR_ITEM_NOT_FOUND for now (to indicate that the channel could not be opened).
          * It may also be :
          * W_ERROR_UICC_COMMUNICATION to indicate that there is communication error
          * W_ERROR_FEATURE_NOT_SUPPORTED to indicate that there is not enough logical channels
          *
          * Proposal: Parameter "bool_t bSuccess" should be changed into "W_ERROR nResult".
          */
         PDFCPostContext3(
            &pSlot->sExchange.sCallbackContext,
            nParam2, /* nDataLength */
            bSuccess == W_FALSE ? W_ERROR_ITEM_NOT_FOUND : W_SUCCESS);
         return;
      }

      case C_SE_OPERATION_EXCHANGE:
      {
         CDebugAssert(pSeHalSmInstance->current.pChannel != null);
         CDebugAssert(pSeHalSmInstance->current.pChannel->nChannelIdentifier == nParam1);

         /*
          * @todo - It's not possible to change bSuccess into an accurate error code!!!
          * W_ERROR_BUFFER_TOO_SHORT cannot be indicated.
          *
          * Proposal: Parameter "bool_t bSuccess" should be changed into "W_ERROR nResult".
          *
          * In the code below, bSuccess == W_FALSE && nParam2 != 0 are used to indicate a
          * W_ERROR_BUFFER_TOO_SHORT error condition.
          */
         if ((bSuccess == W_FALSE) && (nParam2 != 0))
         {
            PDFCPostContext3(
               &pSlot->sExchange.sCallbackContext,
               nParam2, /* nDataLength */
               W_ERROR_BUFFER_TOO_SHORT);
            return;
         }

         PDFCPostContext3(
            &pSlot->sExchange.sCallbackContext,
            nParam2, /* nDataLength */
            bSuccess == W_FALSE ? W_ERROR_UICC_COMMUNICATION : W_SUCCESS);
         return;
      }

      case C_SE_OPERATION_CLOSE:
      {
         CDebugAssert(pSeHalSmInstance->current.pChannel != null);
         CDebugAssert(pSeHalSmInstance->current.pChannel->nChannelIdentifier == nParam1);

         PDFCPostContext3(
            &pSlot->sExchange.sCallbackContext,
            nParam2, /* nChannelReference */
            bSuccess == W_FALSE ? W_ERROR_UICC_COMMUNICATION : W_SUCCESS);
         return;
      }
   }
}

#endif /* (P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC) && P_INCLUDE_SE_SECURITY */
