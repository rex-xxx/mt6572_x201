/*
 * Copyright (c) 2009-2011 Inside Secure, All Rights Reserved.
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

#include "ccclient.h"
#include "ccclient_md.h"
#include <android/log.h>


static const char TAG[] = "ccclienr";

#define LogDebug(format, ...)              __android_log_print(ANDROID_LOG_DEBUG,  TAG, format, ##__VA_ARGS__)
#define LogWarning(format, ...)                        __android_log_print(ANDROID_LOG_WARN,    TAG, format, ##__VA_ARGS__)
#define LogInformation(format, ...)       __android_log_print(ANDROID_LOG_INFO,      TAG, format, ##__VA_ARGS__)
#define LogError(format, ...)                 __android_log_print(ANDROID_LOG_ERROR,  TAG, format, ##__VA_ARGS__)
/* -------------------------------------------------------------------------
   -------------------------------------------------------------------------
         UTILITY FUNCTIONS
   -------------------------------------------------------------------------
   ------------------------------------------------------------------------- */


static bool_t static_CCClientStringCompare(
            const char16_t* pString1,
            const char16_t* pString2)
{
   char16_t nChar;

   do
   {
      if((nChar = *pString1++) != *pString2++)
      {
         return W_FALSE;
      }
   } while (nChar != 0);

   return W_TRUE;
}

static char16_t static_CCClientGetHexChar(uint8_t nValue)
{
   if(nValue > 0x0F)
   {
      CCPrintError("static_CCClientGetHexChar: Wrong hexadecimal value");
      return 0;
   }

   return (nValue < 0x0A)?(nValue + '0'):((nValue - 0x0A) + 'A');
}

static uint8_t static_CCClientGetHexValue(char16_t nChar)
{
   if((nChar >= '0') && (nChar <= '9'))
   {
      return (uint8_t)(nChar - '0');
   }
   if((nChar >= 'a') && (nChar <= 'f'))
   {
      return (uint8_t)(nChar + 10 - 'a');
   }
   if((nChar >= 'A') && (nChar <= 'F'))
   {
      return (uint8_t)(nChar + 10 - 'A');
   }

   CCPrintError("static_CCClientGetHexValue: Wrong hexadecimal character");
   return 0xFF;
}

/**
 * Returns the value of the 32 bit integer encoded in a buffer.
 *
 * @param[in]  pBuffer The buffer where is encoded the integer.
 *
 * @param[in]  nOffset  The offset of the integer.
 *
 * @param[out] pnValue  A pointer on a variable valued with the integer value.
 *
 * @return  The offset following the integer value in the buffer.
 **/
static uint32_t static_CCClientParseIntegeter(
                  const uint8_t* pBuffer,
                  uint32_t nOffset,
                  uint32_t* pnValue)
{
   *pnValue = (pBuffer[nOffset + 3] << 24) | (pBuffer[nOffset + 2] << 16) | (pBuffer[nOffset + 1] << 8) | pBuffer[nOffset];

   return nOffset + 4;
}

/**
 * Writes a 32 bit integer in a buffer.
 *
 * @param[out] pBuffer  The buffer where to encode the value.
 *
 * @param[in]  nOffset  The offset of the integer.
 *
 * @param[in]  nValue  The value to encode.
 *
 * @return  The offset following the integer value in the buffer.
 **/
static uint32_t static_CCClientWriteInteger(
                  uint8_t* pBuffer,
                  uint32_t nOffset,
                  uint32_t nValue)
{
   pBuffer[nOffset] = (uint8_t)(nValue & 0xFF);
   pBuffer[nOffset + 1] = (uint8_t)((nValue >> 8) & 0xFF);
   pBuffer[nOffset + 2] = (uint8_t)((nValue >> 16) & 0xFF);
   pBuffer[nOffset + 3] = (uint8_t)((nValue >> 24) & 0xFF);

   return nOffset + 4;
}

/**
 * Returns the size of a zero-ended Unicode string once encoded in Utf8.
 * The size does not include the terminating zero.
 *
 * @param[in]  pUnicodeString The Unicode string.
 *
 * @return  The size of a string encoded in Utf8.
 **/
static uint32_t static_CCClientGetUtf8Length(
               const char16_t* pUnicodeString)
{
   char16_t c;
   uint32_t nPos = 0;

   while((c = *pUnicodeString++) != 0)
   {
      if(c < 0x0080)
      {
         nPos++;
      }
      else if(c < 0x0800)
      {
         nPos +=2;
      }
      else
      {
         nPos +=3;
      }
   }

   return nPos;
}

/**
 * Returns the value of a string encoded in Utf8 in a buffer.
 *
 * @param[in]  pBuffer The buffer where is encoded the string.
 *
 * @param[in]  nOffset  The offset of the string in the buffer.
 *
 * @param[out] pStringBuffer  A pointer on the buffer receiving the string value.
 *             a zero is added at the end of the string.
 *
 * @return  The offset of the byte following the ending zero in the buffer.
 **/
static uint32_t static_CCClientParseString(
                  const uint8_t* pBuffer,
                  uint32_t nOffset,
                  char16_t* pStringBuffer)
{
   char16_t c;
   uint32_t nIndex = 0;
   char16_t nValue;

   for (;;)
   {
      c = pBuffer[nOffset++];

      if(c == 0x00)
      {
         pStringBuffer[nIndex] = 0x0000;
         return nOffset;
      }

      if ( (c & 0x80) == 0x00 )
      {
         /* [0xxx-xxxx] */
         nValue = c;      /* 0xxx-xxxx */
      }
      else if ( (c & 0xE0) == 0xE0 )
      {
         nValue =  ((((char16_t)c) & 0x0f ) << 12);/* ....-xxxx */
         c = pBuffer[nOffset++];
         nValue |= ((((char16_t)c) & 0x3f) << 6);   /* ..xx-xxxx */
         c = pBuffer[nOffset++];
         nValue |= (((char16_t)c) & 0x3f);          /* ..xx-xxxx */
      }
      else
      {
         nValue =  (((char16_t)c) & 0x1f) << 6;/* ...x-xxxx */
         c = pBuffer[nOffset++];
         nValue |= (((char16_t)c) & 0x3f);       /* ..xx-xxxx */
      }

      pStringBuffer[ nIndex++ ] = nValue;
   }
}

/**
 * Returns the length in characters of a zero-ended Utf8 string.
 *
 * @param[in]  pBuffer  The buffer containing the string.
 *
 * @param[in]  nOffset  the offset where is located the string.
 *
 * @param[in]  nBufferLength  the buffer length in bytes.
 *
 * @return  The length in characters of the string, including a terminating zero.
 *          0 if an error is detected.
 **/
static uint32_t static_CCClientGetUnicodeLength(
                  const uint8_t* pBuffer,
                  uint32_t nOffset,
                  uint32_t nBufferLength)
{
   uint8_t c;
   uint32_t nStringLength = 0;

   for(;;)
   {
      c = pBuffer[nOffset++];

      nStringLength++;

      if(c == 0x00)
      {
         break;
      }

      if((c & 0x80) != 0x00)
      {
         if((c & 0xE0) == 0xC0)
         {
            nOffset++;
         }
         else
         {
            nOffset+=2;
         }
      }

      if(nOffset >= nBufferLength)
      {
         nStringLength = 0;
         break;
      }
   }

   return nStringLength;
}

/**
 * Writes a string in a Utf8 buffer.
 *
 * @param[out] pBuffer  The buffer where to encode the string in Utf8.
 *
 * @param[in]  nOffset  The offset in the buffer where to write the string.
 *
 * @param[in]  pString  The zero ended unicode string to encode in the buffer.
 *
 * @return  The offset following the string in the buffer.
 **/
static uint32_t static_CCClientWriteString(
                   uint8_t* pBuffer,
                   uint32_t nOffset,
                   const char16_t* pString)
{
   char16_t v;
   int32_t i = 0;

   while ((v = pString[i++]) != 0)
   {
      if ( v < 128 )
      {
         /* [0xxx-xxxx] */
         pBuffer[nOffset++] = (uint8_t)v;          /* 00000000-0xxxxxx */
      }
      else if ( v < 2048 )
      {
         /* [110x-xxxx][10xx-xxxx] */
         pBuffer[nOffset++] = (uint8_t)(0xC0 | (v>>6));       /* 00000xxx-xx...... */
         pBuffer[nOffset++] = (uint8_t)(0x80 | (v&0x003F));   /* 00000...-..xxxxxx */
      }
      else
      {
         /* [1110-xxxx][10xx-xxxx][10xx-xxxx] */
         pBuffer[nOffset++] = (uint8_t)(0xE0 | ((v&0xF000)>>12)); /* xxxx....-........ */
         pBuffer[nOffset++] = (uint8_t)(0x80 | ((v&0x0FC0)>>6));  /* ....xxxx-xx...... */
         pBuffer[nOffset++] = (uint8_t)(0x80 | (v&0x003F));       /* ........-..xxxxxx */
      }
   }

   return nOffset;
}

/* -------------------------------------------------------------------------
   -------------------------------------------------------------------------
         CLIENT IMPLEMENTATION
   -------------------------------------------------------------------------
   ------------------------------------------------------------------------- */

#define MSG_ROUTER_HELLO               'A'
#define MSG_PROVIDER_CONNECT           'B'
#define MSG_CLIENT_CONNECT             'C'
#define MSG_CLIENT_CONNECT_WAIT        'D'
#define MSG_CONNECT_OK                 'E'
#define MSG_CONNECT_ERR_NO_PROVIDER    'F'
#define MSG_CONNECT_ERR_PROVIDER_BUSY  'G'
#define MSG_DISCONNECT                 'H'
#define MSG_SPECIFIC_MESSAGE           'K'
#define MSG_ADMIN                      'M'
#define MSG_CONNECT_ERR_VERSION        'N'

#define CCCLIENT_SEND_BUFFER_LENGTH  4096
#define CCCLIENT_RECV_BUFFER_LENGTH  4096

static const char* g_pAddress = null;

typedef struct __tCCClientInstance
{
   CCSocket socket;
   uint8_t aSendBuffer[CCCLIENT_SEND_BUFFER_LENGTH];
   uint8_t aRecvBuffer[CCCLIENT_RECV_BUFFER_LENGTH];
   CCCriticalSection sSendCS;

   const char16_t* pnCCVersion;
} tCCClientInstance;

static int32_t static_CCClientReceiveMessage(
            tCCClientInstance* pInstance,
            uint8_t* pBuffer,
            uint32_t nBufferLength,
            uint8_t* pnType,
            char16_t*** pppParameterList,
            uint32_t* pnParameterNumber,
            const uint8_t** pPayloadBuffer,
            uint32_t* pnPayloadLength,
            bool_t bWait);

static uint32_t static_CCClientSendMessage(
            tCCClientInstance* pInstance,
            uint8_t nType,
            const char16_t** ppParameterList,
            uint32_t nParameterNumber,
            const uint8_t* pPayload1Buffer,
            uint32_t nPayload1Length,
            const uint8_t* pPayload2Buffer,
            uint32_t nPayload2Length);

void CCClientSetAddress(
            const char* pAddress)
{
   g_pAddress = pAddress;
}

static void static_CCClientCreateVersionList(
            char16_t* pVersionList,
            uint32_t nMaxLength,
            const uint8_t* pVersionSupported,
            uint32_t nVersionNumber)
{
   uint32_t nPos = 0;
   uint32_t nIndex = 0;

   while(nIndex < nVersionNumber)
   {
      uint8_t nValue = pVersionSupported[nIndex++];
      if((nIndex * 3) <= nMaxLength)
      {
         pVersionList[nPos++] = static_CCClientGetHexChar((nValue >> 4) & 0x0F);
         pVersionList[nPos++] = static_CCClientGetHexChar(nValue & 0x0F);
         pVersionList[nPos++] = ',';
      }
      else
      {
         CCPrintError("static_CCClientCreateVersionList: Wrong version length");
      }
   }

   if(nPos > 0) nPos--;

   pVersionList[nPos] = 0;
}

static bool_t static_CCParseVersionList(
            const char16_t* pString,
            uint8_t* pVersionList,
            uint32_t nMaxVersionNumber,
            uint32_t* pnVersionNumber)
{
   uint32_t nVersionNumber = 0;

   for(;;)
   {
      uint8_t nHighVersion, nLowVersion, nVersion;
      uint32_t nStringPos = nVersionNumber*3;

      if(nVersionNumber >= nMaxVersionNumber)
      {
         CCPrintError("static_CCClientCreateInstance: Too many versions in the string");
         return W_FALSE;
      }

      if((pString[nStringPos] == 0) || (pString[nStringPos + 1] == 0))
      {
         CCPrintError("static_CCClientCreateInstance: Error in the string format");
         return W_FALSE;
      }

      nHighVersion = static_CCClientGetHexValue(pString[nStringPos]);
      if(nHighVersion > 0x0F)
      {
         CCPrintError("static_CCClientCreateInstance: Error in the string format");
         return W_FALSE;
      }
      nLowVersion = static_CCClientGetHexValue(pString[nStringPos + 1]);
      if(nLowVersion > 0x0F)
      {
         CCPrintError("static_CCClientCreateInstance: Error in the string format");
         return W_FALSE;
      }

      nVersion = (nHighVersion << 4) + nLowVersion;

      if(nVersion == CC_UNKNOWN_VERSION)
      {
         CCPrintError("static_CCClientCreateInstance: CC_UNKNOWN_VERSION is not allowed");
         return W_FALSE;
      }

      pVersionList[nVersionNumber++] = nVersion;

      if(pString[nStringPos + 2] == 0)
      {
         break;
      }
      else if(pString[nStringPos + 2] != ',')
      {
         CCPrintError("static_CCClientCreateInstance: Error in the version separator");
         return W_FALSE;
      }
   }

   *pnVersionNumber = nVersionNumber;
   return W_TRUE;
}

static uint8_t static_CCGetNegociatedVersion(
            const uint8_t* pVersionList1,
            uint32_t nVersionNumber1,
            const uint8_t* pVersionList2,
            uint32_t nVersionNumber2)
{
   uint32_t nPos1, nPos2;
   uint8_t nNegociatedVersion = CC_UNKNOWN_VERSION;

   for(nPos1 = 0; nPos1 < nVersionNumber1; nPos1++)
   {
      for(nPos2 = 0; nPos2 < nVersionNumber2; nPos2++)
      {
         if(pVersionList1[nPos1] == pVersionList2[nPos2])
         {
            if(pVersionList1[nPos1] > nNegociatedVersion)
            {
               nNegociatedVersion = pVersionList1[nPos1];
            }
            break;
         }
      }
   }

   return nNegociatedVersion;
}

static const char16_t g_sVersion0_03[] = { '0', '.', '0', '3', 0 };
static const char16_t g_sVersion1_0[] = { '1', '.', '0', 0 };

static uint32_t static_CCClientCreateInstance(
            uint8_t nType,
            const char16_t* pServiceType,
            const char16_t* pProviderURI,
            const char16_t* pServiceName,
            const uint8_t* pVersionSupported,
            uint32_t nVersionNumber,
            uint8_t* pnNegociatedVersion,
            void** ppConnection)
{
   tCCClientInstance* pInstance;
   uint8_t nNegociatedVersion = CC_UNKNOWN_VERSION;
   int32_t res;
   char16_t** ppParameterList;
   uint8_t nReceivedType;
   uint32_t nParameterNumber;
   const uint8_t* pPayloadBuffer;
   uint32_t nPayloadLength;
   const char16_t* apParameterList[20];
   char16_t aApplicationaName[256];
   char16_t aApplicationIdentifier[32];
   char16_t aVersionList[CC_MAXIMUM_VERSION*3];
   const char* pAddress = g_pAddress;
   uint32_t nError = CC_SUCCESS;

   pInstance = (tCCClientInstance*)CCMalloc(sizeof(tCCClientInstance));

   if(pInstance == null)
   {
      CCPrintError("static_CCClientCreateInstance: Memory allocation error");
      nError = CC_ERROR_MEMORY;
      goto return_function;
   }

   CCCreateCriticalSection(&pInstance->sSendCS);

   if(pAddress == null)
   {
      pAddress = "127.0.0.1";
   }
   LogInformation("Sok: Addr0:[%s]", pAddress);
   nError = CCSocketCreate(pAddress, &pInstance->socket);
   if(nError != CC_SUCCESS)
   {
      CCPrintError("static_CCClientCreateInstance: Error returned by CCSocketCreate()");
      goto return_function;
   }

   if((res = static_CCClientReceiveMessage(
            pInstance,
            pInstance->aRecvBuffer,
            CCCLIENT_RECV_BUFFER_LENGTH,
            &nReceivedType,
            &ppParameterList,
            &nParameterNumber,
            &pPayloadBuffer,
            &nPayloadLength, W_TRUE)) < 0)
   {
      CCPrintError("static_CCClientCreateInstance: Error returned by first static_CCClientReceiveMessage()");
      nError = CC_ERROR_COMMUNICATION;
      goto return_function;
   }

   if((nReceivedType != MSG_ROUTER_HELLO) || (nParameterNumber < 1))
   {
      CCPrintError("static_CCClientCreateInstance: First message received is not HELLO");
      nError = CC_ERROR_PROTOCOL;
      goto return_function;
   }

   if(static_CCClientStringCompare(ppParameterList[0], g_sVersion0_03) != W_FALSE)
   {
      pInstance->pnCCVersion = g_sVersion0_03;
   }
   else if(static_CCClientStringCompare(ppParameterList[0], g_sVersion1_0) != W_FALSE)
   {
      pInstance->pnCCVersion = g_sVersion1_0;
   }
   else
   {
      CCPrintError("static_CCClientCreateInstance: CC version not supported");
      nError = CC_ERROR_CC_VERSION;
      goto return_function;
   };

   CCGetApplicationName(aApplicationaName);

   CCGetApplicationIdentifier(aApplicationIdentifier);

   static_CCClientCreateVersionList(aVersionList, sizeof(aVersionList)/sizeof(char16_t), pVersionSupported, nVersionNumber);

   nParameterNumber = 0;
   apParameterList[nParameterNumber++] = null; /* UUID */

   apParameterList[nParameterNumber++] = null; /* host name */
   apParameterList[nParameterNumber++] = null; /* host address */

   if(pInstance->pnCCVersion == g_sVersion0_03)
   {
      apParameterList[nParameterNumber++] = null; /* host image */
   }

   apParameterList[nParameterNumber++] = aApplicationaName; /* application name */
   apParameterList[nParameterNumber++] = aApplicationIdentifier; /* application identifier */
   if(pInstance->pnCCVersion == g_sVersion0_03)
   {
      apParameterList[nParameterNumber++] = null; /* application image */
   }

   apParameterList[nParameterNumber++] = pServiceName; /* Service name */
   if(pInstance->pnCCVersion == g_sVersion0_03)
   {
      apParameterList[nParameterNumber++] = null;  /* Service Image */
   }
   apParameterList[nParameterNumber++] = pServiceType; /* Service Type */

   if(pInstance->pnCCVersion == g_sVersion1_0)
   {
      apParameterList[nParameterNumber++] = aVersionList; /* Version List */
   }

   apParameterList[nParameterNumber++] = pProviderURI; /* Connection string */

   if(static_CCClientSendMessage(
      pInstance,
      nType,
      apParameterList,
      nParameterNumber,
      (uint8_t*)null, 0, (uint8_t*)null, 0) == 0)
   {
      CCPrintError("static_CCClientCreateInstance: Error returned by static_CCClientSendMessage()");
      nError = CC_ERROR_COMMUNICATION;
      goto return_function;
   };

   if((res = static_CCClientReceiveMessage(
            pInstance,
            pInstance->aRecvBuffer,
            CCCLIENT_RECV_BUFFER_LENGTH,
            &nReceivedType,
            &ppParameterList,
            &nParameterNumber,
            &pPayloadBuffer,
            &nPayloadLength, W_TRUE)) < 0)
   {
      CCPrintError("static_CCClientCreateInstance: Error returned by second static_CCClientReceiveMessage()");
      nError = CC_ERROR_COMMUNICATION;
      goto return_function;
   }

   if(nReceivedType == MSG_CONNECT_ERR_NO_PROVIDER)
   {
      CCPrintError("static_CCClientCreateInstance: Error MSG_CONNECT_ERR_NO_PROVIDER");
      nError = CC_ERROR_NO_PROVIDER;
      goto return_function;
   }
   else if(nReceivedType == MSG_CONNECT_ERR_PROVIDER_BUSY)
   {
      CCPrintError("static_CCClientCreateInstance: Error MSG_CONNECT_ERR_PROVIDER_BUSY");
      nError = CC_ERROR_PROVIDER_BUSY;
      goto return_function;
   }
   else if(nReceivedType == MSG_CONNECT_ERR_VERSION)
   {
      CCPrintError("static_CCClientCreateInstance: Error MSG_CONNECT_ERR_VERSION");
      nError = CC_ERROR_SERVICE_VERSION;
      goto return_function;
   }
   else if(nReceivedType != MSG_CONNECT_OK)
   {
      CCPrintError("static_CCClientCreateInstance: Wrong message type, protocol error");
      nError = CC_ERROR_PROTOCOL;
      goto return_function;
   }

   if(pInstance->pnCCVersion == g_sVersion1_0)
   {
       uint8_t aRemoteVersionList[CC_MAXIMUM_VERSION];
       uint32_t nRemoteVersionNumber;

      if(nParameterNumber != 8)
      {
         CCPrintError("static_CCClientCreateInstance: Wrong parameter number for version 1.0, protocol error");
         nError = CC_ERROR_PROTOCOL;
         goto return_function;
      }

      if(static_CCParseVersionList(ppParameterList[7], aRemoteVersionList, CC_MAXIMUM_VERSION, &nRemoteVersionNumber) == W_FALSE)
      {
         CCPrintError("static_CCClientCreateInstance: Error returned by static_CCParseVersionList()");
         nError = CC_ERROR_PROTOCOL;
         goto return_function;
      }

      nNegociatedVersion = static_CCGetNegociatedVersion(aRemoteVersionList, nRemoteVersionNumber, pVersionSupported, nVersionNumber);

      if(nNegociatedVersion == CC_UNKNOWN_VERSION)
      {
         CCPrintError("static_CCClientCreateInstance: Error returned by static_CCGetNegociatedVersion()");
         nError = CC_ERROR_PROTOCOL;
         goto return_function;
      }
   }
   else if(pInstance->pnCCVersion == g_sVersion0_03)
   {

      if(nParameterNumber != 10)
      {
         CCPrintError("static_CCClientCreateInstance: Wrong parameter number for version 0.03, protocol error");
         nError = CC_ERROR_PROTOCOL;
         goto return_function;
      }
   }

return_function:

   if(nError != CC_SUCCESS)
   {
      nNegociatedVersion = CC_UNKNOWN_VERSION;

      if(pInstance != null)
      {
         CCSocketShutdownClose(&pInstance->socket);

         CCDestroyCriticalSection(&pInstance->sSendCS);

         CCFree(pInstance);

         pInstance = null;
      }
   }

   if(pnNegociatedVersion != null)
   {
      *pnNegociatedVersion = nNegociatedVersion;
   }
   if(ppConnection != null)
   {
      *ppConnection = pInstance;
   }

   return nError;
}

/**
 * Builds a message.
 *
 * @param[out]  pBuffer the buffer receiving the message content.
 *
 * @param[in]   nBufferLength  The maximum length in byte of the message data stored in \a pBuffer.
 *
 * @param[in]   nType  The type of the message.
 *
 * @param[in]   ppParameterList  The array of message parameters.
 *
 * @param[in]   nParameterNumber  The number of message parameters.
 *
 * @param[in]   nPayloadLength  The length in bytes of the binary payload or zero if no payload is provided.
 *
 * @return  The length in bytes of the message or zero if the buffer is too short.
 **/
static uint32_t static_CCClientBuildMessage(
            uint8_t* pBuffer,
            uint32_t nBufferLength,
            uint8_t nType,
            const char16_t** ppParameterList,
            uint32_t nParameterNumber,
            uint32_t nPayloadLength)
{
   uint32_t nLength;
   uint32_t nIndex, nOffset;

   nLength = 1;
   for(nIndex = 0; nIndex < nParameterNumber ;nIndex++)
   {
       if (ppParameterList[nIndex] != null)
       {
          nLength += static_CCClientGetUtf8Length(ppParameterList[nIndex]);
       }
       nLength++;
   }

   if(nPayloadLength != 0)
   {
      nLength += 1 + nPayloadLength;
   }

   if((nLength + 4) > nBufferLength)
   {
      CCPrintError("static_CCClientBuildMessage: Buffer too short");
      return 0;
   }

   nOffset = 0;

   nOffset = static_CCClientWriteInteger(pBuffer, nOffset, nLength);
   pBuffer[nOffset++] = nType;

   for(nIndex = 0; nIndex < nParameterNumber ;nIndex++)
   {
       if (ppParameterList[nIndex] != null)
       {
          nOffset = static_CCClientWriteString(pBuffer, nOffset, ppParameterList[nIndex]);
       }
       pBuffer[nOffset++] = 0x00;
   }

   if(nPayloadLength != 0)
   {
      pBuffer[nOffset++] = 0x01;
   }

   return nLength + 4 - nPayloadLength;
}

/**
 * Parses the content of a message buffer.
 *
 * @param[in]  pBuffer  The message buffer.
 *
 * @param[in]  nBufferLength  The length in bytes of the message buffer.
 *
 * @param[in]  nMessageLength  The message length in bytes.
 *
 * @param[out]  pnType  A pointer on a variable valued with the type of the message.
 *
 * @param[out]  pppParameterList  A pointer on an array receiving the pointers on the string parameters.
 *
 * @param[out]  pnParameterNumber  Set with the maximum number of parameter in the`\a ppParameterList array.
 *              Receive the actual number of parameters.
 *
 * @param[out]  pPayloadBuffer  A pointer on a variable valued with the address of the binary payload buffer, if any.
 *
 * @param[out]  pnPayloadLength  A pointer on a variable receiving the length in bytes of the binary payload buffer.
 *              This value is set to zero if no payload is present or if an error occurs.
 *
 * @return  The actual length in bytes of the message, or zero if an error occurs.
 **/
static uint32_t static_CCClientParseMessage(
            const uint8_t* pBuffer,
            uint32_t nBufferLength,
            uint32_t nMessageLength,
            uint8_t* pnType,
            char16_t*** pppParameterList,
            uint32_t* pnParameterNumber,
            const uint8_t** pPayloadBuffer,
            uint32_t* pnPayloadLength)
{
   uint32_t nOffset = 0;
   uint32_t nLowBoundary;
   uint32_t nParameterNumber = 0;

   if(nMessageLength == 0)
   {
      CCPrintError("static_CCClientParseMessage: Empty message");
      return 0;
   }
   *pnType = pBuffer[nOffset++];

   nLowBoundary = nMessageLength;
   *pppParameterList = (char16_t**)&pBuffer[nLowBoundary];

   while(nOffset < nMessageLength)
   {
      if(pBuffer[nOffset] == 0x01)
      {
         nOffset++;
         if(nOffset > nMessageLength)
         {
            CCPrintError("static_CCClientParseMessage: Message truncated");
            return 0;
         }

         *pnPayloadLength = nMessageLength - nOffset;

         *pPayloadBuffer = &pBuffer[nOffset];
         break;
      }
      else
      {
         uint32_t nUnicodeLength = static_CCClientGetUnicodeLength( pBuffer, nOffset, nBufferLength);
         char16_t* pString;
         if(nUnicodeLength == 0)
         {
            CCPrintError("static_CCClientParseMessage: Error in Utf8 encoding");
            return 0;
         }
         if(nLowBoundary + sizeof(char16_t*) + (nUnicodeLength * sizeof(char16_t)) > nBufferLength)
         {
            return 0;
         }
         nBufferLength -= (nUnicodeLength * sizeof(char16_t));
         nLowBoundary += sizeof(char16_t*);
         if(nUnicodeLength != 1)
         {
            pString = (char16_t*)&pBuffer[nBufferLength];
            nOffset = static_CCClientParseString( pBuffer, nOffset, pString);
         }
         else
         {
            pString = null;
            nOffset++;
         }
         (*pppParameterList)[nParameterNumber++] = pString;
      }
   }

   *pnParameterNumber = nParameterNumber;

   return nMessageLength;
}

static int32_t static_CCClientReceiveMessage(
            tCCClientInstance* pInstance,
            uint8_t* pBuffer,
            uint32_t nBufferLength,
            uint8_t* pnType,
            char16_t*** pppParameterList,
            uint32_t* pnParameterNumber,
            const uint8_t** pPayloadBuffer,
            uint32_t* pnPayloadLength,
            bool_t bWait)
{
   uint32_t nLength;
   static const char16_t aFilteredParameter[] = {'0','x','5','D','4','9','C','F','7','B','A','E','4','6','4','B','D','B','B','2','7','1','B','7','7','8','C','5','B','D','D','2','2','9', 0 };
   int32_t res = CCSocketReceive(
      &pInstance->socket,
      pBuffer, 4, bWait);

   if(res < 0)
   {
      CCPrintError("static_CCClientReceiveMessage: Error returned by CCSocketReceive()");
      return res;
   }

   if(res == 0)
   {
      return 0;
   }

   static_CCClientParseIntegeter( pBuffer, 0, &nLength);

   if(nLength == 0)
   {
      return 0;
   }

   if(nLength > nBufferLength)
   {
      CCPrintError("static_CCClientReceiveMessage: message too long");
      return -1;
   }

   res = CCSocketReceive(
      &pInstance->socket,
      pBuffer, nLength, W_TRUE);

   if(res < 0)
   {
      CCPrintError("static_CCClientReceiveMessage: Error returned by CCSocketReceive()");
      return res;
   }

   if(static_CCClientParseMessage(
            pBuffer, nBufferLength,
            nLength,
            pnType,
            pppParameterList, pnParameterNumber,
            pPayloadBuffer, pnPayloadLength) == 0)
   {
      CCPrintError("static_CCClientReceiveMessage: Error returned by static_CCClientParseMessage()");
      return -1;
   }

   /* filter the  / ID parameters if present (always located at the end of the parameter list) */

   if (* pnParameterNumber >= 2)
   {
      if ( ((* pppParameterList)[* pnParameterNumber - 2] != null) &&
           (static_CCClientStringCompare((* pppParameterList)[* pnParameterNumber - 2], aFilteredParameter) != W_FALSE))
      {
         * pnParameterNumber -= 2;

         CCPrintError("static_CCClientReceiveMessage : removed spurious parmameter");
      }
   }

   return 1;
}

static uint32_t static_CCClientSendMessage(
            tCCClientInstance* pInstance,
            uint8_t nType,
            const char16_t** ppParameterList,
            uint32_t nParameterNumber,
            const uint8_t* pPayload1Buffer,
            uint32_t nPayload1Length,
            const uint8_t* pPayload2Buffer,
            uint32_t nPayload2Length)
{
   uint32_t nMessageHeadLength;
   uint32_t nResult = 0;

   CCEnterCriticalSection(&pInstance->sSendCS);

   nMessageHeadLength = static_CCClientBuildMessage(
      pInstance->aSendBuffer, CCCLIENT_SEND_BUFFER_LENGTH,
      nType,
      ppParameterList, nParameterNumber,
      nPayload1Length + nPayload2Length);

   if(nMessageHeadLength != 0)
   {
      if(CCSocketSend(
         &pInstance->socket, pInstance->aSendBuffer, nMessageHeadLength) != W_FALSE)
      {
         nResult = 1;

         if(nPayload1Length != 0)
         {
            if(CCSocketSend(
               &pInstance->socket, pPayload1Buffer, nPayload1Length) == W_FALSE)
            {
               CCPrintError("static_CCClientSendMessage: Error returned by CCSocketSend()");
               nResult = 0;
            }
         }

         if((nResult > 0) && (nPayload2Length != 0))
         {
            if(CCSocketSend(
               &pInstance->socket, pPayload2Buffer, nPayload2Length) == W_FALSE)
            {
               CCPrintError("static_CCClientSendMessage: Error returned by CCSocketSend()");
               nResult = 0;
            }
         }
      }
      else
      {
         CCPrintError("static_CCClientSendMessage: Error returned by CCSocketSend()");
      }
   }

   CCLeaveCriticalSection(&pInstance->sSendCS);

   return nResult;
}

/* See header file */
bool_t CCClientGetProtocol(
            const char16_t* pProviderURI,
            char16_t* pProtocolBuffer,
            uint32_t nProtocolBufferLength)
{
   uint32_t nPos = 0;
   uint32_t i = 0;
   char16_t c;

   if((pProviderURI[0] != 'c')
   || (pProviderURI[1] != 'c')
   || (pProviderURI[2] != ':'))
   {
      return W_FALSE;
   }

   nPos = 3;

   /* Skip the host name */
   if(pProviderURI[nPos] == '/')
   {
      if(pProviderURI[++nPos] != '/')
      {
         return W_FALSE;
      }
      do
      {
         c = pProviderURI[++nPos];
         if(c == 0x0000)
         {
            return W_FALSE;
         }
      } while(c != L'/');

      nPos++;
   }

   while((pProviderURI[nPos] != 0) && (pProviderURI[nPos] != '?'))
   {
      pProtocolBuffer[i++] = pProviderURI[nPos];
      if(i == nProtocolBufferLength)
      {
         break;
      }
      nPos++;
   }
   pProtocolBuffer[i] = 0;

   return W_TRUE;
}

/* See header file */
uint32_t CCClientOpen(
            const char16_t* pProviderURI,
            bool_t bWait,
            const uint8_t* pVersionSupported,
            uint32_t nVersionNumber,
            uint8_t* pnNegociatedVersion,
            void** ppConnection)
{
   char16_t aServiceName[51];
   uint8_t nType;

   if(pnNegociatedVersion != null)
   {
      *pnNegociatedVersion = CC_UNKNOWN_VERSION;
   }

   if(ppConnection != null)
   {
      *ppConnection = null;
   }

   if((pProviderURI == null)
   || (pVersionSupported == null) || (nVersionNumber == 0) || (nVersionNumber > CC_MAXIMUM_VERSION)
   || (pnNegociatedVersion == null) || (ppConnection == null))
   {
      CCPrintError("CCClientOpen: Bad Parameters");
      return CC_ERROR_BAD_PARAMETER;
   }

   if(CCClientGetProtocol(pProviderURI, aServiceName, 51) == W_FALSE)
   {
      CCPrintError("CCClientOpen: Bad URI syntax");
      return CC_ERROR_URI_SYNTAX;
   }

   nType = (bWait != W_FALSE) ? MSG_CLIENT_CONNECT_WAIT : MSG_CLIENT_CONNECT;

   return static_CCClientCreateInstance(
      nType, aServiceName, pProviderURI, (const char16_t*)null,
      pVersionSupported, nVersionNumber, pnNegociatedVersion,
      ppConnection);
}

/* See header file */
uint32_t CCClientOpenAsProvider(
            const char16_t* pServiceType,
            const char16_t* pServiceName,
            const uint8_t* pVersionSupported,
            uint32_t nVersionNumber,
            uint8_t* pnNegociatedVersion,
            void** ppConnection)
{
   if(pnNegociatedVersion != null)
   {
      *pnNegociatedVersion = CC_UNKNOWN_VERSION;
   }

   if(ppConnection != null)
   {
      *ppConnection = null;
   }

   if((pServiceType == null)
   || (pVersionSupported == null) || (nVersionNumber == 0) || (nVersionNumber > CC_MAXIMUM_VERSION)
   || (pnNegociatedVersion == null) || (ppConnection == null))
   {
      CCPrintError("CCClientOpenAsProvider: Bad Parameters");
      return CC_ERROR_BAD_PARAMETER;
   }

   return static_CCClientCreateInstance(
      MSG_PROVIDER_CONNECT, pServiceType, (const char16_t*)null, pServiceName,
      pVersionSupported, nVersionNumber, pnNegociatedVersion,
      ppConnection);
}

/* See header file */
void* CCClientGetReceptionEvent(
            void* pConnection)
{
   tCCClientInstance* pInstance = (tCCClientInstance*)pConnection;

   if(pInstance != null)
   {
      return CCSocketGetReceptionEvent(&pInstance->socket);
   }

   return null;
}

/* See header file */
void CCClientClose(
            void* pConnection)
{
   tCCClientInstance* pInstance = (tCCClientInstance*)pConnection;

   if(pInstance != null)
   {
      CCSocketShutdownClose(&pInstance->socket);

      CCFree(pInstance);
   }
}

/* See header file */
uint32_t CCClientSendData(
            void* pConnection,
            const uint8_t* pPayloadBuffer,
            uint32_t nPayloadLength)
{
   tCCClientInstance* pInstance = (tCCClientInstance*)pConnection;

   if(pInstance == null)
   {
      return 0;
   }

   return static_CCClientSendMessage(
      pInstance,
      MSG_SPECIFIC_MESSAGE,
      null, 0,
      pPayloadBuffer, nPayloadLength,
      null, 0);
}

/* See header file */
uint32_t CCClientSendDataEx(
            void* pConnection,
            const uint8_t nPayload1,
            const uint8_t* pPayload2Buffer,
            uint32_t nPayload2Length)
{
   tCCClientInstance* pInstance = (tCCClientInstance*)pConnection;
   uint32_t nMessageHeadLength;
   uint32_t nResult = 0;
   uint8_t* pBuffer = pInstance->aSendBuffer;
   uint32_t nLength = 3 + nPayload2Length;

   if(pInstance == null)
   {
      CCPrintError("CCClientSendDataEx: Bad Connection");
      return 0;
   }

   if((nLength + 4) > CCCLIENT_SEND_BUFFER_LENGTH)
   {
      CCPrintError("CCClientSendDataEx: Buffer too long");
      return 0;
   }

   CCEnterCriticalSection(&pInstance->sSendCS);

   nMessageHeadLength = static_CCClientWriteInteger(pBuffer, 0, nLength);
   pBuffer[nMessageHeadLength++] = MSG_SPECIFIC_MESSAGE;
   pBuffer[nMessageHeadLength++] = 0x01;
   pBuffer[nMessageHeadLength++] = nPayload1;
   if(nPayload2Length < 64)
   {
      CCMemcpy(&pBuffer[nMessageHeadLength], pPayload2Buffer, nPayload2Length);
      nMessageHeadLength += nPayload2Length;
      nPayload2Length = 0;
   }

   if(CCSocketSend(
      &pInstance->socket, pBuffer, nMessageHeadLength) != W_FALSE)
   {
      nResult = 1;

      if(nPayload2Length != 0)
      {
         if(CCSocketSend(
            &pInstance->socket, pPayload2Buffer, nPayload2Length) == W_FALSE)
         {
            CCPrintError("CCClientSendDataEx: Error returned by CCSocketSend()");
            nResult = 0;
         }
      }
   }
   else
   {
      CCPrintError("CCClientSendDataEx: Error returned by CCSocketSend()");
   }

   CCLeaveCriticalSection(&pInstance->sSendCS);

   return nResult;
}

/* See header file */
int32_t CCClientReceiveData(
            void* pConnection,
            uint8_t* pBuffer,
            uint32_t nBufferLength,
            uint8_t** ppPayload,
            bool_t bWait)
{
   tCCClientInstance* pInstance = (tCCClientInstance*)pConnection;
   uint8_t nType;
   uint32_t nParameterNumber;
   uint32_t nPayloadLength;
   char16_t** ppParameterList;
   int32_t nResult;

   if(pInstance == null)
   {
      CCPrintError("CCClientReceiveData: Bad Connection");
      return -1;
   }

   nResult = static_CCClientReceiveMessage(
            pInstance,
            pBuffer, nBufferLength,
            &nType,
            &ppParameterList,
            &nParameterNumber,
            (const uint8_t **) ppPayload,
            &nPayloadLength,
            bWait);

   if(nResult <= 0)
   {
      return nResult;
   }

   if(nType != MSG_SPECIFIC_MESSAGE)
   {
      return 0;
   }

   if(nParameterNumber != 0)
   {
      return 0;
   }

   if(nPayloadLength > 0)
   {
      /* If a message is available and another one is ready, reactivate the event */
      CCSocketSignalReceptionEvent(&pInstance->socket);
   }

   return (int32_t)nPayloadLength;
}

/* -------------------------------------------------------------------------
   -------------------------------------------------------------------------
         TRACE IMPLEMENTATION
   -------------------------------------------------------------------------
   ------------------------------------------------------------------------- */

#define TRACE_MAX_SIZE 2048
static char g_aTraceBuffer[TRACE_MAX_SIZE+1];

static void* g_pDebugHandler = null;

static char16_t g_aLogService[] = { 'c', 'c', ':', 'l', 'o', 'g', 0 };

/* CC Trace Protocol version 1.0 */
#define CC_TRACE_PROTOCOL_VERSION_10  0x10

static uint8_t g_aTraceVersions[] = { CC_TRACE_PROTOCOL_VERSION_10 };

static CCCriticalSection g_sCS;

void CCTraceInit(void)
{

   if(CCInterlockedEnter() == W_FALSE)
   {
      CCCreateCriticalSection(&g_sCS);

      CCEnterCriticalSection(&g_sCS);

      CCInitializeTime();

      if(g_pDebugHandler == null)
      {
         uint8_t nNegociatedVersion;

         CCClientOpen(g_aLogService, W_FALSE,
                     g_aTraceVersions, sizeof(g_aTraceVersions),
                     &nNegociatedVersion,
                     &g_pDebugHandler);

      /* nNegociatedVersion is equal to CC_TRACE_PROTOCOL_VERSION_10 or CC_UNKNOWN_VERSION
         in both cases, the Service Provider version is assumed to be CC_TRACE_PROTOCOL_VERSION_10 */
      }

      CCLeaveCriticalSection(&g_sCS);
   }
}

static char* static_PrepareTraceBuffer(
                                       const char* pTag,
                                       uint32_t nTraceLevel)
{
   /* No time header in CCLient trace mode */
   if(g_pDebugHandler == null)
   {
      CCGetRelativeTime(g_aTraceBuffer, TRACE_MAX_SIZE);
   }
   else
   {
      CCGetAbsoluteRawTime(g_aTraceBuffer, TRACE_MAX_SIZE);
   }

   switch(nTraceLevel)
   {
   case P_TRACE_TRACE:
      CCStrCat(g_aTraceBuffer,TRACE_MAX_SIZE, " INF ");
      break;
   case P_TRACE_LOG:
      CCStrCat(g_aTraceBuffer,TRACE_MAX_SIZE, " LOG ");
      break;
   case P_TRACE_WARNING:
      CCStrCat(g_aTraceBuffer,TRACE_MAX_SIZE, " WARN ");
      break;
   case P_TRACE_ERROR:
      CCStrCat(g_aTraceBuffer,TRACE_MAX_SIZE, " ERROR ");
      break;
   default:
      CCStrCat(g_aTraceBuffer,TRACE_MAX_SIZE, " ????? ");
      break;
   }

   CCStrCat(g_aTraceBuffer,TRACE_MAX_SIZE, pTag);
   CCStrCat(g_aTraceBuffer,TRACE_MAX_SIZE, " ");
   return g_aTraceBuffer;
}

void CCTracePrintBuffer(
                            const char* pTag,
                            uint32_t nTraceLevel,
                            const uint8_t* pDataBuffer,
                            uint32_t nLength)
{
   uint32_t nIndex;
   char* pBuffer;
   uint32_t nPos;

   CCTraceInit();

   CCEnterCriticalSection(&g_sCS);

   pBuffer = static_PrepareTraceBuffer(pTag, nTraceLevel);

   nPos = CCStrLen(pBuffer, TRACE_MAX_SIZE);

   for(nIndex = 0; ((nIndex < nLength) && (nPos < TRACE_MAX_SIZE - 4)); nIndex++)
   {
      uint8_t nValue = pDataBuffer[nIndex];
      uint8_t nSubValue;

      pBuffer[nPos++] = ' ';
      nSubValue = nValue >> 4;
      if(nSubValue <= 9)
      {
         pBuffer[nPos++] = nSubValue + '0';
      }
      else
      {
         pBuffer[nPos++] = nSubValue - 10 + 'A';
      }
      nSubValue = nValue & 0x0F;
      if(nSubValue <= 9)
      {
         pBuffer[nPos++] = nSubValue + '0';
      }
      else
      {
         pBuffer[nPos++] = nSubValue - 10 + 'A';
      }
   }

   pBuffer[nPos++] = ' ';
   pBuffer[nPos++] = '}';
   pBuffer[nPos++] = '\n';
   pBuffer[nPos] = 0;

   if(g_pDebugHandler != null)
   {
      if(CCClientSendData(
         g_pDebugHandler,
         (uint8_t *)pBuffer, nPos)==0)
      {
         g_pDebugHandler = null;
      }
   }
   else
   {
      CCDefaultPrintf(nTraceLevel, pBuffer);
   }
   CCLeaveCriticalSection(&g_sCS);
}


void CCTracePrint(
                      const char* pTag,
                      uint32_t nTraceLevel,
                      const char* pMessage,
                      va_list list)
{
   char* pBuffer;
   uint32_t nPos;

   CCTraceInit();
   CCEnterCriticalSection(&g_sCS);

   pBuffer = static_PrepareTraceBuffer(pTag, nTraceLevel);

   nPos = CCStrLen(pBuffer, TRACE_MAX_SIZE);
   nPos += CCVSPrintf( pBuffer + nPos, TRACE_MAX_SIZE - nPos, pMessage, list);

   pBuffer[nPos++] = '\n';
   pBuffer[nPos] = 0;

   if(g_pDebugHandler != null)
   {
      if(CCClientSendData(
         g_pDebugHandler,
         (uint8_t *)pBuffer, nPos)==0)
      {
         g_pDebugHandler = null;
      }
   }
   else
   {
      CCDefaultPrintf(nTraceLevel, pBuffer);
   }
   CCLeaveCriticalSection(&g_sCS);
}
