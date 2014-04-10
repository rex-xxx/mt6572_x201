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
   Contains the NDEF API implementation.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( RTD )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#include "wme_ndef.h"

typedef struct __tURIIdentifyCodeAndLength
{
   uint32_t nLength;
   char* sIdentifier;
}tURIIdentifyCodeAndLength;

const tURIIdentifyCodeAndLength g_URIIdentifyCodeAndLength[] =
{
   {0, ""}, {11, "http://www."}, {12, "https://www."}, {7, "http://"}, {8, "https://"},
   {4, "tel:"}, {7, "mailto:"}, {26, "ftp://anonymous:anonymous@"}, {10, "ftp://ftp."}, {7, "ftps://"},
   {7, "sftp://"}, {6, "smb://"}, {6, "nfs://"}, {6, "ftp://"}, {6, "dav://"},
   {5, "news:"}, {9, "telnet://"}, {5, "imap:"}, {7, "rtsp://"}, {4, "urn:"},
   {4, "pop:"}, {4, "sip:"}, {5, "sips:"}, {5, "tftp:"}, {8, "btspp://"},
   {10, "btl2cap://"}, {9, "btgoep://"}, {10, "tcpobex://"}, {11, "irdaobex://"}, {7, "file://"},
   {11, "urn:epc:id:"}, {12, "urn:epc:tag:"}, {12, "urn:epc:pat:"}, {12, "urn:epc:raw:"}, {8, "urn:epc:"},
   {8, "urn:nfc:"}, {3, "RFU"}
};

uint8_t  PNDEF_GetTNF( uint8_t* pBuffer );

/**
 * Misc
 */

static uint32_t static_PRTDGetTextOffset( uint8_t* pBuffer )
{
   return ( 1 + (pBuffer[0]&0x3f) );
}

static uint32_t static_PRTDIsUTF8( uint8_t* pBuffer )
{
   return ( ((pBuffer[0]&0x80)==0x00) ? W_TRUE : W_FALSE );
}

/**
* Checks if two languages match
*
* @param[in]  const uint8_t* pLanguage
*                      the first language
*             const uint8_t* pLanguage1
*                      the second language
*             uint32_t nLanguageLength
*                      number of characters for the first language
* @return     0 if totally match
*             1 if partially match
*             2 if no match
**/
static uint8_t TextMatch(
                         const uint8_t* pLanguage,
                         const char16_t* pLanguage1,
                         uint32_t nLanguageLength)
{
   uint8_t nResult = 0;
   uint32_t i = 0;
   bool_t bFlag = W_FALSE;

   for (i=0; i<(nLanguageLength-1); i++)
   {
      if (*(pLanguage + i) == '-')
      {
         bFlag = W_TRUE;
      }
      else if (*(pLanguage + i) != *(pLanguage1 + i) && bFlag == W_FALSE)
      {
         nResult = 2;
         break;
      }
      else if (*(pLanguage + i) != *(pLanguage1 + i))
      {
         bFlag = W_FALSE;
         nResult = 1;
         break;
      }
      if (*(pLanguage1 + i + 1) == '-' && i == nLanguageLength - 2 && bFlag == W_FALSE)
      {
         nResult = 1;
      }
   }
   return nResult;
}

/* See Client API Specifications */
bool_t PRTDIsTextRecord(
         tContext* pContext,
         W_HANDLE hRecord )
{
   char16_t cType[2] = {'T',0};
   return PNDEFCheckType( pContext, hRecord, W_NDEF_TNF_WELL_KNOWN, cType);
}

/* See Client API Specifications */
bool_t PRTDIsURIRecord(
         tContext* pContext,
         W_HANDLE hRecord )
{
   char16_t cType[2] = {'U',0};
   return PNDEFCheckType( pContext, hRecord, W_NDEF_TNF_WELL_KNOWN, cType);
}

