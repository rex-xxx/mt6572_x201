/*
 * Copyright (c) 2008-2012 Inside Secure, All Rights Reserved.
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
   Contains the Bluetooth pairing implementation.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( BT_PAIR )

#include "wme_context.h"

#if ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (defined P_INCLUDE_PAIRING)

/*********************        Add For Handover            **********************/

/**
 * @brief Carrier Type Name reserved for handover bluetooth connection
 **/
static const char aHandoverBluetoothCarrierTypeName[] = {   'a','p','p','l','i','c','a','t','i','o','n',
                                                            '/',
                                                            'v','n','d',
                                                            '.',
                                                            'b','l','u','e','t','o','o','t','h',
                                                            '.',
                                                            'e','p',
                                                            '.',
                                                            'o','o','b'};

#define P_BT_PAIRING_EIR_TYPE_FLAG                             0x01
#define P_BT_PAIRING_EIR_TYPE_SERVICE_CLASS_16_BIT_SHORT       0x02
#define P_BT_PAIRING_EIR_TYPE_SERVICE_CLASS_16_BIT_COMPLETE    0x03
#define P_BT_PAIRING_EIR_TYPE_SERVICE_CLASS_32_BIT_SHORT       0x04
#define P_BT_PAIRING_EIR_TYPE_SERVICE_CLASS_32_BIT_COMPLETE    0x05
#define P_BT_PAIRING_EIR_TYPE_SERVICE_CLASS_128_BIT_SHORT      0x06
#define P_BT_PAIRING_EIR_TYPE_SERVICE_CLASS_128_BIT_COMPLETE   0x07

#define P_BT_PAIRING_EIR_TYPE_LOCAL_NAME_SHORT                 0x08
#define P_BT_PAIRING_EIR_TYPE_LOCAL_NAME_COMPLETE              0x09
#define P_BT_PAIRING_EIR_TYPE_CLASS_DEVICE                     0x0D
#define P_BT_PAIRING_EIR_TYPE_PAIRING_HASH                     0x0E
#define P_BT_PAIRING_EIR_TYPE_PAIRING_RAND                     0x0F

#define P_BT_PAIRING_EIR_TYPE_MANUFACTURER_DATA                0xFF

/**
 *Return size which need to be allocated for building the Out of band payload
 **/
static W_ERROR static_PHandoverBluetoothGetConfigurationDataLength(
      tWBTPairingInfo * pBluetoothInfo,
      uint32_t * nLength)
{
   PDebugTrace("static_PHandoverBluetoothGetConfigurationDataLength");

   *nLength = 0;

   if(pBluetoothInfo == null)
   {
      PDebugTrace("static_PHandoverBluetoothGetConfigurationDataLength BAD Parameter");
      return W_ERROR_BAD_PARAMETER;
   }

   *nLength = 8;
   if(pBluetoothInfo->bHasDeviceClass != W_FALSE)
   {
      *nLength += 2 + W_BT_PAIRING_SIZE_OF_DEVICE_CLASS;
   }

   if(pBluetoothInfo->bHasSimplePairingHash != W_FALSE)
   {
      *nLength += 2 + W_BT_PAIRING_SIZE_OF_PAIRING_HASH;
   }

   if(pBluetoothInfo->bHasSimplePairingRandomizer != W_FALSE)
   {
      *nLength += 2 + W_BT_PAIRING_SIZE_OF_PAIRING_RANDOM;
   }

   if(pBluetoothInfo->nLocalNameLength > 0)
   {
      *nLength += 2 + pBluetoothInfo->nLocalNameLength;
   }

   if(pBluetoothInfo->nFlagLength > 0)
   {
      *nLength += 2 + pBluetoothInfo->nFlagLength;
   }

   if(pBluetoothInfo->nUUIDListLength > 0)
   {
      *nLength += 2 + pBluetoothInfo->nUUIDListLength;
   }

   if(pBluetoothInfo->nManufacturerDataLength > 0)
   {
      *nLength += 2 + pBluetoothInfo->nManufacturerDataLength;
   }

   return W_SUCCESS;
}

