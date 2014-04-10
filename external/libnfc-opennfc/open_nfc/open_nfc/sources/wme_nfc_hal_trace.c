/*
 * Copyright (c) 2007-2010 Inside Secure, All Rights Reserved.
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

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#ifdef P_TRACE_ACTIVE

#ifdef P_NFC_HAL_TRACE

/**
 * Writes the names of selected Reader protocol.
 *
 * @param[in]  pTraceBuffer  The trace buffer.
 *
 * @param[in]  pPos  The current position.
 *
 * @param[in]  nProtocolBitField  The protocol bit field.
 *
 * @return The number of characters added to the buffer.
 **/
static void static_PNALLogReaderProtocol(
                              char* pTraceBuffer,
                              uint32_t* pPos,
                              uint16_t nProtocolBitField)
{
   uint8_t nIndex;
   static const char* pName = "Reader:";
   static const char* pNone = "Reader:none ";

   static const uint16_t aProtocolFlag[] = {
      NAL_PROTOCOL_READER_ISO_14443_4_A,
      NAL_PROTOCOL_READER_ISO_14443_4_B,
      NAL_PROTOCOL_READER_ISO_14443_3_A,
      NAL_PROTOCOL_READER_ISO_14443_3_B,
      NAL_PROTOCOL_READER_ISO_15693_3,
      NAL_PROTOCOL_READER_ISO_15693_2,
      NAL_PROTOCOL_READER_FELICA,
      NAL_PROTOCOL_READER_P2P_INITIATOR,
      NAL_PROTOCOL_READER_TYPE_1_CHIP,
      NAL_PROTOCOL_READER_MIFARE_CLASSIC,
      NAL_PROTOCOL_READER_BPRIME,
      NAL_PROTOCOL_READER_KOVIO,
      NAL_PROTOCOL_READER_MIFARE_PLUS
   };

   static const char* aProtocolName[] = {
      "14-4A,", "14-4B,", "14-3A,", "14-3B,", "15-3,", "15-2,",
      "FELICA,", "P2P,", "T1,", "MIFCLA,", "BP,", "KOVIO", "MIFPLS"};

   if(nProtocolBitField == 0)
   {
      PUtilTraceASCIIStringCopy(pTraceBuffer, pPos, pNone);
   }
   else
   {
      PUtilTraceASCIIStringCopy(pTraceBuffer, pPos, pName);

      for(nIndex = 0; nIndex < (sizeof(aProtocolFlag)/sizeof(uint16_t)); nIndex++)
      {
         if((nProtocolBitField & aProtocolFlag[nIndex]) != 0)
         {
            PUtilTraceASCIIStringCopy(pTraceBuffer, pPos, aProtocolName[nIndex]);
         }
      }
      pTraceBuffer[(*pPos) - 1] = 0x20;
   }
}

/**
 * Writes the names of selected card protocol.
 *
 * @param[in]  pTraceBuffer  The trace buffer.
 *
 * @param[in]  pPos  The current position.
 *
 * @param[in]  nProtocolBitField  The protocol bit field.
 **/
static void static_PNALLogCardProtocol(
                              char* pTraceBuffer,
                              uint32_t* pPos,
                              uint16_t nProtocolBitField)
{
   uint8_t nIndex;
   static const char* pName = "Card:";
   static const char* pNone = "Card:none ";

   static const uint16_t aProtocolFlag[] = {
      NAL_PROTOCOL_CARD_ISO_14443_4_A,
      NAL_PROTOCOL_CARD_ISO_14443_4_B,
      NAL_PROTOCOL_CARD_ISO_14443_3_A,
      NAL_PROTOCOL_CARD_ISO_14443_3_B,
      NAL_PROTOCOL_CARD_ISO_15693_3,
      NAL_PROTOCOL_CARD_ISO_15693_2,
      NAL_PROTOCOL_CARD_FELICA,
      NAL_PROTOCOL_CARD_P2P_TARGET,
      NAL_PROTOCOL_CARD_TYPE_1_CHIP,
      NAL_PROTOCOL_CARD_MIFARE_CLASSIC,
      NAL_PROTOCOL_CARD_BPRIME,
      NAL_PROTOCOL_CARD_KOVIO,
      NAL_PROTOCOL_CARD_MIFARE_PLUS};

   static const char* aProtocolName[] = {
      "14-4A,", "14-4B,", "14-3A,", "14-3B,", "15-3,", "15-2,",
      "FELICA,", "P2P,", "T1,", "MIFCLA,", "BP,", "KOVIO", "MIFPLS"};

   if(nProtocolBitField == 0)
   {
      PUtilTraceASCIIStringCopy(pTraceBuffer, pPos, pNone);
   }
   else
   {
      PUtilTraceASCIIStringCopy(pTraceBuffer, pPos, pName);

      for(nIndex = 0; nIndex < (sizeof(aProtocolFlag)/sizeof(uint16_t)); nIndex++)
      {
         if((nProtocolBitField & aProtocolFlag[nIndex]) != 0)
         {
            PUtilTraceASCIIStringCopy(pTraceBuffer, pPos, aProtocolName[nIndex]);
         }
      }
      pTraceBuffer[(*pPos) - 1] = 0x20;
   }
}