static W_ERROR static_PRTDURIGetValue(
         tContext* pContext,
         W_HANDLE hRecord,
         char16_t* pBuffer,
         uint32_t * pBufferLength )
{
   W_ERROR    nError;
   uint8_t  * pPayloadBuffer;
   uint32_t   nPayloadSize;
   uint32_t   nSize;
   uint32_t   i;
   uint8_t    nIdCode;
   uint32_t   nIdCodeLength;
   char16_t *    pTemp = null;

   char16_t      pURI[] = { 'U', 0x00 };

   nError = PNDEFGetRecordObject(pContext, hRecord, null);

   if (nError != W_SUCCESS)
   {
      return nError;
   }

   if(pBufferLength == null)
   {
      nError = W_ERROR_BAD_PARAMETER;
      goto error_cleanup;
   }

   if (PNDEFCheckType(pContext, hRecord, W_NDEF_TNF_WELL_KNOWN, pURI) == W_FALSE)
   {
      nError= W_ERROR_WRONG_RTD;
      goto error_cleanup;
   }

   if ((nError = PNDEFGetRecordInfo(pContext, hRecord, W_NDEF_INFO_PAYLOAD, &nPayloadSize)) != W_SUCCESS)
   {
      goto error_cleanup;
   }

   if ((nError =PNDEFGetPayloadPointer(pContext, hRecord, &pPayloadBuffer)) != W_SUCCESS)
   {
      goto error_cleanup;
   }

   if(pPayloadBuffer == null)
   {
      goto error_cleanup;
   }

   nIdCode = pPayloadBuffer[0];

   if (nIdCode > 0x23)
   {
      nIdCode = 0x00;
   }

   /* If the payloadbuffer is wrong formatted */
   if( (nSize =  PUtilConvertUTF8ToUTF16( null, &pPayloadBuffer[1], nPayloadSize - 1)) == 0)
   {
      nError = W_ERROR_WRONG_RTD;
      goto error_cleanup;
   }

   nIdCodeLength = (uint8_t)g_URIIdentifyCodeAndLength[ nIdCode ].nLength;

   nSize += nIdCodeLength;

   if ((pTemp = CMemoryAlloc((nSize+1)* sizeof(char16_t))) == null)
   {
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto error_cleanup;
   }

   for (i=0; i<nIdCodeLength; i++)
   {
      pTemp[ i ] = g_URIIdentifyCodeAndLength[ nIdCode ].sIdentifier[ i ];
   }

   PUtilConvertUTF8ToUTF16(pTemp + nIdCodeLength, &pPayloadBuffer[1], nPayloadSize - 1);
   pTemp[nSize] = 0;

   * pBufferLength = PUtilConvertStringToPrintableString(pBuffer, pTemp);

   nError = W_SUCCESS;

error_cleanup:

   CMemoryFree(pTemp);
   return nError;

}

/* See Client API Specifications */
W_ERROR PRTDURIGetValue(
         tContext* pContext,
         W_HANDLE hRecord,
         char16_t* pBuffer,
         uint32_t nBufferLength )
{
   W_ERROR   nError;
   uint32_t  nRequestedSize;

   if (pBuffer == null)
   {
      return W_ERROR_BAD_PARAMETER;
   }

   if ((nError = static_PRTDURIGetValue(pContext, hRecord, null, &nRequestedSize)) != W_SUCCESS)
   {
      return nError;
   }

   if (nBufferLength < nRequestedSize + 1)
   {
      return (W_ERROR_BUFFER_TOO_SHORT);
   }

   return static_PRTDURIGetValue(pContext, hRecord, pBuffer, &nRequestedSize);
}

