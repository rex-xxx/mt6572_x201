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
   Contains the implementation of the NFCC functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( NFCC )

#include "wme_context.h"

/* -----------------------------------------------------------------------------

   Instance Creation

----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

void PNFCControllerCreate(
         tNFCController* pNFCController )
{
   CMemoryFill(pNFCController, 0, sizeof(tNFCController));
}

void PNFCControllerDestroy(
         tNFCController* pNFCController )
{
   if(pNFCController->pMessageQueue != null)
   {
      CMemoryFree(pNFCController->pMessageQueue);
   }
   CMemoryFill(pNFCController, 0, sizeof(tNFCController));
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

/* -----------------------------------------------------------------------------

   NFC Controller Properties Functions

----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

W_ERROR PNFCControllerDriverReadInfo(
            tContext* pContext,
            void* pBuffer,
            uint32_t nBufferSize)
{
   tNFCControllerInfo* pNFCControllerInfo = PContextGetNFCControllerInfo( pContext );

   if((pBuffer == null) || (nBufferSize != sizeof(tNFCControllerInfo)))
   {
      return W_ERROR_BAD_PARAMETER;
   }

   CMemoryCopy(pBuffer, pNFCControllerInfo, sizeof(tNFCControllerInfo));

   return W_SUCCESS;
}

#endif /* P_CONFIG_DRIVER */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PNFCControllerUserReadInfo(
            tContext* pContext)
{
   tNFCControllerInfo* pNFCControllerInfo = PContextGetNFCControllerInfo( pContext );
   W_ERROR nError;

   PContextReleaseLock(pContext);
   nError = PNFCControllerDriverReadInfo(pContext, pNFCControllerInfo, sizeof(tNFCControllerInfo));
   PContextLock(pContext);

   return nError;
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/**
 * Checks if a property is in the boolean range.
 *
 * @param[in]  X  The property identifier.
 *
 * @return  A valid condition if the property is in the boolean range.
 **/
#define P_NFCC_PROP_IS_BOOLEAN(x) \
               ((x) >= 0x40)

/**
 * Checks if a property is in the integer range.
 *
 * @param[in]  X  The property identifier.
 *
 * @return  A valid condition if the property is in the integer range.
 **/
#define P_NFCC_PROP_IS_INTEGER(x) \
               (((x) >= 0x20) && ((x) < 0x40))

/**
 * Checks if a property is in the string range.
 *
 * @param[in]  X  The property identifier.
 *
 * @return  A valid condition if the property is in the string range.
 **/
#define P_NFCC_PROP_IS_STRING(x) \
               ((x) < 0x20)

static const char16_t P_FALSE[] = { 'f', 'a', 'l', 's', 'e', 0 };
static const char16_t P_TRUE[] = { 't', 'r', 'u', 'e', 0 };

/**
 * Returns the internal value of a NFC Controller property.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nPropertyIdentifier  The property identifier.
 *
 * @return   The internal property value or null if the property is not found.
 **/
static const char16_t* static_PNFCControllerGetProperty(
         tContext * pContext,
         uint8_t nPropertyIdentifier )
{
   const char16_t* pValue;
   tNFCControllerInfo* pNFCControllerInfo = PContextGetNFCControllerInfo( pContext );
   uint32_t nProtocols = pNFCControllerInfo->nProtocolCapabilities;
   uint32_t nCapabilities = pNFCControllerInfo->nFirmwareCapabilities;

   switch(nPropertyIdentifier)
   {
      case W_NFCC_PROP_LIBRARY_VERSION:
         pValue = PBasicGetLibraryVersion();
         break;
      case W_NFCC_PROP_LIBRARY_IMPL:
         pValue = PBasicGetLibraryImplementation();
         break;
      case W_NFCC_PROP_LOADER_VERSION:
         pValue = (const char16_t*)&pNFCControllerInfo->aLoaderVersion;
         break;
      case W_NFCC_PROP_FIRMWARE_VERSION:
         pValue = (const char16_t*)&pNFCControllerInfo->aFirmwareVersion;
         break;
      case W_NFCC_PROP_HARDWARE_VERSION:
         pValue = (const char16_t*)&pNFCControllerInfo->aHardwareVersion;
         break;
      case W_NFCC_PROP_HARDWARE_SN:
         pValue = (const char16_t*)&pNFCControllerInfo->aHardwareSerialNumber;
         break;
      case W_NFCC_PROP_NFC_HAL_VERSION:
         pValue = (const char16_t*)&pNFCControllerInfo->nNALVersion;
         break;
      case W_NFCC_PROP_SE_NUMBER:
         pValue = (const char16_t*)&pNFCControllerInfo->nSeNumber;
         break;
      case W_NFCC_PROP_READER_ISO_14443_A_MAX_RATE:
         pValue = (const char16_t*)&pNFCControllerInfo->nReaderISO14443_A_MaxRate;
         break;
      case W_NFCC_PROP_READER_ISO_14443_A_INPUT_SIZE:
         pValue = (const char16_t*)&pNFCControllerInfo->nReaderISO14443_A_InputSize;
         break;
      case W_NFCC_PROP_READER_ISO_14443_B_MAX_RATE:
         pValue = (const char16_t*)&pNFCControllerInfo->nReaderISO14443_B_MaxRate;
         break;
      case W_NFCC_PROP_READER_ISO_14443_B_INPUT_SIZE:
         pValue = (const char16_t*)&pNFCControllerInfo->nReaderISO14443_B_InputSize;
         break;
      case W_NFCC_PROP_CARD_ISO_14443_A_MAX_RATE:
         pValue = (const char16_t*)&pNFCControllerInfo->nCardISO14443_A_MaxRate;
         break;
      case W_NFCC_PROP_CARD_ISO_14443_B_MAX_RATE:
         pValue = (const char16_t*)&pNFCControllerInfo->nCardISO14443_B_MaxRate;
         break;
      case W_NFCC_PROP_STANDBY_TIMEOUT:
         pValue = (const char16_t*)&pNFCControllerInfo->nAutoStandbyTimeout;
         break;
      case W_NFCC_PROP_BATTERY_LOW_SUPPORTED:
         pValue = ((nCapabilities & NAL_CAPA_BATTERY_LOW) != 0)?P_TRUE:P_FALSE;
         break;
      case W_NFCC_PROP_BATTERY_OFF_SUPPORTED:
         pValue = ((nCapabilities & NAL_CAPA_BATTERY_OFF) != 0)?P_TRUE:P_FALSE;
         break;
      case W_NFCC_PROP_STANDBY_SUPPORTED:
         pValue = ((nCapabilities & NAL_CAPA_STANDBY_MODE) != 0)?P_TRUE:P_FALSE;
         break;
      case W_NFCC_PROP_ROUTING_TABLE_SUPPORTED:
         pValue = ((nCapabilities & NAL_CAPA_ROUTING_TABLE) != 0)?P_TRUE:P_FALSE;
         break;
      case W_NFCC_PROP_CARD_ISO_14443_A_CID:
         pValue = ((nCapabilities & NAL_CAPA_CARD_ISO_14443_A_CID) != 0)?P_TRUE:P_FALSE;
         break;
      case W_NFCC_PROP_CARD_ISO_14443_A_NAD:
         pValue = ((nCapabilities & NAL_CAPA_CARD_ISO_14443_A_NAD) != 0)?P_TRUE:P_FALSE;
         break;
      case W_NFCC_PROP_CARD_ISO_14443_B_CID:
         pValue = ((nCapabilities & NAL_CAPA_CARD_ISO_14443_B_CID) != 0)?P_TRUE:P_FALSE;
         break;
      case W_NFCC_PROP_CARD_ISO_14443_B_NAD:
         pValue = ((nCapabilities & NAL_CAPA_CARD_ISO_14443_B_NAD) != 0)?P_TRUE:P_FALSE;
         break;
      case W_NFCC_PROP_READER_ISO_14443_A_CID:
         pValue = ((nCapabilities & NAL_CAPA_READER_ISO_14443_A_CID) != 0)?P_TRUE:P_FALSE;
         break;
      case W_NFCC_PROP_READER_ISO_14443_A_NAD:
         pValue = ((nCapabilities & NAL_CAPA_READER_ISO_14443_A_NAD) != 0)?P_TRUE:P_FALSE;
         break;
      case W_NFCC_PROP_READER_ISO_14443_B_CID:
         pValue = ((nCapabilities & NAL_CAPA_READER_ISO_14443_B_CID) != 0)?P_TRUE:P_FALSE;
         break;
      case W_NFCC_PROP_READER_ISO_14443_B_NAD:
         pValue = ((nCapabilities & NAL_CAPA_READER_ISO_14443_B_NAD) != 0)?P_TRUE:P_FALSE;
         break;
      case W_NFCC_PROP_READER_ISO_14443_A_BIT:
         pValue = ((nCapabilities & NAL_CAPA_READER_ISO_14443_A_BIT) != 0)?P_TRUE:P_FALSE;
         break;
      case W_NFCC_PROP_READER_MIFARE_CLASSIC_CRYPTO:
         pValue = ((nProtocols & W_NFCC_PROTOCOL_READER_MIFARE_CLASSIC) != 0)?P_TRUE:P_FALSE;
         break;

      case W_NFCC_PROP_P2P:
         if(((nProtocols & W_NFCC_PROTOCOL_READER_P2P_INITIATOR) != 0)
         && ((nProtocols & W_NFCC_PROTOCOL_CARD_P2P_TARGET) != 0))
         {
            pValue = P_TRUE;
         }
         else
         {
            pValue = P_FALSE;
         }
         break;

      default:
         return null;
   }

   return pValue;
}

/* See Client API Specification */
W_ERROR WNFCControllerGetFirmwareProperty(
                  const uint8_t* pUpdateBuffer,
                  uint32_t nUpdateBufferLength,
                  uint8_t nPropertyIdentifier,
                  char16_t* pValueBuffer,
                  uint32_t nBufferLength,
                  uint32_t* pnValueLength )
{
   W_ERROR nError = W_SUCCESS;
   uint32_t nLength = 0;
   PDebugTrace("WNFCControllerGetFirmwareProperty()");

   if((pUpdateBuffer == null)
   || (nUpdateBufferLength == 0)
   || (pValueBuffer == null)
   || (nBufferLength == 0)
   || (pnValueLength == null))
   {
      PDebugError("WNFCControllerGetFirmwareProperty: Bad parameter");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_function;
   }

   /* ideally, the firmware format is specific to the NFCC HAL implementation , so this should be performed
      in the NFCC HAL itself, but for now, we only support one NFCC HAL implementation so there's no problem
      to do it there... */

   if((nUpdateBufferLength < NAL_FIRMWARE_HEADER_SIZE) ||
      (PUtilReadUint32FromLittleEndianBuffer(pUpdateBuffer) != NAL_FIRMWARE_FORMAT_MAGIC_NUMBER))
   {
      PDebugError("WNFCControllerGetFirmwareProperty: Wrong file format");
      nError = W_ERROR_BAD_FIRMWARE_FORMAT;
      goto return_function;
   }

   /* W_NFCC_PROP_HARDWARE_VERSION is kept for backward compatibility */
   if ((nPropertyIdentifier == W_NFCC_PROP_FIRMWARE_VERSION) || (nPropertyIdentifier == W_NFCC_PROP_HARDWARE_VERSION))
   {
      /* retrieve the title length */

      nLength = PUtilConvertUTF8ToUTF16(null, pUpdateBuffer + 0x1B , pUpdateBuffer[0x1A]);

      if(nLength == 0)
      {
         nError = W_ERROR_WRONG_RTD;
         goto return_function;
      }
      else if(nBufferLength <= nLength ) /* keep room for the leading zero */
      {
         nLength = 32;
         nError = W_ERROR_BUFFER_TOO_SHORT;
         goto return_function;
      }
      else
      {
         PUtilConvertUTF8ToUTF16(pValueBuffer, pUpdateBuffer + 0x1B, pUpdateBuffer[0x1A]);
      }
   }
   else
   {
      PDebugError("WNFCControllerGetFirmwareProperty: Unknown property");
      nError = W_ERROR_ITEM_NOT_FOUND;
      goto return_function;
   }

return_function:

   if(pnValueLength != null)
   {
      *pnValueLength = nLength;
   }

   if(nError == W_SUCCESS)
   {
      pValueBuffer[nLength] = 0;
   }

   return nError;
}

/* See Client API Specifications */
W_ERROR PNFCControllerGetProperty(
         tContext * pContext,
         uint8_t nPropertyIdentifier,
         char16_t* pValueBuffer,
         uint32_t nBufferLength,
         uint32_t* pnValueLength )
{
   W_ERROR nError = W_SUCCESS;

   /* Very special case, can be called before WBasicInit() */
   if(nPropertyIdentifier != W_NFCC_PROP_LIBRARY_VERSION)
   {
      if(pContext == null)
      {
         return W_ERROR_BAD_STATE;
      }
      else
      {
         PContextLock(pContext);
      }
   }

   nError = PNFCControllerInternalGetProperty(
         pContext, nPropertyIdentifier,
         pValueBuffer, nBufferLength,
         pnValueLength );

   if(nPropertyIdentifier != W_NFCC_PROP_LIBRARY_VERSION)
   {
      PContextReleaseLock(pContext);
   }

   return nError;
}

/* See header file */
W_ERROR PNFCControllerInternalGetProperty(
         tContext * pContext,
         uint8_t nPropertyIdentifier,
         char16_t* pValueBuffer,
         uint32_t nBufferLength,
         uint32_t* pnValueLength )
{
   const char16_t* pTempBuffer;
   W_ERROR nError = W_SUCCESS;
   uint32_t nLength = 0;

   /* Check the output length parameter before others because it is needed even if an error occurs */
   if ( pnValueLength == null )
   {
      return W_ERROR_BAD_PARAMETER;
   }

   /* Check the parameter */
   if ( (pValueBuffer == null) && (nBufferLength != 0))
   {
      nError = W_ERROR_BAD_PARAMETER;
      goto return_function;
   }

   /* Very special case, can be called before WBasicInit() */
   if(nPropertyIdentifier == W_NFCC_PROP_LIBRARY_VERSION)
   {
      pTempBuffer = PBasicGetLibraryVersion();
   }
   else
   {
      pTempBuffer = static_PNFCControllerGetProperty(pContext, nPropertyIdentifier);
   }

   if(pTempBuffer == null)
   {
      nError = W_ERROR_ITEM_NOT_FOUND;
      goto return_function;
   }

   if(nPropertyIdentifier == W_NFCC_PROP_LOADER_VERSION)
   {
      if( nBufferLength < NAL_LOADER_DESCRIPTION_STRING_SIZE )
      {
         nLength = NAL_LOADER_DESCRIPTION_STRING_SIZE;
         nError = W_ERROR_BUFFER_TOO_SHORT;
         goto return_function;
      }

      for(nLength = 0; nLength < NAL_LOADER_DESCRIPTION_STRING_SIZE; nLength++)
      {
         if((pValueBuffer[nLength] = ((uint8_t*)pTempBuffer)[nLength]) == 0)  /* ASCII to Unicode */
         {
            break;
         }
      }

      pValueBuffer += nLength;
   }
   else if(nPropertyIdentifier == W_NFCC_PROP_HARDWARE_VERSION)
   {
      if( nBufferLength < NAL_HARDWARE_TYPE_STRING_SIZE )
      {
         nLength = NAL_HARDWARE_TYPE_STRING_SIZE;
         nError = W_ERROR_BUFFER_TOO_SHORT;
         goto return_function;
      }

      for(nLength = 0; nLength < NAL_HARDWARE_TYPE_STRING_SIZE; nLength++)
      {
         if((pValueBuffer[nLength] = ((uint8_t*)pTempBuffer)[nLength]) == 0)  /* ASCII to Unicode */
         {
            break;
         }
      }

      pValueBuffer += nLength;
   }
   else if(nPropertyIdentifier == W_NFCC_PROP_FIRMWARE_VERSION)
   {
      if( nBufferLength < NAL_FIRMWARE_DESCRIPTION_STRING_SIZE )
      {
         nLength = NAL_FIRMWARE_DESCRIPTION_STRING_SIZE;
         nError = W_ERROR_BUFFER_TOO_SHORT;
         goto return_function;
      }

      for(nLength = 0; nLength < NAL_FIRMWARE_DESCRIPTION_STRING_SIZE; nLength++)
      {
         if((pValueBuffer[nLength] = ((uint8_t*)pTempBuffer)[nLength]) == 0)  /* ASCII to Unicode */
         {
            break;
         }
      }

      pValueBuffer += nLength;
   }
   else if(nPropertyIdentifier == W_NFCC_PROP_HARDWARE_SN)
   {
      if( nBufferLength < NAL_HARDWARE_SERIAL_NUMBER_STRING_SIZE )
      {
         nLength = NAL_HARDWARE_SERIAL_NUMBER_STRING_SIZE;
         nError = W_ERROR_BUFFER_TOO_SHORT;
         goto return_function;
      }

      for(nLength = 0; nLength < NAL_HARDWARE_SERIAL_NUMBER_STRING_SIZE; nLength++)
      {
         if((pValueBuffer[nLength] = ((uint8_t*)pTempBuffer)[nLength]) == 0)  /* ASCII to Unicode */
         {
            break;
         }
      }

      pValueBuffer += nLength;
   }
   else if(nPropertyIdentifier == W_NFCC_PROP_NFC_HAL_VERSION)
   {
      uint32_t nValue = *((uint8_t*)pTempBuffer);

      if( nBufferLength < 4)
      {
         nLength = 4;
         nError = W_ERROR_BUFFER_TOO_SHORT;
         goto return_function;
      }

      nLength = PUtilWriteDecimalUint32(pValueBuffer, nValue);
      pValueBuffer += nLength;
   }
   else if(nPropertyIdentifier == W_NFCC_PROP_SE_NUMBER)
   {
      uint32_t nValue = *((uint8_t*)pTempBuffer);

      if( nBufferLength < 2)
      {
         nLength = 2;
         nError = W_ERROR_BUFFER_TOO_SHORT;
         goto return_function;
      }

      nLength = PUtilWriteDecimalUint32(pValueBuffer, nValue);
      pValueBuffer += nLength;
   }
   else if(P_NFCC_PROP_IS_INTEGER(nPropertyIdentifier))
   {
      if( nBufferLength < 9)
      {
         nLength = 9;
         nError = W_ERROR_BUFFER_TOO_SHORT;
         goto return_function;
      }

      nLength = PUtilWriteHexaUint32(pValueBuffer, *(uint32_t*)pTempBuffer);
      pValueBuffer += nLength;
   }
   else
   {
      nLength = PUtilStringLength( pTempBuffer );

      if( nLength + 1 > nBufferLength )
      {
         nError = W_ERROR_BUFFER_TOO_SHORT;
         goto return_function;
      }

      CMemoryCopy( pValueBuffer, pTempBuffer, nLength * sizeof(char16_t));
      pValueBuffer += nLength;
   }

return_function:

   if(nError == W_SUCCESS)
   {
      *pValueBuffer = 0;
      *pnValueLength = nLength;
   }
   else if(nError == W_ERROR_BUFFER_TOO_SHORT)
   {
      *pnValueLength = nLength;
   }
   else
   {
      *pnValueLength = 0;
   }

   return nError;
}

/* See Client API Specifications */
W_ERROR PNFCControllerGetBooleanProperty(
         tContext * pContext,
         uint8_t nPropertyIdentifier,
         bool_t* pbValue )
{
   const char16_t* pTempBuffer = static_PNFCControllerGetProperty(pContext, nPropertyIdentifier);

   if(pbValue == null)
   {
      return W_ERROR_BAD_PARAMETER;
   }

   *pbValue = W_FALSE;

   if(pTempBuffer != null)
   {
      if(P_NFCC_PROP_IS_BOOLEAN(nPropertyIdentifier))
      {
         if(pTempBuffer == P_TRUE)
         {
            *pbValue = W_TRUE;
         }

         return W_SUCCESS;
      }

      return W_ERROR_BAD_PARAMETER;
   }

   return W_ERROR_ITEM_NOT_FOUND;
}

/* See Client API Specifications */
W_ERROR PNFCControllerGetIntegerProperty(
         tContext * pContext,
         uint8_t nPropertyIdentifier,
         uint32_t* pnValue )
{
   const char16_t* pTempBuffer = static_PNFCControllerGetProperty(pContext, nPropertyIdentifier);

   if(pnValue == null)
   {
      return W_ERROR_BAD_PARAMETER;
   }

   *pnValue = 0;

   if(pTempBuffer != null)
   {
      if (nPropertyIdentifier == W_NFCC_PROP_NFC_HAL_VERSION)
      {
         *pnValue = *(uint8_t*)pTempBuffer;

         return W_SUCCESS;
      }

      if(P_NFCC_PROP_IS_INTEGER(nPropertyIdentifier))
      {
         *pnValue = *(uint32_t*)pTempBuffer;

         return W_SUCCESS;
      }

      return W_ERROR_BAD_PARAMETER;
   }

   return W_ERROR_ITEM_NOT_FOUND;
}


static void static_PNFCControllerProductionTestDriverCompleted(
   tContext * pContext,
   void * pCallbackParameter,
   uint32_t nLength,
   W_ERROR nError)
{
   PDFCPostContext3((tDFCCallbackContext * ) pCallbackParameter, nLength, nError);
   CMemoryFree(pCallbackParameter);
}


void PNFCControllerProductionTest(
      tContext * pContext,
      const uint8_t* pParameterBuffer,
      uint32_t nParameterBufferLength,
      uint8_t* pResultBuffer,
      uint32_t nResultBufferLength,
      tPBasicGenericDataCallbackFunction* pCallback,
      void* pCallbackParameter)
{
   tDFCCallbackContext sCallbackContext;
   tDFCCallbackContext * pCallbackContext;
   W_ERROR nError;

   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);

   if  ( ((pParameterBuffer == null) && (nParameterBufferLength != 0)) ||
         ((pParameterBuffer != null) && (nParameterBufferLength == 0)) ||
         ((pResultBuffer == null) && (nResultBufferLength != 0)) ||
         ((pResultBuffer != null) && (nResultBufferLength == 0)))
   {
      nError = W_ERROR_BAD_PARAMETER;
      goto error;
   }

   if ((pCallbackContext = (tDFCCallbackContext *) CMemoryAlloc(sizeof(tDFCCallbackContext))) == null)
   {
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto error;
   }

   * pCallbackContext = sCallbackContext;

   PNFCControllerProductionTestDriver(
      pContext,
      pParameterBuffer, nParameterBufferLength,
      pResultBuffer, nResultBufferLength,
      static_PNFCControllerProductionTestDriverCompleted, pCallbackContext);

   nError = PContextGetLastIoctlError(pContext);
   if (nError != W_SUCCESS)
   {
      PDFCFillCallbackContext(pContext, (tDFCCallback*) static_PNFCControllerProductionTestDriverCompleted, pCallbackContext, &sCallbackContext);
      PDFCPostContext3(&sCallbackContext, 0, nError);
   }

   return;

error:

   PDFCPostContext3(& sCallbackContext, 0, nError);
}