/**
 * Copy the current Parameter name into the output buffer.
 *
 * @param[in]  nProtocolBitField  The protocol bit field.
 *
 * @return The number of characters added to the buffer.
 **/
static uint32_t static_PNALLogParamName(
                              char* pTraceBuffer,
                              uint16_t nParamCode)
{
   uint32_t nPos = 0;
   switch(nParamCode)
   {
      case NAL_PAR_PERSISTENT_POLICY:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "PERSISTENT_POLICY ");
         break;
      case NAL_PAR_POLICY:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "POLICY " );
         break;
      case NAL_PAR_HARDWARE_INFO:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "HARDWARE_INFO " );
         break;
      case NAL_PAR_FIRMWARE_INFO:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "FIRMWARE_INFO " );
         break;
      case NAL_PAR_DETECT_PULSE:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "DETECT_PULSE " );
         break;
      case NAL_PAR_PERSISTENT_MEMORY:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "PERSISTENT_MEMORY " );
         break;
      case NAL_PAR_READER_CONFIG:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "READER_CONFIG " );
         break;
      case NAL_PAR_CARD_CONFIG:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "CARD_CONFIG " );
         break;
      case NAL_PAR_P2P_INITIATOR_LINK_PARAMETERS:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "P2P_INITIATOR_LINK_PARAMETERS " );
         break;
      case NAL_PAR_UICC_SWP:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "UICC_SWP " );
         break;
      case NAL_PAR_UICC_READER_PROTOCOLS:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "UICC_READER_PROTOCOLS " );
         break;
      case NAL_PAR_UICC_CARD_PROTOCOLS:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "UICC_CARD_PROTOCOLS " );
         break;
      case NAL_PAR_RAW_MODE:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "RAW_MODE " );
         break;
      case NAL_PAR_LIST_CARDS:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "LIST_CARDS " );
         break;
      default:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "UNKNOW PARAM 0x00 " );
         PUtilLogUint8(&pTraceBuffer[nPos - 3], nParamCode);
      break;
   }
      return nPos;
}

/**
 * Writes the log of a NFC HAL buffer.
 *
 * @param[in]  pTraceBuffer  A pointer on the buffer where to write the log.
 *
 * @param[in]  pBuffer  The pointer on the buffer to dump.
 *
 * @param[in]  nLength  The length in bytes of the buffer to dump.
 *
 * @param[in]  nPosition  The number of characters added to the buffer.
 *
 * @param[in]  bFromNFCC  The direction flag. W_TRUE for NFCC to NFC Device, W_FALSE for
 *             NFC Device to NFCC.
 *
 * @param[inout] pbTraceChainedFrame  A pointer on a variable valued with a the
 *               NFC HAL continuation flag.
 **/
static void static_PNALLog(
         char* pTraceBuffer,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nPosition)
{
   uint32_t nPos = nPosition;
   uint8_t nNALServiceIdentifier;
   char* pTemp;
   uint8_t nType;
   uint8_t nValue;
   int32_t nSize = (int32_t)nLength;
   bool_t bOptionTrace = W_TRUE;

   if((pTraceBuffer == null) || (pBuffer == null))
   {
      return;
   }
   else
   {
      nNALServiceIdentifier = *pBuffer;
   }
   PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, " NAL-" );
   switch(nNALServiceIdentifier)
   {
      case NAL_SERVICE_ADMIN         : pTemp = "ADMIN"; break;
      case NAL_SERVICE_READER_14_A_4 : pTemp = "READER_14_A-4"; break;
      case NAL_SERVICE_READER_14_B_4 : pTemp = "READER_14_B-4"; break;
      case NAL_SERVICE_READER_14_A_3 : pTemp = "READER_14_A-3"; break;
      case NAL_SERVICE_READER_14_B_3 : pTemp = "READER_14_B-3"; break;
      case NAL_SERVICE_READER_TYPE_1 : pTemp = "READER_TYPE-1"; break;
      case NAL_SERVICE_READER_FELICA : pTemp = "READER_TYPE-3"; break;
      case NAL_SERVICE_READER_15_3   : pTemp = "READER_15-3"; break;
      case NAL_SERVICE_CARD_14_A_4   : pTemp = "CARD_14_A-4"; break;
      case NAL_SERVICE_CARD_14_B_4   : pTemp = "CARD_14_B-4"; break;
      case NAL_SERVICE_P2P_INITIATOR : pTemp = "P2P_INITIATOR"; break;
      case NAL_SERVICE_P2P_TARGET    : pTemp = "P2P_TARGET"; break;
      case NAL_SERVICE_UICC          : pTemp = "UICC"; break;
      case NAL_SERVICE_SECURE_ELEMENT: pTemp = "SECURE_ELEMENT"; break;
      case NAL_SERVICE_READER_15_2   : pTemp = "READER_15-2"; break;
      case NAL_SERVICE_READER_B_PRIME: pTemp = "READER_B_Prime"; break;
      case NAL_SERVICE_READER_KOVIO  : pTemp = "READER_KOVIO"; break;
      default:
         pTemp = "???????????"; break;
   };

   PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, pTemp );

   pBuffer++;
   nSize--;
   /* retrieve the message type and instructin byte value*/
   nType = *pBuffer & 0xC0;
   nValue = *pBuffer;
   pBuffer++;
   nSize--;

   switch(nType)
   {
      case NAL_MESSAGE_TYPE_COMMAND:
         pTemp = " CMD-";
         break;
      case NAL_MESSAGE_TYPE_ANSWER:
         pTemp = " ANS-";
         break;
      case NAL_MESSAGE_TYPE_EVENT:
         pTemp = " EVT-";
         break;
      default:
         pTemp = " <?>-";
         break;
   }

   PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, pTemp );

   switch(nValue)
   {
      /**
      *NAL_MESSAGE_TYPE_COMMAND
      **/
      case NAL_CMD_SET_PARAMETER:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "SET_PARAMETER " );
         nPos += static_PNALLogParamName(&pTraceBuffer[nPos], *pBuffer);
         pBuffer++;
         nSize--;
         break;
      case NAL_CMD_GET_PARAMETER:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "GET_PARAMETER " );
         nPos += static_PNALLogParamName(&pTraceBuffer[nPos], *pBuffer);
         pBuffer++;
         nSize--;
         break;
      case NAL_CMD_DETECTION:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "DETECTION " );
         static_PNALLogCardProtocol(pTraceBuffer, &nPos, (((uint16_t)(*pBuffer) << 8))|(((uint16_t)(*(pBuffer + 1)))));
         pBuffer += 2;
         nSize -=2;
         static_PNALLogReaderProtocol(pTraceBuffer, &nPos, (((uint16_t)(*pBuffer) << 8))|(((uint16_t)(*(pBuffer + 1)))));
         pBuffer += 2;
         nSize -=2;
         break;
      case NAL_CMD_READER_XCHG_DATA:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "READER_XCHG_DATA " );
         break;
      case NAL_CMD_COM_TRANSFER:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "COM_TRANSFER T=0x00 " );
         PUtilLogUint8(&pTraceBuffer[nPos - 3], *pBuffer);
         pBuffer++;
         nSize--;
         break;
      case NAL_CMD_PRODUCTION_TEST:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "PRODUCTION_TEST CD=0x00 " );
         PUtilLogUint8(&pTraceBuffer[nPos - 3], *pBuffer);
         pBuffer++;
         nSize--;
         break;
      case NAL_CMD_SELF_TEST:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "SELF_TEST " );
         break;
      case NAL_CMD_UPDATE_FIRMWARE:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "UPDATE_FIRMWARE " );
         break;
      case NAL_CMD_UICC_START_SWP:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "UICC_START_SWP " );
         break;
         /**
         *NAL_MESSAGE_TYPE_ANSWER
         **/
      case NAL_RES_OK:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "RES_OK " );
         break;
      case NAL_RES_TIMEOUT:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "RES_TIMEOUT " );
         break;
      case NAL_RES_UNKNOWN_COMMAND:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "RES_UNKNOWN_COMMAND " );
         break;
      case NAL_RES_UNKNOWN_PARAM:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "RES_UNKNOWN_PARAM " );
         break;
      case NAL_RES_FEATURE_NOT_SUPPORTED:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "RES_FEATURE_NOT_SUPPORTED " );
         break;
      case NAL_RES_BAD_LENGTH:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "RES_BAD_LENGTH " );
         break;
      case NAL_RES_BAD_DATA:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "RES_BAD_DATA " );
         break;
      case NAL_RES_BAD_VERSION:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "RES_BAD_VERSION " );
         break;
      case NAL_RES_BAD_STATE:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "RES_BAD_STATE " );
         break;
      case NAL_RES_PROTOCOL_ERROR:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "RES_PROTOCOL_ERROR " );
         break;

         /**
         *NAL_MESSAGE_TYPE_EVENT
         **/
      case NAL_EVT_STANDBY_MODE:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "STANDBY_MODE W=0x00 ");
         PUtilLogUint8(&pTraceBuffer[nPos -3], *pBuffer);
         pBuffer++;
         nSize--;
         break;
      case NAL_EVT_READER_TARGET_COLLISION:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "READER_TARGET_COLLISION " );
         break;
      case NAL_EVT_READER_TARGET_DISCOVERED:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "READER_TARGET_DISCOVERED " );
         break;
      case NAL_EVT_UICC_DETECTION_REQUEST:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "UICC_DETECTION_REQUEST " );
         break;
      case NAL_EVT_CARD_SELECTED:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "CARD_SELECTED " );
         break;
      case NAL_EVT_CARD_SEND_DATA:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "CARD_SEND_DATA " );
         break;
      case NAL_EVT_CARD_END_OF_TRANSACTION:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "CARD_END_OF_TRANSACTION R=0x00 " );
         PUtilLogUint8(&pTraceBuffer[nPos - 3], *pBuffer);
         pBuffer++;
         nSize--;
         break;
       case NAL_EVT_SE_CARD_EOT:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "SE_CARD_EOT C=0x0000 " );
         PUtilLogUint16(&pTraceBuffer[nPos - 5], (((uint16_t)(*(pBuffer + 1)) << 8))|(((uint16_t)(*(pBuffer + 2)))));
         pBuffer += 3;
         nSize -=3;
         break;
      case NAL_EVT_P2P_TARGET_DISCOVERED:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "P2P_TARGET_DISCOVERED " );
         break;
      case NAL_EVT_P2P_SEND_DATA:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "P2P_SEND_DATA " );
         break;
      case NAL_EVT_UICC_CONNECTIVITY:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "UICC_CONNECTIVITY " );
         break;
      case NAL_EVT_RF_FIELD:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "RF_FIELD " );
         break;
      case NAL_EVT_RAW_MESSAGE:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "RAW_MESSAGE " );
         break;
      case NAL_EVT_NFCC_ERROR:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "NFCC_ERROR " );
         break;
      default:
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "0x00 " );
         PUtilLogUint8(&pTraceBuffer[nPos - 3], nValue);
         bOptionTrace = W_FALSE;
         break;
   }

   if((bOptionTrace == W_FALSE) || (nSize > 0))
   {
      nPos += PUtilLogArray(&pTraceBuffer[nPos], pBuffer, nSize);
   }
   else
   {
      pTraceBuffer[nPos] = 0;
   }
}

