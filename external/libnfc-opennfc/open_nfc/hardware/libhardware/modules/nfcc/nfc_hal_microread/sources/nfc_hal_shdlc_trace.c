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

#include "nfc_hal_binding.h"

#ifdef P_NAL_TRACE_ACTIVE

#ifdef P_SHDLC_TRACE

#define FLAG_SET_NR  1
#define FLAG_SET_NS  2
#define FLAG_CUT     4

static void static_PSHDLCLogPrepareBuffer(
            tPNALSHDLCTraceContext* pTraceContext,
            char* pTraceBuffer,
            uint32_t* pnPos,
            uint8_t nHeader,
            const char* pCode,
            uint32_t nFlags)
{
   uint32_t nLast = 0;
   uint32_t nPos = *pnPos;
   if(pTraceContext->bFromNFCC)
   {
      PNALUtilTraceASCIIStringCopy(pTraceBuffer, pnPos, "    NR=0 NS=0 ");

      if((nFlags & FLAG_SET_NR) != 0)
      {
         pTraceBuffer[nPos + 7] = (nHeader & 0x07) + '0';
         nLast = nPos + 8;
      }

      if((nFlags & FLAG_SET_NS) != 0)
      {
         pTraceBuffer[nPos + 12] = ((nHeader & 0x38) >> 3) + '0';
         nLast = nPos + 13;
      }
   }
   else
   {
      PNALUtilTraceASCIIStringCopy(pTraceBuffer, pnPos, "         NR=0 ");

      if((nFlags & FLAG_SET_NS) != 0)
      {
         pTraceBuffer[nPos + 4] = 'N';
         pTraceBuffer[nPos + 5] = 'S';
         pTraceBuffer[nPos + 6] = '=';
         pTraceBuffer[nPos + 7] = ((nHeader & 0x38) >> 3) + '0';
         nLast = nPos + 8;
      }

      if((nFlags & FLAG_SET_NR) != 0)
      {
         pTraceBuffer[nPos + 12] = (nHeader & 0x07) + '0';
         nLast = nPos + 13;
      }
   }

   while(*pCode != 0)
   {
      pTraceBuffer[nPos++] = *pCode++;
   }

   if(nLast == 0) nLast = nPos;

   if((nFlags & FLAG_CUT) != 0)
   {
      pTraceBuffer[nLast] = 0;
   }

   *pnPos = nLast;
}

/**
 * Copy the current Parameter name into the output buffer.
 *
 * @param[in]  nPipe  The Pide ID.
 *
 * @param[in]  nParamCode  The Param Code.
 *
 * @return The number of characters added to the buffer.
 **/
static uint32_t static_PHCILogParamName(
                              uint8_t nPipe,
                              char* pTraceBuffer,
                              uint16_t nParamCode)
{
   uint32_t nPos = 0;
   if( nPipe == HCI2_PIPE_ID_HDS_MCARD_ISO_A)
   {
      switch(nParamCode)
      {
         case HCI2_PAR_MCARD_ISO_A_MODE:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_A_MODE ");
            break;
         case HCI2_PAR_MCARD_ISO_A_UID:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_A_UID " );
            break;
         case HCI2_PAR_MCARD_ISO_A_SAK:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_A_SAK " );
            break;
         case HCI2_PAR_MCARD_ISO_A_ATQA:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_A_ATQA " );
            break;
         case HCI2_PAR_MCARD_ISO_A_APPLI_DATA:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_A_APPLI_DATA " );
            break;
         case HCI2_PAR_MCARD_ISO_A_FWI_SFGI:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_A_FWI_SFGI " );
            break;
         case HCI2_PAR_MCARD_ISO_A_CID_SUPPORT:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_A_CID_SUPPORT " );
            break;
         case HCI2_PAR_MCARD_ISO_A_DATARATEMAX:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_A_DATARATEMAX " );
            break;
         default:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "PARAM 0x00 " );
            PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nParamCode);
         break;
      }
   }
   else if( nPipe == HCI2_PIPE_ID_HDS_MCARD_ISO_B)
   {
      switch(nParamCode)
      {
         case HCI2_PAR_MCARD_ISO_B_MODE:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_B_MODE " );
            break;
         case HCI2_PAR_MCARD_ISO_B_UID:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_B_UID " );
            break;
         case HCI2_PAR_MCARD_ISO_B_AFI:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_B_AFI " );
            break;
         case HCI2_PAR_MCARD_ISO_B_ATQB:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_B_ATQB " );
            break;
         case HCI2_PAR_MCARD_ISO_B_ATTRIB_INF_RESP:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_B_ATTRIB_INF_RESP " );
            break;
         case HCI2_PAR_MCARD_ISO_B_DATARATEMAX:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_B_DATARATEMAX " );
            break;
         default:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "PARAM 0x00 " );
            PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nParamCode);
         break;
      }
   }
   else if( nPipe == HCI2_PIPE_ID_MGT)
   {
      switch(nParamCode)
      {
         case HCI2_PAR_MGT_VERSION_HARDWARE:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MGT_VERSION_HARDWARE " );
            break;
         case HCI2_PAR_MGT_VERSION_LOADER:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MGT_VERSION_LOADER " );
            break;
         case HCI2_PAR_MGT_PATCH_STATUS:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MGT_PATCH_STATUS " );
            break;
         case HCI2_PAR_MGT_CURRENT_RF_LOCK_CARD:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MGT_CURRENT_RF_LOCK_CARD " );
            break;
         case HCI2_PAR_MGT_CURRENT_RF_LOCK_READER:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MGT_CURRENT_RF_LOCK_READER " );
            break;
         case HCI2_PAR_MGT_DEFAULT_RF_LOCK_CARD:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MGT_DEFAULT_RF_LOCK_CARD " );
            break;
         case HCI2_PAR_MGT_STANDBY_RF_LOCK_CARD:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "HCI2_PAR_MGT_STANDBY_RF_LOCK_CARD " );
            break;
         case HCI2_PAR_MGT_DEFAULT_RF_LOCK_READER:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MGT_DEFAULT_RF_LOCK_READER " );
            break;
         case HCI2_PAR_MGT_AVAILABLE_MODE_CARD:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "AVAILABLE_MODE_CARD " );
            break;
         case HCI2_PAR_MGT_AVAILABLE_MODE_READER:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "AVAILABLE_MODE_READER " );
            break;
         case HCI2_PAR_MGT_HDS_OWNER_READER:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "HDS_OWNER_READER " );
            break;
          case HCI2_PAR_MGT_HDS_OWNER_CARD:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "HDS_OWNER_CARD " );
            break;
          case HCI2_PAR_MGT_INIT_CONFIG_CURRENT:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "INIT_CONFIG_CURRENT " );
            break;
          case HCI2_PAR_MGT_INIT_CONFIG_BACKUP:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "INIT_CONFIG_BACKUP " );
            break;
          case HCI2_PAR_MGT_SWP_GATE_ACCESS:
              PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "SWP_GATE_ACCESS " );
            break;
          case HCI2_PAR_MGT_SWP_SHDLC_STATUS:
             PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "SWP_SHDLC_STATUS " );
             break;
         case HCI2_PAR_MGT_SIM_OWNER_CARD:
             PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "SIM_OWNER_CARD " );
             break;
         case HCI2_PAR_MGT_SIM_OWNER_READER:
             PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "SIM_OWNER_READER " );
             break;
         case HCI2_PAR_MGT_CARD_DETECT_CONFIG_CURRENT :
             PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "CARD_DETECT_CONFIG_CURRENT " );
             break;
         case HCI2_PAR_MGT_CARD_DETECT_STATE:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "CARD_DETECT_STATE " );
             break;
         default:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "PARAM 0x00 " );
            PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nParamCode);
         break;
      }
   }
   else if( nPipe == HCI2_PIPE_ID_ADMIN)
   {
      switch(nParamCode)
      {
         case HCI2_PAR_ADM_WHITE_LIST:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "WHITE_LIST " );
            break;
         case HCI2_PAR_ADM_SESSION_ID:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "SESSION_ID " );
            break;
         default:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "PARAM 0x00 " );
            PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nParamCode);
         break;
      }
   }
   else if( nPipe == HCI2_PIPE_ID_HDS_INSTANCES)
   {
      switch(nParamCode)
      {
         case HCI2_PAR_INSTANCES_DEFAULT_MCARD_GRANTED_TO_SIM:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "DEFAULT_MCARD_GRANTED_TO SIM " );
            break;
         case HCI2_PAR_INSTANCES_STANDBY_MCARD_GRANTED_TO_SIM:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "DEFAULT_MCARD_STANDBY_TO SIM " );
            break;
         case HCI2_PAR_INSTANCES_CURRENT_MCARD_GRANTED_TO_SIM:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "CURRENT_MCARD_GRANTED_TO_SIM " );
            break;
         case HCI2_PAR_INSTANCES_DEFAULT_MREAD_GRANTED_TO_SIM:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "DEFAULT_MREAD_GRANTED_TO_SIM " );
            break;
         case HCI2_PAR_INSTANCES_CURRENT_MREAD_GRANTED_TO_SIM:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "CURRENT_MREAD_GRANTED_TO_SIM " );
            break;
         default:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "PARAM 0x00 " );
            PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nParamCode);
         break;
      }
   }
   else if( nPipe == HCI2_PIPE_ID_HDS_P2P_TARGET)
   {
      switch(nParamCode)
      {
         case HCI2_PAR_P2P_TARGET_MODE:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "TARGET_MODE " );
            break;

         case HCI2_PAR_P2P_TARGET_TO:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "TARGET_TO " );
            break;

         case HCI2_PAR_P2P_TARGET_GT:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "TARGET_GT " );
            break;

         case HCI2_PAR_P2P_TARGET_MIUX_WKS_LTO:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "HCI2_PAR_P2P_TARGET_MIUX_WKS_LTO " );
            break;

         case HCI2_PAR_P2P_TARGET_ISOA_ACTIVE:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "HCI2_PAR_P2P_TARGET_ISOA_ACTIVE " );
            break;

         default:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "PARAM 0x00 " );
            PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nParamCode);
         break;
      }
   }
   else if( nPipe == HCI2_PIPE_ID_HDS_P2P_INITIATOR)
   {
      switch (nParamCode)
      {
         case HCI2_PAR_P2P_INITIATOR_GI:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "INITIATOR_GI " );
            break;

         case HCI2_PAR_P2P_INITIATOR_GT:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "INITIATOR_GT " );
            break;

         case HCI2_PAR_P2P_INITIATOR_MIUX_WKS_LTO:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "HCI2_PAR_P2P_INITIATOR_MIUX_WKS_LTO " );
            break;

         case HCI2_PAR_P2P_INITIATOR_BRS:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "INITIATOR_BRS " );
            break;

         default:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "PARAM 0x00 " );
            PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nParamCode);
            break;
      }
   }
   else if( nPipe == HCI2_PIPE_ID_HDS_STACKED_ELEMENT)
   {
      switch (nParamCode)
      {
         case HCI2_PAR_SE_SETTINGS_DEFAULT:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "SETTINGS_DEFAULT " );
            break;

         case HCI2_PAR_SE_SETTINGS_CURRENT:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "SETTINGS_CURRENT " );
            break;

         case HCI2_PAR_SE_SETTINGS_STANDBY:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "SETTINGS_STANDBY " );
            break;

         default:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "PARAM 0x00 " );
            PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nParamCode);
            break;
      }
   }
   else if( nPipe == HCI2_PIPE_ID_HDS_MREAD_GEN)
   {
      switch(nParamCode)
      {
         case HCI2_PAR_MREAD_GEN_P2P_ACTIVE:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "HCI2_PAR_MREAD_GEN_P2P_ACTIVE " );
            break;

         default:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "PARAM 0x00 " );
            PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nParamCode);
         break;
      }
   }
   else if (nPipe == HCI2_PIPE_ID_HDS_MCARD)
   {
      switch (nParamCode)
      {
         case HCI2_PAR_MCARD_GEN_ROUTING_TABLE_ENABLED:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "HCI2_PAR_MCARD_GEN_ROUTING_TABLE_ENABLED " );
            break;

         case HCI2_PAR_MCARD_GEN_ROUTING_TABLE:
            PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "HCI2_PAR_MCARD_GEN_ROUTING_TABLE " );
            break;
      }
   }
   else
   {
       PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "PARAM 0x00 " );
       PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nParamCode);
   }
   return nPos;
}

/**
 * Writes the log of a HCI buffer.
 *
 * @param[in]  pTraceContext  The trace context.
 *
 * @param[in]  pTraceBuffer  A pointer on the buffer where to write the log.
 *
 * @param[in]  pBuffer  The pointer on the buffer to dump.
 *
 * @param[in]  nLength  The length in bytes of the buffer to dump.
 *
 * @param[in]  nPosition  The number of characters added to the buffer.
 **/
static void static_PSHDLCLogHCI(
         tPNALSHDLCTraceContext* pTraceContext,
         char* pTraceBuffer,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nPosition)
{
   uint32_t nPos = nPosition;
   uint8_t nPipe = (*pBuffer) & 0x7F;
   char* pTemp;
   uint8_t nType;
   uint8_t nValue;
   bool_t bOptionTrace = W_FALSE;
   bool_t bTraceChainedFrame;

   /* We assume there that we have a valid HCI payload, at least 2 bytes long */
   if (nLength < 2)
   {
      return;
   }

   PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, " : HCI-");

   switch(nPipe)
   {
      case HCI2_PIPE_ID_LMS: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "LMS" ); break;
      case HCI2_PIPE_ID_ADMIN: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "ADMIN" ); break;
      case HCI2_PIPE_ID_MGT: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MGT" ); break;
      case HCI2_PIPE_ID_OS: PNALUtilTraceASCIIStringCopy(pTraceBuffer,  &nPos, "OS" ); break;
      case HCI2_PIPE_ID_HDS_IDT: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "IDT" ); break;
      case HCI2_PIPE_ID_HDS_MREAD_ISO_B: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MREAD_ISO_B" ); break;
      case HCI2_PIPE_ID_HDS_MREAD_ISO_A: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MREAD_ISO_A" ); break;
      case HCI2_PIPE_ID_HDS_MREAD_ISO_B_3: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MREAD_ISO_B_3" ); break;
      case HCI2_PIPE_ID_HDS_MREAD_ISO_A_3: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MREAD_ISO_A_3" ); break;
      case HCI2_PIPE_ID_HDS_MREAD_ISO_15_3: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MREAD_ISO_15_3" ); break;
      case HCI2_PIPE_ID_HDS_MREAD_ISO_15_2: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MREAD_ISO_15_2" ); break;
      case HCI2_PIPE_ID_HDS_MREAD_NFC_T3: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MREAD_NFC_T3" ); break;
      case HCI2_PIPE_ID_HDS_MREAD_NFC_T1: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MREAD_NFC_T1" ); break;
      case HCI2_PIPE_ID_HDS_MCARD_ISO_B: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_B" ); break;
      case HCI2_PIPE_ID_HDS_MCARD_ISO_A: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_A" ); break;
      case HCI2_PIPE_ID_HDS_MREAD_BPRIME: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MREAD_B_PRIME" ); break;
      case HCI2_PIPE_ID_HDS_MREAD_KOVIO: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MREAD_KOVIO" ); break;
      case HCI2_PIPE_ID_HDS_MCARD_ISO_BPRIME: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_BPRIME" ); break;
      case HCI2_PIPE_ID_HDS_MCARD_ISO_15_3: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_15_3" ); break;
      case HCI2_PIPE_ID_HDS_MCARD_ISO_15_2: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_ISO_15_2" ); break;
      case HCI2_PIPE_ID_HDS_MCARD_NFC_T3: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_NFC_T3" ); break;
      case HCI2_PIPE_ID_HDS_SIM_CONNECTIVITY: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "SIM_CONNECTIVITY" ); break;
      case HCI2_PIPE_ID_HDS_MREAD_GEN: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MREAD_GEN " ); break;
      case HCI2_PIPE_ID_HDS_STACKED_ELEMENT: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "STACKED_ELEMENT " ); break;
      case HCI2_PIPE_ID_HDS_INSTANCES: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "INSTANCES " ); break;
      case HCI2_PIPE_ID_HDS_P2P_TARGET: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "P2P_TARGET " ); break;
      case HCI2_PIPE_ID_HDS_P2P_INITIATOR: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "P2P_INITIATOR " ); break;
      case HCI2_PIPE_ID_HDS_TEST_RF: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "TEST_RF " ); break;
      case HCI2_PIPE_ID_HDS_MCARD: PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MCARD_GEN " ); break;

      default:
         PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "???????????" ); break;
   };

   bTraceChainedFrame = pTraceContext->bTraceChainedFrame;
   pTraceContext->bTraceChainedFrame = (((*pBuffer) & 0x80) != 0)?W_FALSE:W_TRUE;

   pBuffer++;
   nLength--;

   if(bTraceChainedFrame != W_FALSE)
   {
      PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, " ... " );
   }
   else
   {
      nType = *pBuffer & 0xC0;
      nValue = *pBuffer & 0x3F;
      pBuffer++;
      nLength--;

      switch(nType)
      {
         case ETSI_MSG_TYPE_CMD:
            pTemp = " CMD-";
            break;
         case ETSI_MSG_TYPE_EVT:
            pTemp = " EVT-";
            break;
         case ETSI_MSG_TYPE_ANS:
            pTemp = " ANS-";
            break;
         default:
            pTemp = " <?>-";
            break;
      }

      PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, pTemp );

      if(nType == ETSI_MSG_TYPE_CMD)
      {
          if (pTraceContext->bFromNFCC != W_TRUE)
          {
            switch(nValue)
            {
               case ETSI_CMD_ANY_SET_PARAMETER:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "SET_PARAM ");
                  nPos += static_PHCILogParamName(nPipe, &pTraceBuffer[nPos], *pBuffer);
                  bOptionTrace = W_TRUE;
                  pBuffer++;
                  nLength--;
                  break;
               case ETSI_CMD_ANY_GET_PARAMETER:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "GET_PARAM ");
                  nPos += static_PHCILogParamName(nPipe, &pTraceBuffer[nPos], *pBuffer);
                  bOptionTrace = W_TRUE;
                  pBuffer++;
                  nLength--;
                  break;
               case ETSI_CMD_WR_XCHG_DATA:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "WR_XCHG_DATA ");
                  bOptionTrace = W_TRUE;
                  break;
               case ETSI_CMD_ANY_OPEN_PIPE:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "OPEN_PIPE ");
                  bOptionTrace = W_TRUE;
                  break;
               case ETSI_CMD_ANY_CLOSE_PIPE:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "CLOSE_PIPE ");
                  bOptionTrace = W_TRUE;
                  break;
               case HCI2_CMD_MREAD_SELECT_HOST:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "SELECT_HOST ELT=0x00 " );
                  PNALUtilLogUint8(&pTraceBuffer[nPos - 3], *pBuffer);
                  bOptionTrace = W_TRUE;
                  pBuffer++;
                  nLength--;
                  break;
               case HCI2_CMD_MREAD_SUBSCRIBE:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "SUBSCRIBE " );
                  bOptionTrace = W_TRUE;
                  break;
               case HCI2_CMD_MGT_IDENTITY:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MGT_IDENTITY ");
                  bOptionTrace = W_TRUE;
                  break;
               case HCI2_CMD_MGT_TEST_SELF_CHIP:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MGT_TEST_SELF_CHIP ");
                  bOptionTrace = W_TRUE;
                  break;
               case HCI2_CMD_OS_LOADER_CONFIRM:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "OS_LOADER_CONFIRM ");
                  bOptionTrace = W_TRUE;
                  break;
               case HCI2_CMD_OS_LOADER_ENTER:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "_OS_LOADER_ENTER ");
                  bOptionTrace = W_TRUE;
                  break;
               case HCI2_CMD_OS_LOADER_QUIT:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "OS_LOADER_QUIT ");
                  bOptionTrace = W_TRUE;
                  break;
               case HCI2_CMD_OS_LOADER_LOADPAGE:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "OS_LOADER_LOADPAGE ");
                  bOptionTrace = W_TRUE;
                  break;
               case HCI2_CMD_OS_LOADER_CHECKCRC:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "OS_LOADER_CHECKCRC ");
                  bOptionTrace = W_TRUE;
                  break;

               case HCI2_CMD_MGT_STANDBY:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MGT_STANDBY ");
                  bOptionTrace = W_TRUE;
                  break;

               default:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "0x00 " );
                  PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nValue);
                  break;
            }
          }
          else
          {
             switch(nValue)
            {
               case HCI2_CMD_MREAD_IS_CARD_FOR_SIM:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "IS_CARD_FOR_SIM " );
                  bOptionTrace = W_TRUE;
                  break;
               default:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "0x00 " );
                  PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nValue);
                  break;
             }
          }
      }
      else if(nType == ETSI_MSG_TYPE_EVT)
      {
         switch(nPipe)
         {
            case HCI2_PIPE_ID_HDS_MREAD_ISO_B:  /* Reader mode*/
            case HCI2_PIPE_ID_HDS_MREAD_NFC_T1:
            case HCI2_PIPE_ID_HDS_MREAD_ISO_A:
            case HCI2_PIPE_ID_HDS_MREAD_ISO_15_3:
            case HCI2_PIPE_ID_HDS_MREAD_ISO_15_2:
            case HCI2_PIPE_ID_HDS_MREAD_NFC_T3:
            case HCI2_PIPE_ID_HDS_MREAD_ISO_B_3:
            case HCI2_PIPE_ID_HDS_MREAD_ISO_A_3:
            case HCI2_PIPE_ID_HDS_MREAD_BPRIME:
            case HCI2_PIPE_ID_HDS_MREAD_GEN:
            case HCI2_PIPE_ID_HDS_MREAD_KOVIO:
            case HCI2_PIPE_ID_HDS_P2P_INITIATOR:
               if (pTraceContext->bFromNFCC != W_TRUE)
               {
                  switch(nValue)
                  {
                     case ETSI_EVT_READER_REQUESTED:
                           PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "READER_REQUESTED " );
                           break;
                     case ETSI_EVT_END_OPERATION:
                           PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "END_OPERATION " );
                           break;
                     case HCI2_EVT_MREADER_DISCOVERY_START_SOME:
                           PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MREADER_DISCOVERY_START_SOME " );
                           break;
                     case HCI2_EVT_P2P_INITIATOR_EXCHANGE_TO_RF:
                           PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "EVT_P2P_INITIATOR_EXCHANGE_TO_RF " );
                           break;
                     default:
                           PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "0x00 " );
                           PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nValue);
                           break;
                  }
               }
               else
               {
                  switch(nValue)
                  {
                     case HCI2_EVT_MREAD_CARD_FOUND:
                        PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "CARD_FOUND " );
                        bOptionTrace = W_TRUE;
                     break;

                     case HCI2_EVT_MREADER_DISCOVERY_OCCURED:
                           PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MREADER_DISCOVERY_OCCURED STATUS=0x00 " );
                           PNALUtilLogUint8(&pTraceBuffer[nPos - 3], *pBuffer);
                           pBuffer++;
                           nLength--;
                           break;
                     case HCI2_EVT_MREADER_SIM_REQUEST:
                           PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "MREADER_SIM_REQUEST R=0x0000 " );
                           PNALUtilLogUint16(&pTraceBuffer[nPos - 5], (((uint16_t)(*pBuffer) << 8))|(((uint16_t)(*(pBuffer + 1)))));
                           pBuffer += 2;
                           nLength -= 2;
                           break;
                      case HCI2_EVT_P2P_INITIATOR_EXCHANGE_FROM_RF:
                           PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "EVT_P2P_INITIATOR_EXCHANGE_FROM_RF " );
                           break;
                     default:
                              PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "0x00 " );
                              PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nValue);
                              break;
                  }
               }
               break;
            case HCI2_PIPE_ID_HDS_MCARD_ISO_B:  /* Card mode*/
            case HCI2_PIPE_ID_HDS_MCARD_ISO_BPRIME:
            case HCI2_PIPE_ID_HDS_MCARD_ISO_A:
            case HCI2_PIPE_ID_HDS_MCARD_ISO_15_3:
            case HCI2_PIPE_ID_HDS_MCARD_ISO_15_2:
            case HCI2_PIPE_ID_HDS_MCARD_NFC_T3:
            case HCI2_PIPE_ID_HDS_P2P_TARGET:
            case HCI2_PIPE_ID_HDS_MCARD:

               if (pTraceContext->bFromNFCC != W_TRUE)
               {
                  switch(nValue)
                  {
                     case ETSI_EVT_SEND_DATA:
                           PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "EVT_SEND_DATA " );
                           break;
                     default:
                           PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "0x00 ");
                           PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nValue);
                           break;
                  }
               }
               else
               {
                   switch(nValue)
                  {
                     case ETSI_EVT_SEND_DATA:
                           PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "EVT_SEND_DATA " );
                           break;
                     case ETSI_EVT_FIELD_ON:
                           PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "EVT_FIELD_ON " );
                           break;
                     case ETSI_EVT_CARD_DEACTIVATED:
                           PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "EVT_CARD_DEACTIVATED " );
                           break;
                     case ETSI_EVT_CARD_ACTIVATED:
                           PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "EVT_CARD_ACTIVATED " );
                           break;
                     case ETSI_EVT_FIELD_OFF:
                           PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "EVT_FIELD_OFF " );
                           break;
                     default:
                           PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "0x00 " );
                           PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nValue);
                           break;
                  }
               }
               break;

            default:

               switch (nValue)
               {
                  case HCI2_EVT_MGT_HDS_SET_UP_SWPLINE :
                     PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "EVT_MGT_HDS_SET_UP_SWPLINE " );
                     break;

                  default:
                     PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "0x00 " );
                     PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nValue);
                     break;
               }
               break;
         }
      }
      else if(nType == ETSI_MSG_TYPE_ANS)
      {
         if (pTraceContext->bFromNFCC == W_TRUE)
         {
            switch(nValue)
            {
               case ETSI_ERR_ANY_OK:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "ANY_OK " );
                  bOptionTrace = W_TRUE;
                  break;
               case ETSI_ERR_ANY_E_NOT_CONNECTED:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "ANY_E_NOT_CONNECTED " );
                  bOptionTrace = W_TRUE;
                  break;
               case ETSI_ERR_ANY_E_CMD_PAR_UNKNOWN:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "ANY_E_CMD_PAR_UNKNOWN " );
                  bOptionTrace = W_TRUE;
                  break;
               case ETSI_ERR_ANY_E_NOK:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "ANY_E_NOK ");
                  bOptionTrace = W_TRUE;
                  break;
               case ETSI_ERR_ADM_E_NO_PIPES_AVAILABLE:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "ADM_E_NO_PIPES_AVAILABLE " );
                  bOptionTrace = W_TRUE;
                  break;
               case ETSI_ERR_ANY_E_REG_PAR_UNKNOWN:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "ANY_E_REG_PAR_UNKNOWN " );
                  bOptionTrace = W_TRUE;
                  break;
               case ETSI_ERR_ANY_E_PIPE_NOT_OPENED:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "ANY_E_PIPE_NOT_OPENED " );
                  bOptionTrace = W_TRUE;
                  break;
               case ETSI_ERR_ANY_E_CMD_NOT_SUPPORTED:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "ANY_E_CMD_NOT_SUPPORTED " );
                  bOptionTrace = W_TRUE;
                  break;
               case ETSI_ERR_ANY_E_TIMEOUT:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "ANY_E_TIMEOUT " );
                  bOptionTrace = W_TRUE;
                  break;
               case ETSI_ERR_ANY_E_REG_ACCESS_DENIED:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "ANY_E_REG_ACCESS_DENIED " );
                  bOptionTrace = W_TRUE;
                  break;
               case ETSI_ERR_ANY_E_PIPE_ACCESS_DENIED:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "ANY_E_PIPE_ACCESS_DENIED " );
                  bOptionTrace = W_TRUE;
                  break;

               case ETSI_ERR_WR_RF_ERROR:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "WR_RF_ERROR " );
                  bOptionTrace = W_TRUE;
                  break;

               default:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "0x00 " );
                  PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nValue);
                  break;
            }
         }
         else
         {
            switch(nValue)
            {
               case ETSI_ERR_ANY_OK:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "ANY_OK " );
                  bOptionTrace = W_TRUE;
                  break;
               default:
                  PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "0x00 " );
                  PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nValue);
                  break;
            }
         }
      }
      else
      {
         PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "0x00 " );
         PNALUtilLogUint8(&pTraceBuffer[nPos - 3], nValue);
      }
   }

   if((bOptionTrace == W_FALSE) || (nLength != 0))
   {
      nPos += PNALUtilLogArray(&pTraceBuffer[nPos], pBuffer, nLength);
   }
   else
   {
      pTraceBuffer[nPos] = 0;
   }
}