static void static_PNFCControllerResetDriverCompleted(
   tContext * pContext,
   void * pCallbackParameter,
   W_ERROR nError)
{
   PDFCPostContext2((tDFCCallbackContext *) pCallbackParameter, nError);
   CMemoryFree(pCallbackParameter);
}


void PNFCControllerReset(
      tContext* pContext,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter,
      uint32_t nMode )
{
   tDFCCallbackContext * pCallbackContext = (tDFCCallbackContext *) CMemoryAlloc(sizeof(tDFCCallbackContext));
   W_ERROR nError;

   if (pCallbackContext != null)
   {
      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, pCallbackContext);

      PNFCControllerResetDriver(
         pContext,
         static_PNFCControllerResetDriverCompleted, pCallbackContext, nMode);

      nError = PContextGetLastIoctlError(pContext);
      if (nError != W_SUCCESS)
      {
         tDFCCallbackContext sCallbackContext;
         PDFCFillCallbackContext(pContext, (tDFCCallback*) static_PNFCControllerResetDriverCompleted, pCallbackContext, &sCallbackContext);
         PDFCPostContext2(&sCallbackContext, nError);
      }
   }
   else
   {
      tDFCCallbackContext sCallbackContext;
      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext2(&sCallbackContext, W_ERROR_OUT_OF_RESOURCE);
   }
}


static void static_PNFCControllerSeltTestCompleted(
   tContext * pContext,
   void * pCallbackParameter,
   W_ERROR nError,
   uint32_t nResult)
{
   PDFCPostContext3((tDFCCallbackContext *) pCallbackParameter, nError, nResult);
   CMemoryFree(pCallbackParameter);
}

/* See API documentation */
void PNFCControllerSelfTest(
      tContext* pContext,
      tPNFCControllerSelfTestCompleted * pCallback,
      void * pCallbackParameter)
{
   tDFCCallbackContext * pCallbackContext = (tDFCCallbackContext *) CMemoryAlloc(sizeof(tDFCCallbackContext));
   W_ERROR nError;

   if (pCallbackContext != null)
   {
      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, pCallbackContext);

      PNFCControllerSelfTestDriver(
         pContext,
         static_PNFCControllerSeltTestCompleted, pCallbackContext);

      nError = PContextGetLastIoctlError(pContext);
      if (nError != W_SUCCESS)
      {
         tDFCCallbackContext sCallbackContext;
         PDFCFillCallbackContext(pContext, (tDFCCallback*) static_PNFCControllerSeltTestCompleted, pCallbackContext, &sCallbackContext);
         PDFCPostContext3(&sCallbackContext, nError, 1);
      }
   }
   else
   {
      tDFCCallbackContext sCallbackContext;

      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext3(&sCallbackContext, W_ERROR_OUT_OF_RESOURCE, 1);
   }
}

/* See API documentation */
W_ERROR PNFCControllerActivateSwpLine(
      tContext* pContext,
      uint32_t nSlotIdentifier)
{
   return PSEDriverActivateSwpLine(pContext, nSlotIdentifier);
}


static void static_PNFCControllerSetRFLockDriverCompleted(
   tContext* pContext,
   void * pCallbackParameter,
   W_ERROR nError)
{
   PDFCPostContext2((tDFCCallbackContext *) pCallbackParameter, nError);
   CMemoryFree(pCallbackParameter);
}


void PNFCControllerSetRFLock(
      tContext* pContext,
      uint32_t nLockSet,
      bool_t bReaderLock,
      bool_t bCardLock,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter )
{
   tDFCCallbackContext * pCallbackContext = (tDFCCallbackContext *) CMemoryAlloc(sizeof(tDFCCallbackContext));
   W_ERROR nError;

   if (pCallbackContext != null)
   {
      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, pCallbackContext);

      PNFCControllerSetRFLockDriver(
         pContext,
         nLockSet, bReaderLock, bCardLock,
         static_PNFCControllerSetRFLockDriverCompleted, pCallbackContext);

      nError = PContextGetLastIoctlError(pContext);
      if (nError != W_SUCCESS)
      {
         tDFCCallbackContext sCallbackContext;
         PDFCFillCallbackContext(pContext, (tDFCCallback *) static_PNFCControllerSetRFLockDriverCompleted, pCallbackContext, &sCallbackContext);
         PDFCPostContext2(&sCallbackContext, nError);
      }
   }
   else
   {
      tDFCCallbackContext sCallbackContext;

      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext2(&sCallbackContext, W_ERROR_OUT_OF_RESOURCE);
   }
}


static void static_PNFCControllerSwitchToRawModeDriverCompleted(
   tContext* pContext,
   void * pCallbackParameter,
   W_ERROR nError)
{
   PDFCPostContext2((tDFCCallbackContext *) pCallbackParameter, nError);
   CMemoryFree(pCallbackParameter);
}


void PNFCControllerSwitchToRawMode(
      tContext * pContext,
      tPBasicGenericCallbackFunction* pCallback,
      void* pCallbackParameter)
{
   tDFCCallbackContext * pCallbackContext = (tDFCCallbackContext *) CMemoryAlloc(sizeof(tDFCCallbackContext));
   W_ERROR nError;

   if (pCallbackContext != null)
   {
      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, pCallbackContext);

      PNFCControllerSwitchToRawModeDriver(
         pContext,
         static_PNFCControllerSwitchToRawModeDriverCompleted, pCallbackContext);

      nError = PContextGetLastIoctlError(pContext);
      if (nError != W_SUCCESS)
      {
         tDFCCallbackContext sCallbackContext;
         PDFCFillCallbackContext(pContext, (tDFCCallback *) static_PNFCControllerSwitchToRawModeDriverCompleted, pCallbackContext, &sCallbackContext);
         PDFCPostContext2(&sCallbackContext, nError);
      }
   }
   else
   {
      tDFCCallbackContext sCallbackContext;

      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext2(&sCallbackContext, W_ERROR_OUT_OF_RESOURCE);
   }
}


