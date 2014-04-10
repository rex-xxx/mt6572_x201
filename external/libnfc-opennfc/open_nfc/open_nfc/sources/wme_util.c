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
   Contains the implementation of some utility functions
*******************************************************************************/

#include "wme_context.h"

/* See header file */
uint32_t PUtilWriteUint16ToBigEndianBuffer(
         uint16_t nValue,
         uint8_t* pBuffer)
{
   pBuffer[0] = (uint8_t)((nValue >> 8) & 0xFF);
   pBuffer[1] = (uint8_t)(nValue & 0xFF);

   return 2;
}

/* See header file */
uint16_t PUtilReadUint16FromBigEndianBuffer(
         const uint8_t* pBuffer)
{
   return (uint16_t)((((uint16_t)pBuffer[0]) << 8) | ((uint16_t)pBuffer[1]));
}

/* See header file */
uint32_t PUtilWriteUint32ToBigEndianBuffer(
         uint32_t nValue,
         uint8_t* pBuffer)
{
   pBuffer[0] = (uint8_t)((nValue >> 24) & 0xFF);
   pBuffer[1] = (uint8_t)((nValue >> 16) & 0xFF);
   pBuffer[2] = (uint8_t)((nValue >> 8) & 0xFF);
   pBuffer[3] = (uint8_t)(nValue & 0xFF);

   return 4;
}

/* See header file */
uint32_t PUtilWriteUint32ToLittleEndianBuffer(
         uint32_t nValue,
         uint8_t* pBuffer)
{
   pBuffer[0] = (uint8_t)(nValue & 0xFF);
   pBuffer[1] = (uint8_t)((nValue >> 8) & 0xFF);
   pBuffer[2] = (uint8_t)((nValue >> 16) & 0xFF);
   pBuffer[3] = (uint8_t)((nValue >> 24) & 0xFF);

   return 4;
}

/* See header file */
uint32_t PUtilReadUint32FromBigEndianBuffer(
         const uint8_t* pBuffer)
{
   return (uint32_t)((((uint32_t)pBuffer[0]) << 24)
   | (((uint32_t)pBuffer[1]) << 16)
   | (((uint32_t)pBuffer[2]) << 8)
   | ((uint32_t)pBuffer[3]));
}

/* See header file */
uint32_t PUtilReadUint32FromLittleEndianBuffer(
         const uint8_t* pBuffer)
{
   return (uint32_t)((((uint32_t)pBuffer[3]) << 24)
   | (((uint32_t)pBuffer[2]) << 16)
   | (((uint32_t)pBuffer[1]) << 8)
   | ((uint32_t)pBuffer[0]));
}

/* See header file */
void PUtilWriteAddressToBigEndianBuffer(
         void* pValue,
         uint8_t* pBuffer)
{
   int8_t i, j = 0;
   uintptr_t nValue = (uintptr_t)pValue;

   for(i = (sizeof(uintptr_t) - 1); i >= 0; i--)
   {
      pBuffer[j++] = (uint8_t)((nValue >> (i * 8)) & 0xFF);
   }
}

/* See header file */
uintptr_t PUtilReadAddressFromBigEndianBuffer(
         const uint8_t* pBuffer)
{
   int8_t i, j = 0;
   uintptr_t nValue = 0;

   for(i = (sizeof(uintptr_t) - 1); i >= 0; i--)
   {
      nValue |= (((uintptr_t)pBuffer[j++]) << (i * 8));
   }
   return (nValue);
}

/* See header file */
bool_t PUtilReadBoolFromLittleEndianBuffer(
         uint8_t* pBuffer)
{
   return (pBuffer[0] != 0);
}

/******************************************************************************

   Utf-8 (8-bit UCS/Unicode Transformation Format) ISO 10646
   ---------------------------------------------------------

                              0zzzzzzz (7)          [   00..7F   ] (Ascii)
                     110yyyyy 10zzzzzz (5+6=11)     [   80..7FF  ]
            1110xxxx 10yyyyyy 10zzzzzz (4+6+6=16)   [  800..FFFF ]


   Utf-16 - (16-bit Unicode Transformation Format) ISO 10646
   ---------------------------------------------------------

                [0000..FFFF] 64Ko available
                (!) surrogate not impleted [D800..DFFF]
                Little Endian specified by NDEF (NFC forum)

******************************************************************************/

/* See header file */
uint32_t PUtilConvertUTF16ToUTF8(
                  uint8_t* pDestUtf8,
                  const char16_t* pSourceUtf16,
                  uint32_t nSourceCharLength )
{
   uint32_t nIndex = 0;
   uint16_t v;
   uint32_t i;

   for (i = 0; i < nSourceCharLength; i++)
   {
      v = pSourceUtf16[i];

      if ( v < 128 )
      {
         /* [0xxx-xxxx] */
         if ( pDestUtf8 != null )
         {
            *pDestUtf8++ = (uint8_t)v;          /* 00000000-0xxxxxx */
         }
         nIndex++;
      }
      else if ( v < 2048 )
      {
         /* [110x-xxxx][10xx-xxxx] */
         if ( pDestUtf8 != null )
         {
            *pDestUtf8++ = (uint8_t)(0xC0 | (v>>6));       /* 00000xxx-xx...... */
            *pDestUtf8++ = (uint8_t)(0x80 | (v&0x3F));     /* 00000...-..xxxxxx */
         }
         nIndex += 2;
      }
      else
      {
         /* [1110-xxxx][10xx-xxxx][10xx-xxxx] */
         if ( pDestUtf8 != null )
         {
            *pDestUtf8++ = (uint8_t)(0xE0 | ((v&0xF000)>>12)); /* xxxx....-........ */
            *pDestUtf8++ = (uint8_t)(0x80 | ((v&0x0FC0)>>6));  /* ....xxxx-xx...... */
            *pDestUtf8++ = (uint8_t)(0x80 | (v&0x3F));         /* ........-..xxxxxx */
         }
         nIndex += 3;
      }
   }

   return nIndex;
}

/* See header file */
uint32_t PUtilConvertUTF8ToUTF16(
                  char16_t* pDestUtf16,
                  const uint8_t* pSourceUtf8,
                  uint32_t nSourceLength )
{
   uint32_t i;
   uint16_t nValue;
   uint32_t nIndex = 0;

   for (i=0; i<nSourceLength; i++)
   {
      if ( ((*pSourceUtf8)&0x80) == 0x00 )
      {
         /* [0xxx-xxxx] */
         nValue = *pSourceUtf8++;      /* 0xxx-xxxx */
      }
      else
      {
         switch ( (*pSourceUtf8) & 0xF0 )
         {
         case 0xE0:
            /* [1110-xxxx][10xx-xxxx][10xx-xxxx] */
            if ( ( (i+2) < nSourceLength )
               && ( (*(pSourceUtf8+1)&0xC0) == 0x80 )
               && ( (*(pSourceUtf8+2)&0xC0) == 0x80 ))
            {
               nValue =  ( (uint16_t)*(pSourceUtf8++)&0x0f ) << (12);/* ....-xxxx */
               nValue |= ( (uint16_t)*(pSourceUtf8++)&0x3f ) << 6;   /* ..xx-xxxx */
               nValue |=   (uint16_t)*(pSourceUtf8++)&0x3f;          /* ..xx-xxxx */
               if ( nValue < 2048 )
               {
                  goto return_error;
               }
               i += 2;
               break;
            }
            goto return_error;

         case 0xC0:
         case 0xD0:
               /* [110x-xxxx][10xx-xxxx] */
            if ( ( (i+1) < nSourceLength )
               && ( (*(pSourceUtf8+1)&0xC0) == 0x80 ))
            {
               nValue =  ( (uint16_t)*(pSourceUtf8++)&0x1f ) << 6;/* ...x-xxxx */
               nValue |=   (uint16_t)*(pSourceUtf8++)&0x3f;       /* ..xx-xxxx */
               if ( nValue < 128 )
               {
                  goto return_error;
               }
               ++i;
               break;
            }
            goto return_error;

         default:
            goto return_error;
         }
      }

      if ( pDestUtf16 != null )
      {
         ((uint16_t*)pDestUtf16)[ nIndex ] = nValue;
      }
      ++nIndex;

   } /* END loop */

   return nIndex;

return_error:

   return 0;
}


static bool_t static_PUtilIsMachineLittleEndian(void)
{
   char16_t      nCar = 0x1234;
   uint8_t  * p = (uint8_t * ) & nCar;

   return (*p == 0x12) ? W_FALSE : W_TRUE;
}


/* see header */
void PUtilConvertUTF16ToUTF16BE(
                  char16_t* pDestUtf16,
                  const char16_t* pSourceUtf16,
                  uint32_t nSourceLength )
{
   char16_t nCurrentChar;

   if (static_PUtilIsMachineLittleEndian() != W_FALSE)
   {
      while (nSourceLength-- != 0)
      {
         nCurrentChar = * pSourceUtf16++ ;

         * pDestUtf16++ =  ((nCurrentChar & 0x00FF) << 8) | ((nCurrentChar >> 8) & 0x00FF);
      }
   }
   else
   {
      if (pDestUtf16 != pSourceUtf16)
      {
         CMemoryMove(pDestUtf16, pSourceUtf16, nSourceLength * sizeof(char16_t));
      }
   }
}

/* see header */
void PUtilConvertUTF16BEToUTF16(
                  char16_t* pDestUtf16,
                  const char16_t* pSourceUtf16,
                  uint32_t nSourceLength )
{

   char16_t nCurrentChar;

   if (static_PUtilIsMachineLittleEndian() != W_FALSE)
   {
      while (nSourceLength-- != 0)
      {
         nCurrentChar = * pSourceUtf16++ ;

         * pDestUtf16++ = ((nCurrentChar & 0x00FF) << 8) | ((nCurrentChar >> 8) & 0x00FF);
      }
   }
   else
   {
      if (pDestUtf16 != pSourceUtf16)
      {
         CMemoryMove(pDestUtf16, pSourceUtf16, nSourceLength * sizeof(char16_t));
      }
   }
}


/* see header */
void PUtilConvertUTF16LEToUTF16(
                  char16_t* pDestUtf16,
                  const char16_t* pSourceUtf16,
                  uint32_t nSourceLength )
{

   char16_t nCurrentChar;

   if (static_PUtilIsMachineLittleEndian() == W_FALSE)
   {
      while (nSourceLength-- != 0)
      {
         nCurrentChar = * pSourceUtf16++ ;

         * pDestUtf16++ = ((nCurrentChar & 0x00FF) << 8) | ((nCurrentChar >> 8) & 0x00FF);
      }
   }
   else
   {
      if (pDestUtf16 != pSourceUtf16)
      {
         CMemoryMove(pDestUtf16, pSourceUtf16, nSourceLength * sizeof(char16_t));
      }
   }
}


/* See header file */
uint32_t PUtilWriteHexaUint8(
               char16_t* pStringBuffer,
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
uint32_t PUtilWriteHexaUint32(
               char16_t* pStringBuffer,
               uint32_t nValue)
{
   pStringBuffer += PUtilWriteHexaUint8(pStringBuffer, (uint8_t)((nValue >> 24) & 0xFF));
   pStringBuffer += PUtilWriteHexaUint8(pStringBuffer, (uint8_t)((nValue >> 16) & 0xFF));
   pStringBuffer += PUtilWriteHexaUint8(pStringBuffer, (uint8_t)((nValue >> 8) & 0xFF));
   PUtilWriteHexaUint8(pStringBuffer, (uint8_t)(nValue & 0xFF));

   return 8;
}

/* See header file */
uint32_t PUtilWriteDecimalUint32(
               char16_t* pStringBuffer,
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
uint32_t PUtilStringLength(
               const char16_t* pString )
{
   const char16_t *pNext;
   uintptr_t nLength = 0;

   if (pString != null)
   {
      for (pNext = pString; *pNext; ++pNext);
      nLength = pNext - pString;
      CDebugAssert(nLength <= (uintptr_t)((uint32_t)-1));
   }
   return (uint32_t) nLength;
}

/* See header file */
int32_t PUtilStringCompare(
               const char16_t* pString1,
               const char16_t* pString2 )
{
  while (*pString1 == *pString2)
  {
      if (*pString1 == 0)
      {
         return 0;
      }
      pString1++;
      pString2++;
  }

  return *pString1 - *pString2;
}

/* See header file */
int32_t PUtilStringNCompare(
               const char16_t * pString1,
               const char16_t * pString2,
               uint32_t      nLength)
{
   while ((nLength != 0) && (*pString1 == *pString2))
   {
      if (*pString1 == 0)
      {
         return 0;
      }
      pString1++;
      pString2++;
      nLength--;
  }

   if (nLength > 0)
   {
      return *pString1 - *pString2;
   }
   else
   {
      return 0;
   }
}


/* See header file */
int32_t PUtilMixedStringCompare(
               const char16_t* pUnicodeString,
               uint32_t nUnicodeLength,
               const char* pASCIIString )
{
   uint32_t nIndex;

   for(nIndex = 0; nIndex < nUnicodeLength; nIndex++)
   {
      char16_t ac = (char16_t)pASCIIString[nIndex];
      char16_t uc = pUnicodeString[nIndex];

      if(ac != uc)
      {
         return uc - ac;
      }

      if(uc == 0)
      {
         if(nIndex == nUnicodeLength - 1)
         {
            return 0;
         }

         return 1;
      }
   }

   return 0 - pASCIIString[nIndex];
}

/* See header file */
char16_t* PUtilStringCopy(
               char16_t* pBuffer,
               uint32_t* pPos,
               const char16_t* pString)
{
   uint32_t nCursor = *pPos;
   char16_t c;

   while( (c = *pString++) != 0)
   {
      pBuffer[nCursor++] = c;
   }

   pBuffer[nCursor] = 0;

   *pPos = nCursor;

   return &pBuffer[nCursor];
}

/* See header file */
uint32_t PUtilAsciiStringLength(
         const char* pString)
{
   uint32_t nLength = 0;

   if ( pString != null )
   {
      while ( *pString++ != 0 )
      {
         nLength++;
      }
   }

   return nLength;
}

/* Precomputed CRC32 table. The polynomial is the following:
 *
 *  X^32+X^26+X^23+X^22+X^16+X^12+X^11+X^10+X^8+X^7+X^5+X^4+X^2+X^1+X^0
 **/
static const uint32_t g_aCrc32Table[] = {
   0x00000000, 0x77073096, 0xee0e612c, 0x990951ba, 0x076dc419, 0x706af48f,
   0xe963a535, 0x9e6495a3, 0x0edb8832, 0x79dcb8a4, 0xe0d5e91e, 0x97d2d988,
   0x09b64c2b, 0x7eb17cbd, 0xe7b82d07, 0x90bf1d91, 0x1db71064, 0x6ab020f2,
   0xf3b97148, 0x84be41de, 0x1adad47d, 0x6ddde4eb, 0xf4d4b551, 0x83d385c7,
   0x136c9856, 0x646ba8c0, 0xfd62f97a, 0x8a65c9ec, 0x14015c4f, 0x63066cd9,
   0xfa0f3d63, 0x8d080df5, 0x3b6e20c8, 0x4c69105e, 0xd56041e4, 0xa2677172,
   0x3c03e4d1, 0x4b04d447, 0xd20d85fd, 0xa50ab56b, 0x35b5a8fa, 0x42b2986c,
   0xdbbbc9d6, 0xacbcf940, 0x32d86ce3, 0x45df5c75, 0xdcd60dcf, 0xabd13d59,
   0x26d930ac, 0x51de003a, 0xc8d75180, 0xbfd06116, 0x21b4f4b5, 0x56b3c423,
   0xcfba9599, 0xb8bda50f, 0x2802b89e, 0x5f058808, 0xc60cd9b2, 0xb10be924,
   0x2f6f7c87, 0x58684c11, 0xc1611dab, 0xb6662d3d, 0x76dc4190, 0x01db7106,
   0x98d220bc, 0xefd5102a, 0x71b18589, 0x06b6b51f, 0x9fbfe4a5, 0xe8b8d433,
   0x7807c9a2, 0x0f00f934, 0x9609a88e, 0xe10e9818, 0x7f6a0dbb, 0x086d3d2d,
   0x91646c97, 0xe6635c01, 0x6b6b51f4, 0x1c6c6162, 0x856530d8, 0xf262004e,
   0x6c0695ed, 0x1b01a57b, 0x8208f4c1, 0xf50fc457, 0x65b0d9c6, 0x12b7e950,
   0x8bbeb8ea, 0xfcb9887c, 0x62dd1ddf, 0x15da2d49, 0x8cd37cf3, 0xfbd44c65,
   0x4db26158, 0x3ab551ce, 0xa3bc0074, 0xd4bb30e2, 0x4adfa541, 0x3dd895d7,
   0xa4d1c46d, 0xd3d6f4fb, 0x4369e96a, 0x346ed9fc, 0xad678846, 0xda60b8d0,
   0x44042d73, 0x33031de5, 0xaa0a4c5f, 0xdd0d7cc9, 0x5005713c, 0x270241aa,
   0xbe0b1010, 0xc90c2086, 0x5768b525, 0x206f85b3, 0xb966d409, 0xce61e49f,
   0x5edef90e, 0x29d9c998, 0xb0d09822, 0xc7d7a8b4, 0x59b33d17, 0x2eb40d81,
   0xb7bd5c3b, 0xc0ba6cad, 0xedb88320, 0x9abfb3b6, 0x03b6e20c, 0x74b1d29a,
   0xead54739, 0x9dd277af, 0x04db2615, 0x73dc1683, 0xe3630b12, 0x94643b84,
   0x0d6d6a3e, 0x7a6a5aa8, 0xe40ecf0b, 0x9309ff9d, 0x0a00ae27, 0x7d079eb1,
   0xf00f9344, 0x8708a3d2, 0x1e01f268, 0x6906c2fe, 0xf762575d, 0x806567cb,
   0x196c3671, 0x6e6b06e7, 0xfed41b76, 0x89d32be0, 0x10da7a5a, 0x67dd4acc,
   0xf9b9df6f, 0x8ebeeff9, 0x17b7be43, 0x60b08ed5, 0xd6d6a3e8, 0xa1d1937e,
   0x38d8c2c4, 0x4fdff252, 0xd1bb67f1, 0xa6bc5767, 0x3fb506dd, 0x48b2364b,
   0xd80d2bda, 0xaf0a1b4c, 0x36034af6, 0x41047a60, 0xdf60efc3, 0xa867df55,
   0x316e8eef, 0x4669be79, 0xcb61b38c, 0xbc66831a, 0x256fd2a0, 0x5268e236,
   0xcc0c7795, 0xbb0b4703, 0x220216b9, 0x5505262f, 0xc5ba3bbe, 0xb2bd0b28,
   0x2bb45a92, 0x5cb36a04, 0xc2d7ffa7, 0xb5d0cf31, 0x2cd99e8b, 0x5bdeae1d,
   0x9b64c2b0, 0xec63f226, 0x756aa39c, 0x026d930a, 0x9c0906a9, 0xeb0e363f,
   0x72076785, 0x05005713, 0x95bf4a82, 0xe2b87a14, 0x7bb12bae, 0x0cb61b38,
   0x92d28e9b, 0xe5d5be0d, 0x7cdcefb7, 0x0bdbdf21, 0x86d3d2d4, 0xf1d4e242,
   0x68ddb3f8, 0x1fda836e, 0x81be16cd, 0xf6b9265b, 0x6fb077e1, 0x18b74777,
   0x88085ae6, 0xff0f6a70, 0x66063bca, 0x11010b5c, 0x8f659eff, 0xf862ae69,
   0x616bffd3, 0x166ccf45, 0xa00ae278, 0xd70dd2ee, 0x4e048354, 0x3903b3c2,
   0xa7672661, 0xd06016f7, 0x4969474d, 0x3e6e77db, 0xaed16a4a, 0xd9d65adc,
   0x40df0b66, 0x37d83bf0, 0xa9bcae53, 0xdebb9ec5, 0x47b2cf7f, 0x30b5ffe9,
   0xbdbdf21c, 0xcabac28a, 0x53b39330, 0x24b4a3a6, 0xbad03605, 0xcdd70693,
   0x54de5729, 0x23d967bf, 0xb3667a2e, 0xc4614ab8, 0x5d681b02, 0x2a6f2b94,
   0xb40bbe37, 0xc30c8ea1, 0x5a05df1b, 0x2d02ef8d
};

/* See header file */
uint32_t PUtilComputeCrc32(
            uint32_t nCrc,
            const uint8_t* pBuffer,
            uint32_t nLength)
{
   nCrc = nCrc ^ 0xffffffff;

   while (nLength--)
      nCrc = g_aCrc32Table[(nCrc ^ *pBuffer++) & 0xff] ^ (nCrc >> 8);

   return nCrc ^ 0xffffffff;
}

/* -----------------------------------------------------------------------------
      Trace Functions
----------------------------------------------------------------------------- */

/* See header file */
const char* PUtilTraceConnectionProperty(
         uint8_t nProtocol)
{
   switch(nProtocol)
   {
      case W_PROP_ISO_14443_3_A: return "ISO 14443-3 A";
      case W_PROP_ISO_14443_4_A: return "ISO 14443-4 A";
      case W_PROP_ISO_14443_3_B: return "ISO 14443-3 B";
      case W_PROP_ISO_14443_4_B: return "ISO 14443-4 B";
      case W_PROP_ISO_15693_3: return "ISO 15693-3";
      case W_PROP_ISO_15693_2: return "ISO 15693-2";
      case W_PROP_ISO_7816_4: return "ISO 7816-4";
      case W_PROP_BPRIME: return "B Prime";
      case W_PROP_NFC_TAG_TYPE_1: return "Type 1 Tag";
      case W_PROP_NFC_TAG_TYPE_2: return "Type 2 Tag";
      case W_PROP_NFC_TAG_TYPE_3: return "Type 3 Tag";
      case W_PROP_NFC_TAG_TYPE_4_A: return "Type 4-A Tag";
      case W_PROP_NFC_TAG_TYPE_4_B: return "Type 4-B Tag";
      case W_PROP_NFC_TAG_TYPE_5: return "Type 5 Tag";
      case W_PROP_NFC_TAG_TYPE_6: return "Type 6 Tag";
      case W_PROP_NFC_TAG_TYPE_7: return "Type 7 Tag";
      case W_PROP_TYPE1_CHIP: return "Type 1 Chip";
      case W_PROP_JEWEL: return "Jewel";
      case W_PROP_TOPAZ: return "Topaz";
      case W_PROP_TOPAZ_512: return "Topaz 512";
      case W_PROP_FELICA: return "FeliCa";
      case W_PROP_PICOPASS_2K: return "Picopass 2K";
      case W_PROP_PICOPASS_32K: return "Picopass 32K";
      case W_PROP_ICLASS_2K: return "I-Class 2K";
      case W_PROP_ICLASS_16K: return "I-Class 16K";
      case W_PROP_MIFARE_UL: return "MIFARE UL";
      case W_PROP_MIFARE_UL_C: return "MIFARE UL C";
      case W_PROP_MIFARE_MINI: return "MIFARE Mini";
      case W_PROP_MIFARE_1K: return "MIFARE 1K";
      case W_PROP_MIFARE_4K: return "MIFARE 4K";
      case W_PROP_MIFARE_DESFIRE_D40: return "MIFARE DESFire D40";
      case W_PROP_MIFARE_DESFIRE_EV1_2K: return "MIFARE DESFire EV1 2K";
      case W_PROP_MIFARE_DESFIRE_EV1_4K: return "MIFARE DESFire EV1 4K";
      case W_PROP_MIFARE_DESFIRE_EV1_8K: return "MIFARE DESFire EV1 8K";
      case W_PROP_MIFARE_PLUS_X_2K: return "MIFARE Plus X 2K";
      case W_PROP_MIFARE_PLUS_X_4K: return "MIFARE Plus X 4K";
      case W_PROP_MIFARE_PLUS_S_2K: return "MIFARE Plus S 2K";
      case W_PROP_MIFARE_PLUS_S_4K: return "MIFARE Plus S 4K";
      case W_PROP_TI_TAGIT: return "Texas Instrument TagIT";
      case W_PROP_ST_LRI_512: return "ST LRI 512";
      case W_PROP_ST_LRI_2K: return "ST LRI 2K";
      case W_PROP_NXP_ICODE: return "NXP ICode ";
      case W_PROP_MY_D_MOVE: return "My-D Move";
      case W_PROP_MY_D_NFC: return "My-D NFC";
      case W_PROP_KOVIO_RFID: return "Kovio RFID";
      case W_PROP_KOVIO: return "Kovio";
      case W_PROP_SECURE_ELEMENT: return "Secure Element";
      case W_PROP_VIRTUAL_TAG: return "Virtual Tag";
      case W_PROP_SNEP: return "SNEP";
      case W_PROP_NPP: return "NPP";
      default: return "Unknown Property";
   }
}

#define P_CODE_2_STRING(X) \
         case X: \
            return #X;

/* See header file */
const char* PUtilTraceError(
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
      P_CODE_2_STRING(W_ERROR_SECURITY)
      P_CODE_2_STRING(W_ERROR_PROGRAMMING)

      default:
         return "*** Unknown Error ***";
   }
}

#ifdef P_TRACE_ACTIVE

/* See header file */
uint32_t PUtilLogUint8(
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
uint32_t PUtilLogUint16(
                  char* pTraceBuffer,
                  uint32_t nValue)
{
    uint32_t nPos = 0;
    nPos +=PUtilLogUint8(&pTraceBuffer[nPos], ((nValue & 0x0000FF00) >> 8));
    nPos +=PUtilLogUint8(&pTraceBuffer[nPos],  (nValue & 0x000000FF));
    return nPos;
}

/* See header file */
uint32_t PUtilLogUint32(
                  char* pTraceBuffer,
                  uint32_t nValue)
{
    uint32_t nPos = 0;
    nPos +=PUtilLogUint8(&pTraceBuffer[nPos], ((nValue & 0xFF000000) >> 24));
    nPos +=PUtilLogUint8(&pTraceBuffer[nPos], ((nValue & 0x00FF0000) >> 16));
    nPos +=PUtilLogUint8(&pTraceBuffer[nPos], ((nValue & 0x0000FF00) >> 8));
    nPos +=PUtilLogUint8(&pTraceBuffer[nPos],  (nValue & 0x000000FF));
    return nPos;
}

/* See header file */
uint32_t PUtilLogArray(
         char* pTraceBuffer,
         uint8_t* pBuffer,
         uint32_t nLength)
{
   uint32_t nPos = 0;
   uint32_t i;

   if(nLength != 0)
   {
      PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "(len=0x00)");

      (void)PUtilLogUint8(&pTraceBuffer[7], nLength);

      for(i = 0; i < nLength; i++)
      {
         pTraceBuffer[nPos++] = ' ';
         nPos += PUtilLogUint8(&pTraceBuffer[nPos], pBuffer[i]);
      }
   }

   pTraceBuffer[nPos] = 0;

   return nPos;
}

/* See header file */
void PUtilTraceASCIIStringCopy(
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

/* See header file */
const char* PUtilTraceBool(
         bool_t bValue)
{
   return bValue?"W_TRUE":"W_FALSE";
}

/* See header file */
const char* PUtilTracePriority(
         W_ERROR nPriority)
{
   switch(nPriority)
   {
         P_CODE_2_STRING(W_PRIORITY_NO_ACCESS)
         P_CODE_2_STRING(W_PRIORITY_MINIMUM)
         P_CODE_2_STRING(W_PRIORITY_MINIMUM + 1)
         P_CODE_2_STRING(W_PRIORITY_MINIMUM + 2)
         P_CODE_2_STRING(W_PRIORITY_MINIMUM + 3)
         P_CODE_2_STRING(W_PRIORITY_MINIMUM + 4)
         P_CODE_2_STRING(W_PRIORITY_MINIMUM + 5)
         P_CODE_2_STRING(W_PRIORITY_MINIMUM + 6)
         P_CODE_2_STRING(W_PRIORITY_MINIMUM + 7)
         P_CODE_2_STRING(W_PRIORITY_MINIMUM + 8)
         P_CODE_2_STRING(W_PRIORITY_MAXIMUM)
         P_CODE_2_STRING(W_PRIORITY_EXCLUSIVE)
         P_CODE_2_STRING(W_PRIORITY_SE)
         P_CODE_2_STRING(W_PRIORITY_SE_FORCED)
      default:
         return "*** Unknown Priority ***";
   }
}

/* See header file */
const char* PUtilTraceCardProtocol(
         tContext* pContext,
         uint32_t nProtocol)
{
   uint8_t nIndex;
   uint32_t nPos = 0;
   char* pTraceBuffer = PContextGetCardProtocolTraceBuffer(pContext);

   static const uint32_t aProtocolFlag[] = {
      W_NFCC_PROTOCOL_CARD_ISO_14443_4_A,
      W_NFCC_PROTOCOL_CARD_ISO_14443_4_B,
      W_NFCC_PROTOCOL_CARD_ISO_14443_3_A,
      W_NFCC_PROTOCOL_CARD_ISO_14443_3_B,
      W_NFCC_PROTOCOL_CARD_ISO_15693_3,
      W_NFCC_PROTOCOL_CARD_ISO_15693_2,
      W_NFCC_PROTOCOL_CARD_FELICA,
      W_NFCC_PROTOCOL_CARD_P2P_TARGET,
      W_NFCC_PROTOCOL_CARD_TYPE_1_CHIP,
      W_NFCC_PROTOCOL_CARD_MIFARE_CLASSIC,
      W_NFCC_PROTOCOL_CARD_BPRIME};

   static const char* aProtocolName[] = {
      "14-4A ", "14-4B ", "14-3A ", "14-3B ", "15-3 ", "15-2 ",
      "FELICA ", "P2P ", "T1 ", "MIFCLA ", "BP "};

   for(nIndex = 0; nIndex < (sizeof(aProtocolFlag)/sizeof(uint32_t)); nIndex++)
   {
      if((nProtocol & aProtocolFlag[nIndex]) != 0)
      {
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, aProtocolName[nIndex]);
      }
   }

   if(nPos != 0)
   {
      nPos--;
   }
   else
   {
      PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "none");
   }

   pTraceBuffer[nPos] = 0;

   return pTraceBuffer;
}

/* See header file */
const char* PUtilTraceReaderProtocol(
         tContext* pContext,
         uint32_t nProtocol)
{
   uint8_t nIndex;
   uint32_t nPos = 0;
   char* pTraceBuffer = PContextGetReaderProtocolTraceBuffer(pContext);

   static const uint32_t aProtocolFlag[] = {
      W_NFCC_PROTOCOL_READER_ISO_14443_4_A,
      W_NFCC_PROTOCOL_READER_ISO_14443_4_B,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_B,
      W_NFCC_PROTOCOL_READER_ISO_15693_3,
      W_NFCC_PROTOCOL_READER_ISO_15693_2,
      W_NFCC_PROTOCOL_READER_FELICA,
      W_NFCC_PROTOCOL_READER_P2P_INITIATOR,
      W_NFCC_PROTOCOL_READER_TYPE_1_CHIP,
      W_NFCC_PROTOCOL_READER_MIFARE_CLASSIC,
      W_NFCC_PROTOCOL_READER_BPRIME,
      W_NFCC_PROTOCOL_READER_KOVIO,
      W_NFCC_PROTOCOL_READER_MIFARE_PLUS};

   static const char* aProtocolName[] = {
      "14-4A ", "14-4B ", "14-3A ", "14-3B ", "15-3 ", "15-2 ",
      "FELICA ", "P2P ", "T1 ", "MIFCLA ", "BP ", "KOVIO ", "MIFPLS "};

   for(nIndex = 0; nIndex < (sizeof(aProtocolFlag)/sizeof(uint32_t)); nIndex++)
   {
      if((nProtocol & aProtocolFlag[nIndex]) != 0)
      {
         PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, aProtocolName[nIndex]);
      }
   }

   if(nPos != 0)
   {
      nPos--;
   }
   else
   {
      PUtilTraceASCIIStringCopy(pTraceBuffer, &nPos, "none");
   }

   pTraceBuffer[nPos] = 0;

   return pTraceBuffer;
}

#undef P_CODE_2_STRING

#else /* P_TRACE_ACTIVE */

   /* @note: this is needeed by Microsoft Visual C++ 2010 Express ; even if functions are not used (always called inside PDebugTrace() */

/* See header file */
const char* PUtilTraceBool(
         bool_t bValue)
{
   return null;
}

/* See header file */
const char* PUtilTracePriority(
         W_ERROR nPriority)
{
   return null;
}

/* See header file */
const char* PUtilTraceCardProtocol(
         tContext* pContext,
         uint32_t nProtocol)
{
   return null;
}

/* See header file */
const char* PUtilTraceReaderProtocol(
         tContext* pContext,
         uint32_t nProtocol)
{
   return null;
}

#endif /* #ifdef P_TRACE_ACTIVE */