W_ERROR PHandoverAddBluetoothCarrier(
   tContext *  pContext,
   W_HANDLE    hConnectionHandover,
   tWBTPairingInfo * pBluetoothInfo,
   uint8_t     nCarrierPowerState)
{
   uint32_t nLength = 0;
   uint16_t  nOffset = 0;
   W_ERROR nError = W_SUCCESS;
   tPHandoverCarrierType g_sBluetoothCarrierType;
   g_sBluetoothCarrierType.nLength = sizeof(aHandoverBluetoothCarrierTypeName);
   g_sBluetoothCarrierType.pName = (uint8_t *)aHandoverBluetoothCarrierTypeName;

   PDebugTrace("PHandoverAddBluetoothCarrier");

   nError = static_PHandoverBluetoothGetConfigurationDataLength(
               pBluetoothInfo,
               &nLength);

   if(nError != W_SUCCESS)
   {
      PDebugTrace("PHandoverAddBluetoothCarrier => Error");
      return nError;
   }
   else
   {
      uint8_t * pData = (uint8_t * ) CMemoryAlloc(nLength);

      if(pData == null)
      {
         PDebugTrace("PHandoverAddBluetoothCarrier => Out of ressource");
         return W_ERROR_OUT_OF_RESOURCE;
      }
      nOffset = 2;

      CMemoryCopy(&pData[nOffset],
                  pBluetoothInfo->aBTAddress,
                  W_BT_PAIRING_SIZE_OF_BT_ADDRESS);

      nOffset += W_BT_PAIRING_SIZE_OF_BT_ADDRESS;

      /* Device Class */
      if(pBluetoothInfo->bHasDeviceClass)
      {
         pData[nOffset++] = W_BT_PAIRING_SIZE_OF_DEVICE_CLASS + 1;
         pData[nOffset++] = P_BT_PAIRING_EIR_TYPE_CLASS_DEVICE;

         CMemoryCopy(&pData[nOffset],
                     pBluetoothInfo->aBTDeviceClass,
                     W_BT_PAIRING_SIZE_OF_DEVICE_CLASS);

         nOffset += W_BT_PAIRING_SIZE_OF_DEVICE_CLASS;
      }

      /* Simple Pairing Hash */
      if(pBluetoothInfo->bHasSimplePairingHash)
      {
         pData[nOffset++] = W_BT_PAIRING_SIZE_OF_PAIRING_HASH + 1;
         pData[nOffset++] = P_BT_PAIRING_EIR_TYPE_PAIRING_HASH;

         CMemoryCopy(&pData[nOffset],
                     pBluetoothInfo->aBTSimplePairingHash,
                     W_BT_PAIRING_SIZE_OF_PAIRING_HASH);

         nOffset += W_BT_PAIRING_SIZE_OF_PAIRING_HASH;
      }

      /* Simple Pairing Randomizer */
      if(pBluetoothInfo->bHasSimplePairingRandomizer)
      {
         pData[nOffset++] = W_BT_PAIRING_SIZE_OF_PAIRING_RANDOM + 1;
         pData[nOffset++] = P_BT_PAIRING_EIR_TYPE_PAIRING_RAND;

         CMemoryCopy(&pData[nOffset],
                     pBluetoothInfo->aBTSimplePairingRandomizer,
                     W_BT_PAIRING_SIZE_OF_PAIRING_RANDOM);

         nOffset += W_BT_PAIRING_SIZE_OF_PAIRING_RANDOM;
      }

      /* Local Name */
      if(pBluetoothInfo->nLocalNameLength > 0)
      {
         pData[nOffset++] = pBluetoothInfo->nLocalNameLength + 1;

         if(pBluetoothInfo->bIsCompleteLocalName != W_FALSE)
         {
            pData[nOffset++] = P_BT_PAIRING_EIR_TYPE_LOCAL_NAME_COMPLETE;
         }else
         {
            pData[nOffset++] = P_BT_PAIRING_EIR_TYPE_LOCAL_NAME_SHORT;
         }

         CMemoryCopy(&pData[nOffset],
                     pBluetoothInfo->aBTLocalName,
                     pBluetoothInfo->nLocalNameLength);

         nOffset += pBluetoothInfo->nLocalNameLength;
      }

      /* Flags */
      if(pBluetoothInfo->nFlagLength > 0)
      {
         pData[nOffset++] = pBluetoothInfo->nFlagLength + 1;

         pData[nOffset++] = P_BT_PAIRING_EIR_TYPE_FLAG;

         CMemoryCopy(&pData[nOffset],
                     pBluetoothInfo->aBTFlag,
                     pBluetoothInfo->nFlagLength);

         nOffset += pBluetoothInfo->nFlagLength;
      }

      /* UUID List */
      if(pBluetoothInfo->nUUIDListLength > 0)
      {
         pData[nOffset++] = pBluetoothInfo->nUUIDListLength + 1;

         if(pBluetoothInfo->nUUIDServiceClassNumber == W_BT_PAIRING_UUID_16_BIT_SERVICE_CLASS)
         {
            if(pBluetoothInfo->bIsCompleteUUIDList != W_FALSE)
            {
               pData[nOffset++] = P_BT_PAIRING_EIR_TYPE_SERVICE_CLASS_16_BIT_COMPLETE;
            }
            else
            {
               pData[nOffset++] = P_BT_PAIRING_EIR_TYPE_SERVICE_CLASS_16_BIT_SHORT;
            }
         }
         else if(pBluetoothInfo->nUUIDServiceClassNumber == W_BT_PAIRING_UUID_32_BIT_SERVICE_CLASS)
         {
            if(pBluetoothInfo->bIsCompleteUUIDList != W_FALSE)
            {
               pData[nOffset++] = P_BT_PAIRING_EIR_TYPE_SERVICE_CLASS_32_BIT_COMPLETE;
            }
            else
            {
               pData[nOffset++] = P_BT_PAIRING_EIR_TYPE_SERVICE_CLASS_32_BIT_SHORT;
            }
         }
         else if(pBluetoothInfo->nUUIDServiceClassNumber == W_BT_PAIRING_UUID_128_BIT_SERVICE_CLASS)
         {
            if(pBluetoothInfo->bIsCompleteUUIDList != W_FALSE)
            {
               pData[nOffset++] = P_BT_PAIRING_EIR_TYPE_SERVICE_CLASS_128_BIT_COMPLETE;
            }
            else
            {
               pData[nOffset++] = P_BT_PAIRING_EIR_TYPE_SERVICE_CLASS_128_BIT_SHORT;
            }
         }
         else
         {
            PDebugError("PHandoverAddBluetoothCarrier BAD Parameter service class not valid");
            CMemoryFree(pData);
            return W_ERROR_BAD_PARAMETER;
         }

         CMemoryCopy(&pData[nOffset],
                     pBluetoothInfo->aBTUUIDList,
                     pBluetoothInfo->nUUIDListLength);

         nOffset += pBluetoothInfo->nUUIDListLength;
      }

      if(pBluetoothInfo->nManufacturerDataLength > 0)
      {
         pData[nOffset ++] = pBluetoothInfo->nManufacturerDataLength;
         pData[nOffset ++] = P_BT_PAIRING_EIR_TYPE_MANUFACTURER_DATA;

         CMemoryCopy(&pData[nOffset],
                     pBluetoothInfo->aBTManufacturerData,
                     pBluetoothInfo->nManufacturerDataLength);

         nOffset += pBluetoothInfo->nManufacturerDataLength;
      }

      /* Length of the payload */
      pData[0] = (nOffset & 0x00FF);
      pData[1] = ((nOffset >> 8) & 0x00FF);

      /*** Add Carrier ***/
      PHandoverAddCarrier(
            pContext,
            hConnectionHandover,
            &g_sBluetoothCarrierType,
            pData,
            nOffset,
            nCarrierPowerState);

      CMemoryFree(pData);
   }
   return W_SUCCESS;
}

static W_ERROR static_tPHandoverBluetoothParser(void * pParserData , uint8_t * pDataReceived, uint32_t nLength)
{
   tWBTPairingInfo * pBTInfo = (tWBTPairingInfo *) pParserData;
   uint32_t nOffset = 8;
   uint8_t nLengthEIR;

   /* Initialize the tWBTPairingInfo structure */
   CMemoryFill(pBTInfo, 0, sizeof(tWBTPairingInfo));

   if(nLength !=  (uint32_t)(pDataReceived[0] | (pDataReceived[1] << 8)) )
   {
      PDebugError("static_tPHandoverBluetoothParser Error data length not valid");
      return W_ERROR_BAD_TAG_FORMAT;
   }

   CMemoryCopy(pBTInfo->aBTAddress,
               &pDataReceived[2],
               W_BT_PAIRING_SIZE_OF_BT_ADDRESS);

   while(nOffset < nLength)
   {
      nLengthEIR = pDataReceived[nOffset++] - 1;
      switch( pDataReceived[nOffset++] )
      {
         case P_BT_PAIRING_EIR_TYPE_FLAG:
            CMemoryCopy(pBTInfo->aBTFlag,
                        &pDataReceived[nOffset],
                        nLengthEIR);

            pBTInfo->nFlagLength = nLengthEIR;
            break;

         case P_BT_PAIRING_EIR_TYPE_SERVICE_CLASS_16_BIT_SHORT:
            CMemoryCopy(pBTInfo->aBTUUIDList,
                        &pDataReceived[nOffset],
                        nLengthEIR);

            pBTInfo->nUUIDListLength = nLengthEIR;
            pBTInfo->nUUIDServiceClassNumber = W_BT_PAIRING_UUID_16_BIT_SERVICE_CLASS;
            pBTInfo->bIsCompleteUUIDList = W_FALSE;

            break;

         case P_BT_PAIRING_EIR_TYPE_SERVICE_CLASS_16_BIT_COMPLETE:
            CMemoryCopy(pBTInfo->aBTUUIDList,
                        &pDataReceived[nOffset],
                        nLengthEIR);

            pBTInfo->nUUIDListLength = nLengthEIR;
            pBTInfo->nUUIDServiceClassNumber = W_BT_PAIRING_UUID_16_BIT_SERVICE_CLASS;
            pBTInfo->bIsCompleteUUIDList = W_TRUE;
            break;

         case P_BT_PAIRING_EIR_TYPE_SERVICE_CLASS_32_BIT_SHORT:
            CMemoryCopy(pBTInfo->aBTUUIDList,
                        &pDataReceived[nOffset],
                        nLengthEIR);

            pBTInfo->nUUIDListLength = nLengthEIR;
            pBTInfo->nUUIDServiceClassNumber = W_BT_PAIRING_UUID_32_BIT_SERVICE_CLASS;
            pBTInfo->bIsCompleteUUIDList = W_FALSE;
            break;

         case P_BT_PAIRING_EIR_TYPE_SERVICE_CLASS_32_BIT_COMPLETE:
            CMemoryCopy(pBTInfo->aBTUUIDList,
                        &pDataReceived[nOffset],
                        nLengthEIR);

            pBTInfo->nUUIDListLength = nLengthEIR;
            pBTInfo->nUUIDServiceClassNumber = W_BT_PAIRING_UUID_32_BIT_SERVICE_CLASS;
            pBTInfo->bIsCompleteUUIDList = W_TRUE;
            break;

         case P_BT_PAIRING_EIR_TYPE_SERVICE_CLASS_128_BIT_SHORT:
            CMemoryCopy(pBTInfo->aBTUUIDList,
                        &pDataReceived[nOffset],
                        nLengthEIR);

            pBTInfo->nUUIDListLength = nLengthEIR;
            pBTInfo->nUUIDServiceClassNumber = W_BT_PAIRING_UUID_128_BIT_SERVICE_CLASS;
            pBTInfo->bIsCompleteUUIDList = W_FALSE;
            break;

         case P_BT_PAIRING_EIR_TYPE_SERVICE_CLASS_128_BIT_COMPLETE:
            CMemoryCopy(pBTInfo->aBTUUIDList,
                        &pDataReceived[nOffset],
                        nLengthEIR);

            pBTInfo->nUUIDListLength = nLengthEIR;
            pBTInfo->nUUIDServiceClassNumber = W_BT_PAIRING_UUID_128_BIT_SERVICE_CLASS;
            pBTInfo->bIsCompleteUUIDList = W_TRUE;
            break;

         case P_BT_PAIRING_EIR_TYPE_LOCAL_NAME_SHORT:
            CMemoryCopy(pBTInfo->aBTLocalName,
                        &pDataReceived[nOffset],
                        nLengthEIR);

            pBTInfo->nLocalNameLength = nLengthEIR;
            pBTInfo->bIsCompleteLocalName = W_FALSE;
            break;

         case P_BT_PAIRING_EIR_TYPE_LOCAL_NAME_COMPLETE:
            CMemoryCopy(pBTInfo->aBTLocalName,
                        &pDataReceived[nOffset],
                        nLengthEIR);

            pBTInfo->nLocalNameLength = nLengthEIR;
            pBTInfo->bIsCompleteLocalName = W_TRUE;
            break;

         case P_BT_PAIRING_EIR_TYPE_CLASS_DEVICE:
            CMemoryCopy(pBTInfo->aBTDeviceClass,
                        &pDataReceived[nOffset],
                        nLengthEIR);

            pBTInfo->bHasDeviceClass = W_TRUE;
            break;

         case P_BT_PAIRING_EIR_TYPE_PAIRING_HASH:
            CMemoryCopy(pBTInfo->aBTSimplePairingHash,
                        &pDataReceived[nOffset],
                        nLengthEIR);

            pBTInfo->bHasSimplePairingHash = W_TRUE;
            break;

         case P_BT_PAIRING_EIR_TYPE_PAIRING_RAND:
            CMemoryCopy(pBTInfo->aBTSimplePairingRandomizer,
                        &pDataReceived[nOffset],
                        nLengthEIR);

            pBTInfo->bHasSimplePairingRandomizer = W_TRUE;
            break;

         case P_BT_PAIRING_EIR_TYPE_MANUFACTURER_DATA:
            CMemoryCopy(pBTInfo->aBTManufacturerData,
                        &pDataReceived[nOffset],
                        nLengthEIR);

            pBTInfo->nManufacturerDataLength = nLengthEIR;
            break;

         default:
            PDebugError("static_tPHandoverBluetoothParser Error during data parsing");
            CMemoryFill(pBTInfo,
                        0x00,
                        sizeof(tWBTPairingInfo));
            return W_ERROR_HETEROGENEOUS_DATA;
            break;
      }
      nOffset += nLengthEIR;
   }

   return W_SUCCESS;
}

W_ERROR PHandoverGetBluetoothInfo(
      tContext * PContext,
      W_HANDLE hConnectionHandover,
      tWBTPairingInfo * pBTInfo
   )
{
   tPHandoverCarrierType sCarrierType;
   sCarrierType.nLength = sizeof(aHandoverBluetoothCarrierTypeName);
   sCarrierType.pName = (uint8_t *)aHandoverBluetoothCarrierTypeName;

   PDebugError("PBTPairingGetRemoteDeviceInfo");
   if(pBTInfo == null)
   {
      PDebugError("PBTPairingGetRemoteDeviceInfo : Error Bad Parameter");
      return W_ERROR_BAD_PARAMETER;
   }

   return PHandoverGetCarrierInfo(
      PContext,
      hConnectionHandover,
      &sCarrierType,
      static_tPHandoverBluetoothParser,
      pBTInfo);

}

/*********************        End Handover            **********************/

#endif /* (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (defined P_INCLUDE_PAIRING) */

#if ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (! defined P_INCLUDE_PAIRING)

/* See client API */
W_ERROR PHandoverGetBluetoothInfo(
      tContext * PContext,
      W_HANDLE hConnectionHandover,
      tWBTPairingInfo * pBTInfo
   )

{
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

/* See client API */
W_ERROR PHandoverAddBluetoothCarrier(
   tContext *  pContext,
   W_HANDLE    hConnectionHandover,
   tWBTPairingInfo * pBluetoothInfo,
   uint8_t     nCarrierPowerState)
{
   return W_ERROR_FUNCTION_NOT_SUPPORTED;
}

#endif /* (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (! defined P_INCLUDE_PAIRING) */