/**
  * Wraps CDebugPrintTrace for log of NFC HAL buffer
  */

void PDebugPrintTraceWrapper(const char* pTag, uint32_t nTraceLevel, const char* pMessage, ...)
{
   va_list list;

   va_start(list, pMessage);

   CDebugPrintTrace(pTag, nTraceLevel, pMessage, list);

   va_end(list);;
}

/**
 * Writes the log of the NFC HAL buffer.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pBuffer  The pointer on the buffer to dump.
 *
 * @param[in]  nLength  The length in bytes of the buffer to dump.
 *
 * @param[in]  bFromNFCC  The direction flag. W_TRUE for NFCC to NFC Device, W_FALSE for
 *             NFC Device to NFCC.
 **/
void PNALFrameLogBuffer(
         tContext* pContext,
         uint8_t* pBuffer,
         uint32_t nLength,
         bool_t bFromNFCC)
{
   char* pTraceBuffer = PContextGetNALTraceBuffer(pContext);
   uint32_t nPos = 0;
   if(bFromNFCC)
   {
      PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "ND<-NFCC " );
   }
   else
   {
      PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "ND->NFCC " );
   }
   static_PNALLog(pTraceBuffer, pBuffer, nLength, nPos);
   PDebugPrintTraceWrapper("NAL", P_TRACE_LOG, pTraceBuffer);
}

#endif /* #ifdef P_NFC_HAL_TRACE */

#endif /* #ifdef P_TRACE_ACTIVE */

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */
