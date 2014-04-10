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

/** @brief AID of the MF */
const uint8_t g_a7816SmMfAid[] = { P_7816SM_MF_FIRST_BYTE, 0x00 };

/** @brief Size of the AID of the MF */
const uint32_t g_n7816SmMfAidSize = sizeof(g_a7816SmMfAid);

static void static_P7816SmTraceBytes(const char* pszLabel, const uint8_t* pData, uint32_t nData)
{
#if P_TRACE_LEVEL_7816SM == P_TRACE_TRACE

   static const char sHex[]="0123456789ABCDEF";
   char sData[261*3 + 1], *psData = sData;
   uint32_t i;
   if (nData > 261) { nData = 261; }
   for(i = 0; i < nData; i++)
   {
      *psData++ = sHex[ (pData[i] >> 4) & 0x0F ];
      *psData++ = sHex[ (pData[i]     ) & 0x0F ];
      *psData++ = ' ';
   }
   *psData = '\0';

   PDebugTrace("%s = %s", pszLabel, sData);

#endif /* P_TRACE_LEVEL_7816SM == P_TRACE_TRACE */
}

/* ----------------------------------------------------------------------------

      Logical Channel Interface

   ---------------------------------------------------------------------------- */

/** The state indicating that there is no current operation */
#define P_7816SM_STATE_IDLE                 0
/** The state indicating that a MANAGE CHANNEL [open] APDU command is ongoing */
#define P_7816SM_STATE_OPEN_PENDING         1
/** The state indicating that a SELECT [AID] APDU command is ongoing */
#define P_7816SM_STATE_SELECT_AID_PENDING   2
/** The state indicating that an APDU command is ongoing */
#define P_7816SM_STATE_APDU_PENDING         3
/** The state indicating that a MANAGE CHANNEL [close] APDU command is ongoing (normal) */
#define P_7816SM_STATE_CLOSE_PENDING        4
/** The state indicating that a MANAGE CHANNEL [close] APDU command is ongoing (error condition) */
#define P_7816SM_STATE_CLOSE_ERROR_PENDING  5
/** The state indicating that MANAGE CHANNEL [close] APDU commands are ongoing */
#define P_7816SM_STATE_CLOSE_ALL_PENDING    6


typedef struct __tP7816SmChannel
{
   /** The channel identifier (0 means the basic logical channel) */
   uint32_t nChannelId;

   /** The channel reference, used to identify the channel in the API */
   uint32_t nChannelReference;

   /* The corresponding instance */
   tP7816SmInstance* pInstance;

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
} tP7816SmChannel;


struct __tP7816SmInstance
{
   /** The current context */
   tContext* pContext;

   /** The pointer to the ISO-7816 Raw Interface object */
   tP7816SmRawInterface* pRawInterface;

   /** The pointer to the ISO-7816 Raw Instance object */
   tP7816SmRawInstance* pRawInstance;

   /** The raw channel */
   tP7816SmChannel sRawChannel;

   /** The logical channels */
   tP7816SmChannel sLogicalChannel[P_7816SM_MAX_LOGICAL_CHANNEL];

   /** The next channel reference */
   uint32_t nNextChannelReference;

   /** Data associated with the current operation */
   struct
   {
      /** The current operation state */
      uint32_t nState;

      /** The error to be returned on error-processing completion */
      uint32_t nCloseResult;

      /** The reader-to-card communication buffer (Command APDU) */
      uint8_t aReaderToCard[5+256];

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

   } current;
};


/* ----------------------------------------------------------------------------
      Internal functions
   ---------------------------------------------------------------------------- */

/**
 * Computes the class byte from a source value and a channel number.
 *
 * @param[inout]  pnClass  A pointer on a value containing the original CLASS byte
 *                and set with the updated value.
 *
 * @param[in]     nChannelId  The logical channel identifier.
 *
 * @return  The error code.
 **/
static W_ERROR static_P7816SmComputeClassByte(uint8_t* pnClass, uint8_t nChannelId)
{
   uint8_t nClass = 0x00;
   uint8_t nSecureMessaging = 0x00;
   uint8_t nSourceClass = *pnClass;

   if(nSourceClass == 0x94)
   {
      /* Special case for the Calypso application */
      if(nChannelId != 0)
      {
         PDebugError("static_P7816SmComputeClassByte: CLASS byte 0x94 can only be used with the base logical channel");
         return W_ERROR_RF_COMMUNICATION;
      }

      return W_SUCCESS;
   }
   else if((nSourceClass & 0x40) == 0x00)
   {
      if(nChannelId > 19)
      {
         PDebugError("static_P7816SmComputeClassByte: Invalid channel value");
         return W_ERROR_RF_COMMUNICATION;
      }

      nSecureMessaging = (nSourceClass & 0x0C) >> 2;
   }
   else
   {
      if(nChannelId > 19)
      {
         PDebugError("static_P7816SmComputeClassByte: Invalid channel value");
         return W_ERROR_RF_COMMUNICATION;
      }

      nSecureMessaging = (nSourceClass & 0x20) >> 5;
      if(nSecureMessaging != 0)
      {
         nSecureMessaging = 2;
      }
   }

   if(nChannelId <= 3)
   {
      nClass = (nSourceClass & 0x90) | (nSecureMessaging << 2) | nChannelId;
   }
   else
   {
      if((nSecureMessaging != 0) && (nSecureMessaging != 2))
      {
         PDebugError("static_P7816SmComputeClassByte: This type of secure messaging is not supported with channel >= 4");
         return W_ERROR_RF_COMMUNICATION;
      }

      if(nSecureMessaging == 2)
      {
         nSecureMessaging = 1;
      }

      CDebugAssert(nChannelId <= 19);
      nClass = 0x40 | (nSourceClass & 0x90) | (nSecureMessaging << 5) | (nChannelId - 4);
   }

   *pnClass = nClass;
   return W_SUCCESS;
}

/**
 * @brief Frees the internal buffer used to accumulate the last command APDU response.
 *
 * @param[in]  pChannel  The channel.
 **/
static void static_P7816SmFreeReceptionBuffer(
            tP7816SmChannel* pChannel)
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
static void static_P7816SmExchangeApduCompleted(
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
static void static_P7816SmSendApdu(
                  tP7816SmInstance* pInstance,
                  tP7816SmChannel* pChannel,
                  uint32_t nReaderToCardLength)
{
   CDebugAssert(pInstance != null);
   CDebugAssert(pChannel != null);
   CDebugAssert(nReaderToCardLength <= sizeof(pInstance->current.aReaderToCard));

   if (pChannel != &pInstance->sRawChannel)
   {
      /* Set the logical channel bits */
      static_P7816SmComputeClassByte(&pInstance->current.aReaderToCard[0], (uint8_t)pChannel->nChannelId);
   }

   static_P7816SmTraceBytes("[7816-SM] C-APDU", pInstance->current.aReaderToCard,
      nReaderToCardLength);

   pInstance->pRawInterface->pExchange(
      pInstance->pContext,
      pInstance->pRawInstance,
      static_P7816SmExchangeApduCompleted,
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
static void static_P7816SmGetResponse(
                  tP7816SmInstance* pInstance,
                  tP7816SmChannel* pChannel,
                  uint8_t nLe)
{
   CDebugAssert(pInstance != null);
   CDebugAssert(pChannel != null);

   pInstance->current.aReaderToCard[0] = P_7816SM_CLA; /* CLA byte is set later */
   pInstance->current.aReaderToCard[1] = P_7816SM_INS_GET_RESPONSE;
   pInstance->current.aReaderToCard[2] = 0x00;
   pInstance->current.aReaderToCard[3] = 0x00;
   pInstance->current.aReaderToCard[4] = nLe;

   static_P7816SmSendApdu(pInstance, pChannel, 5);
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
static void static_P7816SmReissueCommand(
                  tP7816SmInstance* pInstance,
                  tP7816SmChannel* pChannel,
                  uint8_t nLe)
{
   CDebugAssert(pInstance != null);
   CDebugAssert(pChannel != null);

   /* Free reception buffer */
   static_P7816SmFreeReceptionBuffer(pChannel);

   /* The command is assumed to be an outgoing command! */
   pInstance->current.aReaderToCard[4] = nLe;

   static_P7816SmSendApdu(pInstance, pChannel, 5);
}

/**
 * @brief Opens a new supplementary logical channel.
 *
 * This function sends a MANAGE CHANNEL [open] command.
 *
 * @param[in]  pInstance  The instance of the ISO-7816 State Machine.
 *
 **/
static void static_P7816SmSendOpenLogicalChannelApdu(
                  tP7816SmInstance* pInstance)
{
   CDebugAssert(pInstance != null);

   /* Free reception buffer */
   static_P7816SmFreeReceptionBuffer(&pInstance->sLogicalChannel[0]);

   pInstance->current.aReaderToCard[0] = P_7816SM_CLA; /* CLA byte is set later */
   pInstance->current.aReaderToCard[1] = P_7816SM_INS_MANAGE_CHANNEL;
   pInstance->current.aReaderToCard[2] = P_7816SM_P1_MANAGE_CHANNEL_OPEN;
   pInstance->current.aReaderToCard[3] = 0x00; /* P2 */
   pInstance->current.aReaderToCard[4] = 0x01; /* Le */

   /* Use the basic logical channel to send the MANAGE CHANNEL command */
   static_P7816SmSendApdu(pInstance, &pInstance->sLogicalChannel[0], 5);
}

/**
 * @brief Selects an application on a logical channel.
 *
 * This function sends a SELECT [AID] command.
 *
 * @param[in]  pInstance  The instance of the ISO-7816 State Machine.
 *
 * @param[in]  pChannel  The channel to be used to send the APDU command.
 *
 * @param[in]  pAID  The AID of the application to be selected. It may be null
 *                to indicate that the default application must be selected
 *                (In this case, nAIDLength must be 0).
 *
 * @param[in]  nAIDLength  The length of the AID, in range 5-16 (or 0 if pAID is null).
 **/
static void static_P7816SmSelectApplication(
                  tP7816SmInstance* pInstance,
                  tP7816SmChannel* pChannel,
                  const uint8_t* pAID,
                  uint32_t nAIDLength)
{
   CDebugAssert(pInstance != null);
   CDebugAssert(pChannel != null);
   CDebugAssert(((pAID == null) && (nAIDLength == 0)) ||
                ((pAID != null) && ((nAIDLength == 2) || ((nAIDLength >= P_7816SM_MIN_AID_LENGTH) && (nAIDLength <= P_7816SM_MAX_AID_LENGTH)))));

   /* Free reception buffer */
   static_P7816SmFreeReceptionBuffer(pChannel);

   if (nAIDLength == 2)
   {
      pInstance->current.aReaderToCard[0] = P_7816SM_CLA; /* CLA byte is set later */
      pInstance->current.aReaderToCard[1] = P_7816SM_INS_SELECT;
      pInstance->current.aReaderToCard[2] = P_7816SM_P1_SELECT_FILE; /* P1 */
      pInstance->current.aReaderToCard[3] = P_7816SM_P2_SELECT_FILE_WITH_FCP; /* P2 - Return FCP */
      pInstance->current.aReaderToCard[4] = 0x02; /* Lc */
      pInstance->current.aReaderToCard[5] = pAID[0];
      pInstance->current.aReaderToCard[6] = pAID[1];

      static_P7816SmSendApdu(pInstance, pChannel, 7);
      return;
   }

   pInstance->current.aReaderToCard[0] = P_7816SM_CLA; /* CLA byte is set later */
   pInstance->current.aReaderToCard[1] = P_7816SM_INS_SELECT;
   pInstance->current.aReaderToCard[2] = P_7816SM_P1_SELECT_AID; /* P1 */
   pInstance->current.aReaderToCard[3] = P_7816SM_P2_SELECT_AID; /* P2 */
   pInstance->current.aReaderToCard[4] = (uint8_t)nAIDLength; /* Lc */
   if (pAID != null)
   {
      CMemoryCopy(&pInstance->current.aReaderToCard[5], pAID, nAIDLength);

      pInstance->current.aReaderToCard[5 + nAIDLength] = 0x00; /* Le = 0 for EMVCo compliance */
      static_P7816SmSendApdu(pInstance, pChannel, 5 + nAIDLength + 1);
   }
   else
   {
      static_P7816SmSendApdu(pInstance, pChannel, 5);
   }
}

/**
 * Extracts the AID from the FCI returned by the SELECT [AID] command.
 *
 * The ISO/GP SE returns the following answer to SELECT.
 * 6F L              FCI template
 *    84 L  xx-xx    Application AID
 *    A5 L           Proprietary data
 **/
static W_ERROR static_P7816SmExtractAidFromFci(
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
      PDebugWarning("static_P7816SmExtractAidFromFci: Bad TLV format");
      return nError;
   }

   if(PAsn1GetTagValue(&sParser) != 0x6F)
   {
      PDebugTrace("static_P7816SmExtractAidFromFci: Tag '6F' is not found");
      return W_ERROR_BAD_PARAMETER;
   }

   nError = PAsn1ParseIncludedTlv( &sParser, &sIncludedParser);
   if (nError != W_SUCCESS)
   {
      PDebugWarning("static_P7816SmExtractAidFromFci: Bad included TLV format");
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

   PDebugWarning("static_P7816SmExtractAidFromFci: The AID tag '84' is not found");
   return W_ERROR_BAD_PARAMETER;
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
static void static_P7816SmSendCloseChannelApdu(
                  tP7816SmInstance* pInstance,
                  tP7816SmChannel* pChannel)
{
   CDebugAssert(pInstance != null);
   CDebugAssert(pChannel != null);

   /* Free reception buffer */
   static_P7816SmFreeReceptionBuffer(pChannel);

   pInstance->current.aReaderToCard[0] = P_7816SM_CLA; /* CLA byte is set later */
   pInstance->current.aReaderToCard[1] = P_7816SM_INS_MANAGE_CHANNEL;
   pInstance->current.aReaderToCard[2] = P_7816SM_P1_MANAGE_CHANNEL_CLOSE;
   pInstance->current.aReaderToCard[3] = (uint8_t)pChannel->nChannelId; /* P2 */

   static_P7816SmSendApdu(pInstance, pChannel, 4);
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
static W_ERROR static_P7816SmCloseNextLogicalChannel(
                  tP7816SmInstance* pInstance)
{
   uint32_t nChannelId;

   CDebugAssert(pInstance->current.nState == P_7816SM_STATE_IDLE);

   for(nChannelId = P_7816SM_MAX_LOGICAL_CHANNEL - 1; nChannelId >= 1; nChannelId--)
   {
      if (pInstance->sLogicalChannel[nChannelId].nChannelReference != P_7816SM_NULL_CHANNEL)
      {
         /* Force closed state (in case something wrong occurs later, to prevent endless loop) */
         pInstance->sLogicalChannel[nChannelId].nChannelReference = P_7816SM_NULL_CHANNEL;

         pInstance->current.nState = P_7816SM_STATE_CLOSE_ALL_PENDING;
         static_P7816SmSendCloseChannelApdu(pInstance, &pInstance->sLogicalChannel[nChannelId]);

         return W_ERROR_OPERATION_PENDING;
      }
   }

   /* Force closing of the raw channel and the basic logical channel */
   pInstance->sRawChannel.nChannelReference = P_7816SM_NULL_CHANNEL;
   pInstance->sLogicalChannel[0].nChannelReference = P_7816SM_NULL_CHANNEL;

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
static W_ERROR static_P7816SmSaveResponseApdu(
            tP7816SmChannel* pChannel,
            const uint8_t* pCardToReaderBuffer,
            uint32_t nDataLength,
            uint32_t nExtraDataLength)
{
   uint8_t* pNewBuffer;
   uint32_t nNewLength;

   CDebugAssert(pChannel != null);
   CDebugAssert(pCardToReaderBuffer != null);

   if (nDataLength == 0)
   {
      return W_SUCCESS;
   }

   if ((pChannel->nResponseIndex + nDataLength) > pChannel->nAllocatedReceivedAPDUBufferMaxLength)
   {
      nNewLength = pChannel->nResponseIndex + nDataLength + nExtraDataLength;
      pNewBuffer = (uint8_t*)CMemoryAlloc(nNewLength);
      if (pNewBuffer == null)
      {
         PDebugError("static_P7816SmSaveResponseApdu: Failed to allocate %d bytes", nNewLength);
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
   CMemoryCopy( pChannel->pAllocatedReceivedAPDUBuffer + pChannel->nResponseIndex, pCardToReaderBuffer, nDataLength );
   pChannel->nResponseIndex += nDataLength;

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
 *             - W_ERROR_RF_COMMUNICATION to indicate a communication error with
 *                  the ISO-7816 card. In this case, nDataLength is 0.
 **/
static void static_P7816SmExchangeApduCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  uint32_t nDataLength,
                  W_ERROR nResult )
{
   tP7816SmChannel* pChannel = (tP7816SmChannel*)pCallbackParameter;
   tP7816SmInstance* pInstance = pChannel->pInstance;

   const uint8_t* pCardToReader = pInstance->current.aCardToReader;

   CDebugAssert(pInstance != null);
   CDebugAssert(pChannel != null);

   if (nResult == W_SUCCESS)
   {
      static_P7816SmTraceBytes("[7816-SM] R-APDU", pInstance->current.aCardToReader,
         nDataLength);
   }
   else
   {
      PDebugTrace("[7816-SM] R-APDU = error 0x%02X (%s)", nResult, PUtilTraceError(nResult));
   }

   /* Check the error code return */
   if (nResult == W_SUCCESS) for(;;)
   {
      /* The response APDU must at least contain a 2-byte status word */
      if (nDataLength < 2)
      {
         nResult = W_ERROR_RF_COMMUNICATION;
         break;
      }

      /* Process 61xx status words: Retrieve data and send a GET RESPONSE command */
      if (pCardToReader[nDataLength - 2] == P_7816SM_SW1_61)
      {
         nResult = static_P7816SmSaveResponseApdu(pChannel, pCardToReader, nDataLength - 2, pCardToReader[nDataLength - 1] + 2);
         if (nResult != W_SUCCESS)
         {
            break;
         }

         static_P7816SmGetResponse(pInstance, pChannel, pCardToReader[nDataLength - 1]);
         return;
      }

      /* Process 6Cxx status words (T=0): Reissue the same command with the right P3=Le */
      if (pCardToReader[nDataLength - 2] == P_7816SM_SW1_6C)
      {
         static_P7816SmReissueCommand(pInstance, pChannel, pCardToReader[nDataLength - 1]);
         return;
      }

      break;
   }

   switch(pInstance->current.nState)
   {
      case P_7816SM_STATE_OPEN_PENDING:
      {
         /* This is the answer to the MANAGE CHANNEL [open] command */

         if (nResult == W_SUCCESS) for(;;)
         {
            uint32_t nChannelId;

            if ((nDataLength == 2) && (pCardToReader[0] == 0x68) && (pCardToReader[1] == 0x81))
            {
               PDebugError("static_P7816SmExchangeApduCompleted: MANAGE CHANNEL [open] returned 0x6881 (logical channel not supported)");
               nResult = W_ERROR_FEATURE_NOT_SUPPORTED;
               break;
            }

            if ((nDataLength == 2) && (pCardToReader[0] == 0x6A) && (pCardToReader[1] == 0x81))
            {
               PDebugError("static_P7816SmExchangeApduCompleted: MANAGE CHANNEL [open] returned 0x6A81 (function not supported)");
               nResult = W_ERROR_ITEM_NOT_FOUND;
               break;
            }

            /* The answer must contain <channelid>, 0x90, 0x00 */
            if ((nDataLength != 3) || (pCardToReader[nDataLength - 2] != 0x90) || (pCardToReader[nDataLength - 1] != 0x00))
            {
               PDebugError("static_P7816SmExchangeApduCompleted: MANAGE CHANNEL [open] returned a bad response (length=%d, SW=0x%02X%02X)",
                  nDataLength, pCardToReader[nDataLength - 2], pCardToReader[nDataLength - 1]);
               nResult = W_ERROR_RF_COMMUNICATION;
               break;
            }

            /* Extract the <channelid> from the response */
            nChannelId = pCardToReader[0];

            /* The answer must contain a valid <channelid> */
            if ((nChannelId ==0) || (nChannelId >= P_7816SM_MAX_LOGICAL_CHANNEL))
            {
               PDebugError("static_P7816SmExchangeApduCompleted: MANAGE CHANNEL [open] returned a bad channel id (%d)", nChannelId);
               nResult = W_ERROR_RF_COMMUNICATION;
               break;
            }

            PDebugTrace("static_P7816SmExchangeApduCompleted: Channel %d opened. Trying to select application...", nChannelId);

            /* Select the application on the newly opened logical channel */
            pInstance->current.nState = P_7816SM_STATE_SELECT_AID_PENDING;
            static_P7816SmSelectApplication(pInstance, &pInstance->sLogicalChannel[nChannelId],
                ((pInstance->current.nAIDLength == 0) ? (uint8_t*)null : pInstance->current.aAID),
                pInstance->current.nAIDLength);

            return;
         }

         pInstance->current.nState = P_7816SM_STATE_IDLE;

         /* Notify caller of error */
         pInstance->current.callback.u.pSmChannelCallback(
            pContext,
            pInstance->current.callback.pCallbackParameter,
            P_7816SM_NULL_CHANNEL, nResult);

         return;
      }

      case P_7816SM_STATE_SELECT_AID_PENDING:
      {
         /* This is the answer to the SELECT [AID] command */

         if (nResult == W_SUCCESS)
         {
            /* The answer must end with status word '9000', or '62xx' or '63xx' */
            if (((pCardToReader[nDataLength - 2] == 0x90) && (pCardToReader[nDataLength - 1] == 0x00))
               ||(pCardToReader[nDataLength - 2] == 0x62)
               ||(pCardToReader[nDataLength - 2] == 0x63))
            {
               uint32_t nAIDLength;
               const uint8_t* pAID;

               PDebugTrace("static_P7816SmExchangeApduCompleted: Application selected on channel %d", pChannel->nChannelId);

               /* Copy answer to extensible internal buffer - ignore error */
               nResult = static_P7816SmSaveResponseApdu(pChannel, pCardToReader, nDataLength, 0);

               /* The channel is now considered as opened */
               pChannel->nChannelReference = pInstance->nNextChannelReference++;

               /* Remember the AID of the currently selected application */
               pChannel->nAIDLength = 0;
               CMemoryFill(pChannel->aAID, 0x00, sizeof(pChannel->aAID));

               /* If not the MF, first try to extract the AID from the returned FCI (if any) */
               if ((pInstance->current.nAIDLength != 2) && (nDataLength > 2) && (static_P7816SmExtractAidFromFci(pCardToReader, nDataLength - 2, &pAID, &nAIDLength) == W_SUCCESS))
               {
                  if (nAIDLength > P_7816SM_MAX_AID_LENGTH)
                  {
                     PDebugWarning("static_P7816SmExchangeApduCompleted: The AID extracted from the FCI is too long (%d), truncated", nAIDLength);
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
                  PDebugWarning("static_P7816SmExchangeApduCompleted: The AID of the selected application is unknown");
               }
               else
               {
                  static_P7816SmTraceBytes("[7816-SM] Selected application AID", pAID, nAIDLength);
               }

               pChannel->bIsDefaultSelected = (pInstance->current.nAIDLength != 0) ? W_FALSE : W_TRUE;

               CMemoryCopy(pChannel->aAID, pAID, nAIDLength);
               pChannel->nAIDLength = nAIDLength;

               pInstance->current.nState = P_7816SM_STATE_IDLE;

               /* Notify caller of successful completion */
               pInstance->current.callback.u.pSmChannelCallback(
                  pContext,
                  pInstance->current.callback.pCallbackParameter,
                  pChannel->nChannelReference, W_SUCCESS);

               return;
            }

            nResult = W_ERROR_RF_COMMUNICATION;
            /* FALL THROUGH */
         }

         PDebugTrace("static_P7816SmExchangeApduCompleted: failed to select application on channel %d", pChannel->nChannelId);

         if (pChannel->nChannelId == 0)
         {
            /* The application was selected on the basic logical channel */

            /* The channel is now considered as closed */
            pChannel->nChannelReference = P_7816SM_NULL_CHANNEL;

            /* Note: There is no need to close the basic logical channel with MANAGE CHANNEL */
            pInstance->current.nState = P_7816SM_STATE_IDLE;

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
            pInstance->current.nState = P_7816SM_STATE_CLOSE_ERROR_PENDING;
            pInstance->current.nCloseResult = W_ERROR_ITEM_NOT_FOUND;
            static_P7816SmSendCloseChannelApdu(pInstance, pChannel);
            return;
         }
      }

      case P_7816SM_STATE_APDU_PENDING:
      {
         /* This is the answer to an APDU command */

         if (nResult == W_SUCCESS)
         {
            /* Copy answer to extensible internal buffer - ignore error */
            nResult = static_P7816SmSaveResponseApdu(pChannel, pCardToReader, nDataLength, 0);

            nDataLength = pChannel->nResponseIndex;

            if (pChannel->nResponseIndex <= pInstance->current.result.nReceivedApduBufferMaxLength)
            {
               CDebugAssert(pChannel->pAllocatedReceivedAPDUBuffer != null);

               CMemoryCopy(pInstance->current.result.pReceivedApduBuffer,
                  pChannel->pAllocatedReceivedAPDUBuffer,
                  pChannel->nResponseIndex);

               static_P7816SmFreeReceptionBuffer(pChannel);

               nResult = W_SUCCESS;
            }
            else
            {
               PDebugTrace("static_P7816SmExchangeApduCompleted: APDU buffer is too short");

               nResult = W_ERROR_BUFFER_TOO_SHORT;
            }
         }
         else
         {
            nDataLength = 0;
         }

         pInstance->current.nState = P_7816SM_STATE_IDLE;

         /* Notify caller of completion */
         pInstance->current.callback.u.pSmDataCallback(
            pContext,
            pInstance->current.callback.pCallbackParameter,
            nDataLength, nResult);

         return;
      }

      case P_7816SM_STATE_CLOSE_PENDING:
      {
         /* This is the answer to the MANAGE CHANNEL [close] command (normal processing) */

         if (nResult == W_SUCCESS)
         {
            /* The answer must be 0x90, 0x00 */
            if ((nDataLength != 2) || (pCardToReader[0] != 0x90) || (pCardToReader[1] != 0x00))
            {
               PDebugError("static_P7816SmExchangeApduCompleted: MANAGE CHANNEL [close] returned a bad response (length=%d, SW=0x%02X%02X)",
                  nDataLength, pCardToReader[nDataLength - 2], pCardToReader[nDataLength - 1]);

               nResult = W_ERROR_RF_COMMUNICATION;
            }
         }

         PDebugTrace("static_P7816SmExchangeApduCompleted: Channel %d closed", pChannel->nChannelId);

         /* The channel is now considered as closed (whatever result) */
         pChannel->nChannelReference = P_7816SM_NULL_CHANNEL;

         pInstance->current.nState = P_7816SM_STATE_IDLE;

         /* Notify caller of completion */
         pInstance->current.callback.u.pSmChannelCallback(
            pContext,
            pInstance->current.callback.pCallbackParameter,
            P_7816SM_NULL_CHANNEL,
            nResult);

         return;
      }

      case P_7816SM_STATE_CLOSE_ERROR_PENDING:
      {
         /* This is the answer to the MANAGE CHANNEL [close] command (error-condition processing) */

         if (nResult == W_SUCCESS)
         {
            /* The answer must be 0x90, 0x00 */
            if ((nDataLength != 2) || (pCardToReader[0] != 0x90) || (pCardToReader[1] != 0x00))
            {
               PDebugError("static_P7816SmExchangeApduCompleted: MANAGE CHANNEL [close] returned a bad response (length=%d, SW=0x%02X%02X)",
                  nDataLength, pCardToReader[nDataLength - 2], pCardToReader[nDataLength - 1]);

               nResult = W_ERROR_RF_COMMUNICATION;
            }
         }

         PDebugTrace("static_P7816SmExchangeApduCompleted: Channel %d closed", pChannel->nChannelId);

         /* The channel is now considered as closed (whatever result) */
         pChannel->nChannelReference = P_7816SM_NULL_CHANNEL;

         pInstance->current.nState = P_7816SM_STATE_IDLE;

         /* Notify caller of error */
         pInstance->current.callback.u.pSmChannelCallback(
            pContext,
            pInstance->current.callback.pCallbackParameter,
            P_7816SM_NULL_CHANNEL,
            pInstance->current.nCloseResult);

         return;
      }

      case P_7816SM_STATE_CLOSE_ALL_PENDING:
      {
         /* This is the answer to the MANAGE CHANNEL [close] command (normal close-all processing) */

         if (nResult == W_SUCCESS)
         {
            /* The answer must be 0x90, 0x00 */
            if ((nDataLength != 2) || (pCardToReader[0] != 0x90) || (pCardToReader[1] != 0x00))
            {
               PDebugError("static_P7816SmExchangeApduCompleted: MANAGE CHANNEL [close] returned a bad response (length=%d, SW=0x%02X%02X)",
                  nDataLength, pCardToReader[nDataLength - 2], pCardToReader[nDataLength - 1]);

               nResult = W_ERROR_RF_COMMUNICATION;
            }
         }

         /* Remember the first error code that occurred */
         if ((nResult != W_SUCCESS) && (pInstance->current.nCloseResult == W_SUCCESS))
         {
            pInstance->current.nCloseResult = nResult;
         }

         PDebugTrace("static_P7816SmExchangeApduCompleted: Channel %d closed", pChannel->nChannelId);

         /* The channel is now considered as closed (whatever result) */
         pChannel->nChannelReference = P_7816SM_NULL_CHANNEL;

         pInstance->current.nState = P_7816SM_STATE_IDLE;

         if (static_P7816SmCloseNextLogicalChannel(pInstance) == W_SUCCESS)
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
W_ERROR P7816SmCreateInstance(
                  tContext* pContext,
                  tP7816SmRawInterface* pRawInterface,
                  tP7816SmRawInstance* pRawInstance,
                  tP7816SmInstance** ppInstance)
{
   tP7816SmInstance* pInstance;
   uint32_t nChannelId;

   PDebugTrace("P7816SmCreateInstance");

   if (pRawInterface == null)
   {
      PDebugError("P7816SmCreateInstance: pRawInterface == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pRawInterface->pExchange == null)
   {
      PDebugError("P7816SmCreateInstance: pRawInterface->pExchange == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pRawInstance == null)
   {
      PDebugError("P7816SmCreateInstance: pRawInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (ppInstance == null)
   {
      PDebugError("P7816SmCreateInstance: ppInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   pInstance = (tP7816SmInstance*)CMemoryAlloc(sizeof(tP7816SmInstance));
   if ( pInstance == null )
   {
      PDebugError("P7816SmCreateInstance: Failed to allocate tP7816SmInstance");
      return W_ERROR_OUT_OF_RESOURCE;
   }
   CMemoryFill(pInstance, 0, sizeof(*pInstance));

   pInstance->pRawInstance = pRawInstance;
   pInstance->pRawInterface = pRawInterface;
   pInstance->pContext = pContext;

   /* Initialize the raw channel */
   pInstance->sRawChannel.nChannelReference = P_7816SM_NULL_CHANNEL;
   pInstance->sRawChannel.nChannelId = 0; /* not used */
   pInstance->sRawChannel.pInstance = pInstance;

   /* Initialize logical channels */
   for(nChannelId = 0; nChannelId < P_7816SM_MAX_LOGICAL_CHANNEL; nChannelId++)
   {
      pInstance->sLogicalChannel[nChannelId].nChannelReference = P_7816SM_NULL_CHANNEL;
      pInstance->sLogicalChannel[nChannelId].nChannelId = nChannelId;
      pInstance->sLogicalChannel[nChannelId].pInstance = pInstance;
   }

   pInstance->nNextChannelReference = P_7816SM_NULL_CHANNEL + 1;

   *ppInstance = pInstance;

   return W_SUCCESS;
}

/* See Header file */
void P7816SmDestroyInstance(
                  tContext* pContext,
                  tP7816SmInstance* pInstance)
{
   uint32_t nChannelId;

   PDebugTrace("P7816SmDestroyInstance");

   if (pInstance == null)
   {
      PDebugError("P7816SmDestroyInstance: pInstance == null");
      return;
   }

   if (pInstance->sRawChannel.nChannelReference != P_7816SM_NULL_CHANNEL)
   {
      static_P7816SmFreeReceptionBuffer(&pInstance->sRawChannel);

      PDebugWarning("P7816SmDestroyInstance: Raw channel has not been closed");
   }

   for(nChannelId = 0; nChannelId < P_7816SM_MAX_LOGICAL_CHANNEL; nChannelId++)
   {
      if (pInstance->sLogicalChannel[nChannelId].nChannelReference != P_7816SM_NULL_CHANNEL)
      {
         static_P7816SmFreeReceptionBuffer(&pInstance->sLogicalChannel[nChannelId]);

         PDebugWarning("P7816SmDestroyInstance: Logical channel %d has not been closed", nChannelId);
      }
   }

   CMemoryFree(pInstance);
}

/* See the definition of tP7816SmOpenLogicalChannel in header file */
static W_ERROR static_P7816SmOpenChannel(
                  tContext* pContext,
                  tP7816SmInstance* pInstance,
                  tPBasicGenericDataCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  uint32_t nType,
                  const uint8_t* pAID,
                  uint32_t nAIDLength)
{
   PDebugTrace("static_P7816SmOpenChannel (type=%u)", nType);

   if (pInstance == null)
   {
      PDebugError("static_P7816SmOpenChannel: pInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((pCallback == null) && (nType != W_7816_CHANNEL_TYPE_RAW))
   {
      PDebugError("static_P7816SmOpenChannel: pCallback == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((nType != W_7816_CHANNEL_TYPE_RAW) && (nType != W_7816_CHANNEL_TYPE_BASIC) && (nType != W_7816_CHANNEL_TYPE_LOGICAL))
   {
      PDebugError("static_P7816SmOpenChannel: nType (%u) is incorrect", nType);
      return W_ERROR_BAD_PARAMETER;
   }

   if ( ((pAID == null) && (nAIDLength != 0)))
   {
      PDebugError("static_P7816SmOpenChannel: pAID == null && nAIDLength != 0");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pAID != null)
   {
      if (nAIDLength == sizeof(g_a7816SmMfAid))
      {
         if (CMemoryCompare(pAID, g_a7816SmMfAid, nAIDLength) != 0)
         {
            PDebugError("static_P7816SmOpenChannel: pAID != null && nAIDLength == 2 but *pAID != 0x3F00");
            return W_ERROR_BAD_PARAMETER;
         }
      }
      else if ((nAIDLength < P_7816SM_MIN_AID_LENGTH) || (nAIDLength > P_7816SM_MAX_AID_LENGTH))
      {
         PDebugError("static_P7816SmOpenChannel: pAID != null && nAIDLength not in range 5..16");
         return W_ERROR_BAD_PARAMETER;
      }
   }

   if (pInstance->current.nState != P_7816SM_STATE_IDLE)
   {
      PDebugError("static_P7816SmOpenChannel: A communication is currently ongoing");
      return W_ERROR_BAD_STATE;
   }

   if (nType == W_7816_CHANNEL_TYPE_RAW)
   {
      uint32_t nChannelId;

      if (pInstance->sRawChannel.nChannelReference != P_7816SM_NULL_CHANNEL)
      {
         PDebugError("static_P7816SmOpenChannel: The raw channel is already opened");
         return W_ERROR_EXCLUSIVE_REJECTED;
      }

      for(nChannelId = 0; nChannelId < P_7816SM_MAX_LOGICAL_CHANNEL; nChannelId++)
      {
         if (pInstance->sLogicalChannel[nChannelId].nChannelReference != P_7816SM_NULL_CHANNEL)
         {
            PDebugError("static_P7816SmOpenChannel: The basic channel or a logical channel is already opened");
            return W_ERROR_EXCLUSIVE_REJECTED;
         }
      }

      if ((pCallback == null) && (pCallbackParameter == null))
      {
         PDebugError("static_P7816SmOpenChannel: pCallback == null && pCallbackParameter == null");
         return W_ERROR_BAD_PARAMETER;
      }

      pInstance->sRawChannel.nChannelReference = pInstance->nNextChannelReference++;

      /* Free reception buffer */
      static_P7816SmFreeReceptionBuffer(&pInstance->sRawChannel);

      if (pCallback != null)
      {
         /* Notify the opening of the raw channel */
         pCallback(pContext, pCallbackParameter, pInstance->sRawChannel.nChannelReference, W_SUCCESS);
         return W_ERROR_OPERATION_PENDING;
      }
      else
      {
         /* Return the reference of the raw channel */
         *(uint32_t*)pCallbackParameter = pInstance->sRawChannel.nChannelReference;
         return W_SUCCESS;
      }
   }

   if (pInstance->sRawChannel.nChannelReference != P_7816SM_NULL_CHANNEL)
   {
      PDebugError("static_P7816SmOpenChannel: The raw channel is already opened");
      return W_ERROR_EXCLUSIVE_REJECTED;
   }

   if ((nType == W_7816_CHANNEL_TYPE_BASIC)
   && (pInstance->sLogicalChannel[0].nChannelReference != P_7816SM_NULL_CHANNEL))
   {
      PDebugError("static_P7816SmOpenChannel: The basic logical channel is already opened");
      return W_ERROR_EXCLUSIVE_REJECTED;
   }

   /* Remember callback parameters */
   pInstance->current.callback.u.pSmChannelCallback = pCallback;
   pInstance->current.callback.pCallbackParameter = pCallbackParameter;

   /* Copy the AID for further use (in the SELECT [AID] command) */
   pInstance->current.nAIDLength = nAIDLength;
   if (pAID != null)
   {
      CMemoryCopy(pInstance->current.aAID, pAID, nAIDLength);
   }

   if (nType == W_7816_CHANNEL_TYPE_BASIC)
   {
      /* Process the SELECT APDU command on the basic logical channel */
      pInstance->current.nState = P_7816SM_STATE_SELECT_AID_PENDING;

      pInstance->sLogicalChannel[0].nChannelReference = pInstance->nNextChannelReference++;
      static_P7816SmSelectApplication(pInstance, &pInstance->sLogicalChannel[0], pAID, nAIDLength);
   }
   else
   {
      /* Process the MANAGE CHANNEL [open] and SELECT [AID] APDU commands */
      pInstance->current.nState = P_7816SM_STATE_OPEN_PENDING;
      static_P7816SmSendOpenLogicalChannelApdu(pInstance);
   }

   return W_ERROR_OPERATION_PENDING;
}

/**
 * @brief Checks whether a channel is associated with an instance.
 *
 * @param[in]  pInstance  The instance of the ISO-7816 State Machine.
 *
 * @param[in]  pChannel  The channel to be tested.
 *
 * @param[in]  bAll  false to indicate that only the supplementary logical channels
 *                are to be tested, true to indicate that the raw channel and the
 *                basic logical channel are to be tested as well.
 *
  * @result    The pointer on the channel or null if an erro is detected
 **/
static tP7816SmChannel* static_P7816SmCheckLogicalChannel(
                  tP7816SmInstance* pInstance,
                  uint32_t nChannelReference,
                  bool_t bAll)
{
   uint32_t nChannelId;
   tP7816SmChannel* pChannel = (tP7816SmChannel*)null;

   CDebugAssert(pInstance != null);
   if(nChannelReference != P_7816SM_NULL_CHANNEL)
   {
      if (bAll != W_FALSE)
      {
         if(nChannelReference == pInstance->sRawChannel.nChannelReference)
         {
            pChannel = &pInstance->sRawChannel;
         }
         else if(nChannelReference == pInstance->sLogicalChannel[0].nChannelReference)
         {
            pChannel = &pInstance->sLogicalChannel[0];
         }
      }

      if(pChannel == null)
      {
         /* Ignore the raw channel and the basic logical channel */
         for(nChannelId = 1; nChannelId < P_7816SM_MAX_LOGICAL_CHANNEL; nChannelId++)
         {
            if (nChannelReference == pInstance->sLogicalChannel[nChannelId].nChannelReference)
            {
               pChannel = &pInstance->sLogicalChannel[nChannelId];
               break;
            }
         }
      }
   }

   return pChannel;
}

/* See the definition of tP7816SmCloseChannel in header file */
static W_ERROR static_P7816SmCloseChannel(
                  tContext* pContext,
                  tP7816SmInstance* pInstance,
                  uint32_t nChannelReference,
                  tPBasicGenericCallbackFunction* pCallback,
                  void* pCallbackParameter)
{
   tP7816SmChannel* pChannel = null;

   PDebugTrace("P7816SmCloseChannel");

   if (pInstance == null)
   {
      PDebugError("P7816SmCloseChannel: pInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pInstance->current.nState != P_7816SM_STATE_IDLE)
   {
      PDebugError("P7816SmCloseChannel: A communication is currently ongoing");
      return W_ERROR_BAD_STATE;
   }


   if (nChannelReference == P_7816SM_NULL_CHANNEL)
   {
      if (pCallback == null)
      {
         PDebugError("P7816SmCloseChannel: pCallback == null");
         return W_ERROR_BAD_PARAMETER;
      }

      /* Remember callback parameters */
      pInstance->current.callback.u.pSmCallback = pCallback;
      pInstance->current.callback.pCallbackParameter = pCallbackParameter;

      /* Assign the error code that shall be returned by default */
      pInstance->current.nCloseResult = W_SUCCESS;

      /* Process MANAGE CHANNEL [close] commands */
      return static_P7816SmCloseNextLogicalChannel(pInstance);
   }

   if ((nChannelReference != pInstance->sRawChannel.nChannelReference)
   && (nChannelReference != pInstance->sLogicalChannel[0].nChannelReference)
   && (pCallback == null))
   {
      PDebugError("P7816SmCloseChannel: pCallback == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((pChannel = static_P7816SmCheckLogicalChannel(pInstance, nChannelReference, W_TRUE)) == null)
   {
      PDebugError("P7816SmCloseChannel: This channel is not associated with this instance");
      return W_ERROR_ITEM_NOT_FOUND;
   }

   if ((pChannel == &pInstance->sRawChannel) || (pChannel == &pInstance->sLogicalChannel[0]))
   {
      pChannel->nChannelReference = P_7816SM_NULL_CHANNEL;
      return W_SUCCESS;
   }

   /* Remember callback parameters */
   pInstance->current.callback.u.pSmCallback = pCallback;
   pInstance->current.callback.pCallbackParameter = pCallbackParameter;

   /* Process the MANAGE CHANNEL [close] command */
   pInstance->current.nState = P_7816SM_STATE_CLOSE_PENDING;
   static_P7816SmSendCloseChannelApdu(pInstance, pChannel);

   return W_ERROR_OPERATION_PENDING;
}

/* See the definition of tP7816SmExchangeApdu in header file */
static W_ERROR static_P7816SmExchangeApdu(
                  tContext* pContext,
                  tP7816SmInstance* pInstance,
                  uint32_t nChannelReference,
                  tPBasicGenericDataCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  const uint8_t* pSendApduBuffer,
                  uint32_t nSendApduBufferLength,
                  uint8_t* pReceivedApduBuffer,
                  uint32_t nReceivedApduBufferMaxLength)
{
   tP7816SmChannel* pChannel = (tP7816SmChannel*)null;

   PDebugTrace("P7816SmExchangeApdu");

   if (pInstance == null)
   {
      PDebugError("P7816SmExchangeApdu: pInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (nChannelReference == P_7816SM_NULL_CHANNEL)
   {
      PDebugError("P7816SmExchangeApdu: nChannelReference == P_7816SM_NULL_CHANNEL");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pCallback == null)
   {
      PDebugError("P7816SmExchangeApdu: pCallback == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pSendApduBuffer == null)
   {
      PDebugError("P7816SmExchangeApdu: pSendApduBuffer == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (nSendApduBufferLength > sizeof(pInstance->current.aReaderToCard))
   {
      PDebugError("P7816SmExchangeApdu: nSendApduBufferLength is greater than %d",
         (uint32_t) sizeof(pInstance->current.aReaderToCard));
      return W_ERROR_BAD_PARAMETER;
   }

   if (pReceivedApduBuffer == null)
   {
      PDebugError("P7816SmExchangeApdu: pReceivedApduBuffer == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pInstance->current.nState != P_7816SM_STATE_IDLE)
   {
      PDebugError("P7816SmExchangeApdu: A communication is currently ongoing");
      return W_ERROR_BAD_STATE;
   }

   if ((pChannel = static_P7816SmCheckLogicalChannel(pInstance, nChannelReference, W_TRUE)) == null)
   {
      PDebugError("P7816SmExchangeApdu: This channel is not associated with this instance");
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
         PDebugError("P7816SmExchangeApdu: Forbidden to send the MANAGE CHANNEL command");
         return W_ERROR_SECURITY;
      }

      /* Check SELECT [AID] */
      if ((nSendApduBufferLength >= 3) && (pSendApduBuffer[1] == P_7816SM_INS_SELECT) && (pSendApduBuffer[2]== P_7816SM_P1_SELECT_AID))
      {
         PDebugError("P7816SmExchangeApdu: Forbidden to send the SELECT [AID] command");
         return W_ERROR_SECURITY;
      }
   }

   /* Free reception buffer */
   static_P7816SmFreeReceptionBuffer(pChannel);

   /* Copy APDU command to internal buffer */
   CMemoryCopy(pInstance->current.aReaderToCard, pSendApduBuffer, nSendApduBufferLength);

   /* Process the APDU command */
   pInstance->current.nState = P_7816SM_STATE_APDU_PENDING;
   static_P7816SmSendApdu(pInstance, pChannel, nSendApduBufferLength);

   return W_ERROR_OPERATION_PENDING;
}

static W_ERROR static_P7816SmGetData(
                  tContext* pContext,
                  tP7816SmInstance* pInstance,
                  uint32_t nChannelReference,
                  uint32_t nType,
                  uint8_t* pBuffer,
                  uint32_t nBufferMaxLength,
                  uint32_t* pnActualLength)
{
   tP7816SmChannel* pChannel = null;
   uint8_t* pSourceBuffer = null;
   uint32_t nActualLength = 0;

   PDebugTrace("static_P7816SmGetData");

   if(pnActualLength != null)
   {
      *pnActualLength = 0;
   }

   if (pInstance == null)
   {
      PDebugError("static_P7816SmGetData: pInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (nChannelReference == P_7816SM_NULL_CHANNEL)
   {
      PDebugError("static_P7816SmGetData: nChannelReference == P_7816SM_NULL_CHANNEL");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((nType != P_7816SM_DATA_TYPE_IS_RAW_CHANNEL) && (nType != P_7816SM_DATA_TYPE_IS_BASIC_CHANNEL) && (nType != P_7816SM_DATA_TYPE_IS_DEFAULT_SELECTED))
   {
      if ((pBuffer == null) && (nBufferMaxLength != 0))
      {
         PDebugError("static_P7816SmGetData: pBuffer == null && nBufferMaxLength != 0");
         return W_ERROR_BAD_PARAMETER;
      }

      if (pnActualLength == null)
      {
         PDebugError("static_P7816SmGetData: pnActualLength == null");
         return W_ERROR_BAD_PARAMETER;
      }
   }

   if ((pChannel = static_P7816SmCheckLogicalChannel(pInstance, nChannelReference, W_TRUE)) == null)
   {
      PDebugError("static_P7816SmGetData: This channel is not associated with this instance");
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
         PDebugError("static_P7816SmGetData: nType is unknown");
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
         PDebugTrace("static_P7816SmGetData: nBufferMaxLength is too short");
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
      static_P7816SmFreeReceptionBuffer(pChannel);
   }

   return W_SUCCESS;
}

/* See header file */
tP7816SmInterface g_sP7816SmInterface =
{
   static_P7816SmOpenChannel,
   static_P7816SmCloseChannel,
   static_P7816SmExchangeApdu,
   static_P7816SmGetData
};