/* See Client API Specifications */
W_ERROR PRTDTextGetLanguage(
         tContext* pContext,
         W_HANDLE hRecord,
         char16_t* pLanguageBuffer,
         uint32_t nBufferLength )
{
   uint32_t nLanguageLength = 0;
   tNDEFRecord* pRecord = null;
   uint32_t nPayloadOffset = 0;

   if ( (W_SUCCESS != PHandleGetObject( pContext, hRecord, P_HANDLE_TYPE_NDEF_RECORD, (void**)&pRecord))
      || (pRecord == null) )
   {
      return W_ERROR_BAD_HANDLE;
   }

   if (null == pLanguageBuffer || 0 == nBufferLength)
   {
      return W_ERROR_BUFFER_TOO_SHORT;
   }

   if ( PNDEFParseSafeBuffer( pRecord->pRecordBuffer, pRecord->nRecordSize,
         null,null, null,null, &nPayloadOffset,null) == 0 )
   {
      return W_ERROR_BAD_HANDLE;
   }

   if (PRTDIsTextRecord( pContext, hRecord) == W_FALSE)
   {
      return W_ERROR_WRONG_RTD;
   }

   nLanguageLength = *(pRecord->pRecordBuffer + nPayloadOffset++) & 0x3f;
   if (nBufferLength < nLanguageLength + 1)
   {
      return W_ERROR_BUFFER_TOO_SHORT;
   }

   *(pLanguageBuffer + nLanguageLength) = '\0';
   while (nLanguageLength-- > 0)
   {
      *(pLanguageBuffer + nLanguageLength) = (char16_t)(*(pRecord->pRecordBuffer + nPayloadOffset + nLanguageLength));
   }
   return W_SUCCESS;
}

/* See Client API Specifications */
uint8_t PRTDTextLanguageMatch(
         tContext* pContext,
         W_HANDLE hRecord,
         const char16_t* pLanguage1,
         const char16_t* pLanguage2 )
{
   uint32_t nLanguageLength = 0;
   uint8_t* pLanguage = null;
   tNDEFRecord* pRecord = null;

   uint32_t  nPayloadOffset;

   if (W_NULL_HANDLE == hRecord || null == pLanguage1)
   {
      return W_RTD_TEXT_NO_MATCH_FOUND;
   }

   if ( (W_SUCCESS != PHandleGetObject( pContext, hRecord, P_HANDLE_TYPE_NDEF_RECORD, (void**)&pRecord))
      || (pRecord == null) )
   {
      return W_RTD_TEXT_NO_MATCH_FOUND;
   }

   if ( PNDEFParseSafeBuffer( pRecord->pRecordBuffer, pRecord->nRecordSize,
         null,null, null,null, &nPayloadOffset,null ) == 0)
   {
      return W_RTD_TEXT_NO_MATCH_FOUND;
   }

   if ( PRTDIsTextRecord( pContext, hRecord ) == W_FALSE )
   {
      return W_RTD_TEXT_NO_MATCH_FOUND;
   }

   nLanguageLength = (pRecord->pRecordBuffer[ nPayloadOffset]&0x3f) + 1;

   pLanguage = &pRecord->pRecordBuffer[ nPayloadOffset+1 ];

   /* Compare Language 1 */
   switch ( TextMatch(pLanguage, pLanguage1, nLanguageLength) )
   {
   case 0:
      return W_RTD_TEXT_MATCH_1;

   case 1:
      return W_RTD_TEXT_PARTIAL_MATCH_1;
   }

   /* Compare Language 2 */
   if ( pLanguage2 != null )
   {
      switch( TextMatch(pLanguage, pLanguage2, nLanguageLength) )
      {
      case 0:
         return W_RTD_TEXT_MATCH_2;

      case 1:
         return W_RTD_TEXT_PARTIAL_MATCH_2;
      }
   }
   return W_RTD_TEXT_NO_MATCH_FOUND;
}

/* See Client API Specifications */
W_ERROR PRTDTextFind(
         tContext* pContext,
         W_HANDLE hMessage,
         const char16_t* pLanguage1,
         const char16_t* pLanguage2,
         W_HANDLE* phRecord,
         uint8_t* pnMatch )
{
   tNDEFRecordHandleList* pCurrentRecord = null;
   tNDEFMessage* pMessage = null;
   W_HANDLE hCurrentRecord = 0;

   if (phRecord != null)
   {
      * phRecord = W_NULL_HANDLE;
   }

   if (pnMatch != null)
   {
      *pnMatch = W_RTD_TEXT_NO_MATCH_FOUND;
   }

   if ( (W_SUCCESS != PHandleGetObject( pContext, hMessage, P_HANDLE_TYPE_NDEF_MESSAGE, (void**)&pMessage))
      || (pMessage == null) )
   {
      return W_ERROR_BAD_HANDLE;
   }

   if (pMessage->pRecordHandleListCurrent != null)
   {
      pCurrentRecord = pMessage->pRecordHandleListCurrent;
   }
   else
   {
      pCurrentRecord = pMessage->pRecordHandleListBegin;
   }
   hCurrentRecord = pCurrentRecord->hRecord;

   if (( pLanguage1 == null ) || (phRecord == null) || (pnMatch == null))
   {
      return W_ERROR_BAD_PARAMETER;
   }

   if ( PRTDIsTextRecord( pContext, hCurrentRecord) == W_FALSE )
   {
      return W_ERROR_ITEM_NOT_FOUND;
   }

   do
   {
      *pnMatch = PRTDTextLanguageMatch( pContext, hCurrentRecord, pLanguage1, pLanguage2);

      if ( *pnMatch != W_RTD_TEXT_NO_MATCH_FOUND )
      {
         W_ERROR nError;
         W_HANDLE hRecord;
         nError = PHandleDuplicate(pContext, hCurrentRecord, &hRecord);
         if (nError == W_SUCCESS)
         {
            *phRecord = hRecord;
         }
         else
         {
            return nError;
         }
         break;
      }

      if (pCurrentRecord->pNext == null)
      {
         break;
      }
      else
      {
         pCurrentRecord = pCurrentRecord->pNext;
         hCurrentRecord = pCurrentRecord->hRecord;
      }
   }
   while (PRTDIsTextRecord( pContext, hCurrentRecord));

   return W_SUCCESS;
}

/* See Client API Specifications */
uint32_t PRTDTextGetLength(
         tContext* pContext,
         W_HANDLE hRecord )
{
   uint32_t nPayloadLength;
   uint32_t  nPayloadOffset;
   tNDEFRecord* pRecord = null;

   if (PRTDIsTextRecord(pContext, hRecord) == W_FALSE)
   {
      return 0;
   }

   PHandleGetObject( pContext, hRecord, P_HANDLE_TYPE_NDEF_RECORD, (void**)&pRecord );

   if (pRecord == null)
   {
      return 0;
   }

   if ( PNDEFParseSafeBuffer( pRecord->pRecordBuffer, pRecord->nRecordSize,
      null,null, null,null, &nPayloadOffset,&nPayloadLength ) == 0 )
   {
      return 0;
   }
   {
      uint32_t nTextLength;
      uint8_t* pPayload = &pRecord->pRecordBuffer[ nPayloadOffset ];

      if ( static_PRTDIsUTF8( pPayload ) )
      {
         nTextLength = PUtilConvertUTF8ToUTF16(
            null,
            &pPayload[ static_PRTDGetTextOffset( pPayload ) ],
            nPayloadLength - static_PRTDGetTextOffset( pPayload ) );
      }
      else
      {
         nTextLength = ((nPayloadLength - static_PRTDGetTextOffset( pPayload ))/2);
      }
      return nTextLength;
   }
}

/* See Client API Specifications */
uint32_t PRTDURIGetLength(
         tContext* pContext,
         W_HANDLE hRecord )
{
   uint32_t nLength;

   if (static_PRTDURIGetValue(pContext, hRecord, null, &nLength) == W_SUCCESS)
   {
      return (nLength);
   }

   return (0);
}

/* See Client API Specifications */
W_ERROR PRTDTextGetValue(
         tContext* pContext,
         W_HANDLE hRecord,
         char16_t* pBuffer,
         uint32_t nBufferLength )
{
   uint8_t *pPayload = null;

   uint32_t nLangLen =  0;
   uint32_t nUTF16Len = 0;

   uint32_t  nTypeOffset;
   uint32_t  nTypeLength;
   uint32_t  nPayloadOffset;
   uint32_t nPayloadLength;

   uint8_t* pText;
   uint32_t nTextLength;

   char16_t pTextType[] = {(char16_t)'T',0};
   tNDEFRecord *pRecord = null;

   if (null == pBuffer)
   {
      return W_ERROR_BUFFER_TOO_SHORT;
   }
   if ( (W_SUCCESS != PHandleGetObject( pContext, hRecord, P_HANDLE_TYPE_NDEF_RECORD, (void **)&pRecord))
      || (pRecord == null) )
   {
      return W_ERROR_BAD_HANDLE;
   }
   if ( (PNDEFParseSafeBuffer( pRecord->pRecordBuffer, pRecord->nRecordSize,
      &nTypeOffset,&nTypeLength, null,null, &nPayloadOffset, &nPayloadLength ) == 0))
   {
      return W_ERROR_BAD_HANDLE;
   }

   if ((nTypeLength == 0) || (nPayloadLength == 0))
   {
      return W_ERROR_WRONG_RTD;
   }

   if ( (nTypeLength != 1) || ( pTextType[0] != pRecord->pRecordBuffer[ nTypeOffset ] ) )
   {
      return W_ERROR_WRONG_RTD;
   }

   pPayload = &pRecord->pRecordBuffer[ nPayloadOffset ];
   nLangLen = (uint32_t)(*pPayload & 0x1F);

   if(nPayloadLength < (1 /* status byte */+ nLangLen /* lang */ + 1 /* Min text length*/))
   {
      return W_ERROR_WRONG_RTD;
   }

   nTextLength = nPayloadLength - (1+nLangLen);
   pText       = &pPayload[ 1 + nLangLen ];

   if (0 == (*pPayload & 0x80))
   {
      nUTF16Len = PUtilConvertUTF8ToUTF16( null, pText, nTextLength );

      /* Error RTD */
      if(nUTF16Len == 0)
      {
         return W_ERROR_WRONG_RTD;
      }

      /* Check length including zero ending */
      if ( nUTF16Len + 1 > nBufferLength)
      {
         return W_ERROR_BUFFER_TOO_SHORT;
      }
      PUtilConvertUTF8ToUTF16( pBuffer, pText, nTextLength );
      *(pBuffer + nUTF16Len) = 0;
      return W_SUCCESS;
   }
   else
   {
      /* Verify BOM (byte order mark) */

      /* if Little endian */
      if(PUtilCheckIsUTF16LE(pText) != W_FALSE)
      {
         /* Check length including zero ending */
         if ( (nTextLength+2) > (nBufferLength*2) )
         {
            return W_ERROR_BUFFER_TOO_SHORT;
         }
         PUtilConvertUTF16LEToUTF16( pBuffer, (const char16_t *) pText, nTextLength / 2);
         pBuffer[ nTextLength / sizeof(char16_t) ] = 0;
         return W_SUCCESS;
      }
      else /* it's big endian encoded */
      {
         /* Check length including zero ending */
         if ( nTextLength + 2 > (nBufferLength * 2))
         {
            return W_ERROR_BUFFER_TOO_SHORT;
         }

         PUtilConvertUTF16BEToUTF16( pBuffer, (const char16_t *) pText, nTextLength / 2);
         *(pBuffer + (nTextLength / 2)) = 0;
         return W_SUCCESS;
      } /* if little endian */
   }
}