/**
 * Writes the log of a SHDLC buffer.
 *
 * @param[in]  pTraceBuffer  A pointer on the buffer where to write the log.
 *
 * @param[in]  nPos  The current position in the trace buffer.
 *
 * @param[in]  pBuffer  The pointer on the buffer to dump.
 *
 * @param[in]  nLength  The length in bytes of the buffer to dump.
 *
 * @param[in]  pTraceContext The trace context.
 **/
static void static_PSHDLCLogSHDLC(
         char* pTraceBuffer,
         uint32_t nPos,
         uint8_t* pBuffer,
         uint32_t nLength,
         tPNALSHDLCTraceContext* pTraceContext)
{
   uint8_t nHeader = *pBuffer;
   bool_t bUnknown = W_FALSE;

   if( (nHeader & 0xC0) == 0x80)
   {
      static_PSHDLCLogPrepareBuffer(pTraceContext, pTraceBuffer, &nPos, nHeader, "I", FLAG_SET_NR | FLAG_SET_NS);
      nPos += PNALUtilLogArray(&pTraceBuffer[nPos], pBuffer+1, nLength - 1);
   }
   else if (nLength == 1)
   {
      if((nHeader & 0xF8) == 0xC0)
      {
         static_PSHDLCLogPrepareBuffer(pTraceContext, pTraceBuffer, &nPos, nHeader, "RR", FLAG_SET_NR | FLAG_CUT);
      }
      else if((nHeader & 0xF8) == 0xC8)
      {
         static_PSHDLCLogPrepareBuffer(pTraceContext, pTraceBuffer, &nPos, nHeader, "REJ", FLAG_SET_NR | FLAG_CUT);
      }
      else if((nHeader & 0xF8) == 0xD0)
      {
         static_PSHDLCLogPrepareBuffer(pTraceContext, pTraceBuffer, &nPos, nHeader, "RNR", FLAG_SET_NR | FLAG_CUT);
      }
      else if((nHeader & 0xF8) == 0xD8)
      {
         static_PSHDLCLogPrepareBuffer(pTraceContext, pTraceBuffer, &nPos, nHeader, "SREJ", FLAG_SET_NR | FLAG_CUT);
      }
      else if(nHeader == 0xEF)
      {
         static_PSHDLCLogPrepareBuffer(pTraceContext, pTraceBuffer, &nPos, nHeader, "UI", FLAG_CUT);
      }
      else if(nHeader == 0xF9)
      {
         static_PSHDLCLogPrepareBuffer(pTraceContext, pTraceBuffer, &nPos, nHeader, "RST", FLAG_CUT);
      }
      else if(nHeader == 0xE6)
      {
         static_PSHDLCLogPrepareBuffer(pTraceContext, pTraceBuffer, &nPos, nHeader, "UA", FLAG_CUT);
      }
      else
      {
         bUnknown = W_TRUE;
      }
   }
   else if((nLength == 2) && (nHeader == 0xF9))
   {
      static_PSHDLCLogPrepareBuffer(pTraceContext, pTraceBuffer, &nPos, nHeader, "RST ", 0);
      pTraceBuffer[nPos++] = 'W';
      pTraceBuffer[nPos++] = 'S';
      pTraceBuffer[nPos++] = ' ';
      if( /* (pBuffer[1] >= 0) && */ (pBuffer[1] <= 9))       /* unsigned, no need to test >= 0 ! */
      {
         pTraceBuffer[nPos++] = pBuffer[1] + '0';
      }
      else
      {
         pTraceBuffer[nPos++] = '?';
      }
      pTraceBuffer[nPos] = 0;
   }
   else if((nLength == 3) && (nHeader == 0xF9))
   {
      static_PSHDLCLogPrepareBuffer(pTraceContext, pTraceBuffer, &nPos, nHeader, "RST ", 0);
      pTraceBuffer[nPos++] = 'W';
      pTraceBuffer[nPos++] = 'S';
      pTraceBuffer[nPos++] = ' ';
      if(/* (pBuffer[1] >= 0) && */ (pBuffer[1] <= 9))        /* unsigned, no need to test >= 0 ! */
      {
         pTraceBuffer[nPos++] = pBuffer[1] + '0';
      }
      else
      {
         pTraceBuffer[nPos++] = '?';
      }
      pTraceBuffer[nPos++] = ' ';
      pTraceBuffer[nPos++] = 'O';
      pTraceBuffer[nPos++] = 'P';
      if(/* (pBuffer[1] >= 0) && */ (pBuffer[1] <= 9))          /* unsigned, no need to test >= 0 ! */
      {
         pTraceBuffer[nPos++] = pBuffer[1] + '0';
      }
      else
      {
         pTraceBuffer[nPos++] = '?';
      }
      pTraceBuffer[nPos] = 0;
   }
   else
   {
      bUnknown = W_TRUE;
   }

   if( bUnknown )
   {
      static_PSHDLCLogPrepareBuffer(pTraceContext, pTraceBuffer, &nPos, nHeader, "<?> ", 0);
      nPos += PNALUtilLogArray(&pTraceBuffer[nPos], pBuffer, nLength);
   }

   if( (pBuffer[0] & 0xC0) == 0x80)
   {
      static_PSHDLCLogHCI(pTraceContext, pTraceBuffer, &pBuffer[1], nLength - 1, nPos);
   }
}

