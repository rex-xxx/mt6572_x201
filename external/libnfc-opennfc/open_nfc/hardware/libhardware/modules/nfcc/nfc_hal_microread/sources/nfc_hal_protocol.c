/*
 * Copyright (c) 2007-2011 Inside Secure, All Rights Reserved.
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

#define P_MODULE  P_MODULE_DEC( PROTO )

#include "nfc_hal_binding.h"

/* Reader Mode */
#define P_NFC_HAL_BINDING_READER_CONFIG_SIZE_ISO_14443_4_A        4
#define P_NFC_HAL_BINDING_READER_CONFIG_MIN_SIZE_ISO_14443_3_B    5
#define P_NFC_HAL_BINDING_READER_CONFIG_MAX_SIZE_ISO_14443_3_B    250
#define P_NFC_HAL_BINDING_READER_CONFIG_SIZE_ISO_15_3             1
#define P_NFC_HAL_BINDING_READER_CONFIG_SIZE_FELICA               2
#define P_NFC_HAL_BINDING_READER_CONFIG_MIN_SIZE_P2P_INITIATOR    4

/* Card Mode */
#define P_NFC_HAL_BINDING_CARD_CONFIG_MIN_SIZE_ISO_14443_4_A      16
#define P_NFC_HAL_BINDING_CARD_CONFIG_MAX_SIZE_ISO_14443_4_A      (16 + 252)
#define P_NFC_HAL_BINDING_CARD_CONFIG_MIN_SIZE_ISO_14443_4_B      11
#define P_NFC_HAL_BINDING_CARD_CONFIG_MAX_SIZE_ISO_14443_4_B      (11 + 253)
#define P_NFC_HAL_BINDING_CARD_CONFIG_MIN_SIZE_P2P_TARGET         6

#define P_NFC_HAL_BINDING_CARD_CONFIG_UID_SIZE                    0x0A
#define P_NFC_HAL_BINDING_CARD_CONFIG_ATQB_SIZE                   0x0B

#define P_NFC_HAL_BINDING_READER_CONFIG_MIN_SIZE_B_PRIME          3
#define P_NFC_HAL_BINDING_READER_CONFIG_MAX_SIZE_B_PRIME          4

#define P_NFC_HAL_BINDING_ROUTING_TABLE_CONFIG_SIZE               2

/* default link baud rate */
static const uint32_t g_nDefaultLinkBaudRate = 424;


static const uint8_t static_aInvertionByte[] = {
   0x00, 0x80, 0x40, 0xC0, 0x20, 0xA0, 0x60, 0xE0, 0x10, 0x90, 0x50, 0xD0, 0x30, 0xB0, 0x70, 0xF0,
   0x08, 0x88, 0x48, 0xC8, 0x28, 0xA8, 0x68, 0xE8, 0x18, 0x98, 0x58, 0xD8, 0x38, 0xB8, 0x78, 0xF8,
   0x04, 0x84, 0x44, 0xC4, 0x24, 0xA4, 0x64, 0xE4, 0x14, 0x94, 0x54, 0xD4, 0x34, 0xB4, 0x74, 0xF4,
   0x0C, 0x8C, 0x4C, 0xCC, 0x2C, 0xAC, 0x6C, 0xEC, 0x1C, 0x9C, 0x5C, 0xDC, 0x3C, 0xBC, 0x7C, 0xFC,
   0x02, 0x82, 0x42, 0xC2, 0x22, 0xA2, 0x62, 0xE2, 0x12, 0x92, 0x52, 0xD2, 0x32, 0xB2, 0x72, 0xF2,
   0x0A, 0x8A, 0x4A, 0xCA, 0x2A, 0xAA, 0x6A, 0xEA, 0x1A, 0x9A, 0x5A, 0xDA, 0x3A, 0xBA, 0x7A, 0xFA,
   0x06, 0x86, 0x46, 0xC6, 0x26, 0xA6, 0x66, 0xE6, 0x16, 0x96, 0x56, 0xD6, 0x36, 0xB6, 0x76, 0xF6,
   0x0E, 0x8E, 0x4E, 0xCE, 0x2E, 0xAE, 0x6E, 0xEE, 0x1E, 0x9E, 0x5E, 0xDE, 0x3E, 0xBE, 0x7E, 0xFE,
   0x01, 0x81, 0x41, 0xC1, 0x21, 0xA1, 0x61, 0xE1, 0x11, 0x91, 0x51, 0xD1, 0x31, 0xB1, 0x71, 0xF1,
   0x09, 0x89, 0x49, 0xC9, 0x29, 0xA9, 0x69, 0xE9, 0x19, 0x99, 0x59, 0xD9, 0x39, 0xB9, 0x79, 0xF9,
   0x05, 0x85, 0x45, 0xC5, 0x25, 0xA5, 0x65, 0xE5, 0x15, 0x95, 0x55, 0xD5, 0x35, 0xB5, 0x75, 0xF5,
   0x0D, 0x8D, 0x4D, 0xCD, 0x2D, 0xAD, 0x6D, 0xED, 0x1D, 0x9D, 0x5D, 0xDD, 0x3D, 0xBD, 0x7D, 0xFD,
   0x03, 0x83, 0x43, 0xC3, 0x23, 0xA3, 0x63, 0xE3, 0x13, 0x93, 0x53, 0xD3, 0x33, 0xB3, 0x73, 0xF3,
   0x0B, 0x8B, 0x4B, 0xCB, 0x2B, 0xAB, 0x6B, 0xEB, 0x1B, 0x9B, 0x5B, 0xDB, 0x3B, 0xBB, 0x7B, 0xFB,
   0x07, 0x87, 0x47, 0xC7, 0x27, 0xA7, 0x67, 0xE7, 0x17, 0x97, 0x57, 0xD7, 0x37, 0xB7, 0x77, 0xF7,
   0x0F, 0x8F, 0x4F, 0xCF, 0x2F, 0xAF, 0x6F, 0xEF, 0x1F, 0x9F, 0x5F, 0xDF, 0x3F, 0xBF, 0x7F, 0xFF};

static void static_PNALProtocolDecode_Kovio(
            uint8_t * pOutput,
            const uint8_t * pInput,
            uint32_t length)
{
   uint8_t bBegin = 1;
   uint8_t tmp;

   while(length-- > 0)
   {
      tmp = ((*pInput & 0x80) == 0x80);
      *pOutput = ((*pInput << 1) & 0xFE) | bBegin;
      *pOutput = static_aInvertionByte[*pOutput];

      bBegin = tmp;

      pOutput++;
      pInput++;
   }
}



/* See header file */
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_POLICY(
               const uint8_t* pBuffer,
               uint32_t nLength,
               bool_t* pbIsReaderLocked,
               bool_t* pbIsCardLocked,
               uint16_t* pnUICCCardPolicy,
               uint16_t* pnUICCReaderPolicy,
               uint32_t* pnSESwitchPosition,
               uint32_t* pnSESlotIdentifier,
               uint16_t* pnSECardPolicy,
               uint32_t* pnBattOff)
{
   uint16_t nFlags = 0;

   if (nLength != NAL_POLICY_SIZE)
   {
      return NAL_RES_BAD_LENGTH;;
   }

   if((pbIsReaderLocked != null)
   || (pbIsCardLocked != null)
   || (pnSESwitchPosition != null)
   || (pnSESlotIdentifier != null))
   {
      nFlags = static_PNALBindingReadUint16FromNALBuffer(&pBuffer[4]);
   }

   if(pbIsReaderLocked != null)
   {
      *pbIsReaderLocked = ((nFlags & NAL_POLICY_FLAG_READER_LOCK) == 0)?W_TRUE:W_FALSE;
   }

   if(pbIsCardLocked != null)
   {
      *pbIsCardLocked = ((nFlags & NAL_POLICY_FLAG_CARD_LOCK) == 0)?W_TRUE:W_FALSE;
   }

   if(pnUICCCardPolicy != null)
   {
      *pnUICCCardPolicy = static_PNALBindingReadUint16FromNALBuffer(&pBuffer[0]);
   }

   if(pnUICCReaderPolicy != null)
   {
      *pnUICCReaderPolicy = static_PNALBindingReadUint16FromNALBuffer(&pBuffer[2]);
   }

   if(pnSESwitchPosition != null)
   {
      *pnSESwitchPosition = nFlags & NAL_POLICY_FLAG_SE_MASK;
   }

   if(pnSESlotIdentifier != null)
   {
      *pnSESlotIdentifier = (nFlags & NAL_POLICY_FLAG_SE_ID_MASK) >> 4;
   }

   if (pnBattOff != null)
   {
      * pnBattOff = (nFlags & NAL_POLICY_FLAG_BATT_OFF_MASK);
   }

   if(pnSECardPolicy != null)
   {
      uint16_t nSECardPolicy;

      nSECardPolicy = static_PNALBindingReadUint16FromNALBuffer(&pBuffer[6]);

      switch (nSECardPolicy)
      {
         /* only the following combination are allowed */
         case 0:
         case NAL_PROTOCOL_CARD_ISO_14443_4_A:
         case NAL_PROTOCOL_CARD_ISO_14443_4_A | NAL_PROTOCOL_CARD_MIFARE_CLASSIC:
         case NAL_PROTOCOL_CARD_ISO_14443_4_A | NAL_PROTOCOL_CARD_ISO_15693_2:
         case NAL_PROTOCOL_CARD_ISO_14443_4_A | NAL_PROTOCOL_CARD_MIFARE_CLASSIC | NAL_PROTOCOL_CARD_ISO_15693_2:
         case NAL_PROTOCOL_CARD_ISO_14443_4_B:
         case NAL_PROTOCOL_CARD_ISO_14443_4_B | NAL_PROTOCOL_CARD_ISO_15693_2:
         case NAL_PROTOCOL_CARD_ISO_15693_2:
            break;

         default:
            /* invalid policy */
            return NAL_RES_FEATURE_NOT_SUPPORTED;
      }

      *pnSECardPolicy = nSECardPolicy;
   }

   return NAL_RES_OK;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_PAR_POLICY(
               uint8_t* pBuffer,
               bool_t bIsReaderLocked,
               bool_t bIsCardLocked,
               uint16_t nUICCCardPolicy,
               uint16_t nUICCReaderPolicy,
               uint32_t nSESwitchPosition,
               uint32_t nSESlotIdentifier,
               uint16_t nSECardPolicy)
{
   uint16_t nFlags = 0;
   uint32_t nIndex = 0;

   static_PNALBindingWriteUint16ToNALBuffer(nUICCCardPolicy, pBuffer + nIndex);
   nIndex += 2;

   static_PNALBindingWriteUint16ToNALBuffer(nUICCReaderPolicy, pBuffer + nIndex);
   nIndex += 2;

   if (bIsReaderLocked == W_FALSE)
   {
      nFlags |= NAL_POLICY_FLAG_READER_LOCK;
   }

   if (bIsCardLocked == W_FALSE)
   {
      nFlags |= NAL_POLICY_FLAG_CARD_LOCK;
   }

   CNALDebugAssert((nSESwitchPosition | NAL_POLICY_FLAG_SE_MASK) == NAL_POLICY_FLAG_SE_MASK);
   nFlags |= nSESwitchPosition;

   CNALDebugAssert((nSESlotIdentifier | NAL_POLICY_FLAG_SE_ID_MASK) == NAL_POLICY_FLAG_SE_ID_MASK);
   nFlags |= nSESlotIdentifier;

   static_PNALBindingWriteUint16ToNALBuffer(nFlags, pBuffer + nIndex);
   nIndex += 2;

   static_PNALBindingWriteUint16ToNALBuffer(nSECardPolicy, pBuffer + nIndex);
   nIndex += 2;

   return nIndex;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_PAR_HARDWARE_INFO(
               uint8_t* pBuffer,
               const tNALParHardwareInfo *pNALParHardwareInfo)
{
   uint32_t nIndex = 0;
   uint32_t nBytePos, nTempPos;

   pBuffer[nIndex++] = NAL_VERSION;

   if(pNALParHardwareInfo->nSEType == P_NFC_HAL_SE_NONE)
   {
      CNALMemoryCopy(&pBuffer[nIndex], "Microread v", 11);
      PNALUtilWriteVersion(&pBuffer[nIndex + 11], pNALParHardwareInfo->aHardwareVersion);
   }
   else
   {
      CNALMemoryCopy(&pBuffer[nIndex], "Securead v", 10);
      PNALUtilWriteVersion(&pBuffer[nIndex + 10], pNALParHardwareInfo->aHardwareVersion);
   }

   nIndex += NAL_HARDWARE_TYPE_STRING_SIZE;

   nTempPos = nIndex;
   for(nBytePos = 0; nBytePos < P_NFC_HAL_HARDWARE_SERIAL_NUMBER_SIZE; nBytePos++)
   {
      nTempPos += PNALUtilWriteHexaUint8(
         &pBuffer[nTempPos], pNALParHardwareInfo->aHardwareSerialNumber[nBytePos]);
   }
   nIndex += NAL_HARDWARE_SERIAL_NUMBER_STRING_SIZE;

   CNALMemoryCopy(&pBuffer[nIndex], "Loader v", 8);
   PNALUtilWriteVersion(&pBuffer[nIndex + 8], pNALParHardwareInfo->aLoaderVersion);
   nIndex += NAL_LOADER_DESCRIPTION_STRING_SIZE;

   pBuffer[nIndex++] = pNALParHardwareInfo->nFirmwareStatus;

   CNALMemoryFill(pBuffer + nIndex, 0, 4 * (NAL_SE_DESCRIPTION_STRING_SIZE + 2 + 2 + 2));

   if(pNALParHardwareInfo->nSEType != P_NFC_HAL_SE_NONE)
   {
      uint16_t value;

      pBuffer[nIndex++] = 1; /* one SE */

      /* Hardcoding of the Jupiter features */
      CNALMemoryCopy(&pBuffer[nIndex], "SLE97-144SE", 11);
      nIndex += NAL_SE_DESCRIPTION_STRING_SIZE;

      /* Capabilities */
      value = NAL_SE_FLAG_END_OF_TRANSACTION_NOTIFICATION;
      static_PNALBindingWriteUint16ToNALBuffer(value, &pBuffer[nIndex]);
      nIndex += 2;

      /* Host Interface */
      value = NAL_PROTOCOL_READER_ISO_14443_4_B;
      static_PNALBindingWriteUint16ToNALBuffer(value, &pBuffer[nIndex]);
      nIndex += 2;

      /* RF Interface */
      value = NAL_PROTOCOL_CARD_ISO_14443_4_A | NAL_PROTOCOL_CARD_ISO_14443_4_B | NAL_PROTOCOL_CARD_ISO_15693_2 | NAL_PROTOCOL_CARD_MIFARE_CLASSIC;
      static_PNALBindingWriteUint16ToNALBuffer(value, &pBuffer[nIndex]);
      nIndex += 2;
   }

   return 0xFB;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_PAR_FIRMWARE_INFO(
               uint8_t* pBuffer,
               const tNALParFirmwareInfo *pNALParFirmwareInfo,
               uint16_t nAutoStandbyTimeout)
{
   uint32_t nIndex = 0;
   uint32_t nPos = 0;
   uint16_t nVersion;

   /* Build the NFC HAL payload */
   pBuffer[nIndex++] = NAL_VERSION;

   CNALMemoryCopy(&pBuffer[nIndex], "Firmware v", 10);
   nPos += 10;

   nPos += PNALUtilWriteVersion(&pBuffer[nIndex + nPos], pNALParFirmwareInfo->aFirmwareVersion);
   CNALMemoryCopy(&pBuffer[nIndex + nPos], " (", 2);
   nPos += 2;

   /* ROM family */
   switch (pNALParFirmwareInfo->aFirmwareTrackingId[0])
   {
      case 5 :
         CNALMemoryCopy(&pBuffer[nIndex + nPos], "3.0 ", 4);
         break;

      case 6:
         CNALMemoryCopy(&pBuffer[nIndex + nPos], "3.2 ", 4);
         break;

      case 7:
         CNALMemoryCopy(&pBuffer[nIndex + nPos], "3.4 ", 4);
         break;

      case 8:
         CNALMemoryCopy(&pBuffer[nIndex + nPos], "3.5 ", 4);
         break;

      default:
         CNALMemoryCopy(&pBuffer[nIndex + nPos], "??? ", 4);
         break;
   }

   nPos += 4;

   /* Destination */
   CNALMemoryCopy(&pBuffer[nIndex + nPos], & pNALParFirmwareInfo->aFirmwareTrackingId[1], 2);
   nPos += 2;
   pBuffer[nIndex + nPos++] = ' ';

   /* Delivery */
   nPos += PNALUtilWriteHexaUint8(&pBuffer[nIndex + nPos], pNALParFirmwareInfo->aFirmwareTrackingId[3]);
   pBuffer[nIndex + nPos++] = ' ';

   /* Trunk */
   nVersion = static_PNALBindingReadUint16FromHCIBuffer(& pNALParFirmwareInfo->aFirmwareTrackingId[4]);
   nPos += PNALUtilWriteDecimalUint32(&pBuffer[nIndex + nPos], nVersion);

   pBuffer[nIndex + nPos++] = ')';
   pBuffer[nIndex + nPos++ ] = 0;
   nIndex += NAL_FIRMWARE_DESCRIPTION_STRING_SIZE;

   CNALMemoryCopy(
     &pBuffer[nIndex],
     pNALParFirmwareInfo->aCardProtocolCapabilities,
     P_NFC_HAL_BINDING_PROTOCOL_CAPABILITIES_SIZE);
   nIndex += P_NFC_HAL_BINDING_PROTOCOL_CAPABILITIES_SIZE;

   CNALMemoryCopy(
     &pBuffer[nIndex],
     pNALParFirmwareInfo->aReaderProtocolCapabilities,
     P_NFC_HAL_BINDING_PROTOCOL_CAPABILITIES_SIZE);
   nIndex += P_NFC_HAL_BINDING_PROTOCOL_CAPABILITIES_SIZE;

   CNALMemoryCopy(
     &pBuffer[nIndex],
     pNALParFirmwareInfo->aFirmwareCapabilities,
     P_NFC_HAL_BINDING_FIRMWARE_CAPABILITIES_SIZE);
   nIndex += P_NFC_HAL_BINDING_FIRMWARE_CAPABILITIES_SIZE;

   pBuffer[nIndex++] = pNALParFirmwareInfo->nReaderISOADataRateMax;
   pBuffer[nIndex++] = pNALParFirmwareInfo->nReaderISOAInputBufferSize;
   pBuffer[nIndex++] = pNALParFirmwareInfo->nReaderISOBDataRateMax;
   pBuffer[nIndex++] = pNALParFirmwareInfo->nReaderISOBInputBufferSize;
   pBuffer[nIndex++] = pNALParFirmwareInfo->nCardISOADataRateMax;
   pBuffer[nIndex++] = pNALParFirmwareInfo->nCardISOBDataRateMax;

   static_PNALBindingWriteUint16ToNALBuffer( nAutoStandbyTimeout,
                                             &pBuffer[nIndex]);
   nIndex += 2;

   return nIndex;
}

/* See header file */
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_DETECT_PULSE(
               const uint8_t* pBuffer,
               uint32_t nLength,
               uint16_t* pnCardDetectPulse)
{
   if (nLength != NAL_PAR_DETECT_PULSE_SIZE)
   {
      return NAL_RES_BAD_LENGTH;
   }

   * pnCardDetectPulse = static_PNALBindingReadUint16FromNALBuffer(pBuffer);
   return NAL_RES_OK;
}

/* See header file */
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_PERSISTENT_MEMORY(
                        const uint8_t  *pBuffer,
                        uint32_t nLength,
                        uint8_t **ppPersistentStorage)
{
   if (nLength != NAL_PERSISTENT_MEMORY_SIZE)
   {
      return NAL_RES_BAD_LENGTH;
   }
   else
   {
      * ppPersistentStorage = (uint8_t *) pBuffer;
   }

   return NAL_RES_OK;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_PAR_PERSISTENT_MEMORY(
                        uint8_t  *pBuffer,
                        const uint8_t aPersistentStorage[NAL_PERSISTENT_MEMORY_SIZE])

{
   if (aPersistentStorage != null)
   {
      CNALMemoryCopy(pBuffer, aPersistentStorage, NAL_PERSISTENT_MEMORY_SIZE);
   }

   return NAL_PERSISTENT_MEMORY_SIZE;
}

/* See header file */
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_READER_CONFIG_14_A_4(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        uint8_t* pnMaxDataRate,
                        bool_t* pbIsCID,
                        uint8_t* pnCID,
                        uint8_t* pnFSD)
{
   if (nLength != P_NFC_HAL_BINDING_READER_CONFIG_SIZE_ISO_14443_4_A)
   {
      return NAL_RES_BAD_LENGTH;
   }
   else
   {
      uint32_t nIndex = 0;

      if(pnMaxDataRate != null)
      {
         *pnMaxDataRate = pBuffer[nIndex];
      }
      nIndex++;

      if(pbIsCID != null)
      {
         *pbIsCID = (pBuffer[nIndex] == 1)?W_TRUE:W_FALSE;
      }
      nIndex++;

      if(pnCID != null)
      {
         *pnCID = pBuffer[nIndex];
      }
      nIndex++;

      if(pnFSD != null)
      {
         *pnFSD = pBuffer[nIndex];
      }
   }

   return NAL_RES_OK;
}

/* See header file */
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_READER_CONFIG_14_B_3(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        uint8_t* pnMaxDataRate,
                        uint8_t* pnAFI,
                        bool_t* pbIsCID,
                        uint8_t* pnCID,
                        uint8_t* pnFSD,
                        uint8_t** pHigherLayerData,
                        uint32_t* nHigherLayerDataLength)
{
   if ((nLength < P_NFC_HAL_BINDING_READER_CONFIG_MIN_SIZE_ISO_14443_3_B) ||
       (nLength > P_NFC_HAL_BINDING_READER_CONFIG_MAX_SIZE_ISO_14443_3_B))
   {
      return NAL_RES_BAD_LENGTH;
   }
   else
   {
      uint32_t nIndex = 0;

      if(pnMaxDataRate != null)
      {
         *pnMaxDataRate = pBuffer[nIndex];
      }
      nIndex++;

      if(pnAFI != null)
      {
         *pnAFI = pBuffer[nIndex];
      }
      nIndex++;

      if(pbIsCID != null)
      {
         *pbIsCID = (pBuffer[nIndex] == 1)?W_TRUE:W_FALSE;
      }
      nIndex++;

      if(pnCID != null)
      {
         *pnCID = pBuffer[nIndex];
      }
      nIndex++;

      if(pnFSD != null)
      {
         *pnFSD = pBuffer[nIndex];
      }
      nIndex++;

      if(pHigherLayerData != null)
      {
         *pHigherLayerData = (uint8_t *) &pBuffer[nIndex];
         *nHigherLayerDataLength = (nLength - P_NFC_HAL_BINDING_READER_CONFIG_MIN_SIZE_ISO_14443_3_B);
      }
   }

   return NAL_RES_OK;
}

/* See header file */
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_READER_CONFIG_15_3(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        uint8_t* pnAFI)
{
   if (nLength < P_NFC_HAL_BINDING_READER_CONFIG_SIZE_ISO_15_3)
   {
      return NAL_RES_BAD_LENGTH;
   }
   else
   {
      if(pnAFI != null)
      {
         *pnAFI = pBuffer[0];
      }
   }

   return NAL_RES_OK;
}

/* See header file */
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_READER_CONFIG_FELICA(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        uint16_t* pnSystemCode)
{
   if (nLength != P_NFC_HAL_BINDING_READER_CONFIG_SIZE_FELICA)
   {
      return NAL_RES_BAD_LENGTH;
   }
   else
   {
      if(pnSystemCode != null)
      {
         * pnSystemCode = static_PNALBindingReadUint16FromNALBuffer(pBuffer);
      }
   }

   return NAL_RES_OK;
}

/* See header file */
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_READER_CONFIG_B_PRIME(
                        const uint8_t* pBuffer,
                        uint32_t nLength)
{
   if( (nLength >= P_NFC_HAL_BINDING_READER_CONFIG_MIN_SIZE_B_PRIME)
    && (nLength <= P_NFC_HAL_BINDING_READER_CONFIG_MAX_SIZE_B_PRIME))
   {
      return NAL_RES_OK;
   }

   return NAL_RES_BAD_LENGTH;
}

/* See header file */
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_CARD_CONFIG_14_A_4(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        uint8_t* pnUID,
                        uint8_t* pnUIDLen,
                        uint8_t* pnATQAMsb,
                        uint8_t* pnTO,
                        uint8_t* pnTA,
                        uint8_t* pnTB,
                        uint8_t* pnTC,
                        uint8_t** pnApplicationData,
                        uint32_t* pnApplicationDataLength)
{
   if ((nLength < P_NFC_HAL_BINDING_CARD_CONFIG_MIN_SIZE_ISO_14443_4_A) ||
       (nLength > P_NFC_HAL_BINDING_CARD_CONFIG_MAX_SIZE_ISO_14443_4_A))
   {
      return NAL_RES_BAD_LENGTH;
   }
   else
   {
      uint32_t nIndex = 0;
      if(pnUID != null)
      {
         CNALMemoryCopy(pnUID, pBuffer, P_NFC_HAL_BINDING_CARD_CONFIG_UID_SIZE);
      }
      nIndex += P_NFC_HAL_BINDING_CARD_CONFIG_UID_SIZE;

      if(pnUIDLen != null)
      {
         *pnUIDLen = pBuffer[nIndex];
      }
      nIndex++;

      if(pnATQAMsb != null)
      {
         *pnATQAMsb = pBuffer[nIndex];
      }
      nIndex++;

      if(pnTO != null)
      {
         *pnTO = pBuffer[nIndex];
      }
      nIndex++;

      if(pnTA != null)
      {
         *pnTA = pBuffer[nIndex];
      }
      nIndex++;

      if(pnTB != null)
      {
         *pnTB = pBuffer[nIndex];
      }
      nIndex++;

      if(pnTC != null)
      {
         *pnTC = pBuffer[nIndex];
      }
      nIndex++;

      if(pnApplicationData != null)
      {
         *pnApplicationData = (uint8_t *) &pBuffer[nIndex];
         *pnApplicationDataLength = (nLength - P_NFC_HAL_BINDING_CARD_CONFIG_MIN_SIZE_ISO_14443_4_A);
      }
   }

   return NAL_RES_OK;
}

/* See header file */
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_CARD_CONFIG_14_B_4(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        uint8_t* pATQB,
                        uint8_t** ppHigherLayerResponseData,
                        uint32_t* pnHigherLayerResponseDataLength)
{
   if ((nLength < P_NFC_HAL_BINDING_CARD_CONFIG_MIN_SIZE_ISO_14443_4_B) ||
       (nLength > P_NFC_HAL_BINDING_CARD_CONFIG_MAX_SIZE_ISO_14443_4_B))
   {
      return NAL_RES_BAD_LENGTH;
   }
   else
   {
      uint32_t nIndex = 0;

      if(pATQB != null)
      {
         CNALMemoryCopy(pATQB, pBuffer, P_NFC_HAL_BINDING_CARD_CONFIG_ATQB_SIZE);
      }
      nIndex += P_NFC_HAL_BINDING_CARD_CONFIG_ATQB_SIZE;

      if(ppHigherLayerResponseData != null)
      {
         *ppHigherLayerResponseData = (uint8_t *) &pBuffer[nIndex];
         *pnHigherLayerResponseDataLength = (nLength - P_NFC_HAL_BINDING_CARD_CONFIG_ATQB_SIZE);
      }
   }

   return NAL_RES_OK;
}

/* See header file */
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_P2P_INITIATOR_LINK_PARAMETERS(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        uint8_t** pParameters,
                        uint32_t * pnParametersLength)

{
   * pParameters = (uint8_t *) pBuffer;
   * pnParametersLength = nLength;

   return NAL_RES_OK;
}

/* See header file */
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_CARD_CONFIG_P2P_TARGET(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        uint32_t* pnRTX,
                        bool_t* pbAllowTypeATargetProtocol,
                        bool_t* pbAllowActiveMode,
                        uint8_t** pParameters,
                        uint32_t * pnParametersLength)
{
   uint32_t nIndex = 0;

   if (nLength < P_NFC_HAL_BINDING_CARD_CONFIG_MIN_SIZE_P2P_TARGET)
   {
      *pnRTX = 0x08;
      *pbAllowTypeATargetProtocol = W_TRUE;
      *pbAllowActiveMode = W_TRUE;
      *pnParametersLength = 0;
      return NAL_RES_BAD_LENGTH;
   }
   else
   {
      *pnRTX = static_PNALBindingReadUint32FromNALBuffer(pBuffer);
      nIndex += 4;
      *pbAllowTypeATargetProtocol = (pBuffer[nIndex++] != 0);
      *pbAllowActiveMode = (pBuffer[nIndex++] != 0);
      *pParameters = (uint8_t *) &pBuffer[nIndex];
      *pnParametersLength = nLength - nIndex;
   }

   return NAL_RES_OK;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_PAR_UICC_SWP(
                        uint8_t* pBuffer,
                        uint32_t nStatus)
{
   pBuffer[0] = (uint8_t) nStatus;

   return 1;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_PAR_UICC_READER_PROTOCOLS(
                        uint8_t* pBuffer,
                        bool_t bDetect,
                        uint16_t nOpenProtocols,
                        uint8_t nDataRateMax,
                        uint8_t nAFI,
                        const uint8_t* pnHigherLayerData,
                        uint32_t nHigherLayerDataLength)
{
   uint32_t nIndex = 0;

   pBuffer[nIndex++] = (bDetect==W_TRUE)?1:0;

   static_PNALBindingWriteUint16ToNALBuffer(nOpenProtocols, &pBuffer[nIndex]);
   nIndex += 2;

   pBuffer[nIndex++] = nDataRateMax;
   pBuffer[nIndex++] = nAFI;

   if(pnHigherLayerData != null)
   {
      CNALMemoryCopy(&pBuffer[nIndex], pnHigherLayerData, nHigherLayerDataLength);
      nIndex += nHigherLayerDataLength;
   }

   return nIndex;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_PAR_UICC_CARD_PROTOCOLS(
                        uint8_t* pBuffer,
                        uint16_t nOpenProtocols,
                        uint8_t nCardA4Mode,
                        const uint8_t* pnUID,
                        uint8_t nUIDLength,
                        uint8_t nSAK,
                        uint16_t nATQA,
                        const uint8_t* pnApplicationData,
                        uint8_t nFWI_SFGI,
                        uint8_t nCIDSupport,
                        uint8_t nDataRateMax,
                        uint8_t nCardB4Mode,
                        uint32_t nPUPI,
                        uint8_t nAFI,
                        uint32_t nATQB,
                        const uint8_t* pnHigherLayerResponse)
{
   uint32_t nIndex = 0;

   static_PNALBindingWriteUint16ToNALBuffer(nOpenProtocols, &pBuffer[nIndex]);
   nIndex += 2;

   pBuffer[nIndex++] = nCardA4Mode;

   if (pnUID != null)
   {
      CNALMemoryCopy(&pBuffer[nIndex], pnUID, 10);
   }
   nIndex += 10;

   pBuffer[nIndex++] = nUIDLength;
   pBuffer[nIndex++] = nSAK;
   static_PNALBindingWriteUint16ToNALBuffer(nATQA, &pBuffer[nIndex]);
   nIndex += 2;

   if (pnApplicationData != null)
   {
      CNALMemoryCopy(&pBuffer[nIndex], pnApplicationData, 20);
   }
   nIndex += 20;

   pBuffer[nIndex++] = nFWI_SFGI;
   pBuffer[nIndex++] = nCIDSupport;
   pBuffer[nIndex++] = nDataRateMax;
   pBuffer[nIndex++] = nCardB4Mode;

   static_PNALBindingWriteUint32ToNALBuffer(nPUPI, &pBuffer[nIndex]);
   nIndex += 4;

   pBuffer[nIndex++] = nAFI;

   static_PNALBindingWriteUint32ToNALBuffer(nATQB, &pBuffer[nIndex]);
   nIndex += 4;

   if (pnHigherLayerResponse != null)
   {
      CNALMemoryCopy(&pBuffer[nIndex], pnHigherLayerResponse, 20);
   }
   nIndex += 20;

   return nIndex;
}


NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_ROUTING_TABLE_CONFIG(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        bool_t* pbIsEnabled)
{
   uint16_t nRoutingTableConfig;

   if (nLength != P_NFC_HAL_BINDING_ROUTING_TABLE_CONFIG_SIZE)
   {
      return NAL_RES_BAD_LENGTH;
   }

   nRoutingTableConfig = static_PNALBindingReadUint16FromNALBuffer(pBuffer);

   if (nRoutingTableConfig & 0x0001)
   {
      * pbIsEnabled = W_TRUE;
   }
   else
   {
      * pbIsEnabled = W_FALSE;
   }

   return NAL_RES_OK;
}


NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_PAR_ROUTING_TABLE_CONFIG(
                        uint8_t* pBuffer,
                        bool_t   bIsEnabled)
{
   if (bIsEnabled != W_FALSE)
   {
      static_PNALBindingWriteUint16ToNALBuffer(0x0001, pBuffer);
   }
   else
   {
      static_PNALBindingWriteUint16ToNALBuffer(0x0000, pBuffer);
   }

   return 2;
}

#define NAL_ROUTING_ENTRY_TARGET_MASK         0x07

#define NAL_ROUTING_ENTRY_TARGET_HANDSET         0
#define NAL_ROUTING_ENTRY_TARGET_UICC            1
#define NAL_ROUTING_ENTRY_TARGET_SE_0            2
#define NAL_ROUTING_ENTRY_TARGET_SE_1            3
#define NAL_ROUTING_ENTRY_TARGET_SE_2            4
#define NAL_ROUTING_ENTRY_TARGET_SE_3            5

#define NAL_ROUTING_ENTRY_POWER_BAT_OFF      0x08
#define NAL_ROUTING_ENTRY_POWER_PHONE_OFF      0x10
#define NAL_ROUTING_ENTRY_POWER_PHONE_ON      0x20

#define HCI_ROUTING_ENTRY_TARGET_MASK         0x03
#define HCI_ROUTING_ENTRY_POWER_BATT_OFF      0x04
#define HCI_ROUTING_ENTRY_POWER_PHONE_OFF      0x08
#define HCI_ROUTING_ENTRY_POWER_PHONE_ON      0x10

NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_ROUTING_TABLE_ENTRIES(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        uint8_t* pRoutingTable,
                        uint8_t * pnLength)
{
   uint32_t nIndex = 0;
   uint8_t  nAIdLength;
   uint8_t  nFlags;
   uint8_t  nResult = NAL_RES_BAD_LENGTH;

   /* Routing tables entries are in stored in a LV format.
      Check that the encoding is consistent (e.g the total size if the sum of items length */


   while (nIndex  + 1 < nLength)
   {
      nFlags = pBuffer[nIndex];
      nAIdLength = pBuffer[nIndex + 1];

		pRoutingTable[nIndex] = nAIdLength + 1;

      switch (nFlags & NAL_ROUTING_ENTRY_TARGET_MASK)
      {
         case NAL_ROUTING_ENTRY_TARGET_HANDSET :
            pRoutingTable[nIndex+1] = ETSI_HOST_HDS;
            break;

         case NAL_ROUTING_ENTRY_TARGET_UICC :
            pRoutingTable[nIndex+1] = ETSI_HOST_UICC;
            break;

         case NAL_ROUTING_ENTRY_TARGET_SE_0 :
            pRoutingTable[nIndex+1] = ETSI_HOST_RFU3;
            break;

         default:
            return NAL_RES_FEATURE_NOT_SUPPORTED;
      }

      if (nFlags & NAL_ROUTING_ENTRY_POWER_BAT_OFF)
      {
         pRoutingTable[nIndex+1] |= HCI_ROUTING_ENTRY_POWER_BATT_OFF;
      }

      if (nFlags & NAL_ROUTING_ENTRY_POWER_PHONE_ON)
      {
         pRoutingTable[nIndex+1] |= HCI_ROUTING_ENTRY_POWER_PHONE_ON;
      }

      if (nFlags & NAL_ROUTING_ENTRY_POWER_PHONE_OFF)
      {
         pRoutingTable[nIndex+1] |= HCI_ROUTING_ENTRY_POWER_PHONE_OFF;
      }

      CNALMemoryCopy(&pRoutingTable[nIndex + 2], & pBuffer[nIndex + 2], nAIdLength);

      nIndex += 2 + nAIdLength;
   }

   if (nIndex == nLength)
   {
      * pnLength = (uint8_t) nLength;
      /* Ok, parsing is fine */
      nResult =  NAL_RES_OK;
   }
   else
   {
      PNALDebugError("Error during parsing of the routing table entry");
   }

   return nResult;
}


NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_PAR_ROUTING_TABLE_ENTRIES(
                        uint8_t* pBuffer,
                        const uint8_t* pRoutingTable,
                        uint8_t  nRoutingTableLength)
{
   uint32_t nIndex = 0;
   uint8_t  nAIdLength;
   uint8_t  nFlags;

   while (nIndex  + 1 < nRoutingTableLength)
   {
		nAIdLength = pRoutingTable[nIndex] - 1;
      nFlags = pRoutingTable[nIndex+1];
      
      switch (nFlags & HCI_ROUTING_ENTRY_TARGET_MASK)
      {
         case ETSI_HOST_HDS:
            pBuffer[nIndex] = NAL_ROUTING_ENTRY_TARGET_HANDSET;
            break;

         case ETSI_HOST_UICC :
            pBuffer[nIndex] = NAL_ROUTING_ENTRY_TARGET_UICC ;
            break;

         case ETSI_HOST_RFU3:
            pBuffer[nIndex] = NAL_ROUTING_ENTRY_TARGET_SE_0 ;
            break;

         default:
            return NAL_RES_FEATURE_NOT_SUPPORTED;
      }

      if (nFlags & HCI_ROUTING_ENTRY_POWER_BATT_OFF)
      {
         pBuffer[nIndex] |= NAL_ROUTING_ENTRY_POWER_BAT_OFF;
      }

      if (nFlags & HCI_ROUTING_ENTRY_POWER_PHONE_ON)
      {
         pBuffer[nIndex] |= NAL_ROUTING_ENTRY_POWER_PHONE_ON ;
      }

      if (nFlags & HCI_ROUTING_ENTRY_POWER_PHONE_OFF)
      {
         pBuffer[nIndex] |= NAL_ROUTING_ENTRY_POWER_PHONE_OFF;
      }

		pBuffer[nIndex+1] = nAIdLength;
      CNALMemoryCopy(& pBuffer[nIndex + 2], &pRoutingTable[nIndex + 2], nAIdLength);

      nIndex += 2 + nAIdLength;
   }

   if (nIndex != nRoutingTableLength)
   {
      PNALDebugError("Error during parsing of the routing table entry");
   }

   return nIndex;
}


/* ************************************************************************** */
/*                             Event Codes                                    */
/* ************************************************************************** */

/* See header file */
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_EVT_STANDBY_MODE(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        bool_t* pbStandbyFlag)
{
   if (nLength != 1)
   {
      return NAL_RES_BAD_LENGTH;
   }

   if(pbStandbyFlag != null)
   {
      *pbStandbyFlag = pBuffer[0];
   }

   return NAL_RES_OK;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_READER_TARGET_COLLISION(
                        uint8_t  * pBuffer,
                        const uint16_t aReaderProtocols[],
                        uint32_t   nReaderProtocols)
{
   uint32_t i;
   uint32_t nIndex = 0;

   for (i=0; i<nReaderProtocols; i++)
   {
      static_PNALBindingWriteUint16ToNALBuffer(aReaderProtocols[i], pBuffer + nIndex);
      nIndex += 2;
   }

   return nIndex;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_14_A_3(
                        uint8_t* pBuffer,
                        uint16_t nATQA,
                        uint8_t nSAK,
                        const uint8_t* pUID,
                        uint32_t nUIDLength)
{
   uint32_t nIndex = 0;

   static_PNALBindingWriteUint16ToNALBuffer(nATQA, &pBuffer[nIndex]);
   nIndex += 2;

   pBuffer[nIndex++] = nSAK;

   if (pUID != null)
   {
      CNALMemoryCopy(&pBuffer[nIndex], pUID, nUIDLength);
   }
   nIndex += nUIDLength;

   return nIndex;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_14_A_4(
                        uint8_t* pBuffer,
                        uint16_t nATQA,
                        uint8_t  nSAK,
                        const uint8_t* pATS,
                        uint32_t nATSLength,
                        const uint8_t* pApplicationData,
                        uint32_t nApplicationDataLength,
                        const uint8_t* pUID,
                        uint32_t nUIDLength)
{
   uint32_t nIndex = 0;

   static_PNALBindingWriteUint16ToNALBuffer(nATQA, &pBuffer[nIndex]);
   nIndex += 2;

   pBuffer[nIndex++] = nSAK;

   if (pATS != null)
   {
      CNALMemoryCopy(&pBuffer[nIndex], pATS, nATSLength);
      pBuffer[nIndex] = (uint8_t) nATSLength;
   }
   pBuffer[nIndex] = (uint8_t) (pBuffer[nIndex] + nApplicationDataLength);
   nIndex += nATSLength;

   if (pApplicationData != null)
   {
      CNALMemoryCopy(&pBuffer[nIndex], pApplicationData, nApplicationDataLength);
   }
   nIndex += nApplicationDataLength;

   if (pUID != null)
   {
      CNALMemoryCopy(&pBuffer[nIndex], pUID, nUIDLength);
   }
   nIndex += nUIDLength;

   return (nIndex);
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_14_B_3(
                        uint8_t * pBuffer,
                        uint8_t  aATQB[P_NFC_HAL_BINDING_CARD_CONFIG_ATQB_SIZE],
                        const uint8_t * pATTRIB,
                        uint32_t nATTRIBLength)
{
   uint32_t nIndex = 0;

   CNALMemoryCopy(&pBuffer[nIndex], aATQB, P_NFC_HAL_BINDING_CARD_CONFIG_ATQB_SIZE);
   nIndex += P_NFC_HAL_BINDING_CARD_CONFIG_ATQB_SIZE;

   if (pATTRIB != null)
   {
      CNALMemoryCopy(&pBuffer[nIndex], pATTRIB, nATTRIBLength);
   }
   nIndex += nATTRIBLength;

   return nIndex;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_15_3(
                        uint8_t* pBuffer,
                        uint8_t nFlag,
                        uint8_t nDSFID,
                        uint8_t aUID[8])
{
   uint32_t nIndex = 0;

   pBuffer[nIndex++] = nFlag;
   pBuffer[nIndex++] = nDSFID;

   CNALMemoryCopy(&pBuffer[nIndex], aUID, 8);
   nIndex += 8;

   return nIndex;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_TYPE1(
                        uint8_t* pBuffer,
                        const uint8_t aUID[4],
                        const uint8_t aHR[2],
                        uint16_t nATQA)
{
   uint32_t nIndex = 0;

   CNALMemoryCopy(&pBuffer[nIndex], aUID, 4);
   nIndex += 4;

   CNALMemoryCopy(&pBuffer[nIndex], aHR, 2);
   nIndex += 2;

   static_PNALBindingWriteUint16ToNALBuffer(nATQA, &pBuffer[nIndex]);
   nIndex += 2;

   return nIndex;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_FELICA(
                        uint8_t* pBuffer,
                        uint8_t  nIdPm[16],
                        uint16_t nSystemCode)
{
   uint32_t nIndex = 0;

   CNALMemoryCopy(&pBuffer[nIndex], nIdPm, 16);
   nIndex += 16;

   static_PNALBindingWriteUint16ToNALBuffer(nSystemCode, &pBuffer[nIndex]);
   nIndex += 2;

   return nIndex;
}

NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_BPRIME(
                        uint8_t* pBuffer,
                        uint8_t* pAPGENBuffer,
                        uint32_t nAPGENLength
                        )
{
   CNALMemoryCopy(pBuffer, pAPGENBuffer, nAPGENLength);
   return nAPGENLength;
}

NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_KOVIO(
                        uint8_t* pBuffer,
                        uint8_t* pDataReceived,
                        uint32_t nDataLength
                        )
{
   static_PNALProtocolDecode_Kovio(pBuffer,pDataReceived, nDataLength);
   return nDataLength;
}

NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_UICC_DETECTION_REQUEST (
                        uint8_t* pBuffer,
                        bool_t bUICC,
                        uint16_t nReaderProtocolOpened,
                        uint8_t nDataRateMax,
                        uint8_t nAFI,
                        const uint8_t* pnHigherLayerDara,
                        uint32_t nHigherLayerDaraLength)
{
   uint32_t nIndex = 0;

   pBuffer[nIndex++] = (bUICC==W_TRUE)? 1:0;

   static_PNALBindingWriteUint16ToNALBuffer(nReaderProtocolOpened, &pBuffer[nIndex]);
   nIndex += 2;

   pBuffer[nIndex++] = nDataRateMax;
   pBuffer[nIndex++] = nAFI;

   if (pnHigherLayerDara != null)
   {
      CNALMemoryCopy(&pBuffer[nIndex], pnHigherLayerDara, nHigherLayerDaraLength);
   }
   nIndex += nHigherLayerDaraLength;

   return nIndex;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_CARD_SELECTED_14_A_4(
                        uint8_t* pBuffer,
                        uint8_t nDataRate,
                        uint8_t nCID,
                        uint8_t * pUID,
                        uint32_t nUIDLength)
{
   uint32_t nIndex = 0;

   pBuffer[nIndex++] = nDataRate;
   pBuffer[nIndex++] = nCID;

   CNALMemoryCopy(&pBuffer[nIndex], pUID, nUIDLength);
   nIndex += P_NFC_HAL_BINDING_CARD_CONFIG_UID_SIZE;

   pBuffer[nIndex++] = (uint8_t) nUIDLength;

   return nIndex;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_CARD_SELECTED_14_B_4(
                        uint8_t* pBuffer,
                        uint8_t nAFI,
                        uint8_t nDataRate,
                        uint8_t nCID,
                        uint8_t nFrameSize,
                        uint8_t aPUPI[4],
                        const uint8_t* pnHigherLayerData,
                        uint32_t nHigherLayerDataLength)
{
   uint32_t nIndex = 0;

   pBuffer[nIndex++] = nAFI;
   pBuffer[nIndex++] = nDataRate;
   pBuffer[nIndex++] = nCID;
   pBuffer[nIndex++] = nFrameSize;

   CNALMemoryCopy(&pBuffer[nIndex], aPUPI, 4);
   nIndex += 4;

   if (pnHigherLayerData != null)
   {
      CNALMemoryCopy(&pBuffer[nIndex], pnHigherLayerData, nHigherLayerDataLength);
   }
   nIndex += nHigherLayerDataLength;

   return nIndex;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocol_NAL_EVT_CARD_SEND_DATA(
                        uint8_t* pBuffer,
                        const uint8_t* pSendData,
                        uint32_t nSendDataLength)
{
   if ((nSendDataLength > 0) && (pSendData != null))
   {
      CNALMemoryCopy(&pBuffer[0], pSendData, nSendDataLength);
   }

   return nSendDataLength;
}

/* See header file */
NFC_HAL_INTERNAL void PNALProtocolFormat_NAL_EVT_CARD_END_OF_TRANSACTION(
                        uint8_t* pBuffer,
                        bool_t bDeSelection)
{
   pBuffer[0] = (bDeSelection==W_TRUE)?1:0;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_SE_CARD_EOT(
                        uint8_t* pBuffer,
                        uint16_t nCardProtocolBitField,
                        const uint8_t* pAID,
                        uint32_t nAIDLength)
{
   uint32_t nIndex = 0;

   static_PNALBindingWriteUint16ToNALBuffer(nCardProtocolBitField, &pBuffer[nIndex]);
   nIndex += 2;

   if ((pAID != null) && (nAIDLength > 0))
   {
      CNALMemoryCopy(&pBuffer[nIndex], pAID, nAIDLength);
   }
   nIndex += nAIDLength;

   return nIndex;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_P2P_TARGET_DISCOVERED(
                        uint8_t* pBuffer,
                        const uint8_t* pLinkParameters,
                        uint32_t nLinkLength)
{
   if ((pLinkParameters != null) && (nLinkLength > 0))
   {
      CNALMemoryCopy(&pBuffer[0], pLinkParameters, nLinkLength);
   }

   return nLinkLength;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_P2P_INITIATOR_DISCOVERED(
                        uint8_t* pBuffer,
                        const uint8_t * pLinkParameters,
                        uint32_t nLinkLength)
{
   if ((pLinkParameters != null) && (nLinkLength > 0))
   {
      CNALMemoryCopy(&pBuffer[0], pLinkParameters, nLinkLength);
   }

   return nLinkLength;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_UICC_CONNECTIVITY(
                        uint8_t* pBuffer,
                        const uint8_t* pPayload,
                        uint32_t nPayloadLength)
{
   if ((pPayload != null) && (nPayloadLength > 0))
   {
      CNALMemoryCopy(&pBuffer[0], pPayload, nPayloadLength);
   }
   return nPayloadLength;
}

/* See header file */
NFC_HAL_INTERNAL void PNALProtocolFormat_NAL_EVT_RF_FIELD(
                        uint8_t* pBuffer,
                        bool_t bExternalRfField)
{
   pBuffer[0] = (bExternalRfField==W_TRUE)?1:0;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_NFCC_ERROR(
                        uint8_t* pBuffer,
                        uint32_t nCause)
{
   static_PNALBindingWriteUint32ToNALBuffer(nCause, pBuffer);

   return 4;
}