/* See Client API Specifications */
W_ERROR PRTDTextCreateRecord(
         tContext* pContext,
         const char16_t* pLanguage,
         bool_t bUseUtf8,
         const char16_t* pText,
         uint32_t nTextLength,
         W_HANDLE* phRecord )
{
   uint32_t  nLanguageLength;
   uint32_t  nTotalLengthInByte;
   uint8_t  *pBuffer, * pOldBuffer, * pBegin;
   W_ERROR   nError;
   char16_t     pTypeText[] = { 'T', 0x00 };

   if (phRecord != null)
   {
      * phRecord = W_NULL_HANDLE;
   }

   nLanguageLength = PUtilStringLength(pLanguage);

   if ((nLanguageLength < 2) || (nLanguageLength > 63))
   {
      return (W_ERROR_BAD_PARAMETER);
   }

   if ((pText == null) && (nTextLength != 0))
   {
      return W_ERROR_BAD_PARAMETER;
   }

   nTotalLengthInByte = 1 + nLanguageLength;

   if (bUseUtf8)
   {
      nTotalLengthInByte += PUtilConvertUTF16ToUTF8(null, pText, nTextLength);
   }
   else
   {
      if ((nLanguageLength & 1) == 0)
      {
         nTotalLengthInByte ++;
      }

      nTotalLengthInByte += nTextLength * sizeof(char16_t);
   }

   pBuffer = pOldBuffer = pBegin = CMemoryAlloc(nTotalLengthInByte);

   if (pBuffer == null)
   {
      return (W_ERROR_OUT_OF_RESOURCE);
   }

   if ((bUseUtf8 == W_FALSE) && ((nLanguageLength & 1) == 0))
   {
      /* Force 16 bits alignment when copying the text */
      pBegin++;
      pBuffer++;
      nTotalLengthInByte --;
   }

   * pBuffer  = (uint8_t) nLanguageLength;

   if (bUseUtf8 == W_FALSE)
   {
      * pBuffer |= 0x80;
   }

   pBuffer++;

   while (nLanguageLength--)
   {
      * pBuffer++ = (uint8_t) * pLanguage++;
   }

   if (bUseUtf8 == W_FALSE)
   {
      PUtilConvertUTF16ToUTF16BE((char16_t *) pBuffer, pText, nTextLength);
   }
   else
   {
      PUtilConvertUTF16ToUTF8(pBuffer, pText, nTextLength);
   }

   nError = PNDEFCreateRecord(pContext, W_NDEF_TNF_WELL_KNOWN, pTypeText, pBegin, nTotalLengthInByte, phRecord);

   /* Free the allocated buffer */
   CMemoryFree(pOldBuffer);

   return nError;
}

/* See Client API Specifications */
W_ERROR PRTDTextAddRecord(
         tContext* pContext,
         W_HANDLE hMessage,
         const char16_t* pLanguage,
         bool_t bUseUtf8,
         const char16_t* pText,
         uint32_t nTextLength )
{
   W_HANDLE hRecord;
   W_ERROR  nError;

   if(pText == null  && nTextLength != 0)
   {
      return (W_ERROR_BAD_PARAMETER);
   }

   nError = PRTDTextCreateRecord(pContext, pLanguage, bUseUtf8, pText, nTextLength, &hRecord);

   if (nError != W_SUCCESS)
   {
      return (nError);
   }

   nError = PNDEFAppendRecord(pContext, hMessage, hRecord);

   PHandleClose(pContext, hRecord);

   return (nError);
}