/**
 * Wrapper to PCCDebugPrintTrace
 **/

P_NAL_INLINE static void static_PCCDebugPrintTraceWrapper(
         const char* pTag,
         uint32_t nTraceLevel,
         const char* pMessage, ...)
{
   va_list
   list;
   va_start(list, pMessage);
   CNALDebugPrintTrace(pTag, nTraceLevel, pMessage, list);
   va_end(list);
}

/**
 * Writes the log of the SHDLC and HCI buffer.
 *
 * @param[in]  pBuffer  The pointer on the buffer to dump.
 *
 * @param[in]  nLength  The length in bytes of the buffer to dump.
 *
 * @param[in]  bFromNFCC  The direction flag. W_TRUE for NFCC to NFC Device, W_FALSE for
 *             NFC Device to NFCC.
 **/
void PSHDLCLogBuffer(
         tNALBindingContext* pBindingContext,
         uint8_t* pBuffer,
         uint32_t nLength,
         bool_t bFromNFCC)
{
   tPNALSHDLCTraceContext* pTraceContext = PNALContextGetSHDLCTraceContext(pBindingContext);
   char* pTraceBuffer = pTraceContext->aTraceBuffer;
   uint32_t nPos = 0;
   pTraceContext->bFromNFCC = bFromNFCC;
   if(bFromNFCC)
   {
      if((pTraceContext->nLastHeader != pBuffer[0])
      || (pTraceContext->nLastLength != nLength))
      {
         pTraceContext->nLastHeader = pBuffer[0];
         pTraceContext->nLastLength = nLength;
         PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "ND<-NFCC " );
         static_PSHDLCLogSHDLC(pTraceBuffer, nPos, pBuffer, nLength, pTraceContext);
      }
      else
      {
         PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "ND<-NFCC bis" );
      }
   }
   else
   {
      PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "ND->NFCC " );
      static_PSHDLCLogSHDLC(pTraceBuffer, nPos, pBuffer, nLength, pTraceContext);
   }

   static_PCCDebugPrintTraceWrapper("SHDLC", P_TRACE_LOG, pTraceBuffer);
}

#endif /* #ifdef P_SHDLC_TRACE */

#endif /* #ifdef P_NAL_TRACE_ACTIVE */