static void static_PNFCControllerWriteRawMessageCompleted(
   tContext* pContext,
   void * pCallbackParameter,
   W_ERROR nError)
{
   PDFCPostContext2((tDFCCallbackContext *) pCallbackParameter, nError);
   CMemoryFree(pCallbackParameter);
}



void PNFCControllerWriteRawMessage(
      tContext * pContext,
      tPBasicGenericCallbackFunction* pCallback,
      void* pCallbackParameter,
      const uint8_t * pBuffer,
      uint32_t nLength
      )
{
   tDFCCallbackContext * pCallbackContext = (tDFCCallbackContext *) CMemoryAlloc(sizeof(tDFCCallbackContext));
   W_ERROR nError;

   if (pCallbackContext != null)
   {
      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, pCallbackContext);

      PNFCControllerWriteRawMessageDriver(
         pContext,
         static_PNFCControllerSwitchToRawModeDriverCompleted, pCallbackContext,
         pBuffer, nLength);

      nError = PContextGetLastIoctlError(pContext);
      if (nError != W_SUCCESS)
      {
         tDFCCallbackContext sCallbackContext;
         PDFCFillCallbackContext(pContext, (tDFCCallback *) static_PNFCControllerWriteRawMessageCompleted, pCallbackContext, &sCallbackContext);
         PDFCPostContext2(&sCallbackContext, nError);
      }
   }
   else
   {
      tDFCCallbackContext sCallbackContext;

      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext2(& sCallbackContext, W_ERROR_OUT_OF_RESOURCE);
   }
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

/* -----------------------------------------------------------------------------

   RF Lock Functions

----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* Forward declaration */
static void static_PNFCControllerExecuteNextPolicyOperation(
         tContext * pContext,
         tNFCControllerPolicyMonitor* pPolicy);

/**
 * Pushes an operation in the operation list.
 *
 * @param[in] pPolicy  The policy.
 *
 * @param[in] nOperation  The operation to push
 **/
static void static_PNFCControllerPushOperation(
         tNFCControllerPolicyMonitor* pPolicy,
         uint8_t nOperation)
{
   uint32_t nLevel = pPolicy->nPendingOperations;

   CDebugAssert(nLevel < P_NFCC_MAX_NUMBER_PENDING_OPERATIONS);

   pPolicy->aPendingOperations[nLevel++] = nOperation;

   pPolicy->nPendingOperations = nLevel;
}

/**
 * Checks the presence of an operation in the operation list.
 *
 * @param[in] pPolicy  The policy.
 *
 * @param[in] nOperation  The operation to check.
 *
 * @return  W_TRUE if the operation is present, W_FALSE otherwise.
 **/
static bool_t static_PNFCControllerCheckOperation(
         tNFCControllerPolicyMonitor* pPolicy,
         uint8_t nOperation)
{
   uint32_t nPos;

   for(nPos = 0; nPos < pPolicy->nPendingOperations; nPos++)
   {
      if((pPolicy->aPendingOperations[nPos] & 0x7F) == (nOperation & 0x7F))
      {
         return W_TRUE;
      }
   }

   return W_FALSE;
}

/**
 * Checks and removes an operation in the operation list.
 *
 * @param[in] pPolicy  The policy.
 *
 * @param[in] nOperation  The operation to check.
 *
 * @return  The operation code if the operation is present, zero otherwise.
 **/
static uint8_t static_PNFCControllerCheckAndRemoveOperation(
         tNFCControllerPolicyMonitor* pPolicy,
         uint8_t nOperation)
{
   uint32_t nPos;
   uint8_t nTrueOperation;

   for(nPos = 0; nPos < pPolicy->nPendingOperations; nPos++)
   {
      nTrueOperation = pPolicy->aPendingOperations[nPos];
      if((nTrueOperation & 0x7F) == (nOperation & 0x7F))
      {
         pPolicy->nPendingOperations--;

         while(nPos < pPolicy->nPendingOperations)
         {
            pPolicy->aPendingOperations[nPos] = pPolicy->aPendingOperations[nPos + 1];
            nPos++;
         }
         return nTrueOperation;
      }
   }

   return 0;
}

/** Checks if an operation requires to call the callback function */
#define PNFCCOperationIsNotNop(X) \
            (((X) & P_NFCC_OPERATION_NOP) == 0)

/**
 * Receives the completion of the set parameter operation.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pCallbackParameter  The blind parameter containing the operation code.
 *
 * @param[in]  nError  The result of the operation.
 **/
static void static_PNFCControllerSetParameterCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         W_ERROR nError)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   tNFCControllerPolicyMonitor* pPolicy = &pNFCController->sPolicyMonitor;
   uint8_t nOperation = (uint8_t)PUtilConvertPointerToUint(pCallbackParameter);

   PDebugTrace("static_PNFCControllerSetParameterCompleted(nOperation=%02x)", nOperation);

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PNFCControllerSetParameterCompleted: Error %s",
         PUtilTraceError(nError));
   }

   if(static_PNFCControllerCheckAndRemoveOperation(pPolicy, nOperation) == 0)
   {
      CDebugAssert(W_FALSE);  /* Issue detected */
   }

   switch(nOperation & 0x7F)
   {
      case P_NFCC_OPERATION_SET_RF_LOCK_VOLATILE:

         PDebugTrace("static_PNFCControllerSetParameterCompleted : P_NFCC_OPERATION_SET_RF_LOCK_VOLATILE");

         if(nError == W_SUCCESS)
         {
            /* Update the current values */
            pPolicy->sVolatile.bReaderRFLock = pPolicy->sNewVolatile.bReaderRFLock;
            pPolicy->sVolatile.bCardRFLock = pPolicy->sNewVolatile.bCardRFLock;
         }
         else
         {
            /* Erase the result */
            pPolicy->sNewVolatile.bReaderRFLock = pPolicy->sVolatile.bReaderRFLock;
            pPolicy->sNewVolatile.bCardRFLock = pPolicy->sVolatile.bCardRFLock;
         }


         /* Send the result */
         if(PNFCCOperationIsNotNop(nOperation))
         {
            PDFCDriverPostCC2( pPolicy->pSetVolatileRFLockDriverCC, nError );
         }

      break;

      case P_NFCC_OPERATION_SET_RF_LOCK_PERSISTENT:

         PDebugTrace("static_PNFCControllerSetParameterCompleted : P_NFCC_OPERATION_SET_RF_LOCK_PERSISTENT");

         if(nError == W_SUCCESS)
         {
            /* Update the current values */
            pPolicy->sPersistent.bReaderRFLock = pPolicy->sNewPersistent.bReaderRFLock;
            pPolicy->sPersistent.bCardRFLock = pPolicy->sNewPersistent.bCardRFLock;
         }
         else
         {
            /* Erase the result */
            pPolicy->sNewPersistent.bReaderRFLock = pPolicy->sPersistent.bReaderRFLock;
            pPolicy->sNewPersistent.bCardRFLock = pPolicy->sPersistent.bCardRFLock;
         }

         /* Send the result */
         if(PNFCCOperationIsNotNop(nOperation))
         {
            PDFCDriverPostCC2( pPolicy->pSetPersistentRFLockDriverCC, nError );
         }

      break;

      case P_NFCC_OPERATION_SET_UICC_ACCESS_VOLATILE:

         PDebugTrace("static_PNFCControllerSetParameterCompleted : P_NFCC_OPERATION_SET_UICC_ACCESS_VOLATILE");

         if(nError == W_SUCCESS)
         {
            /* Update the current values */
            pPolicy->sVolatile.nUICCProtocolPolicy = pPolicy->sNewVolatile.nUICCProtocolPolicy;
         }
         else
         {
            /* Erase the result */
            pPolicy->sNewVolatile.nUICCProtocolPolicy = pPolicy->sVolatile.nUICCProtocolPolicy;
         }

         /* Send the result */
         if(PNFCCOperationIsNotNop(nOperation))
         {
            PDFCPostContext2( &pPolicy->sSetVolatileUICCPolicyCC, nError );
         }
      break;

      case P_NFCC_OPERATION_SET_UICC_ACCESS_PERSISTENT:

         PDebugTrace("static_PNFCControllerSetParameterCompleted : P_NFCC_OPERATION_SET_UICC_ACCESS_PERSISTENT");

         if(nError == W_SUCCESS)
         {
            /* Update the current values */
            pPolicy->sPersistent.nUICCProtocolPolicy = pPolicy->sNewPersistent.nUICCProtocolPolicy;
         }
         else
         {
            /* Erase the result */
            pPolicy->sNewPersistent.nUICCProtocolPolicy = pPolicy->sPersistent.nUICCProtocolPolicy;
         }

         /* Send the result */
         if(PNFCCOperationIsNotNop(nOperation))
         {
            PDFCPostContext2( &pPolicy->sSetPersistentUICCPolicyCC, nError );
         }
      break;

      case P_NFCC_OPERATION_SET_UICC_READER_PERSISTENT:

         PDebugTrace("static_PNFCControllerSetParameterCompleted : P_NFCC_OPERATION_SET_UICC_READER_PERSISTENT");

         /* Send the result */
         if(PNFCCOperationIsNotNop(nOperation))
         {
            PDFCPostContext2( &pPolicy->sSetPersistentUICCPolicyCC, nError );
         }
      break;

      case P_NFCC_OPERATION_SET_SE_SWITCH_VOLATILE:

         PDebugTrace("static_PNFCControllerSetParameterCompleted : P_NFCC_OPERATION_SET_SE_SWITCH_VOLATILE");

         if(nError == W_SUCCESS)
         {
            /* Update the current values */
            pPolicy->sVolatile.sSEPolicy.nSESwitchPosition = pPolicy->sNewVolatile.sSEPolicy.nSESwitchPosition;
            pPolicy->sVolatile.sSEPolicy.nSlotIdentifier = pPolicy->sNewVolatile.sSEPolicy.nSlotIdentifier;
            pPolicy->sVolatile.sSEPolicy.nSEProtocolPolicy = pPolicy->sNewVolatile.sSEPolicy.nSEProtocolPolicy;
         }
         else
         {
            /* Erase the result */
            pPolicy->sNewVolatile.sSEPolicy.nSESwitchPosition = pPolicy->sVolatile.sSEPolicy.nSESwitchPosition;
            pPolicy->sNewVolatile.sSEPolicy.nSlotIdentifier = pPolicy->sVolatile.sSEPolicy.nSlotIdentifier;
            pPolicy->sNewVolatile.sSEPolicy.nSEProtocolPolicy = pPolicy->sVolatile.sSEPolicy.nSEProtocolPolicy;
         }

         /* Send the result */
         if(PNFCCOperationIsNotNop(nOperation))
         {
            PDFCPostContext2( &pPolicy->sSetVolatileSESwitchCC, nError );
         }
      break;

      case P_NFCC_OPERATION_SET_SE_SWITCH_PERSISTENT:

         PDebugTrace("static_PNFCControllerSetParameterCompleted : P_NFCC_OPERATION_SET_SE_SWITCH_PERSISTENT");

         if(nError == W_SUCCESS)
         {
            /* Update the current values */
            pPolicy->sPersistent.sSEPolicy.nSESwitchPosition = pPolicy->sNewPersistent.sSEPolicy.nSESwitchPosition;
            pPolicy->sPersistent.sSEPolicy.nSlotIdentifier = pPolicy->sNewPersistent.sSEPolicy.nSlotIdentifier;
            pPolicy->sPersistent.sSEPolicy.nSEProtocolPolicy = pPolicy->sNewPersistent.sSEPolicy.nSEProtocolPolicy;
         }
         else
         {
            /* Erase the result */
            pPolicy->sNewPersistent.sSEPolicy.nSESwitchPosition = pPolicy->sPersistent.sSEPolicy.nSESwitchPosition;
            pPolicy->sNewPersistent.sSEPolicy.nSlotIdentifier = pPolicy->sPersistent.sSEPolicy.nSlotIdentifier;
            pPolicy->sNewPersistent.sSEPolicy.nSEProtocolPolicy = pPolicy->sPersistent.sSEPolicy.nSEProtocolPolicy;
         }

         /* Send the result */
         if(PNFCCOperationIsNotNop(nOperation))
         {
            PDFCPostContext2( &pPolicy->sSetPersistentSESwitchCC, nError );
         }
      break;

      default:
         CDebugAssert(W_FALSE); /* Issue detected */
      break;
   }

   pPolicy->bOperationInProgress = W_FALSE;

   /* Execute the next operation if any */
   static_PNFCControllerExecuteNextPolicyOperation(pContext, pPolicy);
}

/**
 * Sets the policy parameter.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pPolicy  The policy monitor.
 *
 * @param[in]  nOperation  The operation code.
 *
 * @param[in]  pOperation  The operation structure.
 **/
