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

/*******************************************************************************
   Contains the implementation of some utility functions
*******************************************************************************/

#include "nfc_hal_binding.h"

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALUtilWriteHexaUint8(
               uint8_t* pStringBuffer,
               uint8_t nValue)
{
   *pStringBuffer = (nValue >> 4) + '0';
   if(*pStringBuffer > '9')
   {
      *pStringBuffer = *pStringBuffer - '0' - 10 + 'A';
   }
   pStringBuffer++;

   *pStringBuffer = (nValue & 0x0F) + '0';
   if(*pStringBuffer > '9')
   {
      *pStringBuffer = *pStringBuffer - '0' - 10 + 'A';
   }

   return 2;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALUtilWriteDecimalUint32(
               uint8_t* pStringBuffer,
               uint32_t nValue)
{
   uint8_t aBuffer[10];
   uint32_t nLength = 0;
   uint32_t nPos;

   do
   {
      aBuffer[nLength++] = (uint8_t)(nValue % 10);
      nValue /= 10;
   }
   while(nValue != 0);

   for(nPos = 1; nPos <= nLength; nPos++)
   {
      *pStringBuffer++ = aBuffer[nLength - nPos] + '0';
   }

   return nLength;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALUtilWriteVersion(
               uint8_t* pStringBuffer,
               const uint8_t* pVersion)
{
   uint32_t nLength = 0;
   uint32_t nTempLength;
   uint8_t nTemp1;

   nTemp1 = *pVersion++;
   nTempLength = PNALUtilWriteDecimalUint32(pStringBuffer, (uint32_t)nTemp1);
   nLength += nTempLength;
   pStringBuffer += nTempLength;

   *pStringBuffer++ = '.';
   nLength ++;

   nTemp1 = *pVersion++;
   nTempLength = PNALUtilWriteDecimalUint32(pStringBuffer, (uint32_t)nTemp1);
   nLength += nTempLength;
   pStringBuffer += nTempLength;

   nTemp1 = *pVersion;
   if(nTemp1 != 0)
   {
      if(((nTemp1 >= 'a') && (nTemp1 <= 'z')) || ((nTemp1 >= 'A') && (nTemp1 <= 'Z')))
      {
         *pStringBuffer++ = nTemp1;
         nLength ++;
      }
      else
      {
         *pStringBuffer++ = '.';
         nLength ++;
         nLength += PNALUtilWriteDecimalUint32(pStringBuffer, (uint32_t)nTemp1);
      }
   }

   return nLength;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALUtilReadUint32FromBigEndianBuffer(
         const uint8_t* pBuffer)
{
   return (uint32_t)((((uint32_t)pBuffer[0]) << 24)
   | (((uint32_t)pBuffer[1]) << 16)
   | (((uint32_t)pBuffer[2]) << 8)
   | ((uint32_t)pBuffer[3]));
}

/* See header file */
NFC_HAL_INTERNAL uintptr_t PNALUtilReadAddressFromBigEndianBuffer(
   const uint8_t* pBuffer)
{
   int8_t i, j = 0;
   uintptr_t nValue = 0;

   for(i = (sizeof(uintptr_t) - 1); i >= 0; i--)
   {
      nValue |= (((uintptr_t)pBuffer[j++]) << (i*8));
   }
   return (nValue);
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALUtilReadUint32FromLittleEndianBuffer(
         const uint8_t* pBuffer)
{
   return (uint32_t)((((uint32_t)pBuffer[3]) << 24)
   | (((uint32_t)pBuffer[2]) << 16)
   | (((uint32_t)pBuffer[1]) << 8)
   | ((uint32_t)pBuffer[0]));
}

/* -----------------------------------------------------------------------------
      Trace Functions
----------------------------------------------------------------------------- */

#ifdef P_NAL_TRACE_ACTIVE

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALUtilLogUint8(
         char* pTraceBuffer,
         uint32_t nValue)
{
   uint8_t nDigit = (uint8_t)((nValue >> 4) & 0x0F);

   if(nDigit <= 9)
   {
      *pTraceBuffer++ = nDigit + '0';
   }
   else
   {
      *pTraceBuffer++ = nDigit - 10 + 'A';
   }

   nDigit = (uint8_t)(nValue & 0x0F);

   if(nDigit <= 9)
   {
      *pTraceBuffer = nDigit + '0';
   }
   else
   {
      *pTraceBuffer = nDigit - 10 + 'A';
   }

   return 2;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALUtilLogUint16(
                  char* pTraceBuffer,
                  uint32_t nValue)
{
    uint32_t nPos = 0;
    nPos +=PNALUtilLogUint8(&pTraceBuffer[nPos], ((nValue & 0x0000FF00) >> 8));
    nPos +=PNALUtilLogUint8(&pTraceBuffer[nPos],  (nValue & 0x000000FF));
    return nPos;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PNALUtilLogArray(
         char* pTraceBuffer,
         uint8_t* pBuffer,
         uint32_t nLength)
{
   uint32_t nPos = 0;
   uint32_t i;

   if(nLength != 0)
   {
      PNALUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "(len=0x00)");

      (void)PNALUtilLogUint8(&pTraceBuffer[7], nLength);

      for(i = 0; i < nLength; i++)
      {
         pTraceBuffer[nPos++] = ' ';
         nPos += PNALUtilLogUint8(&pTraceBuffer[nPos], pBuffer[i]);
      }
   }

   pTraceBuffer[nPos] = 0;

   return nPos;
}

/* See header file */
NFC_HAL_INTERNAL void PNALUtilTraceASCIIStringCopy(
            char* pBuffer,
            uint32_t* pPos,
            const char* pString)
{
   uint32_t nCursor = *pPos;
   char c;

   while( (c = *pString++) != 0)
   {
      pBuffer[nCursor++] = c;
   }

   pBuffer[nCursor] = 0;

   *pPos = nCursor;
}

#define P_CODE_2_STRING(X) \
         case X: \
            return #X; break;

/* See header file */
NFC_HAL_INTERNAL const char* PNALUtilTraceErrorWrapper(
         W_ERROR nError)
{
   switch(nError)
   {
      P_CODE_2_STRING(W_SUCCESS)
      P_CODE_2_STRING(W_ERROR_VERSION_NOT_SUPPORTED)
      P_CODE_2_STRING(W_ERROR_ITEM_NOT_FOUND)
      P_CODE_2_STRING(W_ERROR_BUFFER_TOO_SHORT)
      P_CODE_2_STRING(W_ERROR_PERSISTENT_DATA)
      P_CODE_2_STRING(W_ERROR_NO_EVENT)
      P_CODE_2_STRING(W_ERROR_WAIT_CANCELLED)
      P_CODE_2_STRING(W_ERROR_UICC_COMMUNICATION)
      P_CODE_2_STRING(W_ERROR_BAD_HANDLE)
      P_CODE_2_STRING(W_ERROR_EXCLUSIVE_REJECTED)
      P_CODE_2_STRING(W_ERROR_SHARE_REJECTED)
      P_CODE_2_STRING(W_ERROR_BAD_PARAMETER)
      P_CODE_2_STRING(W_ERROR_RF_PROTOCOL_NOT_SUPPORTED)
      P_CODE_2_STRING(W_ERROR_CONNECTION_COMPATIBILITY)
      P_CODE_2_STRING(W_ERROR_BUFFER_TOO_LARGE)
      P_CODE_2_STRING(W_ERROR_INDEX_OUT_OF_RANGE)
      P_CODE_2_STRING(W_ERROR_OUT_OF_RESOURCE)
      P_CODE_2_STRING(W_ERROR_BAD_TAG_FORMAT)
      P_CODE_2_STRING(W_ERROR_BAD_NDEF_FORMAT)
      P_CODE_2_STRING(W_ERROR_NDEF_UNKNOWN)
      P_CODE_2_STRING(W_ERROR_LOCKED_TAG)
      P_CODE_2_STRING(W_ERROR_TAG_FULL)
      P_CODE_2_STRING(W_ERROR_CANCEL)
      P_CODE_2_STRING(W_ERROR_TIMEOUT)
      P_CODE_2_STRING(W_ERROR_TAG_DATA_INTEGRITY)
      P_CODE_2_STRING(W_ERROR_NFC_HAL_COMMUNICATION)
      P_CODE_2_STRING(W_ERROR_WRONG_RTD)
      P_CODE_2_STRING(W_ERROR_TAG_WRITE)
      P_CODE_2_STRING(W_ERROR_BAD_NFCC_MODE)
      P_CODE_2_STRING(W_ERROR_TOO_MANY_HANDLERS)
      P_CODE_2_STRING(W_ERROR_BAD_STATE)
      P_CODE_2_STRING(W_ERROR_BAD_FIRMWARE_FORMAT)
      P_CODE_2_STRING(W_ERROR_BAD_FIRMWARE_SIGNATURE)
      P_CODE_2_STRING(W_ERROR_DURING_HARDWARE_BOOT)
      P_CODE_2_STRING(W_ERROR_DURING_FIRMWARE_BOOT)
      P_CODE_2_STRING(W_ERROR_FEATURE_NOT_SUPPORTED)
      P_CODE_2_STRING(W_ERROR_CLIENT_SERVER_PROTOCOL)
      P_CODE_2_STRING(W_ERROR_FUNCTION_NOT_SUPPORTED)
      P_CODE_2_STRING(W_ERROR_TAG_NOT_LOCKABLE)
      P_CODE_2_STRING(W_ERROR_ITEM_LOCKED)
      P_CODE_2_STRING(W_ERROR_SYNC_OBJECT)
      P_CODE_2_STRING(W_ERROR_RETRY)
      P_CODE_2_STRING(W_ERROR_DRIVER )
      P_CODE_2_STRING(W_ERROR_MISSING_INFO)
      P_CODE_2_STRING(W_ERROR_P2P_CLIENT_REJECTED)
      P_CODE_2_STRING(W_ERROR_NFCC_COMMUNICATION)
      P_CODE_2_STRING(W_ERROR_RF_COMMUNICATION)
      P_CODE_2_STRING(W_ERROR_BAD_FIRMWARE_VERSION)
      P_CODE_2_STRING(W_ERROR_HETEROGENEOUS_DATA)
      P_CODE_2_STRING(W_ERROR_CLIENT_SERVER_COMMUNICATION)

      default:
         return "*** Unknown Error ***";
   }
}

#endif /* #ifdef P_NAL_TRACE_ACTIVE */
