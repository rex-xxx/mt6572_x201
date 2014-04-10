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

#ifndef __WME_NDEF_H
#define __WME_NDEF_H

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/** Describes a record handle list - Do not use directly */
typedef struct __tNDEFRecordHandleList
{
   W_HANDLE hRecord;
   struct __tNDEFRecord          * pNDEFRecord;
   struct __tNDEFRecordHandleList* pNext;

}tNDEFRecordHandleList;

/** Describes a NDEF message element - Do not use directly */
typedef struct __tNDEFMessage
{
   tHandleObjectHeader Header;

   tNDEFRecordHandleList* pRecordHandleListBegin;
   tNDEFRecordHandleList* pRecordHandleListEnd;
   tNDEFRecordHandleList* pRecordHandleListCurrent;
   uint32_t nMessageSize;
   uint32_t nRecordCount;

   /* Next message handle */
   bool_t     bhMessageNextGivenToUser;
   W_HANDLE hMessageNext;
}tNDEFMessage;

/** Describes a NDEF rocord element - Do not use directly */
typedef struct __tNDEFRecord
{
   tHandleObjectHeader Header;

   uint32_t nRecordSize;               /* total NDEF record size */
   uint8_t* pRecordBuffer;             /* pointer to the NDEF content */

   uint32_t nTypeOffset;               /* start offset of TYPE field */
   uint32_t nTypeSize;                 /* TYPE field size */

   uint32_t nIdentifierOffset;         /* Start offset of ID field */
   uint32_t nIdentifierSize;           /* ID field size */

   uint32_t nPayloadOffset;            /* Start offset of payload field */
   uint32_t nPayloadSize;              /* PAYLOAD size */

}tNDEFRecord;

/**
 * @brief Initialises pRecord pointer from record handle
 *
 * @param[in]  pContext The context
 * @param[in]  hRecord The record handle
 * @param[out]  ppRecord Address of record pointer to be initialized
 *
 * @return W_SUCCESS on success, other value on failure.
 */

W_ERROR PNDEFGetRecordObject(
      tContext * pContext,
      W_HANDLE hRecord,
      tNDEFRecord ** ppRecord);

/**
 * @brief         Checks and retrieves NDEF parameters.
 * Each pointer parameter shall be null if not requiered.
 *
 * @param[in]     pBuffer           The NDEF buffer.
 * @param[in]     nBufferSize       The NDEF buffer length.
 * @param[in/out] pnTypeOffset      A pointer to the type offset.
 * @param[in/out] pnTypeLength      A pointer to the type length.
 * @param[in/out] pnIDOffset        A pointer to the identifier offset.
 * @param[in/out] pnIDLength        A pointer to the identifier length.
 * @param[in/out] pnPayloadOffset   A pointer to the payload offset.
 * @param[in/out] pnPayloadOffset   A pointer to the payload length.
 *
 * @return   The total size of the NDEF buffer or, 0 in case of error.
 **/
uint32_t PNDEFParseBuffer(
         const uint8_t* pBuffer,
         uint32_t nBufferSize,
         uint32_t *pnTypeOffset,
         uint32_t *pnTypeLength,
         uint32_t *pnIDOffset,
         uint32_t *pnIDLength,
         uint32_t *pnPayloadOffset,
         uint32_t *pnPayloadLength );


/**
 * @brief         Calculates the length of the NDEF Record.
 * Each pointer parameter shall be null if not requiered.
 *
 * @param[in]     pBuffer           The NDEF buffer.
 * @param[in]     nBufferSize       The NDEF buffer length.
 *
 * @return   The total size of the NDEF buffer or, 0 in case of error.
 **/
uint32_t PNDEFGetRecordLength(
         const uint8_t* pBuffer,
         uint32_t nBufferSize);

/**
 * @brief         Checks and retrieves NDEF parameters.
 *                the identifier buffer and type buffer are not checked
 * Each pointer parameter shall be null if not requiered.
 *
 * @param[in]     pBuffer           The NDEF buffer.
 * @param[in]     nBufferSize       The NDEF buffer length.
 * @param[in/out] pnTypeOffset      A pointer to the type offset.
 * @param[in/out] pnTypeLength      A pointer to the type length.
 * @param[in/out] pnIDOffset        A pointer to the identifier offset.
 * @param[in/out] pnIDLength        A pointer to the identifier length.
 * @param[in/out] pnPayloadOffset   A pointer to the payload offset.
 * @param[in/out] pnPayloadOffset   A pointer to the payload length.
 *
 * @return   The total size of the NDEF buffer or, 0 in case of error.
 **/
uint32_t PNDEFParseSafeBuffer(
         uint8_t* pBuffer,
         uint32_t nBufferSize,
         uint32_t *pnTypeOffset,
         uint32_t *pnTypeLength,
         uint32_t *pnIDOffset,
         uint32_t *pnIDLength,
         uint32_t *pnPayloadOffset,
         uint32_t *pnPayloadLength );

/**
 * @brief   Parses the NDEF file data.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nTNFToFind  The TNF value to find.
 *
 * @param[in]  pTypeToFind  The type string to find.
 *
 * @param[in]  pIdentifierToFind  The identifier string to find.
 *
 * @param[in]  pNDEFFile  The NDEF file to parse.
 *
 * @param[in]  nMessageLength  The length of the current message.
 *
 * @param[in]  phMessage  The message handle found, if any.
 **/
W_ERROR PNDEFParseFile(
            tContext* pContext,
            uint8_t nTNFToFind,
            const char16_t* pTypeToFind,
            const char16_t* pIdentifierToFind,
            const uint8_t* pNDEFFile,
            uint32_t nMessageLength,
            W_HANDLE* phMessage );


/**
 * @brief   Sets the next message.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hMessage  The message handle.
 *
 * @param[in]  hMessageNext  The next message handle.
 **/
W_ERROR PNDEFSetNextMessage(
            tContext* pContext,
            W_HANDLE hMessage,
            W_HANDLE hMessageNext );

extern tHandleType g_sNDEFType;

extern tHandleType g_sNDEFRecord;

#define P_HANDLE_TYPE_NDEF_MESSAGE (&g_sNDEFType)

#define P_HANDLE_TYPE_NDEF_RECORD (&g_sNDEFRecord)

/**
 * @brief   Formats NDEF type 2 Tag.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  the connection handle
 *
 * @param[in]  pCallback  a pointer on the handler function when the formatting is performed.
 *
 * @param[in]  pCallbackParameter  a blind parameter transmitted to the callback function.
 **/
void PNDEFFormatNDEFType2(tContext* pContext,
                          W_HANDLE hConnection,
                          tPBasicGenericCallbackFunction *pCallback,
                          void *pCallbackParameter,
                          uint8_t nTagType);

/* tag type */
#define W_NDEF_TAG_IT     0
#define W_NDEF_TAG_ICODE  1
#define W_NDEF_TAG_LRI512 2

/**
 * @brief   Formats NDEF type 6 Tag.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  the connection handle
 *
 * @param[in]  pCallback  a pointer on the handler function when the formatting is performed.
 *
 * @param[in]  pCallbackParameter  a blind parameter transmitted to the callback function.
 *
 * @param[in]  nTypeTag  Type of card Tag IT, Tag Icode or LRI512.
 *
 * @param[in]  nTagSize  The tag's size.
 **/
void PNDEFFormatNDEFType6(tContext* pContext,
                          W_HANDLE  hConnection,
                          tPBasicGenericCallbackFunction *pCallback,
                          void *pCallbackParameter,
                          uint8_t nTagType,
                          uint32_t nTagSize);

/**
 * @brief   Formats NDEF type 5 Tag.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  the connection handle
 *
 * @param[in]  pCallback  a pointer on the handler function when the formatting is performed.
 *
 * @param[in]  pCallbackParameter  a blind parameter transmitted to the callback function.
 *
 **/
void PNDEFFormatNDEFType5(tContext* pContext,
                          W_HANDLE  hConnection,
                          tPBasicGenericCallbackFunction *pCallback,
                          void *pCallbackParameter,
                          uint8_t nTagType);

/**
 * @brief   Formats NDEF type 1 Tag.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  the connection handle
 *
 * @param[in]  pCallback  a pointer on the handler function when the formatting is performed.
 *
 * @param[in]  pCallbackParameter  a blind parameter transmitted to the callback function.
 **/
void PNDEFFormatNDEFType1(tContext* pContext,
                          W_HANDLE  hConnection,
                          tPBasicGenericCallbackFunction *pCallback,
                          void *pCallbackParameter,
                          uint8_t nTagType);


#define P_TYPE7_OFFSET_SECTOR_TRAILER_CONDITION_ACCESS 6
#define P_TYPE7_LENGTH_SECTOR_TRAILER_CONDITION_ACCESS 3


#define P_TYPE7_SECTOR_MAD_1                       0
#define P_TYPE7_SECTOR_MAD_2                       16


/**
 * @brief   Formats NDEF type 7 Tag.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hConnection  the connection handle
 *
 * @param[in]  pCallback  a pointer on the handler function when the formatting is performed.
 *
 * @param[in]  pCallbackParameter  a blind parameter transmitted to the callback function.
 *
 * @param[in]  nTagType the type of the card
 **/
void PNDEFFormatNDEFType7(tContext* pContext,
                          W_HANDLE  hConnection,
                          tPBasicGenericCallbackFunction *pCallback,
                          void *pCallbackParameter,
                          uint8_t nTagType);


/**
 * @brief Converts a printable UTF16 string into an UTF8 string
 *
 * @param[in] pDestString The destination string
 *
 * @param[in] pBuffer The byte array
 *
 * @return The size of the destination string
 *
 **/
uint32_t PNDEFUtilConvertPrintableUTF16ToUTF8(
      uint8_t * pOutBuffer,
      const char16_t * pInstring);

uint32_t PUtilConvertStringToPrintableString(
      char16_t * pDestString,
      const char16_t * pInString);

bool_t PNDEFUtilStringIsPrintableURI(
      const char16_t * pURI);

uint32_t PNDEFUtilConvertByteArrayTo8BitCompatibleString(char16_t * pOutString, const uint8_t * pInBuffer, uint32_t nBufferLength);


typedef struct __tNDEFSendMessageInstance
{
   /* Set to W_TRUE if there's a send message operation in progress */
   bool_t  bOperationInProgress;

   /* The callback context to post the callback */
   tDFCCallbackContext sCallbackContext;

   /* The address of the NDEF message to be sent */
   uint8_t * pBuffer;

   /* The length of the NDEF message to be sent */
   uint32_t  nLength;

   /* Protocole array */
   uint8_t  aPropertyArray[4];
   uint32_t nPropertyOffset;
   uint32_t nPropertyLength;

   /* Driver Operation */
   W_HANDLE hDriverOperation;

   /* User Operation */
   W_HANDLE hUserOperation;

} tNDEFSendMessageInstance;

typedef struct __tNDEFMessageHandler tNDEFMessageHandler;
typedef struct __tNDEFMessageHandlerManager tNDEFMessageHandlerManager;


/* Structure used by the WNDEFRegisterMessageHandler */
struct __tNDEFMessageHandler
{
   /* All objects registered using PHandleRegister must begin with a tHandleObjectHeader structure */
   tHandleObjectHeader        sObjectHeader;

   /* The callback context describing the callback function to be called each time a NDEF message has bee received */
   tDFCCallbackContext        sCallbackContext;

   /* The message filter associated to this handler */
   uint8_t  nTNF;
   char16_t *  pTypeString;

   /* SNEP message handler registry */
   W_HANDLE    hSNEPRegistry;

   /* SNEP message handler registry */
   W_HANDLE    hNPPRegistry;

   /* Reader listener registry */
   W_HANDLE    hReaderRegistry;

   /* Previous and next message */
   tNDEFMessageHandler * pPreviousMessage;
   tNDEFMessageHandler * pNextMessage;

   /* The callback context describing the callback function to be called when this object is detroyed */
   tDFCCallbackContext        sCloseCallbackContext;
};

struct __tNDEFMessageHandlerManager
{
   /* Array of List of Message Handler (A per priority)*/
   tNDEFMessageHandler aNDEFMessageHandlerByPriority[W_PRIORITY_MAXIMUM - W_PRIORITY_MINIMUM + 1];

   /* Used by to save the current Tag Connection handle and the current Message Handle*/
   W_HANDLE    hTmpConnection;
   W_HANDLE    hMessage;

   /* Used by the WorkPerformed */
   bool_t      bTmpMessageMatchAtLeastOne;

   /* Save the last Message Handler Called and the connection property used to retrieve the NDEF Message*/
   tNDEFMessageHandler * pLastMessageHandlerCalled;
   uint8_t nLastPropertyCalled;
};


void PNDEFReadMessageOnAnyTagInternal(tContext * pContext,
                                      tPBasicGenericHandleCallbackFunction pCallback,
                                      void * pCallbackParameter,
                                      uint8_t nPriority,
                                      uint8_t nTNF,
                                      const char16_t * pTypeString,
                                      W_HANDLE * phRegistry);



#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#endif /* __WME_NDEF_H */