static void static_PNFCControllerSetPolicyParameter(
         tContext * pContext,
         tNFCControllerPolicyMonitor* pPolicy,
         uint8_t nOperation,
         tNALServiceOperation* pOperation)
{
   tNFCControllerPolicyParameters* pParameters;
   uint8_t nParameterCode;
   uint8_t* pValueBuffer;
   uint16_t nValue16;
   uint32_t nBattOffMode = 0;

   switch(nOperation & 0x7F)
   {
      case P_NFCC_OPERATION_SET_RF_LOCK_VOLATILE:
         pParameters = &pPolicy->sNewVolatile;
         nParameterCode = NAL_PAR_POLICY;
         pValueBuffer = pPolicy->aSetVolatileRFLockOperationBuffer;
         break;

      case P_NFCC_OPERATION_SET_RF_LOCK_PERSISTENT:
         pParameters = &pPolicy->sNewPersistent;
         nParameterCode = NAL_PAR_PERSISTENT_POLICY;
         pValueBuffer = pPolicy->aSetPersistentRFLockOperationBuffer;
         break;

      case P_NFCC_OPERATION_SET_UICC_ACCESS_VOLATILE:
         pParameters = &pPolicy->sNewVolatile;
         nParameterCode = NAL_PAR_POLICY;
         pValueBuffer = pPolicy->aSetVolatileUICCPolicyOperationBuffer;
         break;

      case P_NFCC_OPERATION_SET_UICC_ACCESS_PERSISTENT:
         pParameters = &pPolicy->sNewPersistent;
         nParameterCode = NAL_PAR_PERSISTENT_POLICY;
         pValueBuffer = pPolicy->aSetPersistentUICCPolicyOperationBuffer;
         nBattOffMode = NAL_POLICY_FLAG_ENABLE_UICC_IN_BATT_OFF;
         break;

      case P_NFCC_OPERATION_SET_SE_SWITCH_VOLATILE:
         pParameters = &pPolicy->sNewVolatile;
         nParameterCode = NAL_PAR_POLICY;
         pValueBuffer = pPolicy->aSetVolatileSESwitchOperationBuffer;
         break;

      case P_NFCC_OPERATION_SET_SE_SWITCH_PERSISTENT:
         pParameters = &pPolicy->sNewPersistent;
         nParameterCode = NAL_PAR_PERSISTENT_POLICY;
         pValueBuffer = pPolicy->aSetPersistentSESwitchOperationBuffer;
         nBattOffMode = NAL_POLICY_FLAG_ENABLE_SE_IN_BATT_OFF;
         break;

      default:
         CDebugAssert(W_FALSE);
         return;
   }

   /* NFC HAL SET UICC payload format :
      [0 - 1] Card protocols allowed to the UICC
   */

   PNALWriteCardProtocols(
      pParameters->nUICCProtocolPolicy,
      pValueBuffer);

   /* NFC HAL SET UICC payload format :
      [2 - 3] Reader protocols allowed to the UICC
   */

   PNALWriteReaderProtocols(
      pParameters->nUICCProtocolPolicy,
      &pValueBuffer[2]);


   /* NFC HAL SET UICC payload format :
      [4 - 5] Policy flags
   */

   nValue16 = 0;

   if(pParameters->bReaderRFLock == W_FALSE)
   {
      nValue16 |= NAL_POLICY_FLAG_READER_LOCK;
   }
   if(pParameters->bCardRFLock == W_FALSE)
   {
      nValue16 |= NAL_POLICY_FLAG_CARD_LOCK;
   }

   if(pParameters->sSEPolicy.nSESwitchPosition == P_SE_SWITCH_RF_INTERFACE)
   {
      nValue16 |= NAL_POLICY_FLAG_RF_INTERFACE;
   }
   else if(pParameters->sSEPolicy.nSESwitchPosition == P_SE_SWITCH_FORCED_HOST_INTERFACE)
   {
      nValue16 |= NAL_POLICY_FLAG_FORCED_HOST_INTERFACE;
   }
   else if(pParameters->sSEPolicy.nSESwitchPosition == P_SE_SWITCH_HOST_INTERFACE)
   {
      nValue16 |= NAL_POLICY_FLAG_HOST_INTERFACE;
   }
   else
   {
      CDebugAssert(pParameters->sSEPolicy.nSESwitchPosition == P_SE_SWITCH_OFF);
      nValue16 |= NAL_POLICY_FLAG_SE_OFF;
   }

   /* Set the SE slot identifier */
   nValue16 |= (pParameters->sSEPolicy.nSlotIdentifier << 4);

   /* Set the battery off toggle operation */
   nValue16 |= nBattOffMode;

   PNALWriteUint16ToBuffer(nValue16, &pValueBuffer[4]);

   /* [6 - 7] Card protocols allowed to the SE */

   PNALWriteCardProtocols(
      pParameters->sSEPolicy.nSEProtocolPolicy,
      &pValueBuffer[6]);

   pPolicy->bOperationInProgress = W_TRUE;

   PNALServiceSetParameter(
         pContext,
         NAL_SERVICE_ADMIN,
         pOperation,
         nParameterCode,
         pValueBuffer, NAL_POLICY_SIZE,
         static_PNFCControllerSetParameterCompleted,
         PUtilConvertUintToPointer(nOperation) );
}


/**
 * Global state machine for the set policy functions
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pPolicy  The policy monitor.
 **/
static void static_PNFCControllerExecuteNextPolicyOperation(
         tContext * pContext,
         tNFCControllerPolicyMonitor* pPolicy)
{
   uint8_t nOperation;

   /* Shortcut, if no operation is pending, just return */
   if(pPolicy->nPendingOperations == 0)
   {
      return;
   }

   /* There is already an operation in progress, wait for its completion  */

   if (pPolicy->bOperationInProgress != W_FALSE)
   {
      return;
   }

   nOperation = static_PNFCControllerCheckAndRemoveOperation(pPolicy, P_NFCC_OPERATION_SET_RF_LOCK_VOLATILE);
   if(nOperation != 0)
   {
      PDebugTrace("Processing P_NFCC_OPERATION_SET_RF_LOCK_VOLATILE");

      static_PNFCControllerSetPolicyParameter(pContext, pPolicy,
         nOperation,
         &pPolicy->sSetVolatileRFLockOperation);
      goto push_again;
   }

   nOperation = static_PNFCControllerCheckAndRemoveOperation(pPolicy, P_NFCC_OPERATION_SET_RF_LOCK_PERSISTENT);
   if(nOperation != 0)
   {
      PDebugTrace("Processing P_NFCC_OPERATION_SET_RF_LOCK_PERSISTENT");

      static_PNFCControllerSetPolicyParameter(pContext, pPolicy,
         nOperation,
         &pPolicy->sSetPersistentRFLockOperation);
      goto push_again;
   }

   nOperation = static_PNFCControllerCheckAndRemoveOperation(pPolicy, P_NFCC_OPERATION_SET_UICC_ACCESS_VOLATILE);
   if(nOperation != 0)
   {
      PDebugTrace("Processing P_NFCC_OPERATION_SET_UICC_ACCESS_VOLATILE");

      static_PNFCControllerSetPolicyParameter(pContext, pPolicy,
         nOperation,
         &pPolicy->sSetVolatileUICCPolicyOperation);
      goto push_again;
   }

   nOperation = static_PNFCControllerCheckAndRemoveOperation(pPolicy, P_NFCC_OPERATION_SET_UICC_READER_PERSISTENT);
   if(nOperation != 0)
   {
      tDFCCallbackContext sCallbackContext;

      PDebugTrace("Processing P_NFCC_OPERATION_SET_UICC_READER_PERSISTENT");

      /* Dummy call replacing the useless writting of the policy in persistent memory */
      PDFCFillCallbackContext(
         pContext,
         (tDFCCallback*)static_PNFCControllerSetParameterCompleted,
         PUtilConvertUintToPointer(nOperation),
         &sCallbackContext );

      PDFCPostContext2(&sCallbackContext, W_SUCCESS);

      goto push_again;
   }

   nOperation = static_PNFCControllerCheckAndRemoveOperation(pPolicy, P_NFCC_OPERATION_SET_UICC_ACCESS_PERSISTENT);

   if(nOperation != 0)
   {
      PDebugTrace("Processing P_NFCC_OPERATION_SET_UICC_ACCESS_PERSISTENT");

      static_PNFCControllerSetPolicyParameter(pContext, pPolicy, nOperation, &pPolicy->sSetPersistentUICCPolicyOperation);
      goto push_again;
   }

   nOperation = static_PNFCControllerCheckAndRemoveOperation(pPolicy, P_NFCC_OPERATION_SET_SE_SWITCH_VOLATILE);
   if(nOperation != 0)
   {
      PDebugTrace("Processing P_NFCC_OPERATION_SET_SE_SWITCH_VOLATILE");

      static_PNFCControllerSetPolicyParameter(pContext, pPolicy,
         nOperation,
         &pPolicy->sSetVolatileSESwitchOperation);
      goto push_again;
   }

   nOperation = static_PNFCControllerCheckAndRemoveOperation(pPolicy, P_NFCC_OPERATION_SET_SE_SWITCH_PERSISTENT);
   if(nOperation != 0)
   {
      PDebugTrace("Processing P_NFCC_OPERATION_SET_SE_SWITCH_PERSISTENT");

      static_PNFCControllerSetPolicyParameter(pContext, pPolicy,
         nOperation,
         &pPolicy->sSetPersistentSESwitchOperation);
      goto push_again;
   }

   return;

push_again:

   static_PNFCControllerPushOperation(pPolicy, nOperation);
}

/* See Client API Specifications */
void PNFCControllerSetRFLockDriver(
         tContext * pContext,
         uint32_t nLockSet,
         bool_t bReaderLock,
         bool_t bCardLock,
         tPBasicGenericCallbackFunction* pCallback,
         void* pCallbackParameter)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   tDFCDriverCCReference pDriverCC;
   tNFCControllerPolicyMonitor* pPolicy = &pNFCController->sPolicyMonitor;
   W_ERROR nError;

   /* Check the NFC Controller mode */
   if(PNFCControllerIsActive(pContext) == W_FALSE)
   {
      nError = W_ERROR_BAD_NFCC_MODE;
      goto send_event;
   }

   if((nLockSet | (W_NFCC_STORAGE_VOLATILE | W_NFCC_STORAGE_PERSISTENT)) !=
      (W_NFCC_STORAGE_VOLATILE | W_NFCC_STORAGE_PERSISTENT) ||
      (nLockSet == 0))
   {
      PDebugError("PNFCControllerSetRFLock: Bad parameters");
      nError = W_ERROR_BAD_PARAMETER;
      goto send_event;
   }

   /* Check the parameters */
   if((nLockSet & W_NFCC_STORAGE_VOLATILE) != 0)
   {
      /* This flag is needed to protect the operation against re-entrantcy */
      if(static_PNFCControllerCheckOperation(pPolicy, P_NFCC_OPERATION_SET_RF_LOCK_VOLATILE) != W_FALSE)
      {
         PDebugError("PNFCControllerSetRFLock: operation already active");
         nError = W_ERROR_BAD_STATE;
         goto send_event;
      }
   }

   if((nLockSet & W_NFCC_STORAGE_PERSISTENT) != 0)
   {
      /* This flag is needed to protect the operation against re-entrantcy */
      if(static_PNFCControllerCheckOperation(pPolicy, P_NFCC_OPERATION_SET_RF_LOCK_PERSISTENT) != W_FALSE)
      {
         PDebugError("PNFCControllerSetRFLock: operation already active");
         nError = W_ERROR_BAD_STATE;
         goto send_event;
      }
   }

   if((nLockSet & W_NFCC_STORAGE_VOLATILE) != 0)
   {
      pPolicy->sNewVolatile.bReaderRFLock = bReaderLock;
      pPolicy->sNewVolatile.bCardRFLock = bCardLock;

      if(nLockSet == W_NFCC_STORAGE_VOLATILE)
      {
         static_PNFCControllerPushOperation(pPolicy, P_NFCC_OPERATION_SET_RF_LOCK_VOLATILE);

         PDFCDriverFillCallbackContext( pContext,
            (tDFCCallback*)pCallback, pCallbackParameter,
            &pNFCController->sPolicyMonitor.pSetVolatileRFLockDriverCC );
      }
      else
      {
         static_PNFCControllerPushOperation(pPolicy, P_NFCC_OPERATION_SET_RF_LOCK_VOLATILE | P_NFCC_OPERATION_NOP);
      }
   }

   if((nLockSet & W_NFCC_STORAGE_PERSISTENT) != 0)
   {
      static_PNFCControllerPushOperation(pPolicy, P_NFCC_OPERATION_SET_RF_LOCK_PERSISTENT);

      pPolicy->sNewPersistent.bReaderRFLock = bReaderLock;
      pPolicy->sNewPersistent.bCardRFLock = bCardLock;

      PDFCDriverFillCallbackContext( pContext,
            (tDFCCallback*)pCallback, pCallbackParameter,
            &pNFCController->sPolicyMonitor.pSetPersistentRFLockDriverCC );
   }

   static_PNFCControllerExecuteNextPolicyOperation(pContext, pPolicy);

   return;

send_event:

   PDFCDriverFillCallbackContext( pContext,
         (tDFCCallback*)pCallback, pCallbackParameter,
         &pDriverCC );

   PDFCDriverPostCC2(pDriverCC, nError);
}

/* See Client API Specifications */
void PNFCControllerGetRFLock(
         tContext * pContext,
         uint32_t nLockSet,
         bool_t* pbReaderLock,
         bool_t* pbCardLock )
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   bool_t bReaderLock = W_TRUE;
   bool_t bCardLock = W_FALSE;

   if(PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PNFCControllerGetRFLock: Bad mode for the NFC Controller");
   }
   else if(nLockSet == W_NFCC_STORAGE_VOLATILE)
   {
      bReaderLock = pNFCController->sPolicyMonitor.sVolatile.bReaderRFLock;
      bCardLock = pNFCController->sPolicyMonitor.sVolatile.bCardRFLock;
   }
   else if(nLockSet == W_NFCC_STORAGE_PERSISTENT)
   {
      bReaderLock = pNFCController->sPolicyMonitor.sPersistent.bReaderRFLock;
      bCardLock = pNFCController->sPolicyMonitor.sPersistent.bCardRFLock;
   }
   else
   {
      PDebugError("PNFCControllerGetRFLock: wrong value of the lock set");
      bReaderLock = W_FALSE;
      bCardLock = W_FALSE;
   }

   if(pbReaderLock != null)
   {
      *pbReaderLock = bReaderLock;
   }
   if(pbCardLock != null)
   {
      *pbCardLock = bCardLock;
   }
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

void PNFCControllerGetRFLock(
         tContext * pContext,
         uint32_t nLockSet,
         bool_t* pbReaderLock,
         bool_t* pbCardLock )
{
   uint32_t nLocks = PNFCControllerDriverGetRFLock(pContext, nLockSet);
   /*@ todo If the IOCTL failed, the nLocks is set to 0 */

   if(pbReaderLock != null)
   {
      *pbReaderLock = ((nLocks & 0x01) != 0)?W_TRUE:W_FALSE;
   }
   if(pbCardLock != null)
   {
      *pbCardLock = ((nLocks & 0x02) != 0)?W_TRUE:W_FALSE;
   }
}

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER)

uint32_t PNFCControllerDriverGetRFLock(
         tContext * pContext,
         uint32_t nLockSet)
{
   uint32_t nLocks = 0;
   bool_t bReaderLock;
   bool_t bCardLock;

   PNFCControllerGetRFLock(pContext, nLockSet, &bReaderLock, &bCardLock);

   if(bReaderLock)
   {
      nLocks |= 0x01;
   }
   if(bCardLock)
   {
      nLocks |= 0x02;
   }

   return nLocks;
}

#endif /* P_CONFIG_DRIVER */

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

W_ERROR WNFCControllerSetRFLockSync(
            uint32_t nLockSet,
            bool_t bReaderLock,
            bool_t bCardLock )
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WNFCControllerSetRFLock( nLockSet, bReaderLock, bCardLock,
         PBasicGenericSyncCompletion, &param);
   }

   return PBasicGenericSyncWaitForResult(&param);
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

/* -----------------------------------------------------------------------------

   Get RF Activity Functions

----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

uint32_t PNFCControllerDriverGetRFActivity(
            tContext* pContext)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   uint8_t nReaderState = W_NFCC_RF_ACTIVITY_INACTIVE;
   uint8_t nCardState = W_NFCC_RF_ACTIVITY_INACTIVE;
   uint8_t nP2PState = W_NFCC_RF_ACTIVITY_INACTIVE;

   if(PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PNFCControllerDriverGetRFActivity: Bad mode for the NFC Controller");
   }
   else
   {
      if(pNFCController->sPolicyMonitor.sVolatile.bReaderRFLock == W_FALSE)
      {
         nReaderState = PReaderDriverGetRFActivity(pContext);
      }

      if(pNFCController->sPolicyMonitor.sVolatile.bCardRFLock == W_FALSE)
      {
         if(pNFCController->sPolicyMonitor.sVolatile.sSEPolicy.nSESwitchPosition == P_SE_SWITCH_RF_INTERFACE)
         {
            /* If one of the SE is in card mode, the card RF is active */
            nCardState = W_NFCC_RF_ACTIVITY_ACTIVE;
         }
         else if((pNFCController->sPolicyMonitor.sVolatile.nUICCProtocolPolicy & W_NFCC_PROTOCOL_CARD_ALL) != 0)
         {
            /* If the UICC access a card protocol, the card RF is active */
            nCardState = W_NFCC_RF_ACTIVITY_ACTIVE;
         }
         else
         {
            nCardState = PEmulGetRFActivity(pContext);
         }
      }

      if((pNFCController->sPolicyMonitor.sVolatile.bReaderRFLock == W_FALSE)
      || (pNFCController->sPolicyMonitor.sVolatile.bCardRFLock == W_FALSE))
      {
         nP2PState = PP2PGetRFActivity(pContext);
      }
   }

   return ((uint32_t)nReaderState) | ((((uint32_t)nCardState) << 8) & 0x0000FF00)
      | ((((uint32_t)nP2PState) << 16) & 0x00FF0000);
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

void PNFCControllerGetRFActivity(
            tContext* pContext,
            uint8_t* pnReaderState,
            uint8_t* pnCardState,
            uint8_t* pnP2PState)
{
   uint32_t nRFActivity = PNFCControllerDriverGetRFActivity(pContext);
   /* @todo If the IOCTL failed, the RF activity is set to zero */

   if(pnReaderState != null)
   {
      *pnReaderState = (uint8_t)(nRFActivity & 0xFF);
   }
   if(pnCardState != null)
   {
      *pnCardState = (uint8_t)((nRFActivity >> 8) & 0xFF);
   }
   if(pnP2PState != null)
   {
      *pnP2PState = (uint8_t)((nRFActivity >> 16) & 0xFF);
   }
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

/* -----------------------------------------------------------------------------

   Standby Mode Functions

----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/**
  * Callback called when STANDBY event has been sent
  */

static void static_PNFCControllerStandbyEventSent(
         tContext* pContext,
         void* pCallbackParameter,
         W_ERROR nError,
         uint32_t nReceptionCounter)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   if (nError != W_SUCCESS)
   {
      PDebugError("Standby event could not be sent !");
   }
   /* Standby Operation Completed */
   pNFCController->bStandbyOperationInProgress = W_FALSE;
   /* Check if the pending standby value matches the current value */
   if ( pNFCController->bPendingStandby != pNFCController->bCurrentStandbyOn )
   {
      PNFCControllerSwitchStandbyMode(pContext, pNFCController->bPendingStandby);
   }
}


W_ERROR PNFCControllerSwitchStandbyMode(
            tContext* pContext,
            bool_t bStandbyOn )
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   tNFCControllerInfo* pNFCControllerInfo = PContextGetNFCControllerInfo( pContext );
   uint32_t nCurrentMode = PNALServiceGetVariable(pContext, NAL_PARAM_MODE);


   /* Check the current NFCCOntroller mode */
   if (nCurrentMode != W_NFCC_MODE_ACTIVE )
   {
      return W_ERROR_BAD_NFCC_MODE;
   }

   /* Check the current NFCCOntroller capabilities */
   if ((pNFCControllerInfo->nFirmwareCapabilities & NAL_CAPA_STANDBY_MODE) == 0)
   {
      return W_ERROR_FEATURE_NOT_SUPPORTED;
   }

   /* Store the requested value */
   pNFCController->bPendingStandby = bStandbyOn;

   /* Only send the event to the NFC HAL Service if the requested value changed */
   /* and if there is no standbyg operation is in progress                      */
   if ( (pNFCController->bCurrentStandbyOn != bStandbyOn) && (pNFCController->bStandbyOperationInProgress == W_FALSE) )
   {
      pNFCController->bCurrentStandbyOn = bStandbyOn;
      pNFCController->aNALDataBuffer[0] = (bStandbyOn != W_FALSE) ? 0x01 : 0x00;
      PNALServiceSendEvent(
            pContext,
            NAL_SERVICE_ADMIN,
            &pNFCController->sStandbyOperation,
            NAL_EVT_STANDBY_MODE,
            pNFCController->aNALDataBuffer,
            0x01,
            static_PNFCControllerStandbyEventSent,
            null);
      /* Standby operation is in progress */
      pNFCController->bStandbyOperationInProgress = W_TRUE;
   }
   return (W_SUCCESS);
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

/* -----------------------------------------------------------------------------

   Raw Mode Functions

----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* The Raw mode status */
#define P_NFCC_RAW_MODE_NORMAL            0
#define P_NFCC_RAW_MODE_SWITCH_PENDING    1
#define P_NFCC_RAW_MODE_ACTIVE            2
#define P_NFCC_RAW_MODE_WRITE_PENDING     3

/* The maximum length in bytes of a raw message */
#define P_NFCC_MAX_RAW_MESSAGE_LENGTH   280

/* The maximum number of raw messages stored after reception */
#define P_NFCC_MAX_RAW_MESSAGE_NUMBER   4

typedef struct __tPNFCControllerMessageSlot
{
   uint32_t nLength; /* Length in bytes of the message, zero if ther is no message */
   uint8_t aMessageBuffer[P_NFCC_MAX_RAW_MESSAGE_LENGTH];
} tPNFCControllerMessageSlot;

/**  Raw message event handler function */
static void static_PNFCControllerRawMessageEventReceived(
         tContext* pContext,
         void* pCallbackParameter,
         uint8_t nEventIdentifier,
         const uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nNALMessageReceptionCounter)
{
   tNFCController* pNFCController = (tNFCController*)pCallbackParameter;
   tPNFCControllerMessageSlot* pMessageQueue = (tPNFCControllerMessageSlot*)pNFCController->pMessageQueue;

   if(pMessageQueue[pNFCController->nNextIndexToEnqueue].nLength != 0)
   {
      PDebugError("static_PNFCControllerRawMessageEventReceived: The message queue is full, message lost");
      return;
   }

   if(nLength == 0)
   {
      PDebugError("static_PNFCControllerRawMessageEventReceived: The message length is zero, message lost");
      return;
   }

   if(nLength > P_NFCC_MAX_RAW_MESSAGE_LENGTH)
   {
      PDebugError("static_PNFCControllerRawMessageEventReceived: The message length %d is too large, message lost", nLength);
      return;
   }

   /* Enqueue the received message */
   pMessageQueue[pNFCController->nNextIndexToEnqueue].nLength = nLength;
   CMemoryCopy(
      pMessageQueue[pNFCController->nNextIndexToEnqueue].aMessageBuffer,
      pBuffer, nLength);

   /* Check if a listener is registered */
   if(pNFCController->bRawListenerRegistered != W_FALSE)
   {
      /* Check if there is not any pending message to be retrieved by user */
      if (pNFCController->nNextIndexToDequeue == pNFCController->nNextIndexToEnqueue)
      {
         /* The received message is the only one in the queue, call the handler */
         PDFCDriverPostCC3(pNFCController->pListenerDriverCC, nLength, W_SUCCESS);
      }
      else
      {
         /* Another message must be retrieved by user. The handler will be called when the user will get the pending message. */
         PDebugWarning("static_PNFCControllerRawMessageEventReceived: Another message must be retrieved before calling the handler for this new message");
      }
   }
   else
   {
      PDebugError("static_PNFCControllerRawMessageEventReceived: The message is received, enqueued but there is not any registered listener");
   }

   /* Increment the index to enqueue */
   pNFCController->nNextIndexToEnqueue++;
   if(pNFCController->nNextIndexToEnqueue == P_NFCC_MAX_RAW_MESSAGE_NUMBER)
   {
      pNFCController->nNextIndexToEnqueue = 0;
   }
}

/**  Set Parameter callback function */
static void static_PNFCControllerSwitchToRawMode(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError)
{
   tNFCController* pNFCController = (tNFCController*)pCallbackParameter;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PNFCControllerSwitchToRawMode: Error %s", PUtilTraceError(nError));
      pNFCController->nRawMode = P_NFCC_RAW_MODE_NORMAL;
      PDFCDriverPostCC2( pNFCController->pRawOperationDriverCC, nError );
      return;
   }

   PNALServiceRegisterForEvent(
      pContext,
      NAL_SERVICE_ADMIN,
      NAL_EVT_RAW_MESSAGE,
      &pNFCController->sRawMessageOperation,
      static_PNFCControllerRawMessageEventReceived,
      pNFCController );

   pNFCController->nRawMode = P_NFCC_RAW_MODE_ACTIVE;

   PDFCDriverPostCC2( pNFCController->pRawOperationDriverCC, W_SUCCESS );
}

/** See API Documentation */
void PNFCControllerSwitchToRawModeDriver(
         tContext* pContext,
         tPBasicGenericCallbackFunction* pCallback,
         void* pCallbackParameter)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   uint8_t nValue = 1;
   W_ERROR nError = W_SUCCESS;

   if(pNFCController->nRawMode != P_NFCC_RAW_MODE_NORMAL)
   {
      PDebugError("PNFCControllerSwitchToRawMode: The NFC Controller is already in raw mode");
      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }

   pNFCController->pMessageQueue = CMemoryAlloc(sizeof(tPNFCControllerMessageSlot)*P_NFCC_MAX_RAW_MESSAGE_NUMBER);
   if(pNFCController->pMessageQueue == null)
   {
      PDebugError("PNFCControllerSwitchToRawMode: Cannot allocate the message queue");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }
   CMemoryFill(pNFCController->pMessageQueue, 0, sizeof(tPNFCControllerMessageSlot)*P_NFCC_MAX_RAW_MESSAGE_NUMBER);
   pNFCController->bRawListenerRegistered = W_FALSE;
   pNFCController->nNextIndexToEnqueue = 0;
   pNFCController->nNextIndexToDequeue = 0;

   PDFCDriverFillCallbackContext( pContext,
         (tDFCCallback*)pCallback, pCallbackParameter,
         &pNFCController->pRawOperationDriverCC );

   PNALServiceSetParameter(
      pContext,
      NAL_SERVICE_ADMIN,
      &pNFCController->sRawModeOperation,
      NAL_PAR_RAW_MODE,
      &nValue, 1,
      static_PNFCControllerSwitchToRawMode, pNFCController );

   pNFCController->nRawMode = P_NFCC_RAW_MODE_SWITCH_PENDING;

   return;

return_error:

   {
      tDFCDriverCCReference pDriverCC;

      PDebugError("PNFCControllerSwitchToRawMode: Returning error %s", PUtilTraceError(nError));

      PDFCDriverFillCallbackContext(
         pContext, (tDFCCallback*)pCallback, pCallbackParameter, &pDriverCC );

      PDFCDriverPostCC2( pDriverCC, nError );
   }
}

/** See API Documentation */
W_ERROR PNFCControllerRegisterRawListener(
         tContext* pContext,
         tPBasicGenericDataCallbackFunction* pReceiveMessageEventHandler,
         void* pHandlerParameter)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   tPNFCControllerMessageSlot* pMessageQueue = (tPNFCControllerMessageSlot*)pNFCController->pMessageQueue;

   if((pNFCController->nRawMode != P_NFCC_RAW_MODE_ACTIVE)
   && (pNFCController->nRawMode != P_NFCC_RAW_MODE_WRITE_PENDING))
   {
      PDebugError("PNFCControllerRegisterRawListener: The NFC Controller is not in raw mode");
      return W_ERROR_BAD_STATE;
   }

   if(pNFCController->bRawListenerRegistered != W_FALSE)
   {
      PDebugError("PNFCControllerRegisterRawListener: A listener is already registered");
      return W_ERROR_BAD_STATE;
   }

   PDFCDriverFillCallbackContext(
      pContext,
      (tDFCCallback*)pReceiveMessageEventHandler,
      pHandlerParameter,
      &pNFCController->pListenerDriverCC );

   pNFCController->bRawListenerRegistered = W_TRUE;

   /* Check if a message was enqueued before this handler registration */
   if (pMessageQueue[pNFCController->nNextIndexToDequeue].nLength > 0)
   {
      /* Call the message handler for the first message to dequeue */
      PDFCDriverPostCC3(pNFCController->pListenerDriverCC, pMessageQueue[pNFCController->nNextIndexToDequeue].nLength, W_SUCCESS);
   }

   return W_SUCCESS;
}

static void static_PNFCControllerSendEventCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nError,
            uint32_t nReceptionCounter )
{
   tNFCController* pNFCController = (tNFCController*)pCallbackParameter;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PNFCControllerSendEventCompleted: Receive error %s", PUtilTraceError(nError));
   }

   pNFCController->nRawMode = P_NFCC_RAW_MODE_ACTIVE;

   PDFCDriverPostCC2( pNFCController->pRawOperationDriverCC, nError );
}

/** See API Documentation */
void PNFCControllerWriteRawMessageDriver(
         tContext* pContext,
         tPBasicGenericCallbackFunction* pCallback,
         void* pCallbackParameter,
         const uint8_t* pBuffer,
         uint32_t nLength)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   W_ERROR nError = W_SUCCESS;

   if((pNFCController->nRawMode != P_NFCC_RAW_MODE_ACTIVE)
   && (pNFCController->nRawMode != P_NFCC_RAW_MODE_WRITE_PENDING))
   {
      PDebugError("PNFCControllerWriteRawMessage: The NFC Controller is not in raw mode");
      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }

   if(pNFCController->nRawMode == P_NFCC_RAW_MODE_WRITE_PENDING)
   {
      PDebugError("PNFCControllerWriteRawMessage: A write operation is already pending");
      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }

   PDFCDriverFillCallbackContext( pContext,
         (tDFCCallback*)pCallback, pCallbackParameter,
         &pNFCController->pRawOperationDriverCC );

   /* Send the event */
   PNALServiceSendEvent(
      pContext,
      NAL_SERVICE_ADMIN,
      &pNFCController->sRawModeOperation,
      NAL_EVT_RAW_MESSAGE,
      pBuffer,
      nLength,
      static_PNFCControllerSendEventCompleted,
      pNFCController );

   pNFCController->nRawMode = P_NFCC_RAW_MODE_WRITE_PENDING;

   return;

return_error:

   {
      tDFCDriverCCReference pDriverCC;

      PDebugError("PNFCControllerWriteRawMessage: Returning error %s", PUtilTraceError(nError));

      PDFCDriverFillCallbackContext(
         pContext, (tDFCCallback*)pCallback, pCallbackParameter, &pDriverCC );

      PDFCDriverPostCC2( pDriverCC, nError );
   }
}

/** See API Documentation */
W_ERROR PNFCControllerGetRawMessageData(
         tContext* pContext,
         uint8_t* pBuffer,
         uint32_t nBufferLength,
         uint32_t* pnActualLength)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   tPNFCControllerMessageSlot* pMessageQueue = (tPNFCControllerMessageSlot*)pNFCController->pMessageQueue;
   uint32_t nLength = pMessageQueue[pNFCController->nNextIndexToDequeue].nLength;
   W_ERROR nError = W_SUCCESS;

   if((pNFCController->nRawMode != P_NFCC_RAW_MODE_ACTIVE)
   && (pNFCController->nRawMode != P_NFCC_RAW_MODE_WRITE_PENDING))
   {
      PDebugError("PNFCControllerGetRawMessageData: The NFC Controller is not in raw mode");
      nError = W_ERROR_BAD_STATE;
      goto return_function;
   }

   if((pBuffer == null) || (nBufferLength == 0) || (pnActualLength == null))
   {
      PDebugError("PNFCControllerGetRawMessageData: Bad Parameters");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_function;
   }

   if(nLength == 0)
   {
      PDebugError("PNFCControllerGetRawMessageData: No message to dequeue");
      nError = W_ERROR_ITEM_NOT_FOUND;
      goto return_function;
   }

   if(nBufferLength < nLength)
   {
      PDebugError("PNFCControllerGetRawMessageData: Buffer too short");
      nError = W_ERROR_BUFFER_TOO_SHORT;
      goto return_function;
   }

   CMemoryCopy(pBuffer, pMessageQueue[pNFCController->nNextIndexToDequeue].aMessageBuffer, nLength);
   *pnActualLength = nLength;

   /* The message has been consumed */
   pMessageQueue[pNFCController->nNextIndexToDequeue].nLength = 0;

   pNFCController->nNextIndexToDequeue++;
   if(pNFCController->nNextIndexToDequeue == P_NFCC_MAX_RAW_MESSAGE_NUMBER)
   {
      pNFCController->nNextIndexToDequeue = 0;
   }

   /* Check if there is another queued message to be retrieved by user */
   nLength = pMessageQueue[pNFCController->nNextIndexToDequeue].nLength;
   if (nLength > 0)
   {
      /* Check if a listener is registered */
      if(pNFCController->bRawListenerRegistered != W_FALSE)
      {
         /* Call the handler */
         PDFCDriverPostCC3(pNFCController->pListenerDriverCC, nLength, W_SUCCESS);
      }
      else
      {
         PDebugError("static_PNFCControllerRawMessageEventReceived: another message is queued but there is not any registered listener");
      }
   }

return_function:

   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerGetRawMessageData: returning error %s", PUtilTraceError(nError));
      if(pnActualLength != null)
      {
         *pnActualLength = 0;
      }
   }

   return nError;
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

/* -----------------------------------------------------------------------------

   Get the Secure Element Functions

----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* See header file */
W_ERROR PNFCControllerGetSwpAccessPolicy(
            tContext* pContext,
            uint32_t nStorageType,
            uint32_t* pnSwpPolicyProtocols)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   uint32_t nSwpPolicyProtocols;

   if(PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PNFCControllerGetSwpAccessPolicy: Bad mode for the NFC Controller");
      return W_ERROR_BAD_STATE;
   }

   CDebugAssert((nStorageType == W_NFCC_STORAGE_VOLATILE)
   || (nStorageType == W_NFCC_STORAGE_PERSISTENT));

   if(nStorageType == W_NFCC_STORAGE_VOLATILE)
   {
      nSwpPolicyProtocols = pNFCController->sPolicyMonitor.sVolatile.nUICCProtocolPolicy;
   }
   else
   {
      nSwpPolicyProtocols = pNFCController->sPolicyMonitor.sPersistent.nUICCProtocolPolicy;
   }

   if(pnSwpPolicyProtocols != null)
   {
      *pnSwpPolicyProtocols = nSwpPolicyProtocols;
   }

   return W_SUCCESS;
}

/**
 * Checks the value of a policy with the current context.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in)  nStorageType  The storage type.
 *
 * @param[in]   pSEPolicy  the policy to check.
 *
 * @param[in]   nSwpProtocolPolicy  the SWP policy
 *
 * @param[in]   nDeviceCardPolicy  the device card policy
 *
 * @return  The error code.
 */
static W_ERROR static_PNFCControllerCheckCardPolicy(
            tContext* pContext,
            uint32_t nStorageType,
            const tNFCControllerSEPolicy* pSEPolicy,
            uint32_t nSwpProtocolPolicy,
            uint32_t nDeviceCardPolicy)
{
   tNFCControllerInfo* pNFCControllerInfo = PContextGetNFCControllerInfo( pContext );
   tNFCControllerSeInfo* pSEInfo = null;
   uint32_t nSwpCardPolicy = nSwpProtocolPolicy & W_NFCC_PROTOCOL_CARD_ALL;
   bool_t   bRoutingTableSupport = W_FALSE;

   if((nStorageType != W_NFCC_STORAGE_VOLATILE)
   && (nStorageType != W_NFCC_STORAGE_PERSISTENT))
   {
      PDebugError("static_PNFCControllerCheckCardPolicy: Bad storage parameters");
      return W_ERROR_BAD_PARAMETER;
   }

   if((pSEPolicy->nSESwitchPosition != P_SE_SWITCH_HOST_INTERFACE)
   && (pSEPolicy->nSESwitchPosition != P_SE_SWITCH_FORCED_HOST_INTERFACE)
   && (pSEPolicy->nSESwitchPosition != P_SE_SWITCH_RF_INTERFACE)
   && (pSEPolicy->nSESwitchPosition != P_SE_SWITCH_OFF))
   {
      PDebugError("static_PNFCControllerCheckCardPolicy: Bad switch position value");
      return W_ERROR_BAD_PARAMETER;
   }

   if((pSEPolicy->nSEProtocolPolicy & ~W_NFCC_PROTOCOL_CARD_ALL) != 0)
   {
      PDebugError("static_PNFCControllerCheckCardPolicy: Bad SE protocols");
      return W_ERROR_BAD_PARAMETER;
   }

   if(pNFCControllerInfo->nSeNumber != 0)
   {
      if(pSEPolicy->nSlotIdentifier >= pNFCControllerInfo->nSeNumber)
      {
         PDebugError("static_PNFCControllerCheckCardPolicy: Bad slot identifier value");
         return W_ERROR_BAD_PARAMETER;
      }

      pSEInfo = &pNFCControllerInfo->aSEInfoArray[pSEPolicy->nSlotIdentifier];

      if((pSEPolicy->nSEProtocolPolicy & ~(pSEInfo->nProtocols & W_NFCC_PROTOCOL_CARD_ALL)) != 0)
      {
         PDebugError("PNFCControllerSetSESwitchPosition: Unsuported SE protocols");
         return W_ERROR_FEATURE_NOT_SUPPORTED;
      }

      if(((pSEPolicy->nSESwitchPosition == P_SE_SWITCH_HOST_INTERFACE)
      ||  (pSEPolicy->nSESwitchPosition == P_SE_SWITCH_FORCED_HOST_INTERFACE))
      && ((pSEInfo->nCapabilities & W_SE_FLAG_COMMUNICATION) == 0))
      {
         PDebugError("static_PNFCControllerCheckCardPolicy: Host interface not supported");
         return W_ERROR_BAD_PARAMETER;
      }
   }
   else
   {
      if(pSEPolicy->nSlotIdentifier != 0)
      {
         PDebugError("static_PNFCControllerCheckCardPolicy: Bad slot identifier value");
         return W_ERROR_BAD_PARAMETER;
      }
   }

   bRoutingTableSupport = (pNFCControllerInfo->nFirmwareCapabilities & NAL_CAPA_ROUTING_TABLE ) ? W_TRUE : W_FALSE;

   if(pSEPolicy->nSESwitchPosition == P_SE_SWITCH_OFF)
   {
      if(pSEPolicy->nSlotIdentifier != 0)
      {
         PDebugError("static_PNFCControllerCheckCardPolicy: slot identifier shall be 0 for the OFF position");
         return W_ERROR_BAD_PARAMETER;
      }

      if (bRoutingTableSupport == W_FALSE)
      {
         if((nSwpCardPolicy & nDeviceCardPolicy) != 0)
         {
            PDebugError("static_PNFCControllerCheckCardPolicy: UICC and Device card emulation policy overlapps");
            return W_ERROR_EXCLUSIVE_REJECTED;
         }
      }
   }
   else if(pSEPolicy->nSESwitchPosition == P_SE_SWITCH_RF_INTERFACE)
   {
      if(pSEInfo == null)
      {
         PDebugError("static_PNFCControllerCheckCardPolicy: No SE specified for the RF interface");
         return W_ERROR_BAD_PARAMETER;
      }

      if(pSEPolicy->nSEProtocolPolicy == 0)
      {
         PDebugError("static_PNFCControllerCheckCardPolicy: No protocol specified for the RF interface");
         return W_ERROR_BAD_PARAMETER;
      }

      if (bRoutingTableSupport == W_FALSE)
      {
         if((nSwpCardPolicy & nDeviceCardPolicy) != 0)
         {
            PDebugError("static_PNFCControllerCheckCardPolicy: UICC and Device card emulation policy overlapps");
            return W_ERROR_EXCLUSIVE_REJECTED;
         }
         if((nSwpCardPolicy & pSEPolicy->nSEProtocolPolicy) != 0)
         {
            PDebugError("static_PNFCControllerCheckCardPolicy: UICC and SE card emulation policy overlapps");
            return W_ERROR_EXCLUSIVE_REJECTED;
         }
         if((pSEPolicy->nSEProtocolPolicy & nDeviceCardPolicy) != 0)
         {
            PDebugError("static_PNFCControllerCheckCardPolicy: SE and Device card emulation policy overlapps");
            return W_ERROR_EXCLUSIVE_REJECTED;
         }
      }
   }
   else /* host interface */
   {
      if(pSEInfo == null)
      {
         PDebugError("static_PNFCControllerCheckCardPolicy: No SE specified for the RF interface");
         return W_ERROR_BAD_PARAMETER;
      }

      if(nStorageType == W_NFCC_STORAGE_PERSISTENT)
      {
         PDebugError("static_PNFCControllerCheckCardPolicy: Host interface not allowed for the persistent policy");
         return W_ERROR_BAD_PARAMETER;
      }

      if (bRoutingTableSupport == W_FALSE)
      {
         if((nSwpCardPolicy & nDeviceCardPolicy) != 0)
         {
            PDebugError("static_PNFCControllerCheckCardPolicy: UICC and Device card emulation policy overlapps");
            return W_ERROR_EXCLUSIVE_REJECTED;
         }
         if((nSwpCardPolicy & pSEPolicy->nSEProtocolPolicy) != 0)
         {
            PDebugError("static_PNFCControllerCheckCardPolicy: UICC and SE card emulation policy overlapps");
            return W_ERROR_EXCLUSIVE_REJECTED;
         }
         if((pSEPolicy->nSEProtocolPolicy & nDeviceCardPolicy) != 0)
         {
            PDebugError("static_PNFCControllerCheckCardPolicy: SE and Device card emulation policy overlapps");
            return W_ERROR_EXCLUSIVE_REJECTED;
         }
      }
   }

   return W_SUCCESS;
}

/* See header file */
W_ERROR PNFCControllerCheckCardEmulPolicy(
            tContext* pContext,
            uint32_t nProtocol)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   tNFCControllerPolicyMonitor* pPolicy = &pNFCController->sPolicyMonitor;

   return static_PNFCControllerCheckCardPolicy(
            pContext, W_NFCC_STORAGE_VOLATILE,
            &pPolicy->sNewVolatile.sSEPolicy,
            pPolicy->sNewVolatile.nUICCProtocolPolicy,
            nProtocol);
}

/* See header file */
W_ERROR PNFCControllerCheckPersistentPolicy(
            tContext* pContext)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   tNFCControllerPolicyMonitor* pPolicy = &pNFCController->sPolicyMonitor;

   return static_PNFCControllerCheckCardPolicy(
            pContext, W_NFCC_STORAGE_PERSISTENT,
            &pPolicy->sPersistent.sSEPolicy,
            pPolicy->sPersistent.nUICCProtocolPolicy,
            0);
}

/* See header file */
void PNFCControllerSetSwpAccessPolicy(
            tContext* pContext,
            uint32_t nStorageType,
            uint32_t nSwpProtocolPolicy,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   tDFCCallbackContext sCallbackContext;

   tNFCControllerPolicyMonitor* pPolicy = &pNFCController->sPolicyMonitor;
   W_ERROR nError;

   /* Check the NFC Controller mode */
   if(PNFCControllerIsActive(pContext) == W_FALSE)
   {
      nError = W_ERROR_BAD_NFCC_MODE;
      goto send_event;
   }

   if((nStorageType | (W_NFCC_STORAGE_VOLATILE | W_NFCC_STORAGE_PERSISTENT)) !=
      (W_NFCC_STORAGE_VOLATILE | W_NFCC_STORAGE_PERSISTENT)
      || (nStorageType == 0))
   {
      PDebugError("PNFCControllerSetSwpAccessPolicy: Bad storage parameters");
      nError = W_ERROR_BAD_PARAMETER;
      goto send_event;
   }

   if((nStorageType & W_NFCC_STORAGE_VOLATILE) != 0)
   {
      nError = static_PNFCControllerCheckCardPolicy(
            pContext, W_NFCC_STORAGE_VOLATILE,
            &pPolicy->sNewVolatile.sSEPolicy,
            nSwpProtocolPolicy,
            PEmulGetActiveProtocol(pContext));

      if(nError != W_SUCCESS)
      {
         PDebugError("PNFCControllerSetSwpAccessPolicy: Bad parameters");
         goto send_event;
      }
   }

   if((nStorageType & W_NFCC_STORAGE_PERSISTENT) != 0)
   {
      nError = static_PNFCControllerCheckCardPolicy(
            pContext, W_NFCC_STORAGE_PERSISTENT,
            &pPolicy->sNewPersistent.sSEPolicy,
            nSwpProtocolPolicy,
            0);

      if(nError != W_SUCCESS)
      {
         PDebugError("PNFCControllerSetSwpAccessPolicy: Bad parameters");
         goto send_event;
      }
   }

   /* Check the parameters */
   if((nStorageType & W_NFCC_STORAGE_VOLATILE) != 0)
   {
      if(static_PNFCControllerCheckOperation(pPolicy, P_NFCC_OPERATION_SET_UICC_ACCESS_VOLATILE) != W_FALSE)
      {
         PDebugError("PNFCControllerSetSwpAccessPolicy: operation already active");
         nError = W_ERROR_BAD_STATE;
         goto send_event;
      }
   }

   if((nStorageType & W_NFCC_STORAGE_PERSISTENT) != 0)
   {
      if((static_PNFCControllerCheckOperation(pPolicy, P_NFCC_OPERATION_SET_UICC_ACCESS_PERSISTENT) != W_FALSE)
      || (static_PNFCControllerCheckOperation(pPolicy, P_NFCC_OPERATION_SET_UICC_READER_PERSISTENT) != W_FALSE))
      {
         PDebugError("PNFCControllerSetSwpAccessPolicy: operation already active");
         nError = W_ERROR_BAD_STATE;
         goto send_event;
      }
   }

   if((nStorageType & W_NFCC_STORAGE_VOLATILE) != 0)
   {
      pPolicy->sNewVolatile.nUICCProtocolPolicy = nSwpProtocolPolicy;

      if(nStorageType == W_NFCC_STORAGE_VOLATILE)
      {
         static_PNFCControllerPushOperation(pPolicy, P_NFCC_OPERATION_SET_UICC_ACCESS_VOLATILE);

         PDFCFillCallbackContext( pContext,
            (tDFCCallback*)pCallback, pCallbackParameter,
            &pNFCController->sPolicyMonitor.sSetVolatileUICCPolicyCC );
      }
      else
      {
         static_PNFCControllerPushOperation(pPolicy, P_NFCC_OPERATION_SET_UICC_ACCESS_VOLATILE | P_NFCC_OPERATION_NOP);
      }
   }

   if((nStorageType & W_NFCC_STORAGE_PERSISTENT) != 0)
   {
      PDFCFillCallbackContext( pContext,
            (tDFCCallback*)pCallback, pCallbackParameter,
            &pNFCController->sPolicyMonitor.sSetPersistentUICCPolicyCC );

      pPolicy->sNewPersistent.nUICCProtocolPolicy = nSwpProtocolPolicy;

      static_PNFCControllerPushOperation(pPolicy, P_NFCC_OPERATION_SET_UICC_READER_PERSISTENT | P_NFCC_OPERATION_NOP);
      static_PNFCControllerPushOperation(pPolicy, P_NFCC_OPERATION_SET_UICC_ACCESS_PERSISTENT);

   }

   static_PNFCControllerExecuteNextPolicyOperation(pContext, pPolicy);

   return;

send_event:

   PDFCFillCallbackContext( pContext,
         (tDFCCallback*)pCallback, pCallbackParameter,
         &sCallbackContext );

   PDFCPostContext2(&sCallbackContext, nError);
}

/* See header file */
uint32_t PNFCControllerGetSecureElementNumber(
            tContext* pContext)
{
   tNFCControllerInfo* pNFCControllerInfo = PContextGetNFCControllerInfo( pContext );

   /* Check the NFC Controller mode */
   if(PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PNFCControllerGetSecureElementNumber: Bad NFCC mode");
      return 0;
   }

   return pNFCControllerInfo->nSeNumber;
}

/* See header file */
W_ERROR PNFCControllerGetSecureElementHardwareInfo(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            char16_t* pDescription,
            uint32_t* pnCapabilities,
            uint32_t* pnProtocols,
            uint32_t* pnHalSlotIdentifier)
{
   tNFCControllerInfo* pNFCControllerInfo = PContextGetNFCControllerInfo( pContext );

   /* Check the NFC Controller mode */
   if(PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PNFCControllerGetSecureElementHardwareInfo: Bad NFCC mode");
      return W_ERROR_BAD_NFCC_MODE;
   }

   if(nSlotIdentifier >= pNFCControllerInfo->nSeNumber)
   {
      PDebugError("PNFCControllerGetSecureElementHardwareInfo: Bad SE slot identifier");
      return W_ERROR_BAD_PARAMETER;
   }

   if(pDescription != null)
   {
      uint32_t nPos;
      uint8_t* pBuffer = pNFCControllerInfo->aSEInfoArray[nSlotIdentifier].aDescription;

/* Consistency check at compilation time */
#if NAL_SE_DESCRIPTION_STRING_SIZE < W_SE_DESCRIPTION_LENGTH
#error  Wrong length
#endif /* NAL_SE_DESCRIPTION_STRING_SIZE < W_SE_DESCRIPTION_LENGTH */

      for(nPos = 0; nPos < W_SE_DESCRIPTION_LENGTH; nPos++)
      {
         *pDescription++ = *pBuffer++;
      }
   }

   if(pnCapabilities != null)
   {
      *pnCapabilities = pNFCControllerInfo->aSEInfoArray[nSlotIdentifier].nCapabilities;
   }

   if(pnProtocols != null)
   {
      *pnProtocols = pNFCControllerInfo->aSEInfoArray[nSlotIdentifier].nProtocols;
   }

   if(pnHalSlotIdentifier != null)
   {
      *pnHalSlotIdentifier = pNFCControllerInfo->aSEInfoArray[nSlotIdentifier].nHalSlotIdentifier;
   }

   return W_SUCCESS;
}

/* See header file */
W_ERROR PNFCControllerGetSESwitchPosition(
            tContext* pContext,
            tNFCControllerSEPolicy* pPersistentPolicy,
            tNFCControllerSEPolicy* pVolatilePolicy,
            tNFCControllerSEPolicy* pNewPersistentPolicy,
            tNFCControllerSEPolicy* pNewVolatilePolicy)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );

   /* Check the NFC Controller mode */
   if(PNFCControllerIsActive(pContext) == W_FALSE)
   {
      PDebugError("PNFCControllerGetSESwitchPosition: Bad NFCC mode");
      return W_ERROR_BAD_NFCC_MODE;
   }

   if(pPersistentPolicy != null)
   {
      CMemoryCopy(pPersistentPolicy, &pNFCController->sPolicyMonitor.sPersistent.sSEPolicy, sizeof(tNFCControllerSEPolicy));
   }

   if(pVolatilePolicy != null)
   {
      CMemoryCopy(pVolatilePolicy, &pNFCController->sPolicyMonitor.sVolatile.sSEPolicy, sizeof(tNFCControllerSEPolicy));
   }

   if(pNewPersistentPolicy != null)
   {
      CMemoryCopy(pNewPersistentPolicy, &pNFCController->sPolicyMonitor.sNewPersistent.sSEPolicy, sizeof(tNFCControllerSEPolicy));
   }

   if(pNewVolatilePolicy != null)
   {
      CMemoryCopy(pNewVolatilePolicy, &pNFCController->sPolicyMonitor.sNewVolatile.sSEPolicy, sizeof(tNFCControllerSEPolicy));
   }

   return W_SUCCESS;
}

W_ERROR PNFCControllerCheckSESwitchPosition(
            tContext* pContext,
            uint32_t nStorageType,
            const tNFCControllerSEPolicy* pSEPolicy)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   tNFCControllerPolicyMonitor* pPolicy = &pNFCController->sPolicyMonitor;
   W_ERROR nError = W_SUCCESS;

   if((nStorageType | (W_NFCC_STORAGE_VOLATILE | W_NFCC_STORAGE_PERSISTENT)) !=
      (W_NFCC_STORAGE_VOLATILE | W_NFCC_STORAGE_PERSISTENT) || (nStorageType == 0))
   {
      nError = W_ERROR_BAD_PARAMETER;
      goto return_result;
   }

   if((nStorageType & W_NFCC_STORAGE_VOLATILE) != 0)
   {
      nError = static_PNFCControllerCheckCardPolicy(
            pContext, W_NFCC_STORAGE_VOLATILE,
            pSEPolicy,
            pPolicy->sNewVolatile.nUICCProtocolPolicy,
            PEmulGetActiveProtocol(pContext));

      if(nError != W_SUCCESS)
      {
         goto return_result;
      }
   }

   if((nStorageType & W_NFCC_STORAGE_PERSISTENT) != 0)
   {
      nError = static_PNFCControllerCheckCardPolicy(
            pContext, W_NFCC_STORAGE_PERSISTENT,
            pSEPolicy,
            pPolicy->sNewPersistent.nUICCProtocolPolicy,
            0);

      if(nError != W_SUCCESS)
      {
         goto return_result;
      }
   }

   /* Check the parameters */
   if((nStorageType & W_NFCC_STORAGE_VOLATILE) != 0)
   {
      /* This flag is needed to protect the operation against re-entrantcy */
      if(static_PNFCControllerCheckOperation(pPolicy, P_NFCC_OPERATION_SET_SE_SWITCH_VOLATILE) != W_FALSE)
      {
         nError = W_ERROR_BAD_STATE;
         goto return_result;
      }
   }

   if((nStorageType & W_NFCC_STORAGE_PERSISTENT) != 0)
   {
      /* This flag is needed to protect the operation against re-entrantcy */
      if(static_PNFCControllerCheckOperation(pPolicy, P_NFCC_OPERATION_SET_SE_SWITCH_PERSISTENT) != W_FALSE)
      {
         nError = W_ERROR_BAD_STATE;
         goto return_result;
      }
   }

return_result:
   return nError;
}

/* See header file */
void PNFCControllerSetSESwitchPosition(
            tContext* pContext,
            uint32_t nStorageType,
            const tNFCControllerSEPolicy* pSEPolicy,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   tDFCCallbackContext sCallbackContext;
   tNFCControllerPolicyMonitor* pPolicy = &pNFCController->sPolicyMonitor;
   W_ERROR nError;

   /* Check the NFC Controller mode */
   if(PNFCControllerIsActive(pContext) == W_FALSE)
   {
      nError = W_ERROR_BAD_NFCC_MODE;
      goto send_event;
   }

   nError = PNFCControllerCheckSESwitchPosition(pContext, nStorageType, pSEPolicy);
   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerSetSESwitchPosition: PNFCControllerCheckCardPolicies has returned the error %s", PUtilTraceError(nError));
      goto send_event;
   }

   if((nStorageType & W_NFCC_STORAGE_VOLATILE) != 0)
   {
      CMemoryCopy(&pPolicy->sNewVolatile.sSEPolicy, pSEPolicy, sizeof(tNFCControllerSEPolicy));

      PDFCFillCallbackContext( pContext,
         (tDFCCallback*)pCallback, pCallbackParameter,
         &pNFCController->sPolicyMonitor.sSetVolatileSESwitchCC );

      if(nStorageType == W_NFCC_STORAGE_VOLATILE)
      {
         static_PNFCControllerPushOperation(pPolicy, P_NFCC_OPERATION_SET_SE_SWITCH_VOLATILE);
      }
      else
      {
         static_PNFCControllerPushOperation(pPolicy, P_NFCC_OPERATION_SET_SE_SWITCH_VOLATILE | P_NFCC_OPERATION_NOP);
      }
   }

   if((nStorageType & W_NFCC_STORAGE_PERSISTENT) != 0)
   {
      CMemoryCopy(&pPolicy->sNewPersistent.sSEPolicy, pSEPolicy, sizeof(tNFCControllerSEPolicy));

      static_PNFCControllerPushOperation(pPolicy, P_NFCC_OPERATION_SET_SE_SWITCH_PERSISTENT);

      PDFCFillCallbackContext( pContext,
            (tDFCCallback*)pCallback, pCallbackParameter,
            &pNFCController->sPolicyMonitor.sSetPersistentSESwitchCC );
   }

   static_PNFCControllerExecuteNextPolicyOperation(pContext, pPolicy);

   return;

send_event:

   PDFCFillCallbackContext( pContext,
         (tDFCCallback*)pCallback, pCallbackParameter,
         &sCallbackContext );

   PDFCPostContext2(&sCallbackContext, nError);
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