/* See Client API Specifications */
W_ERROR PRTDURICreateRecord(
         tContext* pContext,
         const char16_t* pURI,
         W_HANDLE* phRecord )
{
   uint32_t nUtf8Len = 0;
   uint8_t *pTemp = null;
   uint8_t *pPayload = null;
   uint32_t nIdentifierCode = 0;
   uint32_t nPayloadLen = 0;
   uint32_t nURLOffset = 0;
   uint32_t nLoop;
   W_ERROR nErr = W_SUCCESS;
   char16_t pTypeString[2] = {(char16_t)'U', 0};
   uint32_t nURILength = ( pURI != null ) ? PUtilStringLength(pURI) : 0;

   if (phRecord != null)
   {
      * phRecord = W_NULL_HANDLE;
   }

   if(pURI != null)
   {
      if (PNDEFUtilStringIsPrintableURI(pURI) == W_FALSE)
      {
         return (W_ERROR_BAD_PARAMETER);
      }

      nUtf8Len = PNDEFUtilConvertPrintableUTF16ToUTF8( null, pURI );

      if ( (nURILength>0) && (nUtf8Len==0) )
      {
         /* change W_ERROR_BAD_PARAMETER by W_ERROR_OUT_OF_RESOURCE */
         return W_ERROR_OUT_OF_RESOURCE;
      }
      pTemp = (uint8_t *)CMemoryAlloc(nUtf8Len + 1);

      if ( pTemp == null )
      {
         return W_ERROR_OUT_OF_RESOURCE;
      }

      PNDEFUtilConvertPrintableUTF16ToUTF8( pTemp, pURI );
   }
   else
   {
      pTemp = (uint8_t *)CMemoryAlloc(1);
      if ( pTemp == null )
      {
         return W_ERROR_OUT_OF_RESOURCE;
      }
   }

   pTemp[nUtf8Len] = 0;

   for (nLoop = 1; nLoop < sizeof(g_URIIdentifyCodeAndLength)/sizeof(tURIIdentifyCodeAndLength); nLoop++)
   {
      if(nUtf8Len >= g_URIIdentifyCodeAndLength[nLoop].nLength)
      {
         if ( 0 == CMemoryCompare(pTemp,g_URIIdentifyCodeAndLength[nLoop].sIdentifier,g_URIIdentifyCodeAndLength[nLoop].nLength))
         {
            /* Store the index of the greatest corresponding URI code length */
            if ( (nIdentifierCode==0)
               || ( g_URIIdentifyCodeAndLength[nLoop].nLength > g_URIIdentifyCodeAndLength[nIdentifierCode].nLength ) )
            {
               nIdentifierCode = nLoop;
            }
         }
      }
   }

   nPayloadLen = 1 + ( nUtf8Len - g_URIIdentifyCodeAndLength[nIdentifierCode].nLength );
   nURLOffset = g_URIIdentifyCodeAndLength[nIdentifierCode].nLength;

   pPayload = (uint8_t *)CMemoryAlloc(nPayloadLen);
   if (null == pPayload)
   {
      CMemoryFree(pTemp);
      return W_ERROR_OUT_OF_RESOURCE;
   }

   *pPayload = (uint8_t)nIdentifierCode;
   CMemoryCopy(pPayload + 1, pTemp + nURLOffset, nPayloadLen - 1);
   CMemoryFree(pTemp);

   nErr = PNDEFCreateRecord(pContext, W_NDEF_TNF_WELL_KNOWN, pTypeString, pPayload, nPayloadLen, phRecord);
   CMemoryFree(pPayload);
   return nErr;
}

/* See Client API Specifications */
W_ERROR PRTDURIAddRecord(
         tContext* pContext,
         W_HANDLE hMessage,
         const char16_t* pURI )
{
   W_ERROR  nError;
   W_HANDLE hRecord;

   if(hMessage == W_NULL_HANDLE)
   {
      return (W_ERROR_BAD_HANDLE);
   }

   if ((nError = PRTDURICreateRecord(pContext, pURI, &hRecord)) != W_SUCCESS)
   {
      return (nError);
   }

   nError = PNDEFAppendRecord(pContext, hMessage, hRecord);

   PHandleClose(pContext, hRecord);

   return (nError);
}


#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */
