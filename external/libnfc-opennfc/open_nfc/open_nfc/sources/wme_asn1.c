/*
 * Copyright (c) 2011 Inside Secure, All Rights Reserved.
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

#define P_MODULE  P_MODULE_DEC( ASN1 )

#include "wme_context.h"

/**
 * Checks and returns the length and value of a ASN1 buffer (DER encoded)
 *
 * @param[in] pBuffer  The buffer containing the ASN1 buffer.
 *
 * @param[in] nBufferLength  The length in bytes of the buffer.
 *
 * @param[out] pnValueLength  A pointer on a variable where is stored the length in bytes of the TLV.
 *
 * @return  A pointer on the Value of the TLV, or null in case of error.
 **/
static const uint8_t* static_PAsn1ParseDERStructure(
         const uint8_t* pBuffer,
         uint32_t nBufferLength,
         uint32_t* pnValueLength)
{
   const uint8_t* pPosition;
   uint8_t nLength;

   if((pBuffer == null) || (pnValueLength == null))
   {
      PDebugError("static_PAsn1ParseDERStructure: The buffer is null");
      return null;
   }

   *pnValueLength = 0;

   if(nBufferLength < 2)
   {
      PDebugError("static_PAsn1ParseDERStructure: The buffer is too short for any TLV");
      return null;
   }
   pPosition = pBuffer + 1;
   if(*pPosition < 0x80)
   {
      /* short form */
      *pnValueLength = *pPosition;
   }
   else
   {
      /* long form */
      nLength = (uint8_t)(*pPosition & 0x7F);
      if(((uint32_t)(nLength+2) >= nBufferLength) || (nLength == 0) || (nLength > 4))
      {
         /* length is too long */
         PDebugError("static_PAsn1ParseDERStructure: The length is too long");
         return null;
      }
      do
      {
         *pnValueLength = ((*pnValueLength) <<8)|*(++pPosition);
      } while(--nLength > 0);
   }
   pPosition++;

   if((pPosition + *pnValueLength) > (pBuffer + nBufferLength))
   {
      PDebugError("static_PAsn1ParseDERStructure: The buffer length is too short to contain the TLV");
      *pnValueLength = 0;
      return null;
   }

   return pPosition;
}

/* See header file */
W_ERROR PAsn1InitializeParser(
         tAsn1Parser* pParser,
         const uint8_t* pBuffer,
         uint32_t nBufferLength)
{
   PDebugTrace("PAsn1InitializeParser()");
   CDebugAssert(pParser != null);

   pParser->pCurrentValue = static_PAsn1ParseDERStructure(
      pBuffer, nBufferLength, &(pParser->nCurrentLength));

   if(pParser->pCurrentValue == null)
   {
      return W_ERROR_BAD_PARAMETER;
   }
   pParser->pBuffer = pBuffer;
   pParser->nBufferLength = nBufferLength;
   pParser->nCursor = 0;

   return W_SUCCESS;
}

/* See header file */
W_ERROR PAsn1MoveToNextTlv(
         tAsn1Parser* pParser)
{
   uintptr_t nCursor;

   PDebugTrace("PAsn1MoveToNextTlv()");

   CDebugAssert(pParser != null);

   if(pParser->nCursor >= pParser->nBufferLength)
   {
      CDebugAssert(pParser->pCurrentValue == null);
      CDebugAssert(pParser->nCurrentLength == 0);
      return W_ERROR_BAD_PARAMETER;
   }

   CDebugAssert(pParser->pCurrentValue != null);

   nCursor = (pParser->pCurrentValue - pParser->pBuffer) + pParser->nCurrentLength;
   CDebugAssert(nCursor <= (uintptr_t)((uint32_t)-1));
   pParser->nCursor = (uint32_t)nCursor;

   if(pParser->nCursor >= pParser->nBufferLength)
   {
      pParser->pCurrentValue = null;
      pParser->nCurrentLength = 0;
      return W_ERROR_BAD_PARAMETER;
   }

   pParser->pCurrentValue = static_PAsn1ParseDERStructure(
      &pParser->pBuffer[pParser->nCursor],
      pParser->nBufferLength - pParser->nCursor, &(pParser->nCurrentLength));

   if(pParser->pCurrentValue == null)
   {
      return W_ERROR_BAD_PARAMETER;
   }

   return W_SUCCESS;
}

/* See header file */
bool_t PAsn1CheckEndOfTlv(
         const tAsn1Parser* pParser)
{
   PDebugTrace("PAsn1CheckEndOfTlv()");

   CDebugAssert(pParser != null);

   if(pParser->nCursor >= pParser->nBufferLength)
   {
      CDebugAssert(pParser->pCurrentValue == null);
      CDebugAssert(pParser->nCurrentLength == 0);
      return W_TRUE;
   }
   return W_FALSE;
}

/** See header file */
W_ERROR PAsn1ParseIncludedTlv(
         const tAsn1Parser* pEnclosingParser,
         tAsn1Parser* pIncludedParser)
{
   PDebugTrace("PAsn1ParseIncludedTlv()");

   if((pEnclosingParser == null) || (pEnclosingParser->pBuffer == null) || (pIncludedParser == null))
   {
      PDebugError("PAsn1ParseIncludedTlv: null parameter");
      return W_ERROR_BAD_PARAMETER;
   }
   if(PAsn1CheckEndOfTlv(pEnclosingParser) != W_FALSE)
   {
      PDebugError("PAsn1ParseIncludedTlv: end of buffer reached");
      return W_ERROR_BAD_PARAMETER;
   }

   return PAsn1InitializeParser(
         pIncludedParser, pEnclosingParser->pCurrentValue, pEnclosingParser->nCurrentLength);
}

/** See header file */
W_ERROR PAsn1ParseIntegerValue(
         const tAsn1Parser* pParser,
         int32_t* pnValue)
{
   uint32_t nIntegerSize;
   const uint8_t* pValue;

   PDebugTrace("PAsn1ParseIntegerValue()");

   if((pParser == null) || (pParser->pBuffer == null) || (pnValue == null))
   {
      PDebugError("PAsn1ParseInteger: null parameter");
      return W_ERROR_BAD_PARAMETER;
   }
   if(PAsn1CheckEndOfTlv(pParser) != W_FALSE)
   {
      PDebugError("PAsn1ParseInteger: end of buffer reached");
      return W_ERROR_BAD_PARAMETER;
   }

   pValue = pParser->pCurrentValue;
   nIntegerSize = pParser->nCurrentLength;

   if(nIntegerSize > 4)
   {
      PDebugError("PAsn1ParseInteger: The integer value is longer than 32 bits");
      return W_ERROR_BUFFER_TOO_SHORT;
   }

   /* Sign extension */
   *pnValue = (int8_t)*pValue++;
   while (--nIntegerSize > 0)
   {
      *pnValue = (*pnValue << 8) + *pValue++;
   }

   PDebugTrace("PAsn1ParseIntegerValue: the value is %d", *pnValue);

   return W_SUCCESS;
}

/** See header file */
W_ERROR PAsn1ParseInteger(
         const tAsn1Parser* pParser,
         int32_t* pnValue)
{
   W_ERROR nError;

   PDebugTrace("PAsn1ParseInteger()");

   nError = PAsn1ParseIntegerValue(pParser, pnValue);
   if (nError != W_SUCCESS)
      { return nError; }

   if(PAsn1GetTagValue(pParser) != P_ASN1_TAG_UNIVERSAL_INTEGER)
   {
      PDebugError("PAsn1ParseInteger: The TLV is not of type UNIVERSAL INTEGER");
      return W_ERROR_BAD_PARAMETER;
   }

   PDebugTrace("PAsn1ParseInteger: the value is %d", *pnValue);

   return W_SUCCESS;
}

/** See header file */
W_ERROR PAsn1ParseOctetString(
         const tAsn1Parser* pParser,
         const uint8_t** ppContent,
         uint32_t* pnLength)
{
   PDebugTrace("PAsn1ParseOctetString()");

   if((pParser == null) || (pParser->pBuffer == null) || (ppContent == null) || (pnLength == null))
   {
      PDebugError("PAsn1ParseOctetString: null parameter");
      return W_ERROR_BAD_PARAMETER;
   }
   if(PAsn1CheckEndOfTlv(pParser) != W_FALSE)
   {
      PDebugError("PAsn1ParseOctetString: end of buffer reached");
      return W_ERROR_BAD_PARAMETER;
   }

   if(PAsn1GetTagValue(pParser) != P_ASN1_TAG_UNIVERSAL_OCTET_STRING)
   {
      PDebugError("PAsn1ParseOctetString: The TLV is not of type UNIVERSAL OCTET STRING");
      return W_ERROR_BAD_PARAMETER;
   }

   *ppContent = pParser->pCurrentValue;
   *pnLength = pParser->nCurrentLength;

   return W_SUCCESS;
}

/* See header file */
bool_t PAsn1CompareTlvToBuffer(
         const tAsn1Parser* pParser,
         const uint8_t* pBuffer,
         uint32_t nBufferLength)
{
   const uint8_t* pValue;
   uint32_t nLength;

   if((pParser == null) || (pBuffer == null) || (nBufferLength < 2))
   {
      PDebugError("PAsn1ParseOctetString: bad parameter");
      return W_FALSE;
   }
   if(PAsn1CheckEndOfTlv(pParser) != W_FALSE)
   {
      PDebugError("PAsn1ParseInteger: end of buffer reached");
      return W_ERROR_BAD_PARAMETER;
   }
   if(PAsn1GetTagValue(pParser) != pBuffer[0])
   {
      PDebugError("PAsn1ParseInteger: types are different");
      return W_ERROR_BAD_PARAMETER;
   }

   pValue = static_PAsn1ParseDERStructure(pBuffer, nBufferLength, &nLength);

   return ((nLength == pParser->nCurrentLength)
   && (CMemoryCompare(pParser->pCurrentValue, pValue, nLength) == 0))?W_TRUE:W_FALSE;
}

/* See header file */
bool_t PAsn1CompareTlv(
         const tAsn1Parser* pParser1,
         const tAsn1Parser* pParser2)
{
   PDebugTrace("PAsn1CompareTlv()");

   if((pParser1 == null) || (pParser2 == null))
   {
      PDebugError("PAsn1CompareTlv: null pointer");
      return W_FALSE;
   }

   if((PAsn1CheckEndOfTlv(pParser1) != W_FALSE) || (PAsn1CheckEndOfTlv(pParser2) != W_FALSE))
   {
      PDebugError("PAsn1CompareTlv: end of buffer reached");
      return W_ERROR_BAD_PARAMETER;
   }

   if(PAsn1GetTagValue(pParser1) != PAsn1GetTagValue(pParser2))
   {
      PDebugError("PAsn1CompareTlv: The TLV types are not identical");
      return W_FALSE;
   }

   return ((pParser1->nCurrentLength == pParser2->nCurrentLength) &&
           (CMemoryCompare(pParser1->pCurrentValue, pParser2->pCurrentValue, pParser1->nCurrentLength) == 0))?W_TRUE:W_FALSE;
}

/* See header file */
void PAsn1GetPointerOnTlvBuffer(
         const tAsn1Parser* pParser,
         const uint8_t** ppContent,
         uint32_t* pnLength)
{
   uintptr_t nLength;

   PDebugTrace("PAsn1GetPointerOnTlvBuffer()");

   CDebugAssert((pParser != null) && (ppContent != null) && (pnLength != null));

   if(PAsn1CheckEndOfTlv(pParser) != W_FALSE)
   {
      PDebugError("PAsn1GetPointerOnTlvBuffer: end of buffer reached");
      return;
   }

   *ppContent = &pParser->pBuffer[pParser->nCursor];
   nLength = (pParser->pCurrentValue - *ppContent) + pParser->nCurrentLength;
   CDebugAssert(nLength <= (uintptr_t)((uint32_t)-1));
   *pnLength = (uint32_t)nLength;
}

/* See Header file */
void PAsn1GetPointerOnBinaryContent(
         const tAsn1Parser* pParser,
         const uint8_t** ppContent,
         uint32_t* pnLength)
{
   PDebugTrace("PAsn1GetPointerOnBinaryContent()");

   CDebugAssert((pParser != null) && (ppContent != null) && (pnLength != null) );

   if(PAsn1CheckEndOfTlv(pParser) != W_FALSE)
   {
      PDebugError("PAsn1GetPointerOnBinaryContent: end of buffer reached");
      return;
   }

   *ppContent = pParser->pCurrentValue;
   *pnLength = pParser->nCurrentLength;
}

/* See header file */
uint8_t PAsn1GetTagValue(
         const tAsn1Parser* pParser)
{
   CDebugAssert(pParser != null);

   if(PAsn1CheckEndOfTlv(pParser) != W_FALSE)
   {
      PDebugError("PAsn1GetTagValue: end of buffer reached");
      return 0;
   }

   PDebugTrace("PAsn1GetTagValue() returning 0x%02X", pParser->pBuffer[pParser->nCursor]);

   return pParser->pBuffer[pParser->nCursor];
}

/* See header file */
uint32_t PAsn1GetTagLength(
         const tAsn1Parser* pParser)
{
   CDebugAssert(pParser != null);

   if(PAsn1CheckEndOfTlv(pParser) != W_FALSE)
   {
      PDebugError("PAsn1GetTagLength: end of buffer reached");
      return 0;
   }

   PDebugTrace("PAsn1GetTagLength() returning %d", pParser->nCurrentLength);

   return pParser->nCurrentLength;
}