/* -----------------------------------------------------------------------------

   PNFCControllerMonitorException()

----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

static uint32_t static_PNFCControllerMonitorExceptionUnregister(
            tContext* pContext,
            void* pObject )
{
   tNFCControllerExceptionMonitor* pMonitor = (tNFCControllerExceptionMonitor*)pObject;

   if(pMonitor->bRegistered != W_FALSE)
   {
      pMonitor->bRegistered = W_FALSE;

      PDFCDriverFlushCall(pMonitor->pDriverCC);

      PNALServiceCancelOperation(pContext, &pMonitor->sServiceOperation);
   }

   return P_HANDLE_DESTROY_DONE;
}

/* Handle type for the NFC Controller exception monitor function */
static tHandleType g_sNFCControllerMonitorExceptionRegistryType =
   { static_PNFCControllerMonitorExceptionUnregister, null, null, null, null, null, null, null, null };

static void static_PNFCControllerMonitorExceptionEventReceived(
         tContext* pContext,
         void* pCallbackParameter,
         uint8_t nEventIdentifier,
         const uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nNALMessageReceptionCounter)
{
   uint32_t nCause;

   if((nEventIdentifier != NAL_EVT_NFCC_ERROR)||(pBuffer == null)||(nLength != 4))
   {
      PDebugError("static_PNFCControllerMonitorExceptionEventReceived: Protocol error");
      /* Protocol error: can't do anything about it */
      return;
   }

   nCause  = PNALReadUint32FromBuffer(pBuffer) | W_NFCC_EXCEPTION_SENT_BY_NAL;

   PNFCControllerNotifyException( pContext, nCause );
}

/* See Client API Specifications */
W_ERROR PNFCControllerMonitorException(
            tContext* pContext,
            tPBasicGenericEventHandler* pHandler,
            void* pHandlerParameter,
            W_HANDLE* phEventRegistry)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   tNFCControllerExceptionMonitor* pMonitor = &pNFCController->sExceptionMonitor;
   W_ERROR nError = W_SUCCESS;

   if(phEventRegistry == null)
   {
      PDebugError("PNFCControllerMonitorException: phEventRegistry is null");
      return W_ERROR_BAD_PARAMETER;
   }

   if(pMonitor->bRegistered != W_FALSE)
   {
      PDebugError("PNFCControllerMonitorException: A listener is already registered");
      nError = W_ERROR_TOO_MANY_HANDLERS;
      goto return_function;
   }

   nError = PHandleRegister( pContext, pMonitor,
            &g_sNFCControllerMonitorExceptionRegistryType, phEventRegistry);

   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerMonitorException: Error returned by PHandleRegister()");
      goto return_function;
   }

   pMonitor->bRegistered = W_TRUE;

   PNALServiceRegisterForEvent(
      pContext,
      NAL_SERVICE_ADMIN,
      NAL_EVT_NFCC_ERROR,
      &pMonitor->sServiceOperation,
      static_PNFCControllerMonitorExceptionEventReceived,
      pMonitor );

   PDFCDriverFillCallbackContext( pContext,
         (tDFCCallback*)pHandler, pHandlerParameter,
         &pMonitor->pDriverCC );

return_function:

   if(nError != W_SUCCESS)
   {
      *phEventRegistry = W_NULL_HANDLE;
   }

   return nError;
}

/* See header file */
void PNFCControllerNotifyException(
            tContext* pContext,
            uint32_t nCause)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   tNFCControllerExceptionMonitor* pMonitor = &pNFCController->sExceptionMonitor;

   if(pMonitor->bRegistered != W_FALSE)
   {
      PDFCDriverPostCC2(pMonitor->pDriverCC, nCause);
   }
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

/* -----------------------------------------------------------------------------

   PNFCControllerMonitorFieldEvents()

----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

static uint32_t static_PNFCControllerMonitorFieldEventsUnregister(
            tContext* pContext,
            void* pObject )
{
   tNFCControllerRFFieldMonitor* pMonitor = (tNFCControllerRFFieldMonitor*)pObject;

   if(pMonitor->bRegistered != W_FALSE)
   {
      pMonitor->bRegistered = W_FALSE;

      PDFCDriverFlushCall(pMonitor->pDriverCC);

      PNALServiceCancelOperation(pContext, &pMonitor->sServiceOperation);
   }

   return P_HANDLE_DESTROY_DONE;
}

/* Handle type for the NFC Controller exception monitor function */
static tHandleType g_sNFCControllerMonitorFieldEventsRegistryType =
   { static_PNFCControllerMonitorFieldEventsUnregister, null, null, null, null, null, null, null, null };


static void static_PNFCControllerMonitorFieldEventReceived(
         tContext* pContext,
         void* pCallbackParameter,
         uint8_t nEventIdentifier,
         const uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nNALMessageReceptionCounter)
{
   tNFCController* pNFCController = (tNFCController*)pCallbackParameter;
   tNFCControllerRFFieldMonitor* pMonitor = &pNFCController->sRFFieldMonitor;

   CDebugAssert(nEventIdentifier == NAL_EVT_RF_FIELD);
   if((pBuffer == null)||(nLength != 1))
   {
      PDebugError("static_PNFCControllerMonitorFieldEventReceived: Protocol error");
      /* Protocol error: can't do anything about it */
      return;
   }

   if(pMonitor->bRegistered != W_FALSE)
   {
      PDFCDriverPostCC2(pMonitor->pDriverCC, (pBuffer[0] == 0)?W_NFCC_EVENT_FIELD_OFF:W_NFCC_EVENT_FIELD_ON);
   }
}

void PNFCControllerCallMonitorFieldCallback(
   tContext * pContext,
   uint32_t   nEvent)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   tNFCControllerRFFieldMonitor* pMonitor = &pNFCController->sRFFieldMonitor;

   if(pMonitor->bRegistered != W_FALSE)
   {
      PDFCDriverPostCC2(pMonitor->pDriverCC, nEvent);
   }
}

/* See Client API Specifications */
W_ERROR PNFCControllerMonitorFieldEvents(
            tContext* pContext,
            tPBasicGenericEventHandler* pHandler,
            void* pHandlerParameter,
            W_HANDLE* phEventRegistry)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   tNFCControllerRFFieldMonitor* pMonitor = &pNFCController->sRFFieldMonitor;
   W_ERROR nError = W_SUCCESS;

   if(phEventRegistry == null)
   {
      PDebugError("PNFCControllerMonitorFieldEvents: phEventRegistry is null");
      return W_ERROR_BAD_PARAMETER;
   }

   if(pMonitor->bRegistered != W_FALSE)
   {
      PDebugError("PNFCControllerMonitorFieldEvents: A listener is already registered");
      nError = W_ERROR_EXCLUSIVE_REJECTED;
      goto return_function;
   }

   nError = PHandleRegister( pContext, pMonitor,
            &g_sNFCControllerMonitorFieldEventsRegistryType, phEventRegistry);

   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerMonitorFieldEvents: Error returned by PHandleRegister()");
      goto return_function;
   }

   pMonitor->bRegistered = W_TRUE;

   PDFCDriverFillCallbackContext( pContext,
         (tDFCCallback*)pHandler, pHandlerParameter,
         &pMonitor->pDriverCC );

   PNALServiceRegisterForEvent(
         pContext,
         NAL_SERVICE_ADMIN,
         NAL_EVT_RF_FIELD,
         &pMonitor->sServiceOperation,
         static_PNFCControllerMonitorFieldEventReceived,
         pNFCController );

return_function:

   if(nError != W_SUCCESS)
   {
      *phEventRegistry = W_NULL_HANDLE;
   }

   return nError;
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

/* -----------------------------------------------------------------------------

   PNFCControllerGetMode()

----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* See Client API Specifications */
uint32_t PNFCControllerGetMode(
                  tContext* pContext)
{
   tNFCControllerInfo* pNFCControllerInfo = PContextGetNFCControllerInfo( pContext );

   uint32_t nMode = PNALServiceGetVariable(pContext, NAL_PARAM_MODE);

   if (pNFCControllerInfo->nFirmwareCapabilities & NAL_CAPA_STANDBY_MODE)
   {
      if (nMode == W_NFCC_MODE_ACTIVE)
      {
         /* W_NFCC_MODE_ACTIVE is a meta state that regroups ACTIVE, STANDBY, SWITCH_TO_ACTIVE and SWITCH_TO_STANDBY modes */
         nMode = PNALServiceGetVariable(pContext, NAL_PARAM_SUB_MODE);
      }
   }

   return nMode;
}

/* See header */
bool_t PNFCControllerIsActive(
                  tContext* pContext)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   uint32_t nMode = PNALServiceGetVariable(pContext, NAL_PARAM_MODE);

   /* Special case for the initialization of the SE and the UICC */
   if((nMode == W_NFCC_MODE_BOOT_PENDING)
   && (pNFCController->bBootedForSEandUICC ))
   {
      return W_TRUE;
   }

   return (nMode == W_NFCC_MODE_ACTIVE) ? W_TRUE : W_FALSE;
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */



/* -----------------------------------------------------------------------------

   PNFCControllerSelfTest()

----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

static void static_PNFCControllerSelfTestCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         uint32_t nLength,
         W_ERROR nError,
         uint32_t nReceptionCounter)
{
   tNFCController* pNFCController = (tNFCController*)pCallbackParameter;
   uint32_t nResult = 0;

   CDebugAssert(pNFCController->sTestMonitor.bOperationPending != W_FALSE);

   if(nError == W_SUCCESS)
   {
      if(nLength != 1)
      {
         nError = W_ERROR_NFC_HAL_COMMUNICATION;
      }
      else
      {
         nResult = pNFCController->sTestMonitor.aCommandAndResponseBuffer[0];
      }
   }

   pNFCController->sTestMonitor.bOperationPending = W_FALSE;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PNFCControllerSelfTestCompleted: Error %s", PUtilTraceError(nError));
      nResult = 1;
   }

   PDFCDriverPostCC3(pNFCController->sTestMonitor.pDriverCC, nError, nResult);
}

/* See Client API Specifications */
void PNFCControllerSelfTestDriver(
            tContext* pContext,
            tPNFCControllerSelfTestCompleted* pCallback,
            void* pCallbackParameter )
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   tDFCDriverCCReference pDriverCC;
   W_ERROR nError;

   PDebugTrace("PNFCControllerSelfTest()");

   if(PNALServiceGetVariable(pContext, NAL_PARAM_MODE) != W_NFCC_MODE_MAINTENANCE)
   {
      PDebugError("PNFCControllerSelfTest: Bad mode for the NFC Controller");
      nError = W_ERROR_BAD_NFCC_MODE;
      goto return_error;
   }

   if(pNFCController->sTestMonitor.bOperationPending != W_FALSE)
   {
      PDebugError("PNFCControllerSelfTest: Test is already pending");
      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }

   pNFCController->sTestMonitor.bOperationPending = W_TRUE;

   PDFCDriverFillCallbackContext( pContext,
         (tDFCCallback*)pCallback, pCallbackParameter,
         &pNFCController->sTestMonitor.pDriverCC );

   PNALServiceExecuteCommand(
      pContext,
      NAL_SERVICE_ADMIN,
      &pNFCController->sTestMonitor.sServiceOperation,
      NAL_CMD_SELF_TEST,
      null, 0,
      pNFCController->sTestMonitor.aCommandAndResponseBuffer,
      sizeof(pNFCController->sTestMonitor.aCommandAndResponseBuffer),
      static_PNFCControllerSelfTestCompleted,
      pNFCController );

   return;

return_error:

   PDebugError("PNFCControllerSelfTest: Error %s", PUtilTraceError(nError));

   PDFCDriverFillCallbackContext( pContext,
         (tDFCCallback*)pCallback, pCallbackParameter,
         &pDriverCC );

   PDFCDriverPostCC3(pDriverCC, nError, 1);
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

/* -----------------------------------------------------------------------------

   PNFCControllerProductionTest()

----------------------------------------------------------------------------- */

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

static void static_PNFCControllerProductionTestCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         uint32_t nLength,
         W_ERROR nError,
         uint32_t nReceptionCounter)
{
   tNFCController* pNFCController = (tNFCController*)pCallbackParameter;

   CDebugAssert(pNFCController->sTestMonitor.bOperationPending != W_FALSE);

   pNFCController->sTestMonitor.bOperationPending = W_FALSE;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PNFCControllerProductionTestCompleted: Error %s", PUtilTraceError(nError));
   }

   PDFCDriverPostCC3(pNFCController->sTestMonitor.pDriverCC, nLength, nError);
}

/* See Client API Specifications */
void PNFCControllerProductionTestDriver(
         tContext* pContext,
         const uint8_t* pParameterBuffer,
         uint32_t nParameterBufferLength,
         uint8_t* pResultBuffer,
         uint32_t nResultBufferLength,
         tPBasicGenericDataCallbackFunction* pCallback,
         void* pCallbackParameter)
{
   tNFCController* pNFCController = PContextGetNFCController( pContext );
   W_ERROR nError = W_SUCCESS;
   tDFCDriverCCReference pDriverCC;

   PDebugTrace("PNFCControllerProductionTest()");

   /* Check the parameters */
   if((pParameterBuffer == null) || (nParameterBufferLength == 0))
   {
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Check the NFC Controller mode */
   if( PNALServiceGetVariable(pContext, NAL_PARAM_MODE) != W_NFCC_MODE_MAINTENANCE )
   {
      PDebugError("PNFCControllerProductionTest: Not in W_NFCC_MODE_MAINTENANCE");
      nError = W_ERROR_BAD_NFCC_MODE;
      goto return_error;
   }

   if(pNFCController->sTestMonitor.bOperationPending != W_FALSE)
   {
      PDebugError("PNFCControllerProductionTest: Test is already pending");
      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }
   pNFCController->sTestMonitor.bOperationPending = W_TRUE;

   PDFCDriverFillCallbackContext( pContext,
         (tDFCCallback*)pCallback, pCallbackParameter,
         &pNFCController->sTestMonitor.pDriverCC );

   PNALServiceExecuteCommand(
      pContext,
      NAL_SERVICE_ADMIN,
      &pNFCController->sTestMonitor.sServiceOperation,
      NAL_CMD_PRODUCTION_TEST,
      pParameterBuffer, nParameterBufferLength,
      pResultBuffer, nResultBufferLength,
      static_PNFCControllerProductionTestCompleted,
      pNFCController );

   return;

return_error:

   if(nError != W_SUCCESS)
   {
      PDebugError("PNFCControllerProductionTest: Error %s", PUtilTraceError(nError));
   }

   PDFCDriverFillCallbackContext( pContext,
         (tDFCCallback*)pCallback, pCallbackParameter,
         &pDriverCC );

   PDFCDriverPostCC3(pDriverCC, 0, nError);
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */


