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
   Contains the reader registry implementation.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( REG )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#ifndef P_READER_DETECTION_MAX_RETRIES
#  define P_READER_DETECTION_MAX_RETRIES  0
#endif

#define P_READER_CARD_REMOVAL_DETECTION_TIMEOUT 300

/* Reader user connection's status */
#define P_READER_CONNECTION_INITIALIZATION                  0  /* initial state, set when the User connection is created */
#define P_READER_CONNECTION_ACTIVE                          1  /* set after the connection is created */
#define P_READER_CONNECTION_EXCHANGE_PENDING                2  /* An exchange is pending */
#define P_READER_CONNECTION_POLLING_PENDING                 3  /* A polling is active */
#define P_READER_CONNECTION_POLLING_PENDING_EXCHANGE_QUEUED 4  /* A polling is active and an exchange is queued to be executed after the polling */

/**
 * @brief   Creates a primary connection.
 *
 * The errors restuned in the callback function are the following:
 *    - W_SUCCESS in case of success.
 *    - W_ERROR_CONNECTION_COMPATIBILITY The card is not compliant with
 *      the requested connection.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hUserConnection  The user connection handle.
 *
 * @param[in]  hDriverConnection  The driver connection handle.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameterThe callback parameter.
 *
 * @param[in]  nConnectionProperty  The connection property.
 *
 * @param[in]  pBuffer  The buffer containing the detection data.
 *
 * @param[in]  nLength  The length in bytes of the detection data.
 **/
typedef void tPReaderUserCreatePrimaryConnection(
            tContext* pContext,
            W_HANDLE hUserConnection,
            W_HANDLE hDriverConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nConnectionProperty,
            const uint8_t* pBuffer,
            uint32_t nLength );

/**
 * @brief   Creates a secondary connection.
 *
 * The errors restuned in the callback function are the following:
 *    - W_SUCCESS in case of success.
 *    - W_ERROR_CONNECTION_COMPATIBILITY The card is not compliant with
 *      the requested connection.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hUserConnection  The connection handle.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameterThe callback parameter.
 *
 * @param[in]  nConnectionProperty  The connection property.
 **/
typedef void tPReaderUserCreateSecondaryConnection(
            tContext* pContext,
            W_HANDLE hUserConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nConnectionProperty );

/**
 * @brief   Removes a secondary connection.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hUserConnection  The connection handle.
 **/
typedef void tPReaderUserRemoveSecondaryConnection(
            tContext* pContext,
            W_HANDLE hUserConnection );


typedef void tPReaderUserPollCard(
            tContext * pContext,
            tHandleObjectHeader * pObject,
            tPBasicGenericCallbackFunction * pCallback,
            void * pCallbackParameter);

/** The connection type */
typedef const struct __tPReaderUserType
{
   uint8_t nConnectionProperty;
   uint32_t nDriverProtocol;
   tPReaderUserCreatePrimaryConnection* pCreatePrimaryConnection;
   tPReaderUserCreateSecondaryConnection* pCreateSecondaryConnection;
   tPReaderUserRemoveSecondaryConnection* pRemoveSecondaryConnection;
} tPReaderUserType;

/* The list of the reader properties (See Client API Specifications) */
static tPReaderUserType g_aPReaderUserTypeArray[] = {
   {  W_PROP_ISO_14443_3_A,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,
      P14P3UserCreateConnection,
      null,
      null
   },
#ifdef P_READER_14P4_STANDALONE_SUPPORT
   {  W_PROP_ISO_14443_4_A,
      W_NFCC_PROTOCOL_READER_ISO_14443_4_A,
      P14P4UserCreateConnection,
      null,
      null
   },
#else
   {  W_PROP_ISO_14443_4_A,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A, /* Warning: use ISO Part 3 */
      null,
      P14P4CreateConnection,
      null
   },
#endif /* P_READER_14P4_STANDALONE_SUPPORT */
   {  W_PROP_ISO_14443_3_B,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_B,
      P14P3UserCreateConnection,
      null,
      null
   },
#ifdef P_READER_14P4_STANDALONE_SUPPORT
   {  W_PROP_ISO_14443_4_B,
      W_NFCC_PROTOCOL_READER_ISO_14443_4_B,
      P14P4UserCreateConnection,
      null,
      null
   },
#else
   {  W_PROP_ISO_14443_4_B,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_B, /* Warning: use ISO Part 3 */
      null,
      P14P4CreateConnection,
      null
   },
#endif /* P_READER_14P4_STANDALONE_SUPPORT */
   {  W_PROP_ISO_15693_3,
      W_NFCC_PROTOCOL_READER_ISO_15693_3,
      P15P3UserCreateConnection,
      null,
      null
   },
   {
      W_PROP_NXP_ICODE,
      W_NFCC_PROTOCOL_READER_ISO_15693_3,
      null,
      P15P3UserCreateSecondaryConnection,
      P15P3UserRemoveSecondaryConnection,
   },
   {
      W_PROP_TI_TAGIT,
      W_NFCC_PROTOCOL_READER_ISO_15693_3,
      null,
      P15P3UserCreateSecondaryConnection,
      P15P3UserRemoveSecondaryConnection,
   },
   {
      W_PROP_ST_LRI_512,
      W_NFCC_PROTOCOL_READER_ISO_15693_3,
      null,
      P15P3UserCreateSecondaryConnection,
      P15P3UserRemoveSecondaryConnection,
   },
   {
      W_PROP_ST_LRI_2K,
      W_NFCC_PROTOCOL_READER_ISO_15693_3,
      null,
      P15P3UserCreateSecondaryConnection,
      P15P3UserRemoveSecondaryConnection,
   },

   {  W_PROP_NFC_TAG_TYPE_1,
      W_NFCC_PROTOCOL_READER_TYPE_1_CHIP,
      null,
      PNDEFCreateConnection,
      PNDEFRemoveConnection
   },
   {  W_PROP_NFC_TAG_TYPE_2,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,
      null,
      PNDEFCreateConnection,
      PNDEFRemoveConnection
   },
   {  W_PROP_NFC_TAG_TYPE_3,
      W_NFCC_PROTOCOL_READER_FELICA,
      null,
      PNDEFCreateConnection,
      PNDEFRemoveConnection
   },
#ifdef P_READER_14P4_STANDALONE_SUPPORT
   {  W_PROP_NFC_TAG_TYPE_4_A,
      W_NFCC_PROTOCOL_READER_ISO_14443_4_A,
      null,
      PNDEFCreateConnection,
      PNDEFRemoveConnection
   },
   {  W_PROP_NFC_TAG_TYPE_4_B,
      W_NFCC_PROTOCOL_READER_ISO_14443_4_B,
      null,
      PNDEFCreateConnection,
      PNDEFRemoveConnection
   },
#else /* P_READER_14P4_STANDALONE_SUPPORT */
   {  W_PROP_NFC_TAG_TYPE_4_A,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,  /* Warning: use ISO Part 3 */
      null,
      PNDEFCreateConnection,
      PNDEFRemoveConnection
   },
   {  W_PROP_NFC_TAG_TYPE_4_B,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_B,  /* Warning: use ISO Part 3 */
      null,
      PNDEFCreateConnection,
      PNDEFRemoveConnection
   },
#endif /* P_READER_14P4_STANDALONE_SUPPORT */

#ifdef P_INCLUDE_PICOPASS
   {  W_PROP_NFC_TAG_TYPE_5,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_B,
      null,
      PNDEFCreateConnection,
      PNDEFRemoveConnection
   },
#else /* P_INCLUDE_PICOPASS */
   {
      W_PROP_NFC_TAG_TYPE_5,
      0,
      null,
      null,
      null
   },
#endif /* P_INCLUDE_PICOPASS */
   {  W_PROP_NFC_TAG_TYPE_6,
      W_NFCC_PROTOCOL_READER_ISO_15693_3,
      null,
      PNDEFCreateConnection,
      PNDEFRemoveConnection
   },
#ifdef P_INCLUDE_MIFARE_CLASSIC
   {  W_PROP_NFC_TAG_TYPE_7,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,
      null,
      PNDEFCreateConnection,
      PNDEFRemoveConnection
   },
#else /* P_INCLUDE_MIFARE_CLASSIC */
   {  W_PROP_NFC_TAG_TYPE_7,
      0,
      null,
      null,
      null
   },
#endif /* P_INCLUDE_MIFARE_CLASSIC */
   {  W_PROP_TYPE1_CHIP,
      W_NFCC_PROTOCOL_READER_TYPE_1_CHIP,
      PType1ChipUserCreateConnection,
      null,
      null
   },
   {
      W_PROP_JEWEL,
      W_NFCC_PROTOCOL_READER_TYPE_1_CHIP,
      null,
      PType1ChipUserCreateSecondaryConnection,
      PType1ChipUserRemoveSecondaryConnection,
   },
   {
      W_PROP_TOPAZ,
      W_NFCC_PROTOCOL_READER_TYPE_1_CHIP,
      null,
      PType1ChipUserCreateSecondaryConnection,
      PType1ChipUserRemoveSecondaryConnection,
   },
   {
      W_PROP_TOPAZ_512,
      W_NFCC_PROTOCOL_READER_TYPE_1_CHIP,
      null,
      PType1ChipUserCreateSecondaryConnection,
      PType1ChipUserRemoveSecondaryConnection,
   },
   {  W_PROP_FELICA,
      W_NFCC_PROTOCOL_READER_FELICA,
      PFeliCaUserCreateConnection,
      null,
      null
   },

#ifdef P_INCLUDE_PICOPASS
   {  W_PROP_PICOPASS_2K,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_B,
      null,
      PPicoCreateConnection,
      PPicoRemoveConnection
   },
   {  W_PROP_PICOPASS_32K,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_B,
      null,
      PPicoCreateConnection,
      PPicoRemoveConnection
   },
#else /* P_INCLUDE_PICOPASS */
   {  W_PROP_PICOPASS_2K,
      0,
      null,
      null,
      null,
   },
   {  W_PROP_PICOPASS_32K,
      0,
      null,
      null,
      null
   },
#endif /* P_INCLUDE_PICOPASS */

   {  W_PROP_ICLASS_2K,
      0,
      null,
      null,
      null
   },
   {  W_PROP_ICLASS_16K,
      0,
      null,
      null,
      null
   },
   {  W_PROP_MIFARE_UL,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,
      null,
      PMifareCreateConnection3A,
      PMifareRemoveConnection3A
   },
   {  W_PROP_MIFARE_MINI,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,
      null,
      PMifareCreateConnection3A,
      PMifareRemoveConnection3A
   },
   {  W_PROP_MIFARE_1K,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,
      null,
      PMifareCreateConnection3A,
      PMifareRemoveConnection3A
   },
   {  W_PROP_MIFARE_4K,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,
      null,
      PMifareCreateConnection3A,
      PMifareRemoveConnection3A
   },
   {  W_PROP_MIFARE_PLUS_S_2K,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,
      null,
      PMifareCreateConnection3A,
      PMifareRemoveConnection3A
   },
   {  W_PROP_MIFARE_PLUS_X_2K,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,
      null,
      PMifareCreateConnection3A,
      PMifareRemoveConnection3A
   },
   {  W_PROP_MIFARE_PLUS_S_4K,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,
      null,
      PMifareCreateConnection3A,
      PMifareRemoveConnection3A
   },
   {  W_PROP_MIFARE_PLUS_X_4K,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,
      null,
      PMifareCreateConnection3A,
      PMifareRemoveConnection3A
   },

#ifdef P_READER_14P4_STANDALONE_SUPPORT
   {  W_PROP_MIFARE_DESFIRE_D40,
      W_NFCC_PROTOCOL_READER_ISO_14443_4_A,
      null,
      PMifareCreateConnection4A,
      null
   },
   {  W_PROP_MIFARE_DESFIRE_EV1_2K,
      W_NFCC_PROTOCOL_READER_ISO_14443_4_A,
      null,
      PMifareCreateConnection4A,
      null
   },
   {  W_PROP_MIFARE_DESFIRE_EV1_4K,
      W_NFCC_PROTOCOL_READER_ISO_14443_4_A,
      null,
      PMifareCreateConnection4A,
      null
   },
   {  W_PROP_MIFARE_DESFIRE_EV1_8K,
      W_NFCC_PROTOCOL_READER_ISO_14443_4_A,
      null,
      PMifareCreateConnection4A,
      null
   },
#else /* P_READER_14P4_STANDALONE_SUPPORT */
   {  W_PROP_MIFARE_DESFIRE_D40,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,  /* Warning: use ISO Part 3 */
      null,
      PMifareCreateConnection4A,
      null
   },
   {  W_PROP_MIFARE_DESFIRE_EV1_2K,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,  /* Warning: use ISO Part 3 */
      null,
      PMifareCreateConnection4A,
      null
   },
   {  W_PROP_MIFARE_DESFIRE_EV1_4K,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,  /* Warning: use ISO Part 3 */
      null,
      PMifareCreateConnection4A,
      null
   },
   {  W_PROP_MIFARE_DESFIRE_EV1_8K,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,  /* Warning: use ISO Part 3 */
      null,
      PMifareCreateConnection4A,
      null
   },

#endif /* P_READER_14P4_STANDALONE_SUPPORT */
   {  W_PROP_ISO_7816_4_A,
      0,
      null,
      P7816CreateConnection,
      null
   },
   {  W_PROP_ISO_7816_4_B,
      0,
      null,
      P7816CreateConnection,
      null
   },
   {
      W_PROP_NFC_TAG_TYPE_2_GENERIC,
      0,
      null,
      PNDEF2GenCreateConnection,
      PNDEF2GenRemoveConnection
   },
   {
      W_PROP_BPRIME,
      W_NFCC_PROTOCOL_READER_BPRIME,
      PBPrimeUserCreateConnection,
      null,
      null
   },
   {  W_PROP_MY_D_MOVE,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,
      null,
      PMyDCreateConnection,
      PMyDRemoveConnection
   },
   {  W_PROP_MY_D_NFC,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,
      null,
      PMyDCreateConnection,
      PMyDRemoveConnection
   },

   {
      W_PROP_KOVIO,
      W_NFCC_PROTOCOL_READER_KOVIO,
      PKovioUserCreateConnection,
      null,
      null
   },

   {
      W_PROP_KOVIO_RFID,
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A,
      null,
      PKovioRFIDCreateConnection,
      PKovioRFIDRemoveConnection,
   }
};

static const uint8_t g_aPReaderUserPropertyChain[] = {

   W_PROP_TYPE1_CHIP,    W_PROP_JEWEL,             0,
   W_PROP_TYPE1_CHIP,    W_PROP_TOPAZ,             W_PROP_NFC_TAG_TYPE_1,   0,
   W_PROP_TYPE1_CHIP,    W_PROP_TOPAZ_512,         W_PROP_NFC_TAG_TYPE_1,   0,
   W_PROP_TYPE1_CHIP,    W_PROP_NFC_TAG_TYPE_1,    0,
   W_PROP_FELICA,        W_PROP_NFC_TAG_TYPE_3,    0,
   W_PROP_ISO_14443_3_A, W_PROP_NFC_TAG_TYPE_2_GENERIC, W_PROP_MIFARE_UL, W_PROP_NFC_TAG_TYPE_2,  0,
   W_PROP_ISO_14443_3_A, W_PROP_NFC_TAG_TYPE_2_GENERIC, W_PROP_MY_D_MOVE, W_PROP_NFC_TAG_TYPE_2,  0,
   W_PROP_ISO_14443_3_A, W_PROP_NFC_TAG_TYPE_2_GENERIC, W_PROP_MY_D_NFC, W_PROP_NFC_TAG_TYPE_2,  0,
   W_PROP_ISO_14443_3_A, W_PROP_NFC_TAG_TYPE_2_GENERIC, W_PROP_KOVIO_RFID, W_PROP_NFC_TAG_TYPE_2,  0,
   W_PROP_ISO_14443_3_A, W_PROP_NFC_TAG_TYPE_2_GENERIC, W_PROP_NFC_TAG_TYPE_2,  0,
   W_PROP_ISO_14443_3_A, W_PROP_MY_D_NFC,          0,
   W_PROP_ISO_14443_3_A, W_PROP_MIFARE_MINI,       0,
#ifdef P_INCLUDE_MIFARE_CLASSIC
   W_PROP_ISO_14443_3_A, W_PROP_MIFARE_1K,         W_PROP_NFC_TAG_TYPE_7,   0,
   W_PROP_ISO_14443_3_A, W_PROP_MIFARE_4K,         W_PROP_NFC_TAG_TYPE_7,   0,
#else /* P_INCLUDE_MIFARE_CLASSIC */
   W_PROP_ISO_14443_3_A, W_PROP_MIFARE_1K,         0,
   W_PROP_ISO_14443_3_A, W_PROP_MIFARE_4K,         0,
#endif /* P_INCLUDE_MIFARE_CLASSIC */

   W_PROP_ISO_14443_3_A, W_PROP_MIFARE_PLUS_S_2K,  0,
   W_PROP_ISO_14443_3_A, W_PROP_MIFARE_PLUS_X_2K,  0,
   W_PROP_ISO_14443_3_A, W_PROP_MIFARE_PLUS_S_4K,  0,
   W_PROP_ISO_14443_3_A, W_PROP_MIFARE_PLUS_X_4K,  0,
   W_PROP_KOVIO,         0,

#ifdef P_READER_14P4_STANDALONE_SUPPORT

   W_PROP_ISO_14443_4_A, W_PROP_ISO_7816_4_A,      W_PROP_MIFARE_DESFIRE_D40, W_PROP_NFC_TAG_TYPE_4_A, 0,
   W_PROP_ISO_14443_4_A, W_PROP_ISO_7816_4_A,      W_PROP_MIFARE_DESFIRE_EV1_2K, W_PROP_NFC_TAG_TYPE_4_A, 0,
   W_PROP_ISO_14443_4_A, W_PROP_ISO_7816_4_A,      W_PROP_MIFARE_DESFIRE_EV1_4K, W_PROP_NFC_TAG_TYPE_4_A, 0,
   W_PROP_ISO_14443_4_A, W_PROP_ISO_7816_4_A,      W_PROP_MIFARE_DESFIRE_EV1_8K, W_PROP_NFC_TAG_TYPE_4_A, 0,

   W_PROP_ISO_14443_4_A, W_PROP_ISO_7816_4_A,      W_PROP_NFC_TAG_TYPE_4_A, 0,
#else /* P_READER_14P4_STANDALONE_SUPPORT */
   W_PROP_ISO_14443_3_A, W_PROP_ISO_14443_4_A,     W_PROP_ISO_7816_4_A,    W_PROP_MIFARE_DESFIRE_D40, W_PROP_NFC_TAG_TYPE_4_A, 0,
   W_PROP_ISO_14443_3_A, W_PROP_ISO_14443_4_A,     W_PROP_ISO_7816_4_A,    W_PROP_MIFARE_DESFIRE_EV1_2K, W_PROP_NFC_TAG_TYPE_4_A, 0,
   W_PROP_ISO_14443_3_A, W_PROP_ISO_14443_4_A,     W_PROP_ISO_7816_4_A,    W_PROP_MIFARE_DESFIRE_EV1_4K, W_PROP_NFC_TAG_TYPE_4_A, 0,
   W_PROP_ISO_14443_3_A, W_PROP_ISO_14443_4_A,     W_PROP_ISO_7816_4_A,    W_PROP_MIFARE_DESFIRE_EV1_8K, W_PROP_NFC_TAG_TYPE_4_A, 0,
   W_PROP_ISO_14443_3_A, W_PROP_ISO_14443_4_A,     W_PROP_ISO_7816_4_A,    W_PROP_NFC_TAG_TYPE_4_A, 0,
#endif /* P_READER_14P4_STANDALONE_SUPPORT */


   W_PROP_ISO_15693_3,   W_PROP_NXP_ICODE,         W_PROP_NFC_TAG_TYPE_6, 0,
   W_PROP_ISO_15693_3,   W_PROP_ST_LRI_512,        W_PROP_NFC_TAG_TYPE_6, 0,
   W_PROP_ISO_15693_3,   W_PROP_ST_LRI_2K,         W_PROP_NFC_TAG_TYPE_6, 0,
   W_PROP_ISO_15693_3,   W_PROP_TI_TAGIT,          W_PROP_NFC_TAG_TYPE_6, 0,
   W_PROP_ISO_15693_3,   W_PROP_NFC_TAG_TYPE_6,    0,

#ifdef P_INCLUDE_PICOPASS
   W_PROP_ISO_14443_3_B, W_PROP_PICOPASS_2K,       W_PROP_NFC_TAG_TYPE_5,  0,
   W_PROP_ISO_14443_3_B, W_PROP_PICOPASS_32K,      W_PROP_NFC_TAG_TYPE_5,  0,
#endif /* P_INCLUDE_PICOPASS */

#ifdef P_READER_14P4_STANDALONE_SUPPORT
   W_PROP_ISO_14443_4_B, W_PROP_ISO_7816_4_B,      W_PROP_NFC_TAG_TYPE_4_B, 0,
#else /* P_READER_14P4_STANDALONE_SUPPORT */
   W_PROP_ISO_14443_3_B, W_PROP_ISO_14443_4_B,     W_PROP_ISO_7816_4_B,      W_PROP_NFC_TAG_TYPE_4_B, 0,
#endif /* P_READER_14P4_STANDALONE_SUPPORT */
};

/* List properties that are internal and not visible from API (WBasicGetConnectionProperties, etc...) */
static const uint8_t g_aPReaderUserInternalProperties[] = {
   W_PROP_NFC_TAG_TYPE_2_GENERIC
};

#define P_READER_INTERNAL_PROPERTY_NUMBER (sizeof(g_aPReaderUserInternalProperties) / sizeof(uint8_t))

/* Maximum number of properties */
#define P_READER_PROPERTY_MAX_NUMBER  \
   (sizeof(g_aPReaderUserTypeArray) / sizeof(tPReaderUserType))

/* Declare a reader registry structure */
typedef struct __tReaderUserListener
{
   /* Connection registry handle */
   tHandleObjectHeader sObjectHeader;

   uint8_t  aPropertyArray[P_READER_PROPERTY_MAX_NUMBER];
   uint32_t nPropertyNumber;

   /* Driver listener handle */
   W_HANDLE hDriverListener;

   /* Response buffer */
   uint8_t aCardToReaderBuffer[NAL_MESSAGE_MAX_LENGTH];

   /* Callback information */
   tDFCCallbackContext sCallbackContext;

} tReaderUserListener;

/* Declare a reader registry structure */
typedef struct __tReaderUserCardRemovalListener
{
   /* Connection object registry */
   tHandleObjectHeader  sObjectHeader;

   /* Registry handle for this listener */
   W_HANDLE hListener;

   /* Callback triggered when the card has been removed */
   tDFCCallbackContext  sCallbackContext;

   /* User connection handle */
   W_HANDLE hUserConnection;

} tReaderUserCardRemovalListener;

/* Declare a reader connection structure */
typedef struct __tReaderUserConnection
{
   /* Connection object registry */
   tHandleObjectHeader sObjectHeader;
   /* Registry handle */
   W_HANDLE hDriverConnection;

   /* The listener */
   tReaderUserListener* pUserListener;

   /* State machine variables for the connection build  */
   uint8_t nCurrentProperty;
   uint8_t nNextProperty;
   uint8_t nListIndex;
   bool_t bConnectionMatch;

   /* Number of received RF Error during the connection build */
   uint32_t nNumberRFError;

   bool_t bPreviousCardApplicationMatch;

   /* Status of the user connection (INITIALIZATION, ACTIVE, EXCHANGE_PENDING, ...) */
   uint32_t nStatus;

   /* Callback context for PReaderExchangeData */
   tDFCCallbackContext sCallbackContext;
   /* Operation handle for PReaderExchangeData */
   W_HANDLE hOperation;

   /* Hold data of the queued PReaderExchangeData which will be executed after the polling completion */
   struct __tQueuedExchangeData
   {
      /* User connection handle */
      W_HANDLE hUserConnection;
      /* Callback context */
      tDFCCallbackContext sCallbackContext;
      /* Data */
      const uint8_t* pReaderToCardBuffer;
      uint32_t       nReaderToCardBufferLength;
      uint8_t*       pCardToReaderBuffer;
      uint32_t       nCardToReaderBufferMaxLength;

      uint8_t        nPropertyIdentifier;
      /* Operation handle */
      W_HANDLE       hOperation;
   } sQueuedExchangeData;

   /* Card removal listener. Not null if card removal detection is active */
   tReaderUserCardRemovalListener* pCardRemovalListener;

   /* The pointer to execute the queued ExchangeData after polling */
   tReaderExecuteQueuedExchange* pExecuteQueuedExchange;
   /* A blind parameter transmitted to the queued ExchangeData */
   void* pExchangeParameter;

} tReaderUserConnection;

/**
 * @brief   Destroyes a user connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PReaderUserConnectionDestroy(
            tContext* pContext,
            void* pObject )
{
   tReaderUserConnection* pUserConnection = (tReaderUserConnection*)pObject;

   PDebugTrace("static_PReaderUserConnectionDestroy");

   /* Commit the user cache, if needed */
   PCacheConnectionUserCommit(pContext);

   /* Close the card removal listener */
   if (pUserConnection->pCardRemovalListener != null)
   {
      PHandleClose(pContext, pUserConnection->pCardRemovalListener->hListener);
      pUserConnection->pCardRemovalListener = null;
   }

   /* Close driver handle */
   PHandleClose( pContext, pUserConnection->hDriverConnection );

   /* Decrement the reference count of the listener object  */
   PHandleDecrementReferenceCount(pContext, pUserConnection->pUserListener);

   CMemoryFree( pUserConnection );

   return P_HANDLE_DESTROY_DONE;
}

static bool_t static_PReaderCheckProperty(
         tContext* pContext,
         void* pObject,
         uint8_t nPropertyValue )
{
   return W_FALSE;
}

tHandleType g_sReaderUserConnection = {
   static_PReaderUserConnectionDestroy, null, null, null, static_PReaderCheckProperty, null, null, null, null };

#define P_HANDLE_TYPE_READER_USER_CONNECTION (&g_sReaderUserConnection)

/**
 * @brief   Destroyes a user card removal listener object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PReaderUserCardRemovalListenerDestroy(
            tContext* pContext,
            void* pObject )
{
   tReaderUserCardRemovalListener* pCardRemovalListener = (tReaderUserCardRemovalListener*)pObject;
   tReaderUserConnection* pUserConnection = null;
   W_ERROR nError;

   PDebugTrace("static_PReaderUserCardRemovalListenerDestroy");

   /* Get the user connection object */
   nError = PHandleGetObject(pContext, pCardRemovalListener->hUserConnection, P_HANDLE_TYPE_READER_USER_CONNECTION, (void**)&pUserConnection);
   if ((nError == W_SUCCESS) && (pUserConnection != null))
   {
      /* If timer is running */
      if (pUserConnection->nStatus == P_READER_CONNECTION_ACTIVE)
      {
         /* Stop timer */
         PMultiTimerCancelDriver(pContext, TIMER_T15_CARD_REMOVAL_DETECTION);
      }

      /* Erase pointer */
      pUserConnection->pCardRemovalListener = null;

      /* Decrement the reference count of the connection. This may destroy the object */
      PHandleDecrementReferenceCount(pContext, pUserConnection);
   }

   /* Free memory */
   CMemoryFree(pCardRemovalListener);

   return P_HANDLE_DESTROY_DONE;
}

tHandleType g_sReaderUserCardRemovalDetection = {
   static_PReaderUserCardRemovalListenerDestroy, null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_READER_USER_CARD_REMOVAL_DETECTION (&g_sReaderUserCardRemovalDetection)


/* Destroy registry callback */
static uint32_t static_PReaderUserListenerDestroy(
            tContext* pContext,
            void* pObject );

tHandleType g_sReaderUserListener = { static_PReaderUserListenerDestroy, null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_READER_USER_LISTENER (&g_sReaderUserListener)

/**
 * @brief   Destroyes a reader listener object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PReaderUserListenerDestroy(
            tContext* pContext,
            void* pObject )
{
   tReaderUserListener* pUserListener = (tReaderUserListener*)pObject;

   PDebugTrace("static_PReaderUserListenerDestroy");

   /* Free the registry buffer */
   if ( pUserListener != null )
   {
      PReaderDriverSetWorkPerformedAndClose(pContext, pUserListener->hDriverListener);

/*       PHandleClose( pContext, pUserListener->hDriverListener ); */

      CMemoryFree( pUserListener );
   }

   return P_HANDLE_DESTROY_DONE;
}

/** See header file */
void* PReaderUserConvertPointerToConnection(
                  tContext* pContext,
                  void* pPointer,
                  tHandleType* pExpectedType,
                  W_HANDLE* phUserConnection)
{
   W_ERROR nError;
   void* pObject;

   CDebugAssert(pPointer != null);

   *phUserConnection = PUtilConvertPointerToHandle(pPointer);

   nError = PHandleGetObject(pContext, *phUserConnection, pExpectedType, &pObject);

   /* Check the result */
   if ( ( nError != W_SUCCESS ) || ( pObject == null ) )
   {
      *phUserConnection = W_NULL_HANDLE;
      pObject = null;
   }

   return pObject;
}

/**
 * @brief  Looks for a property in the resquested listener list.
 *
 * @param[in] nConnectionProperty  The connection property to find.
 *
 * @param[in] pPropertyArray  The requested property array.
 *
 * @param[in] nPropertyNumber  The number of requested property.
 *
 * @return  W_TRUE if the property is found, W_FALSE otherwise.
 **/
static P_INLINE bool_t static_PReaderUserSearchPropertyInRequestedList(
                  uint8_t nConnectionProperty,
                  const uint8_t* pPropertyArray,
                  uint32_t nPropertyNumber)
{
   uint32_t nPos;

   for(nPos = 0; nPos < nPropertyNumber; nPos++)
   {
      if(nConnectionProperty == pPropertyArray[nPos])
      {
         return W_TRUE;
      }
   }

   return W_FALSE;
}

/**
 * @brief  Looks for a property in the chain list.
 *
 * @param[inout]   pnListIndex  The index of the first list updated with the index
 *                 of the list where is found the property.
 *
 * @param[in]      nConncetionProperty  The connection property.
 *
 * @return   The index following the property in the chain list
 *           or -1 if the property is not found.
 **/
static int32_t static_PReaderUserLookInChainList(
                  uint8_t* pnListIndex,
                  uint8_t nConncetionProperty)
{
   uint8_t nListIndex = 0;
   uint32_t nPos = 0;
   uint8_t nValue;

   while(nListIndex < *pnListIndex)
   {
      while(g_aPReaderUserPropertyChain[nPos++] != 0) {}

      if(nPos == sizeof(g_aPReaderUserPropertyChain))
      {
         return -1;
      }

      nListIndex++;
   }

   for(;;)
   {
      nValue = g_aPReaderUserPropertyChain[nPos++];
      if(nValue == 0)
      {
         if(nPos == sizeof(g_aPReaderUserPropertyChain))
         {
            return -1;
         }
         nListIndex++;
      }
      else
      {
         if(nValue == nConncetionProperty)
         {
            break;
         }
      }
   }

   *pnListIndex = nListIndex;
   return nPos;
}

/**
 * @brief   Removes a connection.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hUserConnection  The connection handle.
 *
 * @param[in]  nConnectionProperty  The connection property.
 **/
static void static_PReaderUserRemoveConnection(
                  tContext* pContext,
                  W_HANDLE hUserConnection,
                  uint8_t nConnectionProperty )
{
   uint32_t nIndex;

   for(nIndex = 0; nIndex < P_READER_PROPERTY_MAX_NUMBER; nIndex++)
   {
      if(g_aPReaderUserTypeArray[nIndex].nConnectionProperty == nConnectionProperty)
      {
         if(g_aPReaderUserTypeArray[nIndex].pRemoveSecondaryConnection != null)
         {
            g_aPReaderUserTypeArray[nIndex].pRemoveSecondaryConnection(
               pContext, hUserConnection );
         }
         return;
      }
   }
   CDebugAssert(W_FALSE);
}

/**
 * @brief   Adds a connection.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hUserConnection  The connection handle.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameterThe callback parameter.
 *
 * @param[in]  nConnectionProperty  The connection property.
 **/
static void static_PReaderUserAddConnection(
                  tContext* pContext,
                  W_HANDLE hUserConnection,
                  tPBasicGenericCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  uint8_t nConnectionProperty )
{
   uint32_t nIndex;

   for(nIndex = 0; nIndex < P_READER_PROPERTY_MAX_NUMBER; nIndex++)
   {
      if(g_aPReaderUserTypeArray[nIndex].nConnectionProperty == nConnectionProperty)
      {
         CDebugAssert(g_aPReaderUserTypeArray[nIndex].pCreateSecondaryConnection != null);
         g_aPReaderUserTypeArray[nIndex].pCreateSecondaryConnection(
            pContext, hUserConnection, pCallback, pCallbackParameter, nConnectionProperty );
         return;
      }
   }
   CDebugAssert(W_FALSE);
}

/**
 * @brief   Receives the completion of a connection creation initiated by a create connection call.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified for the call.
 *
 * @param[in]  nError  The error code:
 *               - W_SUCCESS in case of success.
 *               - W_ERROR_CONNECTION_COMPATIBILITY The card is not compliant with the requested connection.
 **/
static void static_PReaderUserCreateConnectionCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  W_ERROR nError )
{
   /* Algorithm

   Variables:
      CURRENT  current property
      NEXT  next property
      LIST#  chain list index
      MATCH  connection match flag

   Initial values:
      CURRENT = 0
      NEXT = initial property
      LIST# = 0
      MATCH = W_FALSE

   If an error is returned =>  LIST# ++
   else (success)
   {
      CURRENT = NEXT
      NEXT = 0
      LIST# = 0
   }

   if CURRENT==0 => failure, no event, stop

   if CURRENT is in listener requested list => MATCH = W_TRUE

   Loop
   {
      Look for CURRENT in the chain lists from LIST# (included)

      If not found
      {
         If MATCH != W_FALSE => Send success event, stop
         Else => failure, no event, stop
      }
      Else If CURRENT is the last of the current chain list
      {
         LIST# ++
      }
      Else
      {
         Look if another property in the requested list is in the remaining
         properties of the current chain list
         If not found
         {
            LIST# ++
         }
         Else
         {
            If following property in the current chain list == NEXT
            {
               LIST# ++
            }
            Else
            {
               Try the detection of the following property in the current chain list
               Set NEXT with the following property
               break loop to wait event
            }
         }
      }
   } End Loop

   */
   W_HANDLE hUserConnection;
   tReaderUserConnection* pUserConnection  = (tReaderUserConnection*)PReaderUserConvertPointerToConnection(
      pContext, pCallbackParameter, P_HANDLE_TYPE_READER_USER_CONNECTION, &hUserConnection);
   /* pUserConnection cannot be null. Else OpenNFC is dead... */
   tReaderUserListener* pUserListener = pUserConnection->pUserListener;
   int32_t nPos;

   PDebugTrace("static_PReaderUserCreateConnectionCompleted");

   /* Check if the create connection was successful */
   if ( nError == W_SUCCESS )
   {
      uint8_t aPropertyArray[15];

      /* Check if the property set by the Create Property function is equal or different
         from the property requested */
      nError = PHandleGetProperties( pContext, hUserConnection, aPropertyArray, sizeof(aPropertyArray) );
      if(nError != W_SUCCESS)
      {
         PDebugError("static_PReaderUserCreateConnectionCompleted: PHandleGetProperties() in error");
         goto return_error;
      }

      if(aPropertyArray[0] == pUserConnection->nNextProperty)
      {
         PDebugTrace( "static_PReaderUserCreateConnectionCompleted: Success for %s",
            PUtilTraceConnectionProperty(pUserConnection->nNextProperty) );
      }
      else
      {
         if (aPropertyArray[0] != W_PROP_ISO_7816_4)
         {

            PDebugTrace( "static_PReaderUserCreateConnectionCompleted: Success for %s replacing %s",
               PUtilTraceConnectionProperty(aPropertyArray[0]), PUtilTraceConnectionProperty(pUserConnection->nNextProperty) );

            pUserConnection->nNextProperty = aPropertyArray[0];
         }
         else
         {
             PDebugTrace( "static_PReaderUserCreateConnectionCompleted: Success for %s",
                PUtilTraceConnectionProperty(pUserConnection->nNextProperty));
         }

      }

      pUserConnection->nCurrentProperty = pUserConnection->nNextProperty;
      pUserConnection->nNextProperty = 0;
      /* Keep bConnectionMatch */
      pUserConnection->nListIndex = 0;
   }
   else
   {
      if ( nError == W_ERROR_CONNECTION_COMPATIBILITY )
      {
         /* Remove the faulty object form the connection */
         static_PReaderUserRemoveConnection( pContext, hUserConnection,
                  pUserConnection->nNextProperty );
         /* Keep nCurrentProperty */
         /* Keep nNextProperty */
         /* Keep bConnectionMatch */
         pUserConnection->nListIndex++;
      }
      else
      {
         PDebugError( "static_PReaderUserCreateConnectionCompleted: receive error %s for %s",
            PUtilTraceError(nError), PUtilTraceConnectionProperty(pUserConnection->nNextProperty) );

         if ( (nError == W_ERROR_RF_COMMUNICATION) &&
              (pUserConnection->nNumberRFError++ < (uint32_t) P_READER_DETECTION_MAX_RETRIES)
            )
         {
            PDebugError( "static_PReaderUserCreateConnectionCompleted: force card redetection");

            PReaderDriverRedetectCard(pContext, pUserConnection->hDriverConnection);
            pUserConnection->hDriverConnection = W_NULL_HANDLE;
            PHandleClose( pContext, hUserConnection );
            return;
         }
         else
         {
            goto return_error;
         }
      }
   }

   if(pUserConnection->nCurrentProperty == 0)
   {
      PDebugError( "static_PReaderUserCreateConnectionCompleted: Error in the first step");
      CDebugAssert(pUserConnection->bConnectionMatch == W_FALSE);
      PHandleClose( pContext, hUserConnection );
      return;
   }

   /* Look for the property in the requested listener list */
   if(static_PReaderUserSearchPropertyInRequestedList(
         pUserConnection->nCurrentProperty,
         pUserListener->aPropertyArray, pUserListener->nPropertyNumber) != W_FALSE)
   {
      PDebugTrace("static_PReaderUserCreateConnectionCompleted: Find a match for %s",
         PUtilTraceConnectionProperty(pUserConnection->nCurrentProperty));
      pUserConnection->bConnectionMatch = W_TRUE;
   }


   while((nPos = static_PReaderUserLookInChainList(
      &pUserConnection->nListIndex, pUserConnection->nCurrentProperty)) >= 0)
   {
      int32_t nPos2 = nPos;

      /* If the current property is the last of the chain list */
      while(g_aPReaderUserPropertyChain[nPos2] != 0)
      {
         /* Look if another property in the requested list is in the remaining
         properties of the current chain list */
         if(static_PReaderUserSearchPropertyInRequestedList(
               g_aPReaderUserPropertyChain[nPos2],
               pUserListener->aPropertyArray, pUserListener->nPropertyNumber) != W_FALSE)
         {
            if(g_aPReaderUserPropertyChain[nPos] == pUserConnection->nNextProperty)
            {
               break;
            }
            else
            {
               /* Try the detection of the next property */
               static_PReaderUserAddConnection(
                  pContext,
                  hUserConnection,
                  static_PReaderUserCreateConnectionCompleted,
                  PUtilConvertHandleToPointer(hUserConnection),
                  g_aPReaderUserPropertyChain[nPos] );

               /* Set the next property */
               pUserConnection->nNextProperty = g_aPReaderUserPropertyChain[nPos];

               return;
            }
         }

         nPos2++;
      }

      pUserConnection->nListIndex++;
   }

   if(pUserConnection->bConnectionMatch != W_FALSE)
   {
      /* Call the callback related to this handle */
      PDFCPostContext3(
         &pUserListener->sCallbackContext,
         hUserConnection,
         W_SUCCESS );
   }
   else
   {
      PHandleClose( pContext, hUserConnection );
   }

   return;

return_error:

   /* Call the callback related to this handle */
   PDFCPostContext3(
      &pUserListener->sCallbackContext,
      W_NULL_HANDLE,
      nError );

   PHandleClose( pContext, hUserConnection );
}

/**
 * @brief   Creates the connection at reader level.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pUserListener  The reader registry structure.
 *
 * @param[in]  hDriverConnection  The driver connection handle.
 *
 * @param[in]  nProperty  The property number.
 *
 * @param[in]  bPreviousCardApplicationMatch  The card application match flag.
 *
 * @param[out] phUserConnection  The connection handle.
 *
 * @return  The result code.
 **/
static W_ERROR static_PReaderUserCreateConnection(
            tContext* pContext,
            tReaderUserListener* pUserListener,
            W_HANDLE hDriverConnection,
            uint8_t nProperty,
            bool_t bPreviousCardApplicationMatch,
            W_HANDLE* phUserConnection)
{
   tReaderUserConnection* pUserConnection;
   W_HANDLE hUserConnection;
   W_ERROR nError;

   PDebugTrace("static_PReaderUserCreateConnection");

   /* Create the reader buffer */
   pUserConnection = (tReaderUserConnection*)CMemoryAlloc( sizeof(tReaderUserConnection) );
   if ( pUserConnection == null )
   {
      PDebugError("static_PReaderUserCreateConnection: pUserConnection == null");
      return W_ERROR_OUT_OF_RESOURCE;
   }
   CMemoryFill(pUserConnection, 0, sizeof(tReaderUserConnection));

   /* Register the reader connection structure */
   if ( ( nError = PHandleRegister(
                        pContext,
                        pUserConnection,
                        P_HANDLE_TYPE_READER_USER_CONNECTION,
                        &hUserConnection) ) != W_SUCCESS )
   {
      PDebugError("static_PReaderUserCreateConnection: error on PHandleRegister %s",
         PUtilTraceError(nError) );
      CMemoryFree(pUserConnection);
      return nError;
   }

   /* Store the driver connection handle */
   pUserConnection->hDriverConnection = hDriverConnection;

   /* Set the user listener */
   pUserConnection->pUserListener = pUserListener;

   /* Set the current value of the application match flag */
   pUserConnection->bPreviousCardApplicationMatch = bPreviousCardApplicationMatch;

   /* Increment the reference count of the listener object */
   PHandleIncrementReferenceCount(pUserListener);

   /* Initalize the state machine variables */
   pUserConnection->nCurrentProperty = 0;
   pUserConnection->nNextProperty = nProperty;
   pUserConnection->nListIndex = 0;
   pUserConnection->bConnectionMatch = W_FALSE;
   pUserConnection->nNumberRFError = 0;

   /* Connection status */
   pUserConnection->nStatus = P_READER_CONNECTION_ACTIVE;

   /* Initialize card removal listener */
   pUserConnection->pCardRemovalListener = null;

   /* Return the connection */
   *phUserConnection = hUserConnection;

   return W_SUCCESS;
}

/** See tPReaderDriverRegisterCompleted **/
static void static_PReaderUserCardDetectionHandler(
                  tContext* pContext,
                  void* pCallbackParameter,
                  uint32_t nDriverProtocol,
                  W_HANDLE hDriverConnection,
                  uint32_t nLength,
                  bool_t bPreviousCardApplicationMatch )
{
   tReaderUserListener* pUserListener = (tReaderUserListener*)pCallbackParameter;
   W_HANDLE hUserConnection = W_NULL_HANDLE;
   W_ERROR nError = W_SUCCESS;
   uint8_t nProperty = 0;
   uint32_t nIndex;
   tPReaderUserCreatePrimaryConnection* pCreatePrimaryConnection;

   uint8_t* pBuffer = pUserListener->aCardToReaderBuffer;

   PDebugTrace("static_PReaderUserCardDetectionHandler()" );
   PDebugTraceBuffer( pBuffer, nLength );

   /* Register the different connection property buffers */
   pCreatePrimaryConnection = null;
   for(nIndex = 0; nIndex < P_READER_PROPERTY_MAX_NUMBER; nIndex++)
   {
      if(g_aPReaderUserTypeArray[nIndex].pCreatePrimaryConnection != null)
      {
         if((g_aPReaderUserTypeArray[nIndex].nDriverProtocol & nDriverProtocol) != 0)
         {
            pCreatePrimaryConnection = g_aPReaderUserTypeArray[nIndex].pCreatePrimaryConnection;
            nProperty = g_aPReaderUserTypeArray[nIndex].nConnectionProperty;
            break;
         }
      }
   }

   if(pCreatePrimaryConnection == null)
   {
      PDebugError(
         "static_PReaderUserCardDetectionHandler: wrong driver protocol %s",
         PUtilTraceReaderProtocol(pContext, nDriverProtocol) );
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Get the connection handle */
   nError = static_PReaderUserCreateConnection(
            pContext,
            pUserListener,
            hDriverConnection,
            nProperty,
            bPreviousCardApplicationMatch,
            &hUserConnection );

   if ( nError != W_SUCCESS )
   {
      PDebugError("static_PReaderUserCardDetectionHandler: static_PReaderUserCreateConnection");
      goto return_error;
   }

   /* Update the User Connection Cache*/
   PCacheConnectionUserUpdate(pContext);

   /* Register the different connection property buffers */
   pCreatePrimaryConnection(
            pContext,
            hUserConnection,
            hDriverConnection,
            static_PReaderUserCreateConnectionCompleted,
            PUtilConvertHandleToPointer(hUserConnection),
            nProperty,
            pBuffer, nLength );

   return;

return_error:

   PDebugError(
         "static_PReaderUserCardDetectionHandler: sending error %s",
         PUtilTraceError(nError) );

   /* Call the callback related to this handle */
   PDFCPostContext3(
      &pUserListener->sCallbackContext,
      W_NULL_HANDLE,
      nError );

   /* If the connection handle exists */
   PHandleClose(pContext, hUserConnection );
}

/**
 * @brief   Checks a connection property and returns the corresponding driver protocol value.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nConnectionProperty  The connection property to check.
 *
 * @return  The protocol value
 *          0 if the property is not supported by the stack
 *          0xFFFFFFFF if the property is invalid
 **/
static uint32_t static_PReaderUserCheckProperty(
         tContext* pContext,
         uint8_t nConnectionProperty)
{
   uint32_t nDriverProtocol = 0;
   uint32_t nIndex;
   bool_t bFound = W_FALSE;

   for(nIndex = 0; nIndex < P_READER_PROPERTY_MAX_NUMBER; nIndex++)
   {
      if(g_aPReaderUserTypeArray[nIndex].nConnectionProperty == nConnectionProperty)
      {
         nDriverProtocol = g_aPReaderUserTypeArray[nIndex].nDriverProtocol;
         bFound = W_TRUE;
         break;
      }
   }

   if(bFound == W_FALSE)
   {
      PDebugWarning("static_PReaderUserCheckProperty: Illegal Property 0x%02X (%s)",
          nConnectionProperty, PUtilTraceConnectionProperty(nConnectionProperty));

      CDebugAssert(nDriverProtocol == 0);
      nDriverProtocol = 0xFFFFFFFF;
   }
   else
   {
      if(PReaderIsPropertySupported(pContext, nConnectionProperty) == W_FALSE)
      {
         PDebugWarning("static_PReaderUserCheckProperty: Unsupported Property %s",
             PUtilTraceConnectionProperty(nConnectionProperty));

         nDriverProtocol = 0;
      }
   }

   return nDriverProtocol;
}

/**
 * @brief   Checks the property array values and builds the driver protocol value.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pSourcePropertyArray  The source property array.
 *
 * @param[in]  nInitialSourcePropertyNumber  The number of properties in the source property array.
 *
 * @param[out] pDestPropertyArray  The destination property array.
 *
 * @param[out] pnDestPropertyNumber  A pointer on a variable valued with the
 *             number of properties in the destination property array.
 *
 * @param[out] pnDriverProtocol  A pointer on a variable valued with the driver protocols.
 *
 * @return  W_SUCCESS if the property is supported, an error otherwise.
 **/
static W_ERROR static_PReaderUserCheckPropertyArray(
            tContext* pContext,
            const uint8_t* pSourcePropertyArray,
            uint32_t nInitialSourcePropertyNumber,
            uint8_t* pDestPropertyArray,
            uint32_t* pnDestPropertyNumber,
            uint32_t* pnDriverProtocol )
{
   uint32_t nDestPropertyNumber = 0;
   uint32_t nDriverProtocol = 0;
   uint32_t nSourcePropertyNumber = nInitialSourcePropertyNumber;

   CDebugAssert(pDestPropertyArray != null);
   CDebugAssert(pnDestPropertyNumber != null);
   CDebugAssert(pnDriverProtocol != null);

   if(((pSourcePropertyArray == null) && (nSourcePropertyNumber != 0))
   || ((pSourcePropertyArray != null) && (nSourcePropertyNumber == 0)))
   {
      PDebugError("static_PReaderUserCheckPropertyArray: Bad null value for the property array or the length");
      *pnDestPropertyNumber = 0;
      *pnDriverProtocol = 0;
      return W_ERROR_BAD_PARAMETER;
   }

   if(pSourcePropertyArray == null)
   {
      PDebugTrace("static_PReaderUserCheckPropertyArray: Registration for every protocol");
      nSourcePropertyNumber = P_READER_PROPERTY_MAX_NUMBER;
   }

   CDebugAssert(nSourcePropertyNumber != 0);

   do
   {
      uint8_t nConnectionProperty;

      if(pSourcePropertyArray != null)
      {
         nConnectionProperty = pSourcePropertyArray[--nSourcePropertyNumber];

         if((nConnectionProperty == W_PROP_BPRIME) && (nInitialSourcePropertyNumber != 1)) /* Ignore this property */
         {
            nConnectionProperty = W_PROP_ISO_7816_4;
         }
      }
      else
      {
         nConnectionProperty = g_aPReaderUserTypeArray[--nSourcePropertyNumber].nConnectionProperty;

         if ((nConnectionProperty == W_PROP_ISO_7816_4_A) || (nConnectionProperty == W_PROP_ISO_7816_4_B))
         {
            continue;
         }

         if(nConnectionProperty == W_PROP_BPRIME) /* Ignore this property */
         {
            nConnectionProperty = W_PROP_ISO_7816_4;
         }
      }

      if(nConnectionProperty == W_PROP_ISO_7816_4)
      {
         /* Ignore this property */
      }
      else if(static_PReaderUserSearchPropertyInRequestedList(
                  nConnectionProperty, pDestPropertyArray, nDestPropertyNumber) != W_FALSE)
      {
         PDebugWarning("static_PReaderUserCheckPropertyArray: The property %s is already in the list",
               PUtilTraceConnectionProperty(nConnectionProperty));
      }
      else
      {
         uint32_t nTempProtocol = static_PReaderUserCheckProperty(pContext, nConnectionProperty);

         if(nTempProtocol == 0xFFFFFFFF)
         {
            PDebugError("static_PReaderUserCheckPropertyArray: Invalid property requested");
            *pnDestPropertyNumber = 0;
            *pnDriverProtocol = 0;
            return W_ERROR_BAD_PARAMETER;
         }

         if(nTempProtocol != 0)
         {
            nDriverProtocol |= nTempProtocol;
            pDestPropertyArray[nDestPropertyNumber++] = nConnectionProperty;

            PDebugTrace("static_PReaderUserCheckPropertyArray: Adding the property %s",
               PUtilTraceConnectionProperty(nConnectionProperty));
         }

         /* Special case ISO 7816 */
          if((nConnectionProperty == W_PROP_ISO_14443_4_A)
         || (nConnectionProperty == W_PROP_ISO_14443_4_B))
         {
            if(static_PReaderUserSearchPropertyInRequestedList(
                        W_PROP_ISO_7816_4, pDestPropertyArray, nDestPropertyNumber) == W_FALSE)
            {
               pDestPropertyArray[nDestPropertyNumber++] = W_PROP_ISO_7816_4;
               PDebugTrace("static_PReaderUserCheckPropertyArray: Spontaneously adding the property W_PROP_ISO_7816_4");
            }

            if (nConnectionProperty == W_PROP_ISO_14443_4_A)
            {
               pDestPropertyArray[nDestPropertyNumber++] = W_PROP_ISO_7816_4_A;
               PDebugTrace("static_PReaderUserCheckPropertyArray: Spontaneously adding the property W_PROP_ISO_7816_4_A");

            }
            else
            {
               pDestPropertyArray[nDestPropertyNumber++] = W_PROP_ISO_7816_4_B;
               PDebugTrace("static_PReaderUserCheckPropertyArray: Spontaneously adding the property W_PROP_ISO_7816_4_B");
            }

         }
      }
   } while(nSourcePropertyNumber > 0);

   if(nDriverProtocol == 0)
   {
      PDebugWarning("static_PReaderUserCheckPropertyArray: No protocol remaining for the registration");
   }
   else
   {
      PDebugTrace("static_PReaderUserCheckPropertyArray: The driver protocols are %s",
            PUtilTraceReaderProtocol(pContext, nDriverProtocol));
   }

   *pnDestPropertyNumber = nDestPropertyNumber;
   *pnDriverProtocol = nDriverProtocol;
   return W_SUCCESS;
}

/* See PReaderListenToCardDetection */
W_ERROR PReaderUserListenToCardDetection(
            tContext* pContext,
            tPReaderCardDetectionHandler *pHandler,
            void *pHandlerParameter,
            uint8_t nPriority,
            const uint8_t* pConnectionPropertyArray,
            uint32_t nPropertyNumber,
            const void* pDetectionConfigurationBuffer,
            uint32_t nDetectionConfigurationBufferLength,
            W_HANDLE *phEventRegistry )
{
   tReaderUserListener* pUserListener = null;
   W_ERROR nError;
   W_HANDLE hUserListenerHandle = W_NULL_HANDLE;
   uint32_t nDriverProtocol;

   PDebugTrace("PReaderUserListenToCardDetection()");

   /* Check the parameters */
   if ( phEventRegistry == null )
   {
      PDebugError("PReaderUserListenToCardDetection: phEventRegistry == null");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   if ( ((pConnectionPropertyArray == null) && (nPropertyNumber != 0)) ||
        ((pConnectionPropertyArray != null) && (nPropertyNumber == 0)))
   {
      PDebugError("PReaderUserListenToCardDetection: inconsistency between pDetectionConfigurationBuffer and  nDetectionConfigurationBufferLength");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Create the reader structure */
   pUserListener = (tReaderUserListener*)CMemoryAlloc( sizeof(tReaderUserListener) );
   if ( pUserListener == null )
   {
      PDebugError("PReaderUserListenToCardDetection: pUserListener == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }
   CMemoryFill(pUserListener, 0, sizeof(tReaderUserListener));

   pUserListener->hDriverListener = W_NULL_HANDLE;

   /* Check the property array */
   if ( ( nError = static_PReaderUserCheckPropertyArray(
                     pContext, pConnectionPropertyArray, nPropertyNumber,
                     pUserListener->aPropertyArray,
                     &pUserListener->nPropertyNumber,
                     &nDriverProtocol ) ) != W_SUCCESS )
   {
      PDebugWarning("PReaderUserListenToCardDetection: unknown/unsupported property");
      goto return_error;
   }

   /* Special case for the B Prime protocol: Must have a configuration for the APGEN */
   if((pUserListener->nPropertyNumber == 1)
   && (pUserListener->aPropertyArray[0] == W_PROP_BPRIME)
   && (pDetectionConfigurationBuffer == null))
   {
      pUserListener->nPropertyNumber = 0;
      nDriverProtocol = 0;
   }

   /* Get a reader handle */
   if ( ( nError = PHandleRegister(
                     pContext,
                     pUserListener,
                     P_HANDLE_TYPE_READER_USER_LISTENER,
                     &hUserListenerHandle) ) != W_SUCCESS )
   {
      PDebugError("PReaderUserListenToCardDetection: error on PHandleRegister()");
      goto return_error;
   }

   /* Store the reader information */
   *phEventRegistry = hUserListenerHandle;

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pHandler,
      pHandlerParameter,
      &pUserListener->sCallbackContext );

   /*use aCardToReaderBuffer to copy parameters*/
   if((pDetectionConfigurationBuffer != null) && (nDetectionConfigurationBufferLength != 0))
   {
      CMemoryCopy(
         pUserListener->aCardToReaderBuffer,
         pDetectionConfigurationBuffer, nDetectionConfigurationBufferLength);
   }
   else
   {
      /* reset the buffer*/
      pUserListener->aCardToReaderBuffer[0] = 0;
   }

   /* Register with the driver, if the protocol list is non-empty */
   if (nDriverProtocol != 0)
   {
      /* Register the protocol(s) */
      nError = PReaderDriverRegister(
                  pContext,
                  static_PReaderUserCardDetectionHandler,
                  pUserListener,
                  nPriority,
                  nDriverProtocol,
                  nDetectionConfigurationBufferLength,
                  pUserListener->aCardToReaderBuffer,
                  NAL_MESSAGE_MAX_LENGTH,
                  &pUserListener->hDriverListener );

      /* Check the result */
      if ( nError != W_SUCCESS )
      {
         PDebugError(
            "PReaderUserListenToCardDetection: PReaderDriverRegister() returned an error");
         goto return_error;
      }
   }

   return W_SUCCESS;

return_error:

   PDebugError("PReaderUserListenToCardDetection: Returning %s",
      PUtilTraceError(nError) );

   if ( phEventRegistry != null )
   {
      *phEventRegistry = W_NULL_HANDLE;
   }

   if(hUserListenerHandle != W_NULL_HANDLE)
   {
      /* Close the user registry */
      PHandleClose( pContext, hUserListenerHandle );
   }
   else
   {
      if (pUserListener != null)
      {
         PHandleClose( pContext, pUserListener->hDriverListener );

         CMemoryFree( pUserListener );
      }
   }

   return nError;
}

/* See Client API Specifications */
W_ERROR PReaderListenToCardDetection(
                  tContext * pContext,
                  tPReaderCardDetectionHandler* pHandler,
                  void* pHandlerParameter,
                  uint8_t nPriority,
                  const uint8_t* pConnectionPropertyArray,
                  uint32_t nPropertyNumber,
                  W_HANDLE* phEventRegistry)
{
   PDebugTrace("PReaderListenToCardDetection()");

   /* Do not allow user to specific priority of the SE */

   if ((nPriority == W_PRIORITY_SE) || (nPriority == W_PRIORITY_SE_FORCED))
   {
      return W_ERROR_BAD_PARAMETER;
   }

   return PReaderUserListenToCardDetection(
            pContext,
            pHandler, pHandlerParameter,
            nPriority,
            pConnectionPropertyArray, nPropertyNumber,
            null, 0,
            phEventRegistry );
}

/* See Client API Specifications */
void PReaderHandlerWorkPerformed(
         tContext * pContext,
         W_HANDLE hUserConnection,
         bool_t bGiveToNextListener,
         bool_t bCardApplicationMatch )
{
   tReaderUserConnection* pUserConnection;
   W_ERROR nError = PHandleGetObject(pContext, hUserConnection, P_HANDLE_TYPE_READER_USER_CONNECTION, (void**)&pUserConnection);

   PDebugTrace("PReaderHandlerWorkPerformed: hUserConnection=0x%08X", hUserConnection);

   /* If the connection has been found */
   if ( ( nError == W_SUCCESS ) && ( pUserConnection != null ) )
   {
      /* Commit the user cache, if needed */
      PCacheConnectionUserCommit(pContext);

      /* Call the driver function */
      PReaderDriverWorkPerformed(
         pContext,
         pUserConnection->hDriverConnection,
         bGiveToNextListener,
         bCardApplicationMatch );

      /* @todo what can we do if the IOCTL failed */

      /* hDriverConnection is closed */
      pUserConnection->hDriverConnection = W_NULL_HANDLE;

      /* Close handle */
      PHandleClose(pContext, hUserConnection );
   }
   else
   {
      PDebugError("PReaderHandlerWorkPerformed: could not get pUserConnection buffer");
   }
}

/* Polling timer callback */
static void static_PReaderNotifyTimerExpiration(tContext * pContext,
                                                void * pCallbackParameter);

/* Polling completion callback */
static void static_PReaderPollingCommandCompleted(
                  tContext * pContext,
                  void * pCallbackParameter,
                  W_ERROR nResult)
{
   tReaderUserConnection * pUserConnection = (tReaderUserConnection *) pCallbackParameter;

   PDebugTrace("static_PReaderPollingCommandCompleted");

   switch (pUserConnection->nStatus)
   {
   case P_READER_CONNECTION_POLLING_PENDING:
      pUserConnection->nStatus = P_READER_CONNECTION_ACTIVE;

      if (pUserConnection->pCardRemovalListener != null)
      {
         /* If they are something wrong with the communication */
         if( (nResult == W_ERROR_RF_COMMUNICATION) ||
             (nResult == W_ERROR_RF_PROTOCOL_NOT_SUPPORTED) ||
             (nResult == W_ERROR_TIMEOUT))
         {
            PDebugTrace("static_PReaderPollingCommandCompleted : CARD REMOVED");
            /* Call event callback */
            PDFCPostContext2(
                  &pUserConnection->pCardRemovalListener->sCallbackContext,
                  W_READER_EVT_CARD_REMOVED);
         }
         else
         {
            /* Start timer */
            PMultiTimerSetDriver(
                     pContext,
                     TIMER_T15_CARD_REMOVAL_DETECTION,
                     PNALServiceDriverGetCurrentTime(pContext) + P_READER_CARD_REMOVAL_DETECTION_TIMEOUT,
                     static_PReaderNotifyTimerExpiration,
                     (void*)PUtilConvertHandleToPointer(pUserConnection->pCardRemovalListener->hUserConnection));
         }
      }
      break;

   case P_READER_CONNECTION_POLLING_PENDING_EXCHANGE_QUEUED:
      /* Check pExecuteQueuedExchange */
      if (pUserConnection->pExecuteQueuedExchange == null)
      {
         PDebugError("static_PReaderPollingCommandCompleted : pExecuteQueuedExchange is null!");
         pUserConnection->nStatus = P_READER_CONNECTION_POLLING_PENDING;
         static_PReaderPollingCommandCompleted(pContext, pCallbackParameter, nResult);
         return;
      }

      if (nResult == W_SUCCESS)
      {
         pUserConnection->nStatus = P_READER_CONNECTION_EXCHANGE_PENDING;
         /* Execute queued operation */
         pUserConnection->pExecuteQueuedExchange(pContext, pUserConnection->pExchangeParameter, W_SUCCESS);
      }
      else
      {
         pUserConnection->nStatus = P_READER_CONNECTION_ACTIVE;

         if(pUserConnection->pCardRemovalListener != null)
         {
            PDebugTrace("static_PReaderPollingCommandCompleted : CARD REMOVED");
            /* Call event callback */
            PDFCPostContext2(
                  &pUserConnection->pCardRemovalListener->sCallbackContext,
                  W_READER_EVT_CARD_REMOVED);
         }

         /* Report error in queued operation callback */
         pUserConnection->pExecuteQueuedExchange(pContext, pUserConnection->pExchangeParameter, W_ERROR_TIMEOUT);
      }

      /* Clear pExecuteQueuedExchange */
      pUserConnection->pExecuteQueuedExchange = null;
      break;
   }

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, pUserConnection);
}

/* Polling timer callback */
static void static_PReaderNotifyTimerExpiration(tContext * pContext,
                                                void * pCallbackParameter)
{
   tReaderUserConnection* pUserConnection = null;

   W_HANDLE hConnection = PUtilConvertPointerToHandle(pCallbackParameter);

   /* Retrieve the user connection object */
   W_ERROR nError = PHandleGetObject(pContext, hConnection, P_HANDLE_TYPE_READER_USER_CONNECTION, (void**)&pUserConnection);
   if ( (pUserConnection == null) || (nError != W_SUCCESS) )
   {
      PDebugError("static_PReaderNotifyTimerExpiration: could not get pUserConnection buffer");
      return;
   }

   if ( (pUserConnection->nStatus == P_READER_CONNECTION_ACTIVE) && (pUserConnection->pCardRemovalListener != null) )
   {
      pUserConnection->nStatus = P_READER_CONNECTION_POLLING_PENDING;

      /* Increment the reference count to keep the connection object alive during the polling.
         The reference count is decreased in static_PReaderPollingCommandCompleted() when the polling is completed */
      PHandleIncrementReferenceCount(pUserConnection);

      /* Send polling command */
      PHandlePoll(pContext, pUserConnection->pCardRemovalListener->hUserConnection, static_PReaderPollingCommandCompleted, pUserConnection);
   }
}

/* see PReaderNotifyExchange */
static W_ERROR static_PReaderNotifyExchangeEx(
         tContext * pContext,
         tReaderUserConnection* pUserConnection,
         tReaderExecuteQueuedExchange* pExecuteQueuedExchange,
         void* pExchangeParameter)
{
   if (pUserConnection == null)
   {
      return W_ERROR_BAD_HANDLE;
   }

   switch (pUserConnection->nStatus)
   {
   case P_READER_CONNECTION_ACTIVE:
      if (pUserConnection->pCardRemovalListener != null)
      {
         /* Stop timer */
         PMultiTimerCancelDriver(pContext, TIMER_T15_CARD_REMOVAL_DETECTION);
      }

      pUserConnection->nStatus = P_READER_CONNECTION_EXCHANGE_PENDING;

      return W_SUCCESS;

   case P_READER_CONNECTION_POLLING_PENDING:
      CDebugAssert(pUserConnection->pExecuteQueuedExchange == null);

      if (pExecuteQueuedExchange == null)
      {
         PDebugError("static_PReaderNotifyExchangeEx : Bad state. pExecuteQueuedExchange is null!");
         return W_ERROR_BAD_STATE;
      }

      pUserConnection->pExecuteQueuedExchange = pExecuteQueuedExchange;
      pUserConnection->pExchangeParameter = pExchangeParameter;

      pUserConnection->nStatus = P_READER_CONNECTION_POLLING_PENDING_EXCHANGE_QUEUED;

      return W_ERROR_OPERATION_PENDING;

   case P_READER_CONNECTION_INITIALIZATION:
   case P_READER_CONNECTION_EXCHANGE_PENDING:
   case P_READER_CONNECTION_POLLING_PENDING_EXCHANGE_QUEUED:
   default:
      return W_ERROR_BAD_STATE;
   }
}

/* see header */
bool_t PReaderCheckConnection(
                  tContext * pContext,
                  W_HANDLE hUserConnection)
{
   void* pObject;
   W_ERROR nError = PHandleGetConnectionObject(pContext, hUserConnection, P_HANDLE_TYPE_READER_USER_CONNECTION, &pObject);

   if ((nError != W_SUCCESS) || (pObject == null))
   {
      return W_FALSE;
   }

   return W_TRUE;
}

/* see header */
W_ERROR PReaderNotifyExchange(
                  tContext * pContext,
                  W_HANDLE hUserConnection,
                  tReaderExecuteQueuedExchange* pExecuteQueuedExchange,
                  void* pExchangeParameter)
{
   tReaderUserConnection* pUserConnection;

   /* Retrieve the user connection object */
   W_ERROR nError = PHandleGetObject(pContext, hUserConnection, P_HANDLE_TYPE_READER_USER_CONNECTION, (void**)&pUserConnection);
   if (pUserConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      PDebugError("PReaderNotifyExchange: could not get pUserConnection buffer");
      return nError;
   }

   return static_PReaderNotifyExchangeEx(pContext, pUserConnection, pExecuteQueuedExchange, pExchangeParameter);
}

/* see header */
static void static_PReaderNotifyExchangeCompletionEx(
         tContext * pContext,
         tReaderUserConnection* pUserConnection)
{
   if (pUserConnection == null)
   {
      return;
   }

   if (pUserConnection->nStatus == P_READER_CONNECTION_EXCHANGE_PENDING)
   {
      pUserConnection->nStatus = P_READER_CONNECTION_ACTIVE;

      if (pUserConnection->pCardRemovalListener != null)
      {
         /* Start timer */
         PMultiTimerSetDriver(
                  pContext,
                  TIMER_T15_CARD_REMOVAL_DETECTION,
                  PNALServiceDriverGetCurrentTime(pContext) + P_READER_CARD_REMOVAL_DETECTION_TIMEOUT,
                  static_PReaderNotifyTimerExpiration,
                  PUtilConvertHandleToPointer(pUserConnection->pCardRemovalListener->hUserConnection));
      }
   }
}

void PReaderNotifyExchangeCompletion(tContext * pContext,
                                     W_HANDLE hUserConnection)
{
   tReaderUserConnection* pUserConnection;

   /* Retrieve the user connection object */
   W_ERROR nError = PHandleGetObject(pContext, hUserConnection, P_HANDLE_TYPE_READER_USER_CONNECTION, (void**)&pUserConnection);
   if (pUserConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
   }
   if (nError != W_SUCCESS)
   {
      PDebugError("PReaderNotifyExchangeCompletion: could not get pUserConnection buffer");
      return;
   }

   static_PReaderNotifyExchangeCompletionEx(pContext, pUserConnection);
}

/* See Client API Specifications */
W_ERROR PReaderListenToCardRemovalDetection(
                  tContext * pContext,
                  W_HANDLE hConnection,
                  tPBasicGenericEventHandler *pEventHandler,
                  void* pCallbackParameter,
                  W_HANDLE* phEventRegistry)
{
   tReaderUserConnection* pUserConnection;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDebugTrace("PReaderListenToCardRemovalDetection");

   /* Check the parameters */
   if (phEventRegistry == null)
   {
      PDebugError("PReaderListenToCardRemovalDetection: phEventRegistry == null");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Set callback context */
   PDFCFillCallbackContext(pContext, (tDFCCallback*)pEventHandler, pCallbackParameter, &sCallbackContext);
   if (sCallbackContext.pFunction == null)
   {
      PDebugError("PReaderListenToCardRemovalDetection: pEventHandler == null");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Get the user connection object */
   nError = PHandleGetObject(pContext, hConnection, P_HANDLE_TYPE_READER_USER_CONNECTION, (void**)&pUserConnection);
   if(nError != W_SUCCESS)
   {
      goto return_error;
   }
   /* Check the connection */
   if (pUserConnection == null)
   {
      nError = W_ERROR_BAD_HANDLE;
      goto return_error;
   }
   /* Check the connection's status */
   if ((pUserConnection->nStatus != P_READER_CONNECTION_ACTIVE) &&
       (pUserConnection->nStatus != P_READER_CONNECTION_EXCHANGE_PENDING))
   {
      nError = W_ERROR_BAD_STATE;
      goto return_error;
   }

   /* if the tag removal detection is already activated */
   if (pUserConnection->pCardRemovalListener != null)
   {
      nError = W_ERROR_EXCLUSIVE_REJECTED;
      goto return_error;
   }

   /* if we cannot polled this reader's type */
   if (PHandleIsPollingSupported(pContext, hConnection) == W_FALSE)
   {
      nError = W_ERROR_CONNECTION_COMPATIBILITY;
      goto return_error;
   }

   /* Create the reader structure */
   pUserConnection->pCardRemovalListener = (tReaderUserCardRemovalListener*) CMemoryAlloc(sizeof(tReaderUserCardRemovalListener));
   if (pUserConnection->pCardRemovalListener == null)
   {
      PDebugError("PReaderListenToCardRemovalDetection: pCardRemovalListener == null");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }
   CMemoryFill(pUserConnection->pCardRemovalListener, 0, sizeof(tReaderUserCardRemovalListener));

   /* Get a handle */
   if ( ( nError = PHandleRegister(
                     pContext,
                     pUserConnection->pCardRemovalListener,
                     P_HANDLE_TYPE_READER_USER_CARD_REMOVAL_DETECTION,
                     phEventRegistry) ) != W_SUCCESS )
   {
      PDebugError("PReaderListenToCardRemovalDetection: error on PHandleRegister()");
      CMemoryFree(pUserConnection->pCardRemovalListener);
      pUserConnection->pCardRemovalListener = null;
      goto return_error;
   }

   /* Store the callback context */
   pUserConnection->pCardRemovalListener->sCallbackContext = sCallbackContext;

   /* Store the listener handle */
   pUserConnection->pCardRemovalListener->hListener = *phEventRegistry;

   /* Store the user connection handle */
   pUserConnection->pCardRemovalListener->hUserConnection = hConnection;

   /* Increment the reference count to keep the connection object alive.
      The reference count is decreased in static_PReaderUserCardRemovalListenerDestroy() */
   PHandleIncrementReferenceCount(pUserConnection);

   if (pUserConnection->nStatus == P_READER_CONNECTION_ACTIVE)
   {
      /* Start timer */
      PMultiTimerSetDriver(
               pContext,
               TIMER_T15_CARD_REMOVAL_DETECTION,
               PNALServiceDriverGetCurrentTime(pContext) + P_READER_CARD_REMOVAL_DETECTION_TIMEOUT,
               static_PReaderNotifyTimerExpiration,
               PUtilConvertHandleToPointer(pUserConnection->pCardRemovalListener->hUserConnection));
   }

   return W_SUCCESS;

return_error:
   if (phEventRegistry != null)
   {
      *phEventRegistry = W_NULL_HANDLE;
   }

   return nError;
}

/* See Client API Specifications */
W_ERROR PReaderGetIdentifier(
         tContext * pContext,
         W_HANDLE hConnection,
         uint8_t* pIdentifierBuffer,
         uint32_t nIdentifierBufferMaxLength,
         uint32_t* pnIdentifierActualLength)
{
   return PHandleGetIdentifier(
         pContext,
         hConnection,
         pIdentifierBuffer, nIdentifierBufferMaxLength,
         pnIdentifierActualLength);
}

/* Callback for PReaderExchangeData */
static void static_PReaderExchangeDataCompleted(
         tContext* pContext,
         void * pCallbackParameter,
         uint32_t nDataLength,
         W_ERROR nResult)
{
   tReaderUserConnection* pUserConnection = (tReaderUserConnection*) pCallbackParameter;

   if (pUserConnection->hOperation != W_NULL_HANDLE)
   {
      /* Check operation status */
      if ( (nResult == W_SUCCESS) && (PBasicGetOperationState(pContext, pUserConnection->hOperation) == P_OPERATION_STATE_CANCELLED) )
      {
         PDebugWarning("static_PReaderExchangeDataCompleted: operation cancelled");
         nResult = W_ERROR_CANCEL;
      }

      /* Close operation */
      PBasicSetOperationCompleted(pContext, pUserConnection->hOperation);
      PHandleClose(pContext, pUserConnection->hOperation);
      pUserConnection->hOperation = W_NULL_HANDLE;
   }

   /* Send the error */
   PDFCPostContext3(&pUserConnection->sCallbackContext, nDataLength, nResult);

   /* Manage user connection status and polling */
   static_PReaderNotifyExchangeCompletionEx(pContext, pUserConnection);

   /* Decrement the reference count of the connection. This may destroy the object */
   PHandleDecrementReferenceCount(pContext, pUserConnection);
}

/* Function to execute the queued ExchangeData after polling */
static void static_PReaderExecuteQueuedExchangeData(
         tContext * pContext,
         void* pObject,
         W_ERROR nResult)
{
   tReaderUserConnection* pUserConnection = (tReaderUserConnection*) pObject;
   W_HANDLE hOperation = W_NULL_HANDLE;

   PDebugTrace("static_PPicoExecuteQueuedExchange");

   /* Restore operation handle */
   pUserConnection->hOperation = pUserConnection->sQueuedExchangeData.hOperation;
   /* Restore callback context */
   pUserConnection->sCallbackContext = pUserConnection->sQueuedExchangeData.sCallbackContext;

   /* Check operation status */
   if ( (pUserConnection->hOperation != W_NULL_HANDLE) &&
        (nResult == W_SUCCESS) &&
        (PBasicGetOperationState(pContext, pUserConnection->hOperation) == P_OPERATION_STATE_CANCELLED) )
   {
      PDebugWarning("static_PReaderExecuteQueuedExchangeData: operation cancelled");
      nResult = W_ERROR_CANCEL;
   }

   if (nResult != W_SUCCESS)
   {
      /* If an error has been detected during the polling, return directly */
      static_PReaderExchangeDataCompleted(pContext, pUserConnection, 0, nResult);
   }
   else
   {
      /* Exchange data */
      PHandleExchangeData(
            pContext,
            pUserConnection->sQueuedExchangeData.hUserConnection,
            pUserConnection->sQueuedExchangeData.nPropertyIdentifier,
            static_PReaderExchangeDataCompleted, pUserConnection,
            pUserConnection->sQueuedExchangeData.pReaderToCardBuffer, pUserConnection->sQueuedExchangeData.nReaderToCardBufferLength,
            pUserConnection->sQueuedExchangeData.pCardToReaderBuffer, pUserConnection->sQueuedExchangeData.nCardToReaderBufferMaxLength,
            (pUserConnection->hOperation != W_NULL_HANDLE) ? &hOperation : null);

      /* Add the sub operation if needed */
      if( (pUserConnection->hOperation != W_NULL_HANDLE) && (hOperation != W_NULL_HANDLE) )
      {
         if(PBasicAddSubOperationAndClose(pContext, pUserConnection->hOperation, hOperation) != W_SUCCESS)
         {
            PDebugError("PReaderExchangeData: error returned by PBasicAddSubOperationAndClose()");
         }
      }
   }

   /* Reset queued data */
   CMemoryFill(&pUserConnection->sQueuedExchangeData, 0, sizeof(pUserConnection->sQueuedExchangeData));
}

/* See header */
void PReaderExchangeDataInternal(
         tContext * pContext,
         W_HANDLE hConnection,
         tPBasicGenericDataCallbackFunction* pCallback,
         void* pCallbackParameter,
         const uint8_t* pReaderToCardBuffer,
         uint32_t nReaderToCardBufferLength,
         uint8_t* pCardToReaderBuffer,
         uint32_t nCardToReaderBufferMaxLength,
         W_HANDLE* phOperation )
{
   PHandleExchangeData(
         pContext,
         hConnection,
         W_PROP_ANY,
         pCallback, pCallbackParameter,
         pReaderToCardBuffer, nReaderToCardBufferLength,
         pCardToReaderBuffer, nCardToReaderBufferMaxLength,
         phOperation);
}

/* See Client API Specifications */
void PReaderExchangeDataEx(
         tContext * pContext,
         W_HANDLE hConnection,
         uint8_t nPropertyIdentifier,
         tPBasicGenericDataCallbackFunction* pCallback,
         void* pCallbackParameter,
         const uint8_t* pReaderToCardBuffer,
         uint32_t nReaderToCardBufferLength,
         uint8_t* pCardToReaderBuffer,
         uint32_t nCardToReaderBufferMaxLength,
         W_HANDLE* phOperation )
{
   tReaderUserConnection* pUserConnection;
   tDFCCallbackContext sCallbackContext;
   W_HANDLE hOperation = W_NULL_HANDLE;
   W_ERROR nResult;

   /* Build callback context */
   PDFCFillCallbackContext(pContext, (tDFCCallback*) pCallback, pCallbackParameter, &sCallbackContext);

   /* Retrieve the user connection object */
   nResult = PHandleGetObject(pContext, hConnection, P_HANDLE_TYPE_READER_USER_CONNECTION, (void**)&pUserConnection);
   if (pUserConnection == null)
   {
      /* This is not a READER_USER_CONNECTION (for example, a SE_USER_CONNECTION or LogicalChannel),
         use directly PHandleExchangeData() */
      PHandleExchangeData(
            pContext,
            hConnection,
            nPropertyIdentifier,
            pCallback, pCallbackParameter,
            pReaderToCardBuffer, nReaderToCardBufferLength,
            pCardToReaderBuffer, nCardToReaderBufferMaxLength,
            phOperation);

      return;
   }

   /* Check parameters */
   if ( ((pReaderToCardBuffer == null) && (nReaderToCardBufferLength != 0)) ||
        ((pCardToReaderBuffer == null) && (nCardToReaderBufferMaxLength != 0)) )
   {
      PDebugError("PReaderExchangeData: null parameters");
      nResult = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Get an operation handle */
   if (phOperation != null)
   {
      if ((*phOperation = PBasicCreateOperation(pContext, null, null)) == W_NULL_HANDLE)
      {
         PDebugError("PReaderExchangeData: Cannot allocate the operation");
         nResult = W_ERROR_OUT_OF_RESOURCE;
         goto return_error;
      }

      /* Duplicate the handle to be referenced internally and in the returned handle */
      nResult = PHandleDuplicate(pContext, *phOperation, &hOperation);
      if(nResult != W_SUCCESS)
      {
         PDebugError("PReaderExchangeData: Error returned by PHandleDuplicate()");
         PHandleClose(pContext, *phOperation);
         goto return_error;
      }
   }

   /* Notify this exchange to manage user connection status and polling */
   nResult = static_PReaderNotifyExchangeEx(pContext, pUserConnection, static_PReaderExecuteQueuedExchangeData, pUserConnection);

   switch (nResult)
   {
   case W_SUCCESS:
      /* Continue this operation */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the static_PReaderExchangeDataCompleted when the operation is completed */
      PHandleIncrementReferenceCount(pUserConnection);

      /* Store the operation handle */
      CDebugAssert(pUserConnection->hOperation == W_NULL_HANDLE);
      pUserConnection->hOperation = hOperation;

      /* Store the callback context */
      pUserConnection->sCallbackContext = sCallbackContext;

      /* Exchange data */
      PHandleExchangeData(
            pContext,
            hConnection,
            nPropertyIdentifier,
            static_PReaderExchangeDataCompleted, pUserConnection,
            pReaderToCardBuffer, nReaderToCardBufferLength,
            pCardToReaderBuffer, nCardToReaderBufferMaxLength,
            (pUserConnection->hOperation != W_NULL_HANDLE) ? &hOperation : null);

      /* Add the sub operation if needed */
      if( (pUserConnection->hOperation != W_NULL_HANDLE) && (hOperation != W_NULL_HANDLE) )
      {
         if(PBasicAddSubOperationAndClose(pContext, pUserConnection->hOperation, hOperation) != W_SUCCESS)
         {
            PDebugError("PReaderExchangeData: error returned by PBasicAddSubOperationAndClose()");
         }
      }

      return;

   case W_ERROR_OPERATION_PENDING:
      /* A polling is pending. Save data to execute this operation after the polling completion. */

      /* Increment the ref count to avoid prematurely freeing during the operation
         The ref count will be decremented in the static_PReaderExchangeDataCompleted when the operation is completed */
      PHandleIncrementReferenceCount(pUserConnection);

      /* Save the user connection handle */
      pUserConnection->sQueuedExchangeData.hUserConnection = hConnection;

      /* Save the opration handle */
      CDebugAssert(pUserConnection->sQueuedExchangeData.hOperation == W_NULL_HANDLE);
      pUserConnection->sQueuedExchangeData.hOperation = hOperation;

      /* Save the callback context */
      pUserConnection->sQueuedExchangeData.sCallbackContext = sCallbackContext;

      /* Save data */
      pUserConnection->sQueuedExchangeData.pReaderToCardBuffer = pReaderToCardBuffer;
      pUserConnection->sQueuedExchangeData.nReaderToCardBufferLength = nReaderToCardBufferLength;
      pUserConnection->sQueuedExchangeData.pCardToReaderBuffer = pCardToReaderBuffer;
      pUserConnection->sQueuedExchangeData.nCardToReaderBufferMaxLength = nCardToReaderBufferMaxLength;

      pUserConnection->sQueuedExchangeData.nPropertyIdentifier = nPropertyIdentifier;

      return;

   default:
      /* Return this error */
      if ((phOperation != null) && (*phOperation != W_NULL_HANDLE))
      {
         PHandleClose(pContext, *phOperation);
      }
      goto return_error;
   }

return_error:
   PDebugError("PReaderExchangeData: returning error %s", PUtilTraceError(nResult));

   PDFCPostContext3(&sCallbackContext, 0, nResult);

   if( hOperation != W_NULL_HANDLE)
   {
      PHandleClose(pContext, hOperation);
   }

   if (phOperation != null)
   {
      *phOperation = W_NULL_HANDLE;
   }
}

/* See Client API Specifications */
W_ERROR WReaderExchangeDataExSync(
            W_HANDLE hConnection,
            uint8_t  nPropertyIdentifier,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            uint32_t* pnCardToReaderBufferActualLength )
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WReaderExchangeDataEx(
         hConnection,
         nPropertyIdentifier,
         PBasicGenericSyncCompletionUint32, &param,
         pReaderToCardBuffer, nReaderToCardBufferLength,
         pCardToReaderBuffer, nCardToReaderBufferMaxLength,
         null );
   }

   return PBasicGenericSyncWaitForResultUint32(&param, pnCardToReaderBufferActualLength);
}

void WReaderExchangeData(
                  W_HANDLE hConnection,
                  tWBasicGenericDataCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  const uint8_t* pReaderToCardBuffer,
                  uint32_t nReaderToCardBufferLength,
                  uint8_t* pCardToReaderBuffer,
                  uint32_t nCardToReaderBufferMaxLength,
                  W_HANDLE* phOperation )
{
   WReaderExchangeDataEx(hConnection,
      W_PROP_ANY,
      pCallback,
      pCallbackParameter,
      pReaderToCardBuffer,
      nReaderToCardBufferLength,
      pCardToReaderBuffer,
      nCardToReaderBufferMaxLength,
      phOperation);
}


/* See Client API Specifications */
bool_t PReaderIsPropertySupported(
         tContext * pContext,
         uint8_t nPropertyIdentifier )
{
   bool_t bValue;
   tNFCControllerInfo* pNFCControllerInfo = PContextGetNFCControllerInfo( pContext );
   uint32_t nProtocols = pNFCControllerInfo->nProtocolCapabilities;

#ifdef P_INCLUDE_PICOPASS
   uint32_t nCapabilities = pNFCControllerInfo->nFirmwareCapabilities;
#endif /* P_INCLUDE_PICOPASS */

   switch(nPropertyIdentifier)
   {
   case W_PROP_ISO_14443_3_B:
      bValue = ((nProtocols & W_NFCC_PROTOCOL_READER_ISO_14443_3_B) != 0)?W_TRUE:W_FALSE;
      break;
   case W_PROP_ISO_14443_4_B:
   case W_PROP_NFC_TAG_TYPE_4_B:
      bValue = ((nProtocols & W_NFCC_PROTOCOL_READER_ISO_14443_4_B) != 0)?W_TRUE:W_FALSE;
      break;
   case W_PROP_NFC_TAG_TYPE_6:
   case W_PROP_ISO_15693_3:
   case W_PROP_TI_TAGIT:
   case W_PROP_ST_LRI_512:
   case W_PROP_ST_LRI_2K:
   case W_PROP_NXP_ICODE:
      bValue = ((nProtocols & W_NFCC_PROTOCOL_READER_ISO_15693_3) != 0)?W_TRUE:W_FALSE;
      break;
   case W_PROP_NFC_TAG_TYPE_1:
   case W_PROP_TYPE1_CHIP:
   case W_PROP_JEWEL:
   case W_PROP_TOPAZ:
   case W_PROP_TOPAZ_512:
      bValue = ((nProtocols & W_NFCC_PROTOCOL_READER_TYPE_1_CHIP) != 0)?W_TRUE:W_FALSE;
      break;
   case W_PROP_NFC_TAG_TYPE_3:
   case W_PROP_FELICA:
      bValue = ((nProtocols & W_NFCC_PROTOCOL_READER_FELICA) != 0)?W_TRUE:W_FALSE;
      break;

   case W_PROP_NFC_TAG_TYPE_5:
   case W_PROP_PICOPASS_2K:
   case W_PROP_PICOPASS_32K:
#ifdef P_INCLUDE_PICOPASS
      if((nProtocols & W_NFCC_PROTOCOL_READER_ISO_14443_3_B) != 0)
      {
         bValue = ((nCapabilities & NAL_CAPA_READER_ISO_14443_B_PICO) != 0)?W_TRUE:W_FALSE;
      }
      else
      {
         bValue = W_FALSE;
      }
#else /* P_INCLUDE_PICOPASS */
      bValue = W_FALSE;
#endif /* P_INCLUDE_PICOPASS */
      break;

   case W_PROP_ISO_15693_2:
   case W_PROP_ICLASS_2K:
   case W_PROP_ICLASS_16K:
      bValue = ((nProtocols & W_NFCC_PROTOCOL_READER_ISO_15693_2) != 0)?W_TRUE:W_FALSE;
      break;
   case W_PROP_ISO_14443_3_A:
   case W_PROP_NFC_TAG_TYPE_2:
   case W_PROP_NFC_TAG_TYPE_2_GENERIC:
   case W_PROP_MY_D_MOVE:
   case W_PROP_MY_D_NFC:
   case W_PROP_KOVIO_RFID:
   case W_PROP_MIFARE_UL:
   case W_PROP_MIFARE_UL_C:
   case W_PROP_MIFARE_MINI:  /* should use W_NFCC_PROTOCOL_READER_MIFARE_CLASSIC */
   case W_PROP_MIFARE_1K:    /* should use W_NFCC_PROTOCOL_READER_MIFARE_CLASSIC */
   case W_PROP_MIFARE_4K:    /* should use W_NFCC_PROTOCOL_READER_MIFARE_CLASSIC */
   case W_PROP_MIFARE_PLUS_X_2K:  /* should use W_NFCC_PROTOCOL_READER_MIFARE_PLUS */
   case W_PROP_MIFARE_PLUS_X_4K:  /* should use W_NFCC_PROTOCOL_READER_MIFARE_PLUS */
   case W_PROP_MIFARE_PLUS_S_2K:  /* should use W_NFCC_PROTOCOL_READER_MIFARE_PLUS */
   case W_PROP_MIFARE_PLUS_S_4K:  /* should use W_NFCC_PROTOCOL_READER_MIFARE_PLUS */
      bValue = ((nProtocols & W_NFCC_PROTOCOL_READER_ISO_14443_3_A) != 0)?W_TRUE:W_FALSE;
      break;
   case W_PROP_ISO_14443_4_A:
   case W_PROP_NFC_TAG_TYPE_4_A:
   case W_PROP_MIFARE_DESFIRE_D40:
   case W_PROP_MIFARE_DESFIRE_EV1_2K:
   case W_PROP_MIFARE_DESFIRE_EV1_4K:
   case W_PROP_MIFARE_DESFIRE_EV1_8K:
      bValue = ((nProtocols & W_NFCC_PROTOCOL_READER_ISO_14443_4_A) != 0)?W_TRUE:W_FALSE;
      break;
   case W_PROP_BPRIME:
      bValue = ((nProtocols & W_NFCC_PROTOCOL_READER_BPRIME) != 0)?W_TRUE:W_FALSE;
      break;
   /* Special values not usable with detection filter */
   case W_PROP_ISO_7816_4:
      bValue = (((nProtocols & W_NFCC_PROTOCOL_READER_ISO_14443_4_A) != 0)
             || ((nProtocols & W_NFCC_PROTOCOL_READER_ISO_14443_4_B) != 0))?W_TRUE:W_FALSE;;
      break;
   case W_PROP_KOVIO:
      bValue = ((nProtocols & W_NFCC_PROTOCOL_READER_KOVIO) != 0)?W_TRUE:W_FALSE;
      break;

   case W_PROP_NFC_TAG_TYPE_7:
#ifdef P_INCLUDE_MIFARE_CLASSIC
      bValue = ((nProtocols & W_NFCC_PROTOCOL_READER_MIFARE_CLASSIC) != 0)?W_TRUE:W_FALSE;
#else /* P_INCLUDE_MIFARE_CLASSIC */
      bValue = W_FALSE;
#endif /* P_INCLUDE_MIFARE_CLASSIC */
      break;
   case W_PROP_SECURE_ELEMENT:
   case W_PROP_VIRTUAL_TAG:
   case W_PROP_P2P_TARGET:
   case W_PROP_ISO_7816_4_A:
   case W_PROP_ISO_7816_4_B:


   default:
      PDebugError("PReaderIsPropertySupported: Wrong property value %08X", nPropertyIdentifier);
      return W_FALSE;
   }

   return bValue;
}

/* See Client API Specifications */
bool_t PReaderPreviousApplicationMatch(
         tContext * pContext,
         W_HANDLE hUserConnection)
{
   tReaderUserConnection* pUserConnection;
   W_ERROR nError = PHandleGetObject(pContext, hUserConnection, P_HANDLE_TYPE_READER_USER_CONNECTION, (void**)&pUserConnection);

   PDebugTrace("PReaderPreviousApplicationMatch: hUserConnection=0x%08X", hUserConnection);

   if ( ( nError == W_SUCCESS ) && ( pUserConnection != null ) )
   {
      return pUserConnection->bPreviousCardApplicationMatch;
   }

   PDebugError("PReaderPreviousApplicationMatch: Bad handle value");

   return W_FALSE;
}

/* See Client API Specifications */
W_ERROR WReaderSetPulsePeriodSync(
                  uint32_t nPulsePeriod )
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WReaderSetPulsePeriod(PBasicGenericSyncCompletion, &param, nPulsePeriod);
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See header file */
W_ERROR PReaderUserGetConnectionObject(
            tContext* pContext,
            W_HANDLE hUserConnection,
            tHandleType* pExpectedType,
            void** ppObject)
{
   return PHandleGetConnectionObject(pContext, hUserConnection, pExpectedType, ppObject);
}

/* See header file */
bool_t PReaderUserIsPropertyVisible(uint8_t nProperty)
{
   uint32_t i;

   for (i = 0; i < P_READER_INTERNAL_PROPERTY_NUMBER; i++)
   {
      if (nProperty == g_aPReaderUserInternalProperties[i])
      {
         return W_FALSE;
      }
   }

   return W_TRUE;
}

/* See API Specification */
uint8_t PReaderUserGetNbCardDetected(tContext * pContext)
{
   return (uint8_t)PReaderDriverGetNbCardDetected(pContext);
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */


