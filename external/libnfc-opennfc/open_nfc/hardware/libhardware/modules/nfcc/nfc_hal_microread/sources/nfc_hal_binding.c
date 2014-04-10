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

#define P_MODULE  P_MODULE_DEC( NAL_BIND )

#include "nfc_hal_binding.h"

/******************************************************************************************************************
*                                         Definition
*******************************************************************************************************************/

/* NFC HAL Binding timeout (in ms) */
#define TIMER_T7_TIMEOUT_NFC_HAL_BINDING    400

/* NFCC wake up state machine */
#define P_NFC_HAL_BINDING_WAKE_UP_START_FROM_RESET             0x01

/* Card Mode */
#define P_NFC_HAL_BINDING_UID_SIMPLE                           4
#define P_NFC_HAL_BINDING_UID_DOUBLE                           7
#define P_NFC_HAL_BINDING_UID_TRIPLE                           10
#define P_NFC_HAL_BINDING_DEFAULT_ATQA_LSB                     0x01
#define P_NFC_HAL_BINDING_CID_SUPPORT                          0x02
#define P_NFC_HAL_BINDING_CARD_CONFIG_MIN_SIZE_ISO_14443_4_A   0x10
#define P_NFC_HAL_BINDING_CARD_CONFIG_MIN_SIZE_ISO_14443_4_B   0x0C

/* exchange data timeout */
#define  P_NFC_HAL_BINDING_DEFAULT_HCI_TIMEOUT                 0x0B

/* NFC_HAL/HCI control byte infos */
#define P_NFC_HAL_BINDING_HCI_GENERATE_CRC                     0x80
#define P_NFC_HAL_BINDING_HCI_CHECK_CRC                        0x40
#define P_NFC_HAL_BINDING_HCI_14_3A_EXTENDED_OPTION            0x20
#define P_NFC_HAL_BINDING_HCI_15_3_SEND_EOF                    0x20

/* CRC type */
#define P_NFC_HAL_BINDING_CRC_A                                1
#define P_NFC_HAL_BINDING_CRC_B                                2

/* Firmware Update State Machine */
#define P_NFC_HAL_BINDING_UPDATE_EVENT_INITIAL                0x00

#define P_NFC_HAL_BINDING_UPDATE_EVENT_COMMAND_EXECUTED        0x01
#define P_NFC_HAL_BINDING_UPDATE_STATE_INIT_STATUS             0x0025
#define P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED       0x0200
#define P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_CHUNK              0x0015

#define P_NFC_HAL_BINDING_UPDATE_CONFIRM_INIT                0x10

/* NFC_HAL-HCI Parameter code */
#define P_NFC_HAL_BINDING_NBR_MAX_PARAMETER_PER_CMD            0x04
#define P_NFC_HAL_BINDING_HCI_SERVICE_NONE                     0xFF
#define P_NFC_HAL_BINDING_NAL_SERVICE_NONE                     0xFF

#define P_NFC_HAL_BINDING_SET_HCI_PARAM_COMPLETED              0x00
#define P_NFC_HAL_BINDING_SET_HCI_PARAM_CARD_GRANTED_TO_SIM    0x01
#define P_NFC_HAL_BINDING_SET_HCI_PARAM_SE_POLICY              0x02

/* Mifare SAK */
#define P_NFC_HAL_BINDING_SAK_MIFARE_1K         0x08
#define P_NFC_HAL_BINDING_SAK_MIFARE_4K         0x18

/* Parameters size */
#define PERSISTENT_MEMORY_SIZE                           0x08

#define P_NFC_HAL_BINDING_CARD_MODE_DISABLE_STATE              0x10
#define P_NFC_HAL_BINDING_CARD_MODE_ENABLE_STATE               0x11
#define P_NFC_HAL_BINDING_READER_MODE_DEACTIVATE_STATE         0x12
#define P_NFC_HAL_BINDING_READER_MODE_START_STATE              0x13


#define P_NFC_HAL_BINDING_NBR_CARD_PROTOCOLS                   0x0A


#define P_NFC_HAL_BINDING_SIZE_SYSTEM_CODE_TYPE_3              sizeof(uint16_t)
#define P_NFC_HAL_BINDING_SIZE_ENTRY_TYPE_3                    18

/* debug macro */
#define P_DEBUG_CHECK_COND(bCond)                     \
         if((bCond) == W_FALSE) {                               \
         PNALDebugError( "P_DEBUG_CHECK_COND: bad parameter");   \
         return (W_ERROR_BAD_PARAMETER);                      \
         }

static uint8_t aSetPicoreadParameterToRemoveParityBit[] = {HCI2_PAR_MREAD_GEN_PICOREAD_OPTION, 0x01, 0x00};

/* global NAL binding context */
tNALBindingContext  g_sNALBindingContext;

/**
 * @brief   Updates the reception counter
 */

NFC_HAL_INTERNAL void PNALBindingUpdateReceptionCounter(
   tNALBindingContext * pBindingContext,
   uint32_t             nReceptionCounter)
{
   PNALDebugTrace("nReceptionCounter : %d - previous value %d\n", nReceptionCounter, pBindingContext->nReceptionCounter);

   if (nReceptionCounter == 0)
   {
      PNALDebugWarning("nReceptionCounter is zero : ignored");
      return;
   }

   if (nReceptionCounter < pBindingContext->nReceptionCounter)
   {
      PNALDebugError("the reception counter decreased !!!");
      return;
   }

   pBindingContext->nReceptionCounter = nReceptionCounter;
}

/**
 * @brief Convert a number of byte including parity bit to the number of bit
 *
 * @param[in] nNumberOfByte   the number of received byte
 *
 * @param[out] nOutNumberOfBit   the number of bit containing in byte
 *
 * @return W_SUCCESS if no error occured
 **/
static W_ERROR static_PNALBindingConvertByteToBit(uint32_t nNumberOfByte, uint32_t * nOutNumberOfBit);

/**
 * @brief   Build the NFC HAL Response.
 *
 * @param[in]  nNALServiceIdentifier  The NFC HAL Service Identifier .
 *
 * @param[out]  pNALReceptionBuffer  The buffer containing the NFC HAL response to be send.
 *
 * @param[out]  nNALReceptionBufferLength  The NFC HAL buffer size.
 *
 * @param[in]  pBuffer  The buffer containing the HCI response received from NFCC.
 *
 * @param[in]  nLength  The buffer size.
 **/
typedef void tPNALBindingBuildNALResponse(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 uint8_t  *pNALReceptionBuffer,
                                 uint32_t *nNALReceptionBufferLength,
                                 const uint8_t  *pBuffer,
                                 uint32_t  nLength);

/**
 * @brief   Build the NFC HCI Command
 *
 * @param[in]  nNALServiceIdentifier  The NFC HAL Service Identifier .
 *
 * @param[in]  pNALReceptionBuffer  The buffer containing the NFC HAL command received
 *
 * @param[in]  pNALReceptionBuffer  The NFC HAL buffer size.
 *
 * @param[out]  pBuffer  The buffer containing the HCI command to be send
 *
 * @param[out]  nLength  The buffer size.
 **/
typedef uint8_t tPNALBindingBuildHCICommand(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 const uint8_t  *pNALReceptionBuffer,
                                 uint32_t  nNALReceptionBufferLength,
                                 uint8_t  *pBuffer,
                                 uint32_t  *nLength);

/* NFC_HAL-HCI parameter code structure*/
typedef struct __tNALBindingHCIParam
{
   uint8_t nHCIServiceIdentifier;
   const uint8_t pGetHCIParamCommand[P_NFC_HAL_BINDING_NBR_MAX_PARAMETER_PER_CMD];
   uint32_t nGetHCIParamCommandLength;
}tNALBindingHCIParam;

typedef struct __tNALBindingServiceEntry
{
   uint8_t nNALServiceIdentifier;
   const tNALBindingHCIParam *pNALBindingHCIParam;
   uint32_t nNbrOfHCIServices;
   tPNALBindingBuildNALResponse *pFunctionBuildNALResponse;
} tNALBindingServiceEntry;

/******************************************************************************************************************
*                                        Functions declaration
*******************************************************************************************************************/

static void static_PNALBindingDestroy(
         tNALVoidContext* pNALContext);

static void static_PNALBindingGetParameterCompleted(
   tNALBindingContext* pBindingContext,
   void* pCallbackParameter,
   uint8_t* pBuffer,
   uint32_t nLength,
   W_ERROR nError,
   uint8_t nStatusCode,
   uint32_t nReceptionCounter);

static void static_PNALBindingSetParameterCompleted(
   tNALBindingContext* pBindingContext,
   void* pCallbackParameter,
   uint8_t* pBuffer,
   uint32_t nLength,
   W_ERROR nError,
   uint8_t nStatusCode,
   uint32_t nReceptionCounter);

static void static_PNALBindingSetPicoreadOptionParameterCompleted(
            tNALBindingContext* pBindingContext,
            void* pCallbackParameter,
            uint8_t* pBuffer,
            uint32_t nLength,
            W_ERROR nError,
            uint8_t nStatusCode,
            uint32_t nReceptionCounter);

static void static_PNALBindingDeactivateCardMode(
   tNALBindingContext* pBindingContext,
   uint8_t nNALServiceIdentifier,
   uint8_t* pBuffer,
   uint32_t nLength,
   W_ERROR nError,
   uint8_t nStatusCode);

static void static_PNALBindingCardModeDeactivatedCompleted(
   tNALBindingContext* pBindingContext,
   void* pCallbackParameter,
   uint8_t* pBuffer,
   uint32_t nLength,
   W_ERROR nError,
   uint8_t nStatusCode,
   uint32_t nReceptionCounter);

static void static_PNALBindingActivateCardMode(
   tNALBindingContext* pBindingContext,
   uint8_t nNALServiceIdentifier,
   uint8_t* pBuffer,
   uint32_t nLength,
   W_ERROR nError,
   uint8_t nStatusCode);

static void static_PNALBindingCardModeActivatedCompleted(
   tNALBindingContext* pBindingContext,
   void* pCallbackParameter,
   uint8_t* pBuffer,
   uint32_t nLength,
   W_ERROR nError,
   uint8_t nStatusCode,
   uint32_t nReceptionCounter);

static void static_PNALBindingGetInitiatorGTbytesCompleted(
   tNALBindingContext* pBindingContext,
   void* pCallbackParameter,
   uint8_t* pBuffer,
   uint32_t nLength,
   W_ERROR nError,
   uint8_t nStatusCode,
   uint32_t nReceptionCounter);

static void static_PNALBindingSendEventCompleted(
   tNALBindingContext* pBindingContext,
   void* pCallbackParameter,
   W_ERROR nError,
   uint32_t nReceptionCounter );

static void static_PNALBindingCardSendEventCompleted(
   tNALBindingContext* pBindingContext,
   void* pCallbackParameter,
   W_ERROR nError,
   uint32_t nReceptionCounter );

static void static_PNALBindingCheckDetectionState(
   tNALBindingContext *pBindingContext,
   uint8_t nNALServiceIdentifier);

static void static_PNALBindingStartNewDetection(
   tNALBindingContext *pBindingContext,
   uint8_t nNALServiceIdentifier,
   uint8_t* pHCISendBuffer);

static void static_PNALBindingDetectionStateMachine(
   tNALBindingContext *pBindingContext,
   uint8_t nNALServiceIdentifer,
   uint8_t nPerformDetectionState);

static void static_PNALBindingBuildNALResponsePersistentPolicy(
   tNALBindingContext * pBindingContext,
   uint8_t   nNALServiceIdentifier,
   uint8_t  *pNALReceptionBuffer,
   uint32_t *nNALReceptionBufferLength,
   const uint8_t  *pBuffer,
   uint32_t  nLength);

static void static_PNALBindingBuildNALResponseHardwareInfo(
   tNALBindingContext * pBindingContext,
   uint8_t   nNALServiceIdentifier,
   uint8_t  *pNALReceptionBuffer,
   uint32_t *nNALReceptionBufferLength,
   const uint8_t  *pBuffer,
   uint32_t  nLength);

static void static_PNALBindingBuildNALResponseFirmwareInfo(
   tNALBindingContext * pBindingContext,
   uint8_t   nNALServiceIdentifier,
   uint8_t  *pNALReceptionBuffer,
   uint32_t *nNALReceptionBufferLength,
   const uint8_t  *pBuffer,
   uint32_t  nLength);

static void static_PNALBindingBuildNALResponseGetCardList(
   tNALBindingContext * pBindingContext,
   uint8_t   nNALServiceIdentifier,
   uint8_t  *pNALReceptionBuffer,
   uint32_t *nNALReceptionBufferLength,
   const uint8_t  *pBuffer,
   uint32_t  nLength);

static void static_PNALBindingBuildNALResponsePersistentMemory(
   tNALBindingContext * pBindingContext,
   uint8_t   nNALServiceIdentifier,
   uint8_t  *pNALReceptionBuffer,
   uint32_t *nNALReceptionBufferLength,
   const uint8_t  *pBuffer,
   uint32_t  nLength);

static void static_PNALBindingBuildNALResponseUICCSWP(
   tNALBindingContext * pBindingContext,
   uint8_t   nNALServiceIdentifier,
   uint8_t  *pNALReceptionBuffer,
   uint32_t *nNALReceptionBufferLength,
   const uint8_t  *pBuffer,
   uint32_t  nLength);

static void static_PNALBindingBuildNALResponseUICCReaderProtocols(
   tNALBindingContext * pBindingContext,
   uint8_t   nNALServiceIdentifier,
   uint8_t  *pNALReceptionBuffer,
   uint32_t *nNALReceptionBufferLength,
   const uint8_t  *pBuffer,
   uint32_t  nLength);

static void static_PNALBindingBuildNALResponseUICCCardProtocols(
   tNALBindingContext * pBindingContext,
   uint8_t   nNALServiceIdentifier,
   uint8_t  *pNALReceptionBuffer,
   uint32_t *nNALReceptionBufferLength,
   const uint8_t  *pBuffer,
   uint32_t  nLength);

static void static_PNALBindingBuildNALResponseRoutingTableConfig(
   tNALBindingContext * pBindingContext,
   uint8_t   nNALServiceIdentifier,
   uint8_t  *pNALReceptionBuffer,
   uint32_t *nNALReceptionBufferLength,
   const uint8_t  *pBuffer,
   uint32_t  nLength);

static void static_PNALBindingBuildNALResponseRoutingTableEntries(
   tNALBindingContext * pBindingContext,
   uint8_t   nNALServiceIdentifier,
   uint8_t  *pNALReceptionBuffer,
   uint32_t *nNALReceptionBufferLength,
   const uint8_t  *pBuffer,
   uint32_t  nLength);


static void static_PNALBindingPulseDurationToCardDetectConfig(
   tNALBindingContext * pBindingContext,
   const uint16_t    nNALDetectPulse,
   uint8_t         * pBuffer);

#ifdef P_INCLUDE_MIFARE_CLASSIC
   static void static_PNALBindingMifareExchangeCompleted(
      tNALBindingContext * pBindingContext,
      uint8_t nStatus);
#endif

/******************************************************************************************************************
*                                         constants
*******************************************************************************************************************/

/**
 * the parameter code of NFC_HAL-HCI equivalence
 **/

/* Card detect parameters */
static const uint8_t g_aCardDetectRFconfig[][2] = P_MGT_RF_CARD_DETECT;

/* default NFC HAL Reader Config parameters*/

#define LLCP_PARAMS     0x14,                                                                      \
                        0x46, 0x66, 0x6D,                         /* magic number */               \
                        0x01, 0x01, 0x11,                         /* version 1.1*/                 \
                        0x02, 0x02, 0x00, 0x80,                   /* MIUX    128 */                \
                        0x03, 0x02, 0x00, 0x03,                   /* WKS     Signalling, SDO */    \
                        0x04, 0x01, 0xFF,                         /* LTO     Max value*/           \
                        0x07, 0x01, 0x03                         /* OPT */

   /* note : these parameters are configured using the SUBSCRIBE COMMAND :!!! */

static const uint8_t g_aHCIReaderIsoB4Param[]  = {P_NFC_HAL_BINDING_DEFAULT_AFI_ISOB4, P_NFC_HAL_BINDING_DATA_RATE_MAX_DEFAULT, 0x00};
static const uint8_t g_aHCIReaderIsoA4Param[]  = {P_NFC_HAL_BINDING_DATA_RATE_MAX_DEFAULT};
static const uint8_t g_aHCIReaderIso153Param[] = {P_NFC_HAL_BINDING_DEFAULT_AFI_ISO15_3};

static const uint8_t g_aHCIReaderType3Param[]  = {P_NFC_HAL_BINDING_DEFAULT_SYSTEM_CODE_FELICA, P_NFC_HAL_BINDING_DEFAULT_SYSTEM_CODE_FELICA, 0x01, 0x0F};

   /* P2P Initiator does not support subscribe, the parameter is set using SET_PROPERTIES */

static const uint8_t g_aHCIP2PInitiatorParam[] = { HCI2_PAR_P2P_INITIATOR_BRS, 0x01, 0x03,       /* Default rate to 424 kbps */
                                                   HCI2_PAR_P2P_INITIATOR_GI,  LLCP_PARAMS };

/* Reader B PRIME */
#define DEFAULT_APGEN_PARAMS  0x03, 0x00, 0x0B, 0x7F
static const uint8_t g_aHCIReaderBPrime[]      = {DEFAULT_APGEN_PARAMS};

/* default NFC HAL Card Config parameters */
static const uint8_t g_aHCICardIsoB4Param[] =
{
   HCI2_PAR_MCARD_ISO_B_MODE,                   0x01,    HCI2_VAL_MCARD_MODE_DISABLE,
   HCI2_PAR_MCARD_ISO_B_AFI ,                   0x01,    0x00,
   HCI2_PAR_MCARD_ISO_B_ATQB,                   0x04,    0x00, 0x00, 0x00, 0x40,
   HCI2_PAR_MCARD_ISO_B_UID,                    0x00,
   HCI2_PAR_MCARD_ISO_B_ATTRIB_INF_RESP,        0x00,
   HCI2_PAR_MCARD_ISO_B_DATARATEMAX,            0x03,    0x00, 0x00, 0x00
};
static const uint8_t g_aHCICardIsoA4Param[] =
{
   HCI2_PAR_MCARD_ISO_A_MODE,                   0x01,    HCI2_VAL_MCARD_MODE_DISABLE,
   HCI2_PAR_MCARD_ISO_A_ATQA,                   0x02,    0x00, 0xC1,
   HCI2_PAR_MCARD_ISO_A_SAK,                    0x01,    0x01,
   HCI2_PAR_MCARD_ISO_A_UID,                    0x00,
   HCI2_PAR_MCARD_ISO_A_FWI_SFGI,               0x01,    0x70,
   HCI2_PAR_MCARD_ISO_A_CID_SUPPORT,            0x01,    0x00,
   HCI2_PAR_MCARD_ISO_A_DATARATEMAX,            0x03,    0x00, 0x00, 0x00
};
static const uint8_t g_aHCIP2PTargetParam[] =
{
   HCI2_PAR_P2P_TARGET_MODE,                    0x01,    HCI2_VAL_MCARD_MODE_DISABLE,

   HCI2_PAR_P2P_TARGET_GT,                      LLCP_PARAMS,

   HCI2_PAR_P2P_TARGET_TO,                      0x01,    0x08                                       /* 77.3 ms */
};
/* card mode A parameters */
static const uint8_t g_aGetCardISO14443Type4AParam[] = {
   HCI2_PAR_MCARD_ISO_A_DATARATEMAX,
   /* The CID parameter is not supported by the NFCC*/
};

/* Card mode B parameters */

static const uint8_t g_aGetCardISO14443Type4BParam[] = {
    HCI2_PAR_MCARD_ISO_B_AFI,
    HCI2_PAR_MCARD_ISO_B_DATARATEMAX
};
static const uint8_t g_aIsoB4TypeBSelectedCard[] = {0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00};

/* default link parameters carried in discovered P2P target/initiator payload */
static const uint8_t g_aDefaultLinkParameters[]  = {0x46, 0x66, 0x6D,  0x01, 0x01, 0x10};

const tNALBindingProtocolEntry g_aNALBindingReaderProtocolArray[] =
{
   {
      HCI2_PROTOCOL_READER_ISO_14443_4_B,
      P_HCI_SERVICE_MREAD_ISO_14_B_4,
      HCI2_PIPE_ID_HDS_MREAD_ISO_B,
      g_aHCIReaderIsoB4Param,
      sizeof(g_aHCIReaderIsoB4Param),
      null,
      0,
      NAL_PROTOCOL_READER_ISO_14443_4_B,
      NAL_SERVICE_READER_14_B_4,
      P_NFC_HAL_BINDING_PROTOCOL_ATTR_OPEN | P_NFC_HAL_BINDING_PROTOCOL_ATTR_SUBSCRIBE | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_CMD | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_EVT
      /* iso B4 RF Setting is done while setting iso B3 and vice versa
       * so only one pipe is set with RF setting
       */
   },
   {
      HCI2_PROTOCOL_READER_TYPE_1,
      P_HCI_SERVICE_MREAD_NFC_T1,
      HCI2_PIPE_ID_HDS_MREAD_NFC_T1,
      null,
      0,
      null,
      0,
      NAL_PROTOCOL_READER_TYPE_1_CHIP,
      NAL_SERVICE_READER_TYPE_1,
      P_NFC_HAL_BINDING_PROTOCOL_ATTR_OPEN | P_NFC_HAL_BINDING_PROTOCOL_ATTR_SUBSCRIBE | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_CMD | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_EVT
   },
   {
      HCI2_PROTOCOL_READER_ISO_14443_4_A,
      P_HCI_SERVICE_MREAD_ISO_14_A_4,
      HCI2_PIPE_ID_HDS_MREAD_ISO_A,
      g_aHCIReaderIsoA4Param,
      sizeof(g_aHCIReaderIsoA4Param),
      null,
      0,
      NAL_PROTOCOL_READER_ISO_14443_4_A,
      NAL_SERVICE_READER_14_A_4,
      P_NFC_HAL_BINDING_PROTOCOL_ATTR_OPEN | P_NFC_HAL_BINDING_PROTOCOL_ATTR_SUBSCRIBE | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_CMD | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_EVT
      /* iso A4 RF Setting is done while setting iso A3 and vice versa
       * so only pipe is set with RF setting
       */
   },
   {
      HCI2_PROTOCOL_READER_ISO_15693_3,
      P_HCI_SERVICE_MREAD_ISO_15_3,
      HCI2_PIPE_ID_HDS_MREAD_ISO_15_3,
      g_aHCIReaderIso153Param,
      sizeof(g_aHCIReaderIso153Param),
      null,
      0,
      NAL_PROTOCOL_READER_ISO_15693_3,
      NAL_SERVICE_READER_15_3,
      P_NFC_HAL_BINDING_PROTOCOL_ATTR_OPEN | P_NFC_HAL_BINDING_PROTOCOL_ATTR_SUBSCRIBE | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_CMD | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_EVT
   },
   {
      HCI2_PROTOCOL_READER_ISO_15693_2,
      P_HCI_SERVICE_MREAD_ISO_15_2,
      HCI2_PIPE_ID_HDS_MREAD_ISO_15_2,
      null,
      0,
      null,
      0,
      NAL_PROTOCOL_READER_ISO_15693_2,
      P_NFC_HAL_BINDING_NAL_SERVICE_NONE,
      P_NFC_HAL_BINDING_PROTOCOL_ATTR_CLOSE
   },
   {
      HCI2_PROTOCOL_READER_FELICA,
      P_HCI_SERVICE_MREAD_FELICA,
      HCI2_PIPE_ID_HDS_MREAD_NFC_T3,
      g_aHCIReaderType3Param,
      sizeof(g_aHCIReaderType3Param),
      null,
      0,
      NAL_PROTOCOL_READER_FELICA,
      NAL_SERVICE_READER_FELICA,
      P_NFC_HAL_BINDING_PROTOCOL_ATTR_OPEN |  P_NFC_HAL_BINDING_PROTOCOL_ATTR_SUBSCRIBE | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_CMD | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_EVT
   },
   {
      HCI2_PROTOCOL_READER_ISO_14443_3_B,
      P_HCI_SERVICE_MREAD_ISO_14_B_3,
      HCI2_PIPE_ID_HDS_MREAD_ISO_B_3,
      g_aHCIReaderIsoB4Param,
      sizeof(g_aHCIReaderIsoB4Param),
      null,
      0,
      NAL_PROTOCOL_READER_ISO_14443_3_B,
      NAL_SERVICE_READER_14_B_3,
      P_NFC_HAL_BINDING_PROTOCOL_ATTR_OPEN | P_NFC_HAL_BINDING_PROTOCOL_ATTR_SUBSCRIBE | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_CMD | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_EVT
   },
   {
      HCI2_PROTOCOL_READER_ISO_14443_3_A,
      P_HCI_SERVICE_MREAD_ISO_14_A_3,
      HCI2_PIPE_ID_HDS_MREAD_ISO_A_3,
      null,
      0,
      null,
      0,
      NAL_PROTOCOL_READER_ISO_14443_3_A,
      NAL_SERVICE_READER_14_A_3,
      P_NFC_HAL_BINDING_PROTOCOL_ATTR_OPEN | P_NFC_HAL_BINDING_PROTOCOL_ATTR_SUBSCRIBE | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_CMD | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_EVT
   },
   {
      HCI2_PROTOCOL_READER_P2P_INITIATOR,
      P_HCI_SERVICE_P2P_INITIATOR,
      HCI2_PIPE_ID_HDS_P2P_INITIATOR,
      g_aHCIP2PInitiatorParam,
      sizeof(g_aHCIP2PInitiatorParam),
      null,
      0,
      NAL_PROTOCOL_READER_P2P_INITIATOR,
      NAL_SERVICE_P2P_INITIATOR,
      P_NFC_HAL_BINDING_PROTOCOL_ATTR_OPEN | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_EVT
   },

   {
      HCI2_PROTOCOL_READER_B_PRIME,
      P_HCI_SERVICE_MREAD_BPRIME,
      HCI2_PIPE_ID_HDS_MREAD_BPRIME,
      g_aHCIReaderBPrime,
      sizeof(g_aHCIReaderBPrime),
      null,
      0,
      NAL_PROTOCOL_READER_BPRIME,
      NAL_SERVICE_READER_B_PRIME,
      P_NFC_HAL_BINDING_PROTOCOL_ATTR_OPEN | P_NFC_HAL_BINDING_PROTOCOL_ATTR_SUBSCRIBE | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_CMD | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_EVT
   },

   {
      HCI2_PROTOCOL_READER_KOVIO,
      P_HCI_SERVICE_MREAD_KOVIO,
      HCI2_PIPE_ID_HDS_MREAD_KOVIO,
      null,
      0,
      null,
      0,
      NAL_PROTOCOL_READER_KOVIO,
      NAL_SERVICE_READER_KOVIO,
      P_NFC_HAL_BINDING_PROTOCOL_ATTR_OPEN | P_NFC_HAL_BINDING_PROTOCOL_ATTR_SUBSCRIBE | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_CMD | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_EVT
   }
};

const uint32_t g_nNALBindingReaderProtocolArraySize = sizeof(g_aNALBindingReaderProtocolArray) / sizeof(g_aNALBindingReaderProtocolArray[0]);

/* Card Protocol Array */
const tNALBindingProtocolEntry g_aNALBindingCardProtocolArray[] =
{
   {
      HCI2_PROTOCOL_CARD_ISO_14443_4_B,
      P_HCI_SERVICE_MCARD_ISO_14_B_4,
      HCI2_PIPE_ID_HDS_MCARD_ISO_B,
      g_aHCICardIsoB4Param,
      sizeof(g_aHCICardIsoB4Param),
      g_aGetCardISO14443Type4BParam,
      sizeof(g_aGetCardISO14443Type4BParam),
      NAL_PROTOCOL_CARD_ISO_14443_4_B,
      NAL_SERVICE_CARD_14_B_4,
      P_NFC_HAL_BINDING_PROTOCOL_ATTR_OPEN | P_NFC_HAL_BINDING_PROTOCOL_ATTR_SUBSCRIBE | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_EVT
   },
   {
      HCI2_PROTOCOL_CARD_ISO_14443_4_A,
      P_HCI_SERVICE_MCARD_ISO_14_A_4,
      HCI2_PIPE_ID_HDS_MCARD_ISO_A,
      g_aHCICardIsoA4Param,
      sizeof(g_aHCICardIsoA4Param),
      g_aGetCardISO14443Type4AParam,
      sizeof(g_aGetCardISO14443Type4AParam),
      NAL_PROTOCOL_CARD_ISO_14443_4_A,
      NAL_SERVICE_CARD_14_A_4,
      P_NFC_HAL_BINDING_PROTOCOL_ATTR_OPEN | P_NFC_HAL_BINDING_PROTOCOL_ATTR_SUBSCRIBE | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_EVT
   },
   {
      HCI2_PROTOCOL_CARD_BPRIME,
      P_HCI_SERVICE_MCARD_BPRIME,
      HCI2_PIPE_ID_HDS_MCARD_ISO_BPRIME,
      null,
      0,
      null,
      0,
      NAL_PROTOCOL_CARD_BPRIME,
      P_NFC_HAL_BINDING_NAL_SERVICE_NONE,
      P_NFC_HAL_BINDING_PROTOCOL_ATTR_CLOSE
   },
   {
      HCI2_PROTOCOL_CARD_ISO_15693_3,
      P_HCI_SERVICE_MCARD_ISO_15_3,
      HCI2_PIPE_ID_HDS_MCARD_ISO_15_3,
      null,
      0,
      null,
      0,
      NAL_PROTOCOL_CARD_ISO_15693_3,
      P_NFC_HAL_BINDING_NAL_SERVICE_NONE,
      P_NFC_HAL_BINDING_PROTOCOL_ATTR_CLOSE
   },
   {
      HCI2_PROTOCOL_CARD_ISO_15693_2,
      P_HCI_SERVICE_MCARD_ISO_15_2,
      HCI2_PIPE_ID_HDS_MCARD_ISO_15_2,
      null,
      0,
      null,
      0,
      NAL_PROTOCOL_CARD_ISO_15693_2,
      P_NFC_HAL_BINDING_NAL_SERVICE_NONE,
      P_NFC_HAL_BINDING_PROTOCOL_ATTR_CLOSE
   },
   {
      HCI2_PROTOCOL_CARD_FELICA,
      P_HCI_SERVICE_MCARD_FELICA,
      HCI2_PIPE_ID_HDS_MCARD_NFC_T3,
      null,
      0,
      null,
      0,
      NAL_PROTOCOL_CARD_FELICA,
      P_NFC_HAL_BINDING_NAL_SERVICE_NONE,
      P_NFC_HAL_BINDING_PROTOCOL_ATTR_CLOSE
   },
   {
      HCI2_PROTOCOL_CARD_P2P_TARGET,
      P_HCI_SERVICE_P2P_TARGET,
      HCI2_PIPE_ID_HDS_P2P_TARGET,
      g_aHCIP2PTargetParam,
      sizeof(g_aHCIP2PTargetParam),
      null,
      0,
      NAL_PROTOCOL_CARD_P2P_TARGET,
      NAL_SERVICE_P2P_TARGET,
      P_NFC_HAL_BINDING_PROTOCOL_ATTR_OPEN | P_NFC_HAL_BINDING_PROTOCOL_ATTR_SUBSCRIBE | P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_EVT
   }
};

const uint32_t g_nNALBindingCardProtocolArraySize = sizeof(g_aNALBindingCardProtocolArray) / sizeof(g_aNALBindingCardProtocolArray[0]);

typedef struct __tNALBindingParameterEntry
{
   struct {
      const uint8_t *                pGetParameterCommand;
      const uint32_t                 nGetParameterCommandSize;
      tPNALBindingBuildNALResponse   * pBuildGetParameterAnswer;
   } sGetParameter;

   struct {
      tPNALBindingBuildHCICommand   * pBuildSetParameterCommand;
   } sSetParameter;
} tNALBindingParameterEntry;

/* NAL_PAR_PERSISTENT_MEMORY */
static const uint8_t g_aGetNALPersistentMemoryParam[] =
{
   P_HCI_SERVICE_ADMINISTRATION,
      0x01,
      HCI2_PAR_ADM_SESSION_ID
};

/* NAL_PAR_UICC_SWP */
static const uint8_t g_aGetNALUICCSWPParam[] =
{
   P_HCI_SERVICE_PIPE_MANAGEMENT,
      0x01,
      HCI2_PAR_MGT_SWP_SHDLC_STATUS
};

/* NAL_PAR_UICC_READER_PROTOCOLS */
static const uint8_t g_aGetNALUICCReaderProtocolsParam[] =
{
   P_HCI_SERVICE_PIPE_MANAGEMENT,
      0x01,
      HCI2_PAR_MGT_SIM_OWNER_READER

   /* others parameters are already in NALBindingInstance*/
};

/* NAL_PAR_UICC_CARD_PROTOCOLS */
static const uint8_t g_aGetNALUICCCardProtocolsParam[] =
{
   P_HCI_SERVICE_PIPE_MANAGEMENT,
      0x01,
      HCI2_PAR_MGT_SIM_OWNER_CARD

   /* others parameters are already in NALBindingInstance*/
};

/* NAL_PAR_LIST_CARDS */
static const uint8_t g_aGetNALListCardParam[] =
{
   P_HCI_SERVICE_MREAD_FELICA,
   0x01,
   HCI2_PAR_MREAD_FELICA_LIST_CARD

};

/* NAL_PAR_ROUTING_TABLE_CONFIG */
static const uint8_t g_aGetNALRoutingTableConfig[] =
{
   P_HCI_SERVICE_MCARD,
   0x01,
   HCI2_PAR_MCARD_GEN_ROUTING_TABLE_ENABLED
};

/* NAL_PAR_ROUTING_TABLE_ENTRIES */
static const uint8_t g_aGetNALRoutingTableEntries[] =
{
   P_HCI_SERVICE_MCARD,
   0x01,
   HCI2_PAR_MCARD_GEN_ROUTING_TABLE
};

uint8_t PNALBindingBuildHCICommandGenericPolicy(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 const uint8_t  *pNALReceptionBuffer,
                                 uint32_t  nNALReceptionBufferLength,
                                 uint8_t  *pBuffer,
                                 uint32_t *pnLength,
                                 bool_t      bPersistent,
                                 bool_t      bFromPersistent)
{
   uint32_t nIndex = 0;
   bool_t bIsReaderLocked;
   bool_t bIsCardLocked;
   uint16_t nUICCCardPolicy;
   uint16_t nUICCReaderPolicy;
   uint32_t nSESwitchPosition;
   uint32_t nSESlotIdentifier;
   uint16_t nSECardPolicy;

   bool_t bIsReaderLockedCurrent;
   bool_t bIsCardLockedCurrent;
   uint16_t nUICCCardPolicyCurrent;
   uint16_t nUICCReaderPolicyCurrent;
   uint32_t nSESwitchPositionCurrent;
   uint32_t nSESlotIdentifierCurrent;
   uint32_t nBattOff;
   uint16_t nSECardPolicyCurrent;
   uint16_t nHCIPolicy;
   uint8_t  nHCIMode;
   uint8_t nResult;

   nResult = PNALProtocolParse_NAL_PAR_POLICY(pNALReceptionBuffer, nNALReceptionBufferLength,
                                                &bIsReaderLocked, &bIsCardLocked, &nUICCCardPolicy, &nUICCReaderPolicy,
                                                &nSESwitchPosition, &nSESlotIdentifier, &nSECardPolicy, &nBattOff);

   if (nResult == NAL_RES_OK)
   {
      uint32_t nServiceLengthIdx;

      if (bFromPersistent)
      {
         nResult = PNALProtocolParse_NAL_PAR_POLICY(pBindingContext->aParam_NAL_PAR_PERSISTENT_POLICY, NAL_POLICY_SIZE,
                                                      &bIsReaderLockedCurrent, &bIsCardLockedCurrent, &nUICCCardPolicyCurrent, &nUICCReaderPolicyCurrent,
                                                      &nSESwitchPositionCurrent, &nSESlotIdentifierCurrent, &nSECardPolicyCurrent, null);
      }
      else
      {
         nResult = PNALProtocolParse_NAL_PAR_POLICY(pBindingContext->aParam_NAL_PAR_POLICY, NAL_POLICY_SIZE,
                                                      &bIsReaderLockedCurrent, &bIsCardLockedCurrent, &nUICCCardPolicyCurrent, &nUICCReaderPolicyCurrent,
                                                      &nSESwitchPositionCurrent, &nSESlotIdentifierCurrent, &nSECardPolicyCurrent, null);
      }

      CNALMemoryCopy(pBindingContext->aSetParam_NAL_PAR_POLICY_or_NAL_PAR_PERSISTENT_POLICY, pNALReceptionBuffer, NAL_POLICY_SIZE);

      if ((bIsCardLocked != bIsCardLockedCurrent) || (bIsReaderLocked != bIsReaderLockedCurrent))
      {
         pBuffer[nIndex++] = P_HCI_SERVICE_PIPE_MANAGEMENT;
         nServiceLengthIdx = nIndex;
         pBuffer[nIndex++] = 0;

         if (bIsCardLocked != bIsCardLockedCurrent)
         {
            pBuffer[nIndex++]= bPersistent ? HCI2_PAR_MGT_DEFAULT_RF_LOCK_CARD : HCI2_PAR_MGT_CURRENT_RF_LOCK_CARD;
            pBuffer[nIndex++] = 1;
            pBuffer[nIndex++] = bIsCardLocked ? HCI2_VAL_LOCKED : HCI2_VAL_UNLOCKED;

            pBuffer[nServiceLengthIdx] += 3;
         }

         if (bIsReaderLocked != bIsReaderLockedCurrent)
         {
            pBuffer[nIndex++] = bPersistent ? HCI2_PAR_MGT_DEFAULT_RF_LOCK_READER : HCI2_PAR_MGT_CURRENT_RF_LOCK_READER;
            pBuffer[nIndex++] = 1;
            pBuffer[nIndex++] = bIsReaderLocked ? HCI2_VAL_LOCKED : HCI2_VAL_UNLOCKED;

            pBuffer[nServiceLengthIdx] += 3;
         }
      }

      /* @fixme : for now, we have to support both MR 3.3 and  MR 3.4.
         Do not try to set the SE position if there's no SE */

      if (pBindingContext->sNALParHardwareInfo.nSEType != P_NFC_HAL_SE_NONE)
      {
         if ((nSESwitchPosition != nSESwitchPositionCurrent) || (nSECardPolicy != nSECardPolicyCurrent) || (nBattOff == NAL_POLICY_FLAG_ENABLE_SE_IN_BATT_OFF))
         {
            CNALDebugAssert(nBattOff != NAL_POLICY_FLAG_ENABLE_UICC_IN_BATT_OFF);

            pBuffer[nIndex++] = P_HCI_SERVICE_SE,
            nServiceLengthIdx = nIndex;
            pBuffer[nIndex++] = 0;

            static_PNALBindingGetHCICardProtocolCapabilities(&nHCIPolicy, nSECardPolicy);

            switch (nSESwitchPosition)
            {
               default:     /* can not occur, just to avoid warning */
               case NAL_POLICY_FLAG_SE_OFF: /* OFF */
                  nHCIMode = 0x80;
                  nHCIPolicy = 0; /* Force the filter to 0 */
                  break;

               case NAL_POLICY_FLAG_RF_INTERFACE: /* RF Interface */
                  nHCIMode = 0x10;
                  break;

               case NAL_POLICY_FLAG_FORCED_HOST_INTERFACE: /* Forced Host Interface */
                  nHCIMode = 0x03;
                  break;

               case NAL_POLICY_FLAG_HOST_INTERFACE: /* Host Interface */
                  nHCIMode = 0x01;
                  break;
            }

            pBuffer[nIndex++] = bPersistent ? HCI2_PAR_SE_SETTINGS_DEFAULT : HCI2_PAR_SE_SETTINGS_CURRENT;
            pBuffer[nIndex++] = 2;
            pBuffer[nIndex++] = nHCIMode;
            pBuffer[nIndex++] = (uint8_t) nHCIPolicy;

            pBuffer[nServiceLengthIdx] += 4;
         }
      }

      if ((nUICCCardPolicy != nUICCCardPolicyCurrent) || (nUICCReaderPolicy != nUICCReaderPolicyCurrent) ||(nBattOff == NAL_POLICY_FLAG_ENABLE_UICC_IN_BATT_OFF) )
      {
         pBuffer[nIndex++] = P_HCI_SERVICE_INSTANCE;

         CNALDebugAssert(nBattOff != NAL_POLICY_FLAG_ENABLE_SE_IN_BATT_OFF);

         nServiceLengthIdx = nIndex;
         pBuffer[nIndex++] = 0;

         if ((nUICCCardPolicy != nUICCCardPolicyCurrent) || (nBattOff == NAL_POLICY_FLAG_ENABLE_UICC_IN_BATT_OFF))
         {
            pBuffer[nIndex++] =  bPersistent ?  HCI2_PAR_INSTANCES_DEFAULT_MCARD_GRANTED_TO_SIM  : HCI2_PAR_INSTANCES_CURRENT_MCARD_GRANTED_TO_SIM;
            pBuffer[nIndex++] = 2;

            static_PNALBindingGetHCICardProtocolCapabilities(&nHCIPolicy, nUICCCardPolicy);
            static_PNALBindingWriteUint16ToHCIBuffer(nHCIPolicy, &pBuffer[nIndex]);

            nIndex += 2;
            pBuffer[nServiceLengthIdx] += 4;
         }

         if (nUICCReaderPolicy != nUICCReaderPolicyCurrent)
         {
            pBuffer[nIndex++] = bPersistent ? HCI2_PAR_INSTANCES_DEFAULT_MREAD_GRANTED_TO_SIM : HCI2_PAR_INSTANCES_CURRENT_MREAD_GRANTED_TO_SIM;
            pBuffer[nIndex++] = 2;

            static_PNALBindingGetHCIReaderProtocolCapabilities(&nHCIPolicy, nUICCReaderPolicy);
            static_PNALBindingWriteUint16ToHCIBuffer(nHCIPolicy, &pBuffer[nIndex]);
            nIndex += 2;

            pBuffer[nServiceLengthIdx] += 4;
         }
      }

      * pnLength = nIndex;
   }

   return nResult;
}

static uint8_t static_PNALBindingBuildHCICommandPersistentPolicy(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 const uint8_t  *pNALReceptionBuffer,
                                 uint32_t  nNALReceptionBufferLength,
                                 uint8_t  *pBuffer,
                                 uint32_t *pnLength)
{
   return PNALBindingBuildHCICommandGenericPolicy(
                                 pBindingContext, nNALServiceIdentifier,
                                 pNALReceptionBuffer + 3, (uint8_t) (nNALReceptionBufferLength - 3),
                                 pBuffer, pnLength, W_TRUE, W_TRUE);
}

static uint8_t static_PNALBindingBuildHCICommandPolicy(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 const uint8_t  *pNALReceptionBuffer,
                                 uint32_t  nNALReceptionBufferLength,
                                 uint8_t  *pBuffer,
                                 uint32_t *pnLength)
{
   return PNALBindingBuildHCICommandGenericPolicy(
                                 pBindingContext, nNALServiceIdentifier,
                                 pNALReceptionBuffer + 3, (uint8_t ) (nNALReceptionBufferLength - 3),
                                 pBuffer, pnLength, W_FALSE, W_FALSE);
}

uint8_t PNALBindingBuildHCICommandPulse(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 const uint8_t  *pNALReceptionBuffer,
                                 uint32_t  nNALReceptionBufferLength,
                                 uint8_t  *pBuffer,
                                 uint32_t *pnLength)
{
   uint32_t nIndex = 0;
   uint16_t nNALDetectPulse;
   uint8_t  nResult;


   pBuffer[nIndex++] = P_HCI_SERVICE_PIPE_MANAGEMENT;
   pBuffer[nIndex++] = 4;
   pBuffer[nIndex++]= HCI2_PAR_MGT_CARD_DETECT_CONFIG_CURRENT;
   pBuffer[nIndex++] = 2;

   nResult = PNALProtocolParse_NAL_PAR_DETECT_PULSE(pNALReceptionBuffer + 3, nNALReceptionBufferLength - 3, &nNALDetectPulse);

   if (nResult == NAL_RES_OK)
   {
      CNALMemoryCopy(pBindingContext->aParam_NAL_PAR_DETECT_PULSE, pNALReceptionBuffer, nNALReceptionBufferLength);

      static_PNALBindingPulseDurationToCardDetectConfig(pBindingContext, nNALDetectPulse, pBuffer + nIndex);
      nIndex += 2;
      * pnLength = nIndex;
   }

   return nResult;
}

uint8_t PNALBindingBuildHCICommandPulseWithCheck(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 const uint8_t  *pNALReceptionBuffer,
                                 uint32_t  nNALReceptionBufferLength,
                                 uint8_t  *pBuffer,
                                 uint32_t *pnLength)
{
   uint8_t  nResult;

   if (pBindingContext->bCardDetectEnabled == W_FALSE)
   {
      nResult = PNALBindingBuildHCICommandPulse(
                     pBindingContext,
                     nNALServiceIdentifier,
                     pNALReceptionBuffer,
                     nNALReceptionBufferLength,
                     pBuffer,
                     pnLength);
   }
   else
   {
      nResult = NAL_RES_FEATURE_NOT_SUPPORTED;
   }

   return nResult;
}


static uint8_t static_PNALBindingBuildHCICommandPersistentMemory(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 const uint8_t  *pNALReceptionBuffer,
                                 uint32_t  nNALReceptionBufferLength,
                                 uint8_t  *pBuffer,
                                 uint32_t *pnLength)
{
   uint32_t nIndex = 0;
   uint8_t * pPersistentMemory;
   uint8_t  nResult;

   nResult = PNALProtocolParse_NAL_PAR_PERSISTENT_MEMORY(
                                 pNALReceptionBuffer + 3, nNALReceptionBufferLength - 3,
                                 & pPersistentMemory);

   if (nResult == NAL_RES_OK)
   {
      pBuffer[nIndex++] = P_HCI_SERVICE_ADMINISTRATION;
      pBuffer[nIndex++] = 10;
      pBuffer[nIndex++]= HCI2_PAR_ADM_SESSION_ID;
      pBuffer[nIndex++] = 8;
      CNALMemoryCopy(pBuffer + nIndex, pPersistentMemory, 8);
      nIndex += 8;

      * pnLength = nIndex;
   }

   return nResult;
}

static uint8_t static_PNALBindingBuildHCICommandReaderConfig(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 const uint8_t *pNALReceptionBuffer,
                                 uint32_t  nNALReceptionBufferLength,
                                 uint8_t  *pBuffer,
                                 uint32_t *pnLength)
{
   uint32_t nIndex = 0;
   uint8_t nResult = NAL_RES_OK;

   switch (nNALServiceIdentifier)
   {
      case NAL_SERVICE_READER_14_A_4 :
      {
         uint8_t nMaxDataRate;

         nResult = PNALProtocolParse_NAL_PAR_READER_CONFIG_14_A_4(
                        pNALReceptionBuffer + 3, nNALReceptionBufferLength - 3,
                        &nMaxDataRate,
                        null,
                        null,
                        null);

         if (nResult == NAL_RES_OK)
         {
              pBuffer[nIndex++] = nMaxDataRate;
            * pnLength = nIndex;
         }
         break;
      }

      case NAL_SERVICE_READER_14_B_3:
      {
         uint8_t nMaxDataRate;
         uint8_t nAFI;
         uint8_t * pHigherLayerData;
         uint32_t nHigherLayerDataLength;

         nResult =  PNALProtocolParse_NAL_PAR_READER_CONFIG_14_B_3(
                        pNALReceptionBuffer + 3, nNALReceptionBufferLength - 3,
                        & nMaxDataRate,
                        & nAFI,
                        null,
                        null,
                        null,
                        &pHigherLayerData,
                        &nHigherLayerDataLength);

         if (nResult == NAL_RES_OK)
         {
            if(nHigherLayerDataLength > NAL_READER_14_B_4_HIGHER_LAYER_DATA_MAX_LENGTH )
            {
               PNALDebugError("static_PNALBindingBuildHCICommandReaderConfig: High layer data too large");
               nResult = NAL_RES_BAD_LENGTH;
               break;
            }

            if(nHigherLayerDataLength > HCI2_PAR_MREADER_ISO_B_HIGH_LAYER_DATA_MSG_SIZE)
            {
               PNALDebugError("static_PNALBindingBuildHCICommandReaderConfig: High layer data too large for this NFCC");
               nResult = NAL_RES_FEATURE_NOT_SUPPORTED;
               break;
            }

            pBuffer[nIndex++] = nAFI;
            pBuffer[nIndex++] = nMaxDataRate;
            pBuffer[nIndex++] = (uint8_t) nHigherLayerDataLength;
            CNALMemoryCopy(pBuffer + nIndex, pHigherLayerData, nHigherLayerDataLength);
            nIndex += nHigherLayerDataLength;

            * pnLength = nIndex;
         }
         break;
      }

      case NAL_SERVICE_READER_15_3 :
      {
         uint8_t nAFI;

         nResult = PNALProtocolParse_NAL_PAR_READER_CONFIG_15_3(
                        pNALReceptionBuffer + 3, nNALReceptionBufferLength - 3,
                        & nAFI);

         if (nResult == NAL_RES_OK)
         {
            pBuffer[nIndex++] = nAFI;
            * pnLength = nIndex;
         }

         break;
      }

      case NAL_SERVICE_READER_FELICA :
      {
         uint16_t nSystemCode;

         nResult = PNALProtocolParse_NAL_PAR_READER_CONFIG_FELICA(
                        pNALReceptionBuffer + 3, nNALReceptionBufferLength - 3,
                        & nSystemCode);

         if (nResult == NAL_RES_OK)
         {
            static_PNALBindingWriteUint16ToHCIBuffer(nSystemCode, pBuffer + nIndex);
            nIndex += 2;

            pBuffer[nIndex++] = 1;
            pBuffer[nIndex++] = 0x0F;
            * pnLength = nIndex;
         }

         break;
      }

      case NAL_SERVICE_READER_B_PRIME:
      {
         nResult = PNALProtocolParse_NAL_PAR_READER_CONFIG_B_PRIME(
                  pNALReceptionBuffer + 3, nNALReceptionBufferLength - 3);

         if(nResult == NAL_RES_OK)
         {
            pBuffer[nIndex++] = (uint8_t) (nNALReceptionBufferLength - 3);
            CNALMemoryCopy(pBuffer + 1, (pNALReceptionBuffer + 3), (nNALReceptionBufferLength - 3));
            *pnLength = (nNALReceptionBufferLength - 3) + 1;
         }

         break;
      }
   }

   return nResult;
}

static uint8_t static_PNALBindingBuildHCICommandCardConfig(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 const uint8_t  *pNALReceptionBuffer,
                                 uint32_t  nNALReceptionBufferLength,
                                 uint8_t  *pBuffer,
                                 uint32_t *pnLength)
{
   uint32_t nIndex = 0;
   uint8_t nResult;
   uint8_t nDivisor;
   uint8_t nDS;
   uint8_t nDR;

   switch (nNALServiceIdentifier)
   {
      case NAL_SERVICE_CARD_14_A_4:
      {
         uint8_t aUID[10];
         uint8_t nUIDLength;
         uint8_t nATQAMsb;
         uint8_t nTO;
         uint8_t nTA;
         uint8_t nTB;
         uint8_t nTC;
         uint8_t * pApplicationData;
         uint32_t nApplicationDataLength;
         uint8_t nb8b7;
         uint16_t nATQA;

         nResult = PNALProtocolParse_NAL_PAR_CARD_CONFIG_14_A_4(
                        pNALReceptionBuffer  + 3, nNALReceptionBufferLength - 3,
                        aUID,
                        &nUIDLength,
                        &nATQAMsb,
                        &nTO,
                        &nTA,
                        &nTB,
                        &nTC,
                        &pApplicationData,
                        &nApplicationDataLength);

         if (nResult == NAL_RES_OK)
         {
            if(nApplicationDataLength > NAL_CARD_14_A_4_APPLICATION_DATA_MAX_LENGTH)
            {
               PNALDebugError("static_PNALBindingBuildHCICommandCardConfig: High layer response data too large");
               nResult = NAL_RES_BAD_LENGTH;
               break;
            }

            if(nApplicationDataLength > HCI2_PAR_MCARD_ISO_A_APPLI_DATA_MSG_SIZE)
            {
               PNALDebugError("static_PNALBindingBuildHCICommandCardConfig: High layer response data too large for this NFCC");
               nResult = NAL_RES_FEATURE_NOT_SUPPORTED;
               break;
            }
            /* Store the parameters */

            pBuffer[nIndex++] = P_HCI_SERVICE_MCARD_ISO_14_A_4;
            pBuffer[nIndex++] = 0;

            /* mode: disable mode if activated */
            pBuffer[nIndex++] = HCI2_PAR_MCARD_ISO_A_MODE;
            pBuffer[nIndex++] = 0x01;
            pBuffer[nIndex++] = HCI2_VAL_MCARD_MODE_DISABLE;

            /* - ATQA */
            pBuffer[nIndex++] = HCI2_PAR_MCARD_ISO_A_ATQA;
            pBuffer[nIndex++] = 0x02;
            /* build ATQA */
            switch (nUIDLength)
            {
               case P_NFC_HAL_BINDING_UID_SIMPLE :
                  nb8b7 = 0x00;
                  break;
               case P_NFC_HAL_BINDING_UID_DOUBLE:
                  nb8b7 = 0x40;
                  break;
               case P_NFC_HAL_BINDING_UID_TRIPLE:
                  nb8b7 = 0x80;
                  break;
               default:
                  nb8b7 = 0x00;
                  break;
            }
            nATQA = (nATQAMsb << 8) | nb8b7 | P_NFC_HAL_BINDING_DEFAULT_ATQA_LSB;
            static_PNALBindingWriteUint16ToHCIBuffer(nATQA, &pBuffer[nIndex]);
            nIndex += 2;

            /* SAK card compliant with iso-4 */
            pBuffer[nIndex++] = HCI2_PAR_MCARD_ISO_A_SAK;
            pBuffer[nIndex++] = 0x01;
            pBuffer[nIndex++] = 0x20;

            /* Set UID */
            pBuffer[nIndex++] = HCI2_PAR_MCARD_ISO_A_UID;
            pBuffer[nIndex++] = nUIDLength;

            if (nUIDLength != 0)
            {
               CNALMemoryCopy(pBuffer + nIndex, aUID, nUIDLength);
               CNALMemoryCopy(pBindingContext->aUIDCard, aUID, nUIDLength);
               nIndex += nUIDLength;
            }

            pBindingContext->nUIDCardLength = nUIDLength;

            /* - FWI/SFGI */
            pBuffer[nIndex++] = HCI2_PAR_MCARD_ISO_A_FWI_SFGI;
            pBuffer[nIndex++] = 0x01;
            if ((nTB & 0xF0) == 0xF0)
            {
               nTB &= 0x7F; /* FWI=15 is RFU (ISO) or means "default" (thus 7) in NFC Forum Digital spec */
            }
            else if (nTB < 0x70)
            {
               pBuffer[nIndex++] = 0x70;
            }
            else
            {
               pBuffer[nIndex++] = nTB;
            }

            /* - CID support */
            pBuffer[nIndex++] = HCI2_PAR_MCARD_ISO_A_CID_SUPPORT;
            pBuffer[nIndex++] = 0x01;

            if ( (nTC & P_NFC_HAL_BINDING_CID_SUPPORT)!= 0)
            {
               pBuffer[nIndex++] = 0x01;
            }
            else
            {
               pBuffer[nIndex++] = 0x00;
            }

            /* - Datarate */
            pBuffer[nIndex++] = HCI2_PAR_MCARD_ISO_A_DATARATEMAX;
            pBuffer[nIndex++] = 0x03;

            /*  get divisor and the datarate in both directions */
            nDivisor = (nTA & 0x80) >> 7;
            nDS = (nTA & 0x70) >> 4;
            nDR = nTA & 0x07;
            switch(nDR)
            {
               /* FIXME  : bitfield or absolute values ??? */

               case 0x00:
                  pBuffer[nIndex++] = 0x00;
                  break;
               case 0x01:
                  pBuffer[nIndex++] = 0x01;
                  break;
               case 0x02:
                  pBuffer[nIndex++] = 0x02;
                  break;
               case 0x04:
                  pBuffer[nIndex++] = 0x03;
                  break;
               default:
                  pBuffer[nIndex++] = 0x00;
                  break;
            }

            switch(nDS)
            {
               case 0x00:
                  pBuffer[nIndex++] = 0x00;
                  break;
               case 0x01:
                  pBuffer[nIndex++] = 0x01;
                  break;
               case 0x02:
                  pBuffer[nIndex++] = 0x02;
                  break;
               case 0x04:
                  pBuffer[nIndex++] = 0x03;
                  break;

               default:
                  pBuffer[nIndex++] = 0x00;
                  break;
            }

            pBuffer[nIndex++] = nDivisor;

            /* - Application data */
            pBuffer[nIndex++] = HCI2_PAR_MCARD_ISO_A_APPLI_DATA;
            pBuffer[nIndex++] = (uint8_t) nApplicationDataLength;

            if (nApplicationDataLength)
            {
               CNALMemoryCopy(pBuffer + nIndex, pApplicationData, nApplicationDataLength);
               nIndex += nApplicationDataLength;
            }

            pBuffer[1] = (uint8_t) nIndex - 2;
         }
      }
      break;

      case NAL_SERVICE_CARD_14_B_4:
      {
         uint8_t   aATQB[11];
         uint8_t * pHigherLayerResponseData;
         uint32_t  nHigherLayerResponseDataLength;

         nResult = PNALProtocolParse_NAL_PAR_CARD_CONFIG_14_B_4(
                        pNALReceptionBuffer  + 3, nNALReceptionBufferLength - 3,
                        aATQB,
                        &pHigherLayerResponseData,
                        &nHigherLayerResponseDataLength);

         if (nResult == NAL_RES_OK)
         {
            if(nHigherLayerResponseDataLength > NAL_CARD_14_B_4_HIGHER_LAYER_RESPONSE_MAX_LENGTH)
            {
               PNALDebugError("static_PNALBindingBuildHCICommandCardConfig: High layer response data too large");
               nResult = NAL_RES_BAD_LENGTH;
               break;
            }

            if(nHigherLayerResponseDataLength > HCI2_PAR_MCARD_ISO_B_ATTRIB_INF_RESP_MSG_SIZE)
            {
               PNALDebugError("static_PNALBindingBuildHCICommandCardConfig: High layer response data too large for this NFCC");
               nResult = NAL_RES_FEATURE_NOT_SUPPORTED;
               break;
            }

            pBuffer[nIndex++] = P_HCI_SERVICE_MCARD_ISO_14_B_4;
            pBuffer[nIndex++] = 0;

            /* Card mode: disable mode if activated */
            pBuffer[nIndex++] = HCI2_PAR_MCARD_ISO_B_MODE;
            pBuffer[nIndex++] = 0x01;
            pBuffer[nIndex++] = HCI2_VAL_MCARD_MODE_DISABLE;

            /* Set AFI */
            pBuffer[nIndex++] = HCI2_PAR_MCARD_ISO_B_AFI;
            pBuffer[nIndex++] = 0x01;
            pBuffer[nIndex++] = aATQB[4];

            /* Set additional ATQB bytes */
            pBuffer[nIndex++] = HCI2_PAR_MCARD_ISO_B_ATQB;
            pBuffer[nIndex++] = 0x04;
            CNALMemoryCopy(pBuffer + nIndex, &aATQB[5], 3);
            nIndex += 3;
            pBuffer[nIndex++] = aATQB[10];

            /* Set PUPI */
            pBuffer[nIndex++] = HCI2_PAR_MCARD_ISO_B_UID;
            if((aATQB[0] == 0) &&(aATQB[1] == 0) &&(aATQB[2] == 0) &&(aATQB[3] == 0))
            {
              pBuffer[nIndex++] = 0x00;
            }
            else
            {
               pBuffer[nIndex++] = 0x04;
               CNALMemoryCopy(pBuffer + nIndex, aATQB, 4);
               nIndex += 0x04;
            }

            /* Set the data rate max */
            pBuffer[nIndex++] = HCI2_PAR_MCARD_ISO_B_DATARATEMAX;
            pBuffer[nIndex++] = 0x03;
            /*  get divisor and the datarate in both directions */
            nDivisor = (aATQB[8] & 0x80) >> 7;
            nDS = (aATQB[8] & 0x70) >> 4;
            nDR = aATQB[8] & 0x07;

            switch(nDR)
            {
               /* FIXME  : bitfield or absolute values ??? */
               case 0x00:
                  pBuffer[nIndex++] = 0x00;
                  break;
               case 0x01:
                  pBuffer[nIndex++] = 0x01;
                  break;
               case 0x02:
                  pBuffer[nIndex++] = 0x02;
                  break;
               case 0x04:
                  pBuffer[nIndex++] = 0x03;
                  break;
               default:
                  pBuffer[nIndex++] = 0x00;
                  break;
            }
             switch(nDS)
            {
               case 0x00:
                  pBuffer[nIndex++] = 0x00;
                  break;
               case 0x01:
                  pBuffer[nIndex++] = 0x01;
                  break;
               case 0x02:
                  pBuffer[nIndex++] = 0x02;
                  break;
               case 0x04:
                  pBuffer[nIndex++] = 0x03;
                  break;
               default:
                  pBuffer[nIndex++] = 0x00;
                  break;
            }
            pBuffer[nIndex++] = nDivisor;
            /* Set the Higher Layer Response */

            pBuffer[nIndex++] = HCI2_PAR_MCARD_ISO_B_ATTRIB_INF_RESP;

            pBuffer[nIndex++] = (uint8_t) nHigherLayerResponseDataLength;

            if (nHigherLayerResponseDataLength != 0)
            {
               CNALMemoryCopy(pBuffer + nIndex, pHigherLayerResponseData, nHigherLayerResponseDataLength);
               nIndex += nHigherLayerResponseDataLength;
            }

            pBuffer[1] = (uint8_t) nIndex - 2;
         }
      }
      break;

      case NAL_SERVICE_P2P_TARGET:
      {
         uint32_t nRTX;
         bool_t bAllowTypeATargetProtocol;
         bool_t bAllowActiveMode;
         uint8_t* pGT;
         uint32_t nGTLength;

         nResult = PNALProtocolParse_NAL_PAR_CARD_CONFIG_P2P_TARGET(pNALReceptionBuffer + 3, nNALReceptionBufferLength - 3,
                                                                    &nRTX, &bAllowTypeATargetProtocol, &bAllowActiveMode, &pGT, &nGTLength);

         if (nResult == NAL_RES_OK)
         {
            if ((nGTLength == 20) && (pGT[0] == 0x46) && (pGT[1] == 0x66) && (pGT[2] == 0x6D) )
            {
               /* Typical P2P : update the parameter in RAM (WKS, MIUX, LTO) */
               pBuffer[nIndex++] = P_HCI_SERVICE_MREAD;
               pBuffer[nIndex++] = 3;
               pBuffer[nIndex++] = HCI2_PAR_MREAD_GEN_P2P_ACTIVE;
               pBuffer[nIndex++] = 1;
               pBuffer[nIndex++] = (uint8_t)bAllowActiveMode;

               pBuffer[nIndex++] = P_HCI_SERVICE_P2P_TARGET;
               pBuffer[nIndex++] = 22;
               pBuffer[nIndex++] = HCI2_PAR_P2P_TARGET_MODE;
               pBuffer[nIndex++] = 0x01;
               pBuffer[nIndex++] = HCI2_VAL_MCARD_MODE_DISABLE;
               pBuffer[nIndex++] = HCI2_PAR_P2P_TARGET_TO;
               pBuffer[nIndex++] = 1;
               pBuffer[nIndex++] = 0x08;
               pBuffer[nIndex++] = HCI2_PAR_P2P_TARGET_ISOA_ACTIVE;
               pBuffer[nIndex++] = 1;
               pBuffer[nIndex++] = (uint8_t)bAllowTypeATargetProtocol;
               pBuffer[nIndex++] = HCI2_PAR_P2P_TARGET_MIUX_WKS_LTO;
               pBuffer[nIndex++] = 11;
               CNALMemoryCopy(&pBuffer[nIndex], pGT + 6 , 11);
               nIndex += 11;
            }
            else
            {
               /* @fixme : not P2P (not used now) */
               nResult = NAL_RES_BAD_DATA;
               return nResult;
            }
         }
      }
      break;

      default:
         nResult = NAL_RES_UNKNOWN_PARAM;
         break;
   }

   * pnLength = nIndex;
   return (nResult);
}

static uint8_t static_PNALBindingBuildHCICommandInitiatorLinkParameters(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 const uint8_t  *pNALReceptionBuffer,
                                 uint32_t  nNALReceptionBufferLength,
                                 uint8_t  *pBuffer,
                                 uint32_t *pnLength)
{
   uint32_t nIndex = 0;
   uint8_t  * pGI;
   uint32_t nGILength;
   uint8_t nResult;

   nResult = PNALProtocolParse_NAL_PAR_P2P_INITIATOR_LINK_PARAMETERS(
                  pNALReceptionBuffer + 3, nNALReceptionBufferLength - 3, &pGI, &nGILength);

   if (nResult == NAL_RES_OK)
   {
      if ((nGILength == 20) && (pGI[0] == 0x46) && (pGI[1] == 0x66) && (pGI[2] == 0x6D) )
      {
         /* Typical P2P : update the parameter in RAM (WKS, MIUX, LTO) */

         pBuffer[nIndex++] = P_HCI_SERVICE_P2P_INITIATOR;
         pBuffer[nIndex++] = 13;
         pBuffer[nIndex++]= HCI2_PAR_P2P_INITIATOR_MIUX_WKS_LTO;
         pBuffer[nIndex++]= 11;
         CNALMemoryCopy(&pBuffer[nIndex], pGI + 6 , 11);
         nIndex += 11;
      }
      else
      {
         /* @fixme : not P2P (not used now) */
         nResult = NAL_RES_BAD_DATA;
         return nResult;
      }

      * pnLength = nIndex;
   }

   return nResult;
}


static uint8_t static_PNALBindingBuildHCICommandRoutingTableConfig(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 const uint8_t  *pNALReceptionBuffer,
                                 uint32_t  nNALReceptionBufferLength,
                                 uint8_t  *pBuffer,
                                 uint32_t *pnLength)
{
   uint32_t nIndex = 0;
   uint8_t nResult;
   bool_t bIsEnabled;

   nResult = PNALProtocolParse_NAL_PAR_ROUTING_TABLE_CONFIG(pNALReceptionBuffer + 3, nNALReceptionBufferLength - 3, &bIsEnabled);

   if (nResult == NAL_RES_OK)
   {
      pBuffer[nIndex++] = P_HCI_SERVICE_MCARD;
      pBuffer[nIndex++] = 3;
      pBuffer[nIndex++] = HCI2_PAR_MCARD_GEN_ROUTING_TABLE_ENABLED;
      pBuffer[nIndex++] = 1;
      pBuffer[nIndex++] = bIsEnabled ? 0x01 : 0x00;
   }
   else
   {
      nResult = NAL_RES_BAD_DATA;
   }

   * pnLength = nIndex;

   return nResult;
}

static uint8_t static_PNALBindingBuildHCICommandRoutingTableEntries(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 const uint8_t  *pNALReceptionBuffer,
                                 uint32_t  nNALReceptionBufferLength,
                                 uint8_t  *pBuffer,
                                 uint32_t *pnLength)
{
   uint32_t nIndex = 0;
   uint8_t nResult;
   uint8_t nLength;

   nResult = PNALProtocolParse_NAL_PAR_ROUTING_TABLE_ENTRIES(pNALReceptionBuffer + 3, nNALReceptionBufferLength - 3, & pBuffer[nIndex+4], &nLength);

   if (nResult == NAL_RES_OK)
   {
      pBuffer[nIndex++] = P_HCI_SERVICE_MCARD;
      pBuffer[nIndex++] = 2 + nLength;
      pBuffer[nIndex++] = HCI2_PAR_MCARD_GEN_ROUTING_TABLE;
      pBuffer[nIndex++] = nLength;

      nIndex += nLength;
   }
   else
   {
      nResult = NAL_RES_BAD_DATA;
   }

   * pnLength = nIndex;

   return nResult;
}



static uint8_t static_PNALBindingEnterRawMode(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 const uint8_t  *pNALReceptionBuffer,
                                 uint32_t  nNALReceptionBufferLength,
                                 uint8_t  *pBuffer,
                                 uint32_t *pnLength)
{
   uint8_t nIndex = 0;

   pBuffer[nIndex++] = P_HCI_SERVICE_ADMINISTRATION;
   pBuffer[nIndex++] = 10;
   pBuffer[nIndex++]= HCI2_PAR_ADM_SESSION_ID;
   pBuffer[nIndex++] = 8;
   CNALMemoryFill(pBuffer + nIndex, 0, 8);

   nIndex += 8;
   * pnLength = nIndex;

   return NAL_RES_OK;
}

static const tNALBindingParameterEntry g_aNALBindingParameterEntryArray[] =
{
   /* NAL_PAR_PERSISTENT_POLICY */
   {
      {
         null,
         0,
         static_PNALBindingBuildNALResponsePersistentPolicy,
      },
      {
         static_PNALBindingBuildHCICommandPersistentPolicy
      }
   },

   /* NAL_PAR_POLICY */
   {
      {  /* write only parameter */
         null,
         0,
         null
      },
      {
         static_PNALBindingBuildHCICommandPolicy
      }
   },

   /* NAL_PAR_HARDWARE_INFO */
   {
      {
         null,
         0,
         static_PNALBindingBuildNALResponseHardwareInfo
      },
      {
         /* read only parameter */
         null
      }
   },

   /* NAL_PAR_FIRMWARE_INFO */
   {
      {
         null,
         0,
         static_PNALBindingBuildNALResponseFirmwareInfo
      },
      {  /* read only parameter */
         null,
      }
   },

   /* NAL_PAR_DETECT_PULSE */
   {
      {
         /* write only parameter */
         null,
         0,
         null
      },
      {
         PNALBindingBuildHCICommandPulseWithCheck
      }
   },

   /* NAL_PAR_PERSISTENT_MEMORY */
   {
      {
         g_aGetNALPersistentMemoryParam,
         sizeof(g_aGetNALPersistentMemoryParam),
         static_PNALBindingBuildNALResponsePersistentMemory,
      },
      {
         static_PNALBindingBuildHCICommandPersistentMemory
      }
   },

   /* NAL_PAR_READER_CONFIG */
   {
      {
         /* read only parameter */
         null,
         0,
         null,
      },
      {
         static_PNALBindingBuildHCICommandReaderConfig
      }
   },

   /* NAL_PAR_CARD_CONFIG */
   {
      { /* read only parameter */
         null,
         0,
         null,
      },
      {
         static_PNALBindingBuildHCICommandCardConfig
      }
   },

   /* NAL_PAR_P2P_INITIATOR_LINK_PARAMETERS */
   {
      {  /* write only parameter */
         null,
         0,
         null,
      },
      {
         static_PNALBindingBuildHCICommandInitiatorLinkParameters
      }
   },

   /* NAL_PAR_UICC_SWP */
   {
      {
         g_aGetNALUICCSWPParam,
         sizeof(g_aGetNALUICCSWPParam),
         static_PNALBindingBuildNALResponseUICCSWP,
      },
      {
         /* read only parameter */
         null
      }
   },

   /* NAL_PAR_UICC_READER_PROTOCOLS */
   {
      {
         g_aGetNALUICCReaderProtocolsParam,
         sizeof(g_aGetNALUICCReaderProtocolsParam),
         static_PNALBindingBuildNALResponseUICCReaderProtocols,
      },
      {
         /* read only parameter */
         null
      }
   },

   /* NAL_PAR_UICC_CARD_PROTOCOLS */
   {
      {
         g_aGetNALUICCCardProtocolsParam,
         sizeof(g_aGetNALUICCCardProtocolsParam),
         static_PNALBindingBuildNALResponseUICCCardProtocols,
      },
      {
         /* read only parameter */
         null
      }
   },

   /* XXXXXXXXXX */
   {
      {
         null,    /* FIXME */
         0,
         null,
      },
      {
         /* read only parameter */
         null
      }
   },

   /* NAL_PAR_RAW_MODE */
   {
      {
         null,
         0,
         null,
      },
      {
         static_PNALBindingEnterRawMode,
      }
   },

   /* NAL_PAR_UICC_CARD_PROTOCOLS */
   {
      {
         g_aGetNALListCardParam,
         sizeof(g_aGetNALListCardParam),
         static_PNALBindingBuildNALResponseGetCardList,
      },
      {
         /* read only parameter */
         null
      }
   },

   /* NAL_PAR_ROUTING_TABLE_CONFIG */

   {
      {
         g_aGetNALRoutingTableConfig,
         sizeof(g_aGetNALRoutingTableConfig),
         static_PNALBindingBuildNALResponseRoutingTableConfig,
      },
      {
         static_PNALBindingBuildHCICommandRoutingTableConfig,
      }
   },

   /* NAL_PAR_ROUTING_TABLE_CONFIG_ENTRIES */

   {
      {
         g_aGetNALRoutingTableEntries,
         sizeof(g_aGetNALRoutingTableEntries),
         static_PNALBindingBuildNALResponseRoutingTableEntries,
      },
      {
         static_PNALBindingBuildHCICommandRoutingTableEntries,
      }
   },




};

/******************************************************************************************************************
*                                            local functions                                                      *
*******************************************************************************************************************/

/**
 * Posts the read completed callback of upper layer.
 *
 * @param[in]  pBindingContext The NFC HAL Binding Instance.
 **/
void PNALBindingCallReadCallback(
            tNALBindingContext * pBindingContext,
            uint32_t nReadLength,
            uint32_t nReceptionCounter)
{
   CNALDebugAssert(pBindingContext->bInPoll == W_TRUE);

   pBindingContext->bInPoll = W_FALSE;
   CNALSyncLeaveCriticalSection(&pBindingContext->hCriticalSection);

   pBindingContext->pNALReadCallbackFunction(
      pBindingContext->pCallbackContext,
      pBindingContext->pNALReadCallbackParameter,
      nReadLength, nReceptionCounter);

   CNALSyncEnterCriticalSection(&pBindingContext->hCriticalSection);
   pBindingContext->bInPoll = W_TRUE;
}

/**
 * Posts the write completed callback of upper layer.
 *
 * @param[in]  pBindingContext The NFC HAL Binding Instance.
 **/
static void static_PNALBindingCallWriteCallback(
            tNALBindingContext * pBindingContext)
{
   tNALBindingWriteCompleted * pNALWriteCallbackFunction;

   pNALWriteCallbackFunction = pBindingContext->pNALWriteCallbackFunction;
   pBindingContext->pNALWriteCallbackFunction = null;

   if (pNALWriteCallbackFunction != null)
   {
      pBindingContext->bInPoll = W_FALSE;
      CNALSyncLeaveCriticalSection(&pBindingContext->hCriticalSection);

      pNALWriteCallbackFunction(
         pBindingContext->pCallbackContext,
         pBindingContext->pNALWriteCallbackParameter,
         pBindingContext->nReceptionCounter);

      CNALSyncEnterCriticalSection(&pBindingContext->hCriticalSection);
      pBindingContext->bInPoll = W_TRUE;
   }
   else
   {
      PNALDebugError("static_PNALBindingCallWriteCallback : write callback has already been called");
   }
}

/* Update CRC Function*/
static P_NAL_INLINE uint16_t static_PNALBindingUpdateCrc(
                                            uint8_t ch,
                                            uint16_t *lpwCrc)
{
   ch = (ch^(uint8_t)((*lpwCrc) & 0x00FF));
   ch = (ch^(ch<<4));
   *lpwCrc = (*lpwCrc >> 8)^((uint16_t)ch << 8)^((uint16_t)ch<<3)^((uint16_t)ch>>4);
   return(*lpwCrc);
}

/**
 * @brief   Calcul the the two CRC_A/CRC_B bytes.
 *
 * @param[in]  nCRCType The CRC type: CRC_A or CRC_B.
 *
 * @param[in]  pData The pointer to data.
 *
 * @param[in]  nLength The data length.
 *
 * @param[out]  pTransmitFirst The first byte of calculated crc.
 *
  * @param[out]  pTransmitFirst The second byte of calculated crc.
 *@return  nothing
 **/
static void static_PNALBindingComputeCrc(
                                   uint8_t nCRCType,
                                   const uint8_t *pData,
                                   uint32_t nLength,
                                   uint8_t *pTransmitFirst,
                                   uint8_t *pTransmitSecond)
{
   uint8_t nBlock;
   uint16_t nCrc;

   switch(nCRCType)
   {
      case P_NFC_HAL_BINDING_CRC_A:
      nCrc = 0x6363; /* ITU-V.41 */
      break;
      case P_NFC_HAL_BINDING_CRC_B:
      nCrc = 0xFFFF; /* ISO/IEC 13239 (formerly ISO/IEC 3309) */
      break;
      default:
      return;
   }
   do
   {
      nBlock = *pData++;
      static_PNALBindingUpdateCrc(nBlock, &nCrc);
   } while (--nLength);
   if (nCRCType == P_NFC_HAL_BINDING_CRC_B)
   {
      nCrc = ~nCrc; /* ISO/IEC 13239 (formerly ISO/IEC 3309) */
   }
   *pTransmitFirst = (uint8_t) (nCrc & 0xFF);
   *pTransmitSecond = (uint8_t) ((nCrc >> 8) & 0xFF);
   return;
}

#ifdef P_INCLUDE_MIFARE_CLASSIC

/**
 * @brief   Callback call by the Mifare function when the command has been treated
 *
 * @param[out] nStatus  the status of the exchange
 *
 * @param[out] nBitLength  the Length (in bit) of the response
 **/
static void static_PNALBindingMifareExchangeCompleted(
   tNALBindingContext * pBindingContext,
   uint8_t nStatus)
{
   uint32_t nNALReceptionBufferLength = 2;
   PNALDebugTrace("static_PNALBindingMifareExchangeCompleted");

   pBindingContext->pNALReceptionBuffer[0] = NAL_SERVICE_READER_14_A_3;
   pBindingContext->pNALReceptionBuffer[1] = (uint8_t) nStatus;


   if(nStatus == NAL_RES_OK)
   {
      nNALReceptionBufferLength += CNALMifareGetClearCommandResponse(
                                             pBindingContext->pMifareContext,
                                             &pBindingContext->pNALReceptionBuffer[2],
                                             pBindingContext->nNALReceptionBufferLengthMax - 2);
   }

   pBindingContext->bMifareExchange = W_FALSE;

   /* post the result*/
   PNALBindingCallReadCallback(pBindingContext, nNALReceptionBufferLength, pBindingContext->nReceptionCounter);
}

/**
 * @brief   Build The HCI exchange command.
 *
 * @param[in]  nHCIServiceIdentifier The HCI service Identifier.
 *
 * @param[out]  pHCISendBuffer The HCI Send Buffer.
 *
 * @param[out]  pHCISendBufferLength The HCI Send Buffer Length.
 *
 * @param[in]  pCmdBuffer The Cmd Buffer
 *
 * @param[in]  nCmdBufferLength The Cmd Buffer Length.
 **/
static uint8_t static_PNALBindingBuildHCIForMifareXchangeDataCmd(
                 tNALBindingContext * pBindingContext,
                 uint8_t nHCIServiceIdentifier,
                 uint8_t *pHCISendBuffer,
                 uint32_t *pHCISendBufferLength,
                 const uint8_t * pCmdBuffer,
                 uint32_t  nCmdBufferLength,
                 uint8_t nStatus)
{
   uint8_t nReturnedStatus = NAL_RES_OK;
   uint32_t nCommandBitLentgh = 0; /* Total length of the command which will send to the card */
   uint8_t nExpectedBitLength = 0;
   *pHCISendBufferLength = 0;

   pHCISendBuffer[0] = P_NFC_HAL_BINDING_HCI_14_3A_EXTENDED_OPTION | ETSI_TIMEOUT_READER_XCHG_DATA_ENABLE;

   /* Send and received Bit */
   pHCISendBuffer[1] = 0;

   /* Enter in the Mifare Operation */
   if(pBindingContext->bMifareExchange == W_FALSE)
   {
      /* The CNALMifareProcessMifareCommand copy data to send and calculate the length (in bit) of the HCIBuffer*/
      nReturnedStatus = CNALMifareProcessClearCommand(
                                       pBindingContext->pMifareContext,
                                       pCmdBuffer,
                                       nCmdBufferLength);

      pBindingContext->bMifareExchange = W_TRUE;
   }
   /* Else we continue the Mifare Operation */
   else
   {
      nReturnedStatus = CNALMifareProcessCardResponse(
                                       pBindingContext->pMifareContext,
                                       nStatus,
                                       pCmdBuffer,
                                       nCmdBufferLength);
   }

   /* Error during the process */
   if(nReturnedStatus != NAL_RES_OK)
   {
      pBindingContext->bMifareExchange = W_FALSE;
      return nReturnedStatus;
   }

   /* Get the next command */
   nCommandBitLentgh = CNALMifareGetNextCardCommand(
                                       pBindingContext->pMifareContext,
                                       &pHCISendBuffer[2],
                                       278 * 8,
                                       &nExpectedBitLength);

   /* if no command have to be sent, the operation is completed*/
   if(nCommandBitLentgh == 0)
   {
      pBindingContext->bMifareExchange = W_FALSE;
      return NAL_RES_OK;
   }

   /*Set the timeout value*/
   if( pBindingContext->nCurrentMifareTimeout > 14)
   {
      /*Set default Timeout*/
      pHCISendBuffer[0] |= P_NFC_HAL_BINDING_DEFAULT_HCI_TIMEOUT;
   }
   else
   {
      /* set the timeout*/
      pHCISendBuffer[0] |= pBindingContext->nCurrentMifareTimeout;
   }


   pBindingContext->nExpectedBitBeforeByte = (uint8_t) nExpectedBitLength;
   /* Set Length */
   pHCISendBuffer[1] = ((uint8_t) (0x07 & nExpectedBitLength) << 4) | (uint8_t) (0x07 & (nCommandBitLentgh - ((nCommandBitLentgh / 8) * 8)));

   *pHCISendBufferLength =  2 /* OPTION + Bit */ + ((nCommandBitLentgh + 7) / 8);

   return NAL_RES_OK;
}
#endif

/**
 * @brief   Build The HCI exchange command.
 *
 * @param[in]  nHCIServiceIdentifier The HCI service Identifier.
 *
 * @param[out]  pHCISendBuffer The HCI Send Buffer.
 *
 * @param[out]  pHCISendBufferLength The HCI Send Buffer Length.
 *
 * @param[in]  pNALBuffer The NFC HAL Buffer.
 *
 * @param[in]  nHCISendBufferLength The NFC HAL Buffer Length.
 **/
static void static_PNALBindingBuildHCIXchangeDataCmd(
                 uint8_t nHCIServiceIdentifier,
                 uint8_t *pHCISendBuffer,
                 uint32_t *pHCISendBufferLength,
                 const uint8_t * pNALBuffer,
                 uint32_t  nNALBufferLength)
{
   uint8_t nNALControlByte = pNALBuffer[0];
   uint8_t nIndex = 1;

   pHCISendBuffer[0] = 0;

   if(nNALControlByte & NAL_TIMEOUT_READER_XCHG_DATA_ENABLE)
   {
      uint8_t nTimeout = (nNALControlByte & NAL_TIMEOUT_READER_XCHG_DATA_BITS_MASK)>> NAL_TIMEOUT_READER_XCHG_DATA_BITS_OFFSET;

      pHCISendBuffer[0] |= ETSI_TIMEOUT_READER_XCHG_DATA_ENABLE;

      /*Set the timeout value*/
      if( nTimeout > 14)
      {
        /*Set default Timeout*/
        pHCISendBuffer[0] |= P_NFC_HAL_BINDING_DEFAULT_HCI_TIMEOUT;
      }
      else
      {
         /* set the timeout*/
         pHCISendBuffer[0] |= nTimeout;
      }
   }
   /* Set the HCI crc bit flags */
   switch(nHCIServiceIdentifier)
   {
      case P_HCI_SERVICE_MREAD_ISO_14_A_3:
         /* if the parity bit should not be added, the Chip doesn't add the CRC and doesn't check the received CRC. */
         if((nNALControlByte & NAL_ISO_14_A_3_ADD_FIXED_BIT_NUMBER) != 0 )
         {
            pHCISendBuffer[0] |= P_NFC_HAL_BINDING_HCI_14_3A_EXTENDED_OPTION;
            pHCISendBuffer[1] = ((0x07 & pNALBuffer[nNALBufferLength - 2]) << 4) | (0x07 & pNALBuffer[(nNALBufferLength - 1)]);

            /* the byte containing the number of bit to send must be removed from the payload sent to the card.
            in case of sending 0 bit of the last byte, we need to keep the last byte because no bit of the last byte are sent*/
            nIndex = 2;

            nNALBufferLength -= 2;

         }else
         {

            if((nNALControlByte & NAL_ISO_14_A_3_CRC_CHECK) != 0)
            {
               pHCISendBuffer[0] |= P_NFC_HAL_BINDING_HCI_GENERATE_CRC | P_NFC_HAL_BINDING_HCI_CHECK_CRC;
            }
            else
            {
               pHCISendBuffer[0] |= P_NFC_HAL_BINDING_HCI_GENERATE_CRC;
            }

            if ((nNALControlByte & NAL_ISO_14_A_3_T2T_ACK_NACK_CHECK) != 0)
            {
               pHCISendBuffer[0] |= P_NFC_HAL_BINDING_HCI_14_3A_EXTENDED_OPTION;
               pHCISendBuffer[1] = 0x40;
               nIndex = 2;
            }
         }
         break;
      case P_HCI_SERVICE_MREAD_NFC_T1:
         /* Add crc bytes */
         static_PNALBindingComputeCrc(
                                      P_NFC_HAL_BINDING_CRC_B,
                                      &pNALBuffer[1],
                                      (nNALBufferLength - 1),
                                      &pHCISendBuffer[nNALBufferLength],
                                      &pHCISendBuffer[nNALBufferLength + 1]);
      break;
      case P_HCI_SERVICE_MREAD_ISO_15_3:
          if((nNALControlByte & NAL_ISO_15_3_SEND_EOF_ONLY) != 0)
         {
            pHCISendBuffer[0] |= P_NFC_HAL_BINDING_HCI_15_3_SEND_EOF;
         }
         else
         {
            pHCISendBuffer[0] |= P_NFC_HAL_BINDING_HCI_GENERATE_CRC | P_NFC_HAL_BINDING_HCI_CHECK_CRC;
         }
      break;
      default:
         pHCISendBuffer[0] |= P_NFC_HAL_BINDING_HCI_GENERATE_CRC | P_NFC_HAL_BINDING_HCI_CHECK_CRC;
         break;
   }
   /*copy Raw Frame Data*/
   CNALMemoryCopy(
        &pHCISendBuffer[nIndex],
        &pNALBuffer[1],
        (nNALBufferLength - 1));

   /* Set the HCI buffer Length */
   *pHCISendBufferLength = nNALBufferLength - 1 + nIndex;

   if(nHCIServiceIdentifier == P_HCI_SERVICE_MREAD_NFC_T1)
   {
      *pHCISendBufferLength += 2;
   }
 }

/**
 * @brief   returns the Reader Service Identifier.
 *
 * @param[in]  nHCIProtocolCapability The HCI protocol.
 *
 *@return  The HCI Reader service Identifier
 **/
static P_NAL_INLINE uint8_t static_PNALBindingGetHCIServiceReaderIdentifierFromHCIProtocol(
                        uint16_t nHCIProtocolCapability)
{
   uint8_t nProtocolIndex;

   for(nProtocolIndex = 0; nProtocolIndex < sizeof(g_aNALBindingReaderProtocolArray)/sizeof(tNALBindingProtocolEntry); nProtocolIndex++)
   {
      if((g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIProtocolCapability & nHCIProtocolCapability) != 0)
      {
         return  g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier;
      }
   }
   return P_NFC_HAL_BINDING_HCI_SERVICE_NONE;
}

/**
 * @brief   returns the card Service Identifier.
 *
 * @param[in]  nHCIProtocolCapability The HCI protocol.
 *
 *@return  The HCI card service Identifier
 **/
static P_NAL_INLINE uint8_t static_PNALBindingGetHCIServiceCardIdentifierFromHCIProtocol(
                        uint16_t nHCIProtocolCapability)
{
   uint8_t nProtocolIndex;

   for(nProtocolIndex = 0; nProtocolIndex < sizeof(g_aNALBindingCardProtocolArray)/sizeof(tNALBindingProtocolEntry); nProtocolIndex++)
   {
      if((g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIProtocolCapability & nHCIProtocolCapability) != 0)
      {
         return  g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIServiceIdentifier;
      }
   }
   return P_NFC_HAL_BINDING_HCI_SERVICE_NONE;
}

/**
 * @brief   returns the  HCI Service.
 *
 * @param[in]  pBindingContext The NFC HAL Binding Instance
 *
 * @param[in]  nNALServiceIdentifier The NFC HAL Service Identifier.
 *
 * @param[in]  nMessageParam The NFC HAL Message Param Code.
 *
 *@return  The  HCI Service Protocol.
 **/
static uint8_t static_PNALBindingGetHCIServiceIdentifier(
                                                       tNALBindingContext *pBindingContext,
                                                       uint8_t nNALServiceIdentifier,
                                                       uint8_t nMessageParam)
{
   uint8_t nProtocolIndex;

/*    uint8_t nHCIServiceIdentifier; */

   for(nProtocolIndex = 0; nProtocolIndex < sizeof(g_aNALBindingReaderProtocolArray)/sizeof(tNALBindingProtocolEntry); nProtocolIndex++)
   {
      if(g_aNALBindingReaderProtocolArray[nProtocolIndex].nNALServiceIdentifier == nNALServiceIdentifier)
      {
         return  g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier;
      }
   }
   for(nProtocolIndex = 0; nProtocolIndex < sizeof(g_aNALBindingCardProtocolArray)/sizeof(tNALBindingProtocolEntry); nProtocolIndex++)
   {
      if(g_aNALBindingCardProtocolArray[nProtocolIndex].nNALServiceIdentifier == nNALServiceIdentifier)
      {
         return  g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIServiceIdentifier;
      }
   }

   return P_NFC_HAL_BINDING_HCI_SERVICE_NONE;
}

/**
 * @brief   returns the CARDX_MODE parameter : X equals to A, B.....
 *
 * @param[in]  The HCI card Service Identifier.
 *
 **/
static P_NAL_INLINE uint8_t static_PNALBindingGetCardTypeMode(
                        uint8_t nHCICardServiceIdentifier)
{
   switch(nHCICardServiceIdentifier)
   {
      case P_HCI_SERVICE_MCARD_ISO_14_A_4:
         return HCI2_PAR_MCARD_ISO_A_MODE;
      case P_HCI_SERVICE_MCARD_ISO_14_B_4:
         return HCI2_PAR_MCARD_ISO_B_MODE;
      case P_HCI_SERVICE_P2P_TARGET:
         return HCI2_PAR_P2P_TARGET_MODE;
      default:
         PNALDebugWarning("static_PNALBindingGetCardTypeMode:unknow Card protocol Identifier: 0x%2x", nHCICardServiceIdentifier);
         return 0x01;;
  }
}

/**
 * @brief   Check the authorization of the NFC HAL Message according the NFC state.
 *
 * @param[in]  nNALMessageCode The NFC HAL Message code (cdm, event, ans) value.
 *
 * @param[in]  nNALParamCode The NFC HAL Parameter code.
 *
 * @param[in]  nNALBindingStatus The NFC HAL Binding State.
 *
 *@return :W_TRUE if the Message is accepted
 *         W_FALSE if the message is denied
 **/
static  bool_t static_PNALBindingMsgAuthorization(
                        uint8_t nNALMessageCode,
                        uint8_t nNALParamCode,
                        uint32_t nNALBindingStatus)
{
   bool_t bResult = W_FALSE;

   switch(nNALBindingStatus)
   {
      case P_NFC_HAL_BINDING_VALID_STATUS:
         bResult = W_TRUE;
      break;
      case P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS:
         if(((nNALMessageCode == NAL_CMD_GET_PARAMETER)&&
           (nNALParamCode == NAL_PAR_HARDWARE_INFO))||
           (nNALMessageCode == NAL_CMD_UPDATE_FIRMWARE))
         {
            bResult = W_TRUE;
         }
         break;
      case P_NFC_HAL_BINDING_NO_HARDWARE_STATUS:
         if(nNALMessageCode == NAL_CMD_UPDATE_FIRMWARE)
         {
            bResult = W_TRUE;
         }
         break;
      case P_NFC_HAL_BINDING_INIT_STATUS:
         break;
      default:
       break;
   }
   return bResult;
}

/**
 * @brief The Production test EVENT has been sent
 *
 * @see Definition of PHCIServiceExecuteCommand
 **/
static void static_PNALBindingProductTestExecuteEvtCompleted(
            tNALBindingContext* pBindingContext,
            void* pCallbackParameter,
            W_ERROR nError,
            uint32_t nReceptionCounter )
{
   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   pBindingContext->pNALReceptionBuffer[0] = NAL_SERVICE_ADMIN;

   PNALDebugTrace(
         "static_PNALBindingProductTestExecuteEvtCompleted: executed Event code: 0x%08X",
                   (uint8_t)PNALUtilConvertPointerToUint32(pCallbackParameter));
   /* Check the result */
   if ( nError != W_SUCCESS )
   {
      PNALDebugError(
         "static_PNALBindingProductTestExecuteEvtCompleted: nError = %s", PNALUtilTraceError(nError) );
      pBindingContext->pNALReceptionBuffer[1] = NAL_RES_BAD_STATE;
   }
   else
   {
      pBindingContext->pNALReceptionBuffer[1] = NAL_RES_OK;
   }

   /* Post the result*/
   PNALBindingCallReadCallback(pBindingContext, 0x02, pBindingContext->nReceptionCounter);
}

/**
 * @brief The Production test COMMAND has been executed
 *
 * @see Definition of PHCIServiceExecuteCommand
 **/
static void static_PNALBindingProductTestExecuteCmdCompleted(
            tNALBindingContext* pBindingContext,
            void* pCallbackParameter,
            uint8_t* pBuffer,
            uint32_t nLength,
            W_ERROR nError,
            uint8_t nStatusCode,
            uint32_t nReceptionCounter)
{
   uint32_t nNALLength = 0x02;

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   pBindingContext->pNALReceptionBuffer[0] = NAL_SERVICE_ADMIN;

   PNALDebugTrace(
         "static_PNALBindingProductTestExecuteCmdCompleted: executed command code: 0x%08X",
                   (uint8_t)PNALUtilConvertPointerToUint32(pCallbackParameter));

   if (( nStatusCode != ETSI_ERR_ANY_OK )||
      ( nError != W_SUCCESS ))
   {
      PNALDebugError(
         "static_PNALBindingProductTestExecuteCmdCompleted: nStatusCode: 0x%08X, nError: 0x%08X",
                   nStatusCode, nError );
          /* Test not executed  */
         pBindingContext->pNALReceptionBuffer[1] = NAL_RES_BAD_STATE;
   }
   else
   {
      /* Test executed */
      pBindingContext->pNALReceptionBuffer[1] = NAL_RES_OK;
      if(( nLength > 0)&&
         (pBuffer != null))
      {
         CNALMemoryCopy(
               &pBindingContext->pNALReceptionBuffer[2],
               pBuffer,
               nLength);
         nNALLength += nLength;
      }
   }

   /* Post the result*/
   PNALBindingCallReadCallback(pBindingContext, nNALLength, pBindingContext->nReceptionCounter);
}

/**
 * @brief The self test COMMAND has been executed
 *
 * @see Definition of PHCIServiceExecuteCommand
 **/
static void static_PNALBindingSelfTestExecuteCmdCompleted(
            tNALBindingContext* pBindingContext,
            void* pCallbackParameter,
            uint8_t* pBuffer,
            uint32_t nLength,
            W_ERROR nError,
            uint8_t nStatusCode,
            uint32_t nReceptionCounter)
{
   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   pBindingContext->pNALReceptionBuffer[0] = NAL_SERVICE_ADMIN;

   if (( nStatusCode != ETSI_ERR_ANY_OK )||
      ( nError != W_SUCCESS ))
   {
      PNALDebugError(
         "static_PNALBindingSelfTestExecuteCmdCompleted: nStatusCode: 0x%08X, nError: 0x%08X",
                   nStatusCode, nError );
          /* Test not executed  */
         pBindingContext->pNALReceptionBuffer[1] = NAL_RES_BAD_STATE;
         pBindingContext->pNALReceptionBuffer[2] = 0x00;
   }
   else if(pBuffer == null)
   {
       PNALDebugError(
          "static_PNALBindingSelfTestExecuteCmdCompleted: null buffer");
       pBindingContext->pNALReceptionBuffer[1] = NAL_RES_BAD_STATE;
       pBindingContext->pNALReceptionBuffer[2] = 0x00;
   }
   else
   {
      /* Test executed */
      pBindingContext->pNALReceptionBuffer[1] = NAL_RES_OK;
      /* Set the status code: 0 means success */
      pBindingContext->pNALReceptionBuffer[2] = 0x00;
   }

   /* Post the result*/
   PNALBindingCallReadCallback(pBindingContext, 0x03, pBindingContext->nReceptionCounter);
}

/**
 * @brief   Build the HCI Product test command from NFC HAL command parameters.
 *
 * @param[in]  pBindingContext The NFC HAL Binding Instance
 *
 * @param[in]  pNALParamBuffer The NFC HAL buffer containing parameters.
 *
 * @param[in]  nNALParamLength The NFC HAL parameters length.
 *
 * @param[out]  pHCIBuffer The HCI buffer.
 *
 *@return :W_TRUE if the mapping is successfull, otherwise W_FALSE.
 *
 **/
static bool_t static_PNALBindingSendHCIProductTestCommand(
                               tNALBindingContext *pBindingContext,
                               uint8_t * pNALParamBuffer,
                               uint32_t nNALParamLength,
                               uint8_t * pHCIBuffer)
{
   bool_t bError = W_TRUE;
   uint8_t nHCIPipeIdentifier;
   uint8_t nHCIMsgType;
   uint8_t nHCIMsgCode;
   uint8_t nPipe;

   if((pNALParamBuffer == null)||
    (nNALParamLength < 2))
   {
      PNALDebugError("static_PNALBindingSendHCIProductTestCommand: bad parameters");
      return W_FALSE;
   }
   nHCIPipeIdentifier = pNALParamBuffer[0];
   /* get the type of Message */
   nHCIMsgType =  (pNALParamBuffer[1] & ETSI_MSG_TYPE_MASK);
   /* Retrieve the command or event code */
   nHCIMsgCode = (pNALParamBuffer[1] & ETSI_MSG_INFO_MASK);

   switch (nHCIPipeIdentifier)
   {
      case HCI2_PIPE_ID_LMS :
         nPipe = P_HCI_SERVICE_LINK_MANAGEMENT;
         break;

      case HCI2_PIPE_ID_ADMIN  :
         nPipe = P_HCI_SERVICE_ADMINISTRATION;
         break;

      case HCI2_PIPE_ID_MGT :
         PNALDebugTrace("static_PNALBindingSendHCIProductTestCommand: used for the production test");
         nPipe = P_HCI_SERVICE_PIPE_MANAGEMENT;
         break;

      case HCI2_PIPE_ID_OS  :
         nPipe = P_HCI_SERVICE_FIRMWARE;
         break;

      case HCI2_PIPE_ID_HDS_IDT :
         nPipe = P_HCI_SERVICE_IDENTITY;
         break;

      case HCI2_PIPE_ID_HDS_MCARD_ISO_B :
         nPipe = P_HCI_SERVICE_MCARD_ISO_14_B_4;
         break;

      case HCI2_PIPE_ID_HDS_MCARD_ISO_BPRIME :
         nPipe = P_HCI_SERVICE_MCARD_BPRIME;
         break;

      case HCI2_PIPE_ID_HDS_MCARD_ISO_A :
         nPipe = P_HCI_SERVICE_MCARD_ISO_14_A_4;
         break;

      case HCI2_PIPE_ID_HDS_MCARD_ISO_15_3 :
         nPipe = P_HCI_SERVICE_MCARD_ISO_15_3;
         break;

      case HCI2_PIPE_ID_HDS_MCARD_ISO_15_2 :
         nPipe = P_HCI_SERVICE_MCARD_ISO_15_2;
         break;

      case HCI2_PIPE_ID_HDS_MCARD_NFC_T3 :
         nPipe = P_HCI_SERVICE_MCARD_FELICA;
         break;

      case HCI2_PIPE_ID_HDS_MCARD_ISO_B_2 :
         nPipe = HCI2_PIPE_ID_HDS_MCARD_ISO_B_2;
         break;

      case HCI2_PIPE_ID_HDS_MCARD_CUSTOM :
         nPipe = P_UNKNOW_SERVICE_ID;
         break;

      case HCI2_PIPE_ID_HDS_P2P_TARGET :
         nPipe = P_HCI_SERVICE_P2P_TARGET;
         break;

      case HCI2_PIPE_ID_HDS_MREAD_ISO_B :
         nPipe = P_HCI_SERVICE_MREAD_ISO_14_B_4;
         break;

      case HCI2_PIPE_ID_HDS_MREAD_NFC_T1 :
         nPipe = P_HCI_SERVICE_MREAD_NFC_T1;
         break;

      case HCI2_PIPE_ID_HDS_MREAD_ISO_A :
         nPipe = P_HCI_SERVICE_MREAD_ISO_14_A_4;
         break;

      case HCI2_PIPE_ID_HDS_MREAD_ISO_15_3 :
         nPipe = P_HCI_SERVICE_MREAD_ISO_15_3;
         break;

      case HCI2_PIPE_ID_HDS_MREAD_ISO_15_2 :
         nPipe = P_HCI_SERVICE_MREAD_ISO_15_2;
         break;

      case HCI2_PIPE_ID_HDS_MREAD_NFC_T3 :
         nPipe = P_HCI_SERVICE_MREAD_FELICA;
         break;

      case HCI2_PIPE_ID_HDS_MREAD_ISO_B_3 :
         nPipe = P_HCI_SERVICE_MREAD_ISO_14_B_3;
         break;

      case HCI2_PIPE_ID_HDS_MREAD_BPRIME :
         nPipe = P_HCI_SERVICE_MREAD_BPRIME;
         break;

      case HCI2_PIPE_ID_HDS_MREAD_ISO_A_3 :
         nPipe = P_HCI_SERVICE_MREAD_ISO_14_A_3;
         break;

      case HCI2_PIPE_ID_HDS_P2P_INITIATOR :
         nPipe = HCI2_PIPE_ID_HDS_P2P_INITIATOR;
         break;

      case HCI2_PIPE_ID_HDS_SIM_CONNECTIVITY :
         nPipe = P_HCI_SERVICE_UICC_CONNECTIVITY;
         break;

      case HCI2_PIPE_ID_HDS_MREAD_GEN :
         nPipe = P_HCI_SERVICE_MREAD;
         break;

      case HCI2_PIPE_ID_HDS_STACKED_ELEMENT :
         nPipe = P_HCI_SERVICE_SE;
         break;

      case HCI2_PIPE_ID_HDS_INSTANCES :
         nPipe = P_HCI_SERVICE_INSTANCE;
         break;

      case HCI2_PIPE_ID_HDS_MREAD_KOVIO :
         nPipe = P_HCI_SERVICE_MREAD_KOVIO;
         break;

      case HCI2_PIPE_ID_HDS_TEST_RF:
         nPipe = P_HCI_SERVICE_TEST_RF;
         break;

      default :
         /* invalid PIPE ID */
         nPipe = P_UNKNOW_SERVICE_ID;
         break;
   }

   if ((nPipe != P_HCI_SERVICE_PIPE_MANAGEMENT) &&
       (nPipe != P_HCI_SERVICE_TEST_RF) && ((nHCIMsgType != ETSI_MSG_TYPE_CMD) || (nHCIMsgCode != ETSI_CMD_ANY_GET_PARAMETER)))
   {
      PNALDebugError("static_PNALBindingSendHCIProductTestCommand: unsupported message type");
      return W_FALSE;
   }

   if(nNALParamLength > 0x02)
   {
      CNALMemoryCopy(
         pHCIBuffer,
         &pNALParamBuffer[2],
         (nNALParamLength - 0x02));
   }
   if(nHCIMsgType == ETSI_MSG_TYPE_EVT)
   {
      PHCIServiceSendEvent(
                        pBindingContext,
                        nPipe,
                        PNALBindingGetOperation(pBindingContext),
                        nHCIMsgCode,
                        pHCIBuffer,
                        (nNALParamLength - 0x02),
                        static_PNALBindingProductTestExecuteEvtCompleted,
                        PNALUtilConvertUint32ToPointer(nHCIMsgCode),
                        W_FALSE );
   }
   else if(nHCIMsgType == ETSI_MSG_TYPE_CMD)
   {
      PHCIServiceExecuteCommand(
                        pBindingContext,
                        nPipe,
                        PNALBindingGetOperation(pBindingContext),
                        nHCIMsgCode,
                        pHCIBuffer, (nNALParamLength - 0x02),
                        pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
                        static_PNALBindingProductTestExecuteCmdCompleted, PNALUtilConvertUint32ToPointer(nHCIMsgCode),
                        W_FALSE
                        );
   }
   else
   {
     PNALDebugError("static_PNALBindingSendHCIProductTestCommand: unknown Message type");
     return W_FALSE;
   }
   return bError;
}

/**
 * @brief The ETSI_CMD_WR_XCHG_DATA has been executed
 *
 * @see Definition of PHCIServiceExecuteCommand
 **/
static void static_PNALBindingExchangeDataExecuteCommandCompleted(
            tNALBindingContext* pBindingContext,
            void* pCallbackParameter,
            uint8_t* pBuffer,
            uint32_t nLength,
            W_ERROR nError,
            uint8_t nStatusCode,
            uint32_t nReceptionCounter)
{
   uint32_t nNALReceptionBufferLength;
   uint8_t nNALServiceIdentifier = (uint8_t)PNALUtilConvertPointerToUint32(pCallbackParameter);

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   pBindingContext->pNALReceptionBuffer[0] = nNALServiceIdentifier;
   pBindingContext->pNALReceptionBuffer[1] = NAL_RES_OK;
   nNALReceptionBufferLength = 0x02;
   if (( nStatusCode != ETSI_ERR_ANY_OK )||
      ( nError != W_SUCCESS ))
   {
      PNALDebugError(
         "static_PNALBindingExchangeDataExecuteCommandCompleted: nStatusCode: 0x%08X, nError: 0x%08X",
                   nStatusCode, nError );
      nLength = 0x00;
      if((nStatusCode == ETSI_ERR_ANY_E_TIMEOUT)||
         (nError == W_ERROR_TIMEOUT))
      {
         pBindingContext->pNALReceptionBuffer[1] = NAL_RES_TIMEOUT;
      }
      else
      {
         pBindingContext->pNALReceptionBuffer[1] = NAL_RES_PROTOCOL_ERROR;
      }
   }
   else if(pBuffer == null)
   {
       PNALDebugError(
          "static_PNALBindingExchangeDataExecuteCommandCompleted: null buffer");
       pBindingContext->pNALReceptionBuffer[1] = NAL_RES_PROTOCOL_ERROR;
       nLength = 0x00;
   }
   else
   {
      /* In case of exchanging data in bit mode, the following value is used to specify the number of bit returned.*/
      uint8_t nBitReturned = 0;

      /*
       * Decrement the size of the answer of one byte
       * due to supplemental RF status byte in HCI2_CMD_MREADER_EXCHANGE from MR fm 6.11a
       */
      nLength--;

      if(pBindingContext->pNALReceptionBuffer[0] == NAL_SERVICE_READER_TYPE_1)
      {
            if(nLength < 0x03)
            {
               /*Bad Frame*/
               PNALDebugError(
                 "static_PNALBindingExchangeDataExecuteCommandCompleted: bad length: 0x%x", nLength);
               pBindingContext->pNALReceptionBuffer[1] = NAL_RES_PROTOCOL_ERROR;
               nLength = 0x00;
            }
            else
            {
               uint8_t nCrcFirst, nCrcSecond;
               /* Calcul the CRC bytes*/
               static_PNALBindingComputeCrc(
                                         P_NFC_HAL_BINDING_CRC_B,
                                         pBuffer,
                                         (nLength - 2),
                                         &nCrcFirst,
                                         &nCrcSecond);
               /* Check the crc bytes */
               if((nCrcFirst != pBuffer[nLength - 2])
                ||(nCrcSecond != pBuffer[nLength - 1]))
                {
                    /*Bad Frame*/
                   PNALDebugError(
                     "static_PNALBindingExchangeDataExecuteCommandCompleted: bad crc ");
                   pBindingContext->pNALReceptionBuffer[1] = NAL_RES_PROTOCOL_ERROR;
                   nLength = 0x00;
                }
                else
                {
                   nLength -= 0x02;
                }
          }
      }

      if (pBindingContext->pNALReceptionBuffer[0] == NAL_SERVICE_READER_14_A_3)
      {
         if( pBindingContext->bBitMode == W_TRUE )
         {
            /* if data must be returned, the number of bit of the last byte must be calculated.
               Then, the last byte must be right shifted (see documentation)
            */
            if(nLength > 0)
            {
               uint32_t nRealNumberOfBit = 0;
               if(pBindingContext->nExpectedBitBeforeByte)
               {
                  nBitReturned = pBindingContext->nExpectedBitBeforeByte;
                  nLength = 1; /* only the first byte is used */
                  pBuffer[0] >>= (8 - pBindingContext->nExpectedBitBeforeByte);
               }
               else
               {
                  (void)static_PNALBindingConvertByteToBit(nLength, &nRealNumberOfBit);
                  nBitReturned = (uint8_t) (8 - ((8 * nLength) - nRealNumberOfBit));
                  /* left shift 8 - nBitReturned because the last received bit is b6 (byte 7) */
                  if(nBitReturned < 8)
                  {
                     /* shift last byte */
                     pBuffer[nLength - 1] = (pBuffer[nLength - 1] >> (7 - nBitReturned));
                  }
               }
            }
         }
         else
         {
            /* Check NACK / ACK */
            uint8_t* pHCISendBuffer = pBindingContext->aNALBindingNALService[nNALServiceIdentifier].aNALOperationBuffer;

            if ( ((pHCISendBuffer[0] & P_NFC_HAL_BINDING_HCI_14_3A_EXTENDED_OPTION) != 0) &&
                  (pHCISendBuffer[1] == 0x40))
            {
               if (pBuffer[0] != 0xA0)
               {
                  pBindingContext->pNALReceptionBuffer[1] = NAL_RES_PROTOCOL_ERROR;
               }

               nLength = 0;
            }
         }
      }

#ifdef P_INCLUDE_MIFARE_CLASSIC
      /*-----------  Mifare treatment ------------*/
      if ((pBindingContext->pNALReceptionBuffer[0] == NAL_SERVICE_READER_14_A_3)
      && (pBindingContext->bMifareExchange == W_TRUE) )
      {
         uint32_t nRealNumberOfBit = 0;
         uint8_t * pHCISendBuffer = pBindingContext->aNALBindingNALService[nNALServiceIdentifier].aNALOperationBuffer;
         uint8_t nHCIServiceIdentifier = static_PNALBindingGetHCIServiceIdentifier(pBindingContext, nNALServiceIdentifier, 0);

         (void)static_PNALBindingConvertByteToBit(nLength, &nRealNumberOfBit);

         nStatusCode = static_PNALBindingBuildHCIForMifareXchangeDataCmd(
                                    pBindingContext,
                                    nNALServiceIdentifier,
                                    pHCISendBuffer,
                                    &pBindingContext->nHCICmdBufferLength,
                                    pBuffer,
                                    nRealNumberOfBit,
                                    pBindingContext->pNALReceptionBuffer[1]);

         /* If error or no data to send, the mifare operation is completed */
         if( (nStatusCode != NAL_RES_OK)
           ||(pBindingContext->nHCICmdBufferLength == 0))
         {
            static_PNALBindingMifareExchangeCompleted(pBindingContext, nStatusCode);
         }
         else
         {

            PHCIServiceExecuteCommand(
                     pBindingContext,
                     nHCIServiceIdentifier,
                     PNALBindingGetOperation(pBindingContext),
                     ETSI_CMD_WR_XCHG_DATA,
                     pHCISendBuffer,
                     pBindingContext->nHCICmdBufferLength,
                     pBindingContext->aHCIResponseBuffer,
                     sizeof(pBindingContext->aHCIResponseBuffer),
                     static_PNALBindingExchangeDataExecuteCommandCompleted,
                     PNALUtilConvertUint32ToPointer(nNALServiceIdentifier),
                     W_FALSE );
         }

         return;
      }
#endif


      if(nLength != 0x00)
      {
         CNALMemoryCopy(
                 &pBindingContext->pNALReceptionBuffer[2],
                 pBuffer,
                 nLength);
         nNALReceptionBufferLength += nLength;



         if (pBindingContext->pNALReceptionBuffer[0] == NAL_SERVICE_READER_14_A_3
          && pBindingContext->bBitMode == W_TRUE)
         {
            /* the number of bit must be added at the end of the payload returned */
            pBindingContext->pNALReceptionBuffer[nNALReceptionBufferLength] = nBitReturned;
            nNALReceptionBufferLength += 1;
         }
      }
   }

   /* post the result*/
   PNALBindingCallReadCallback(pBindingContext, nNALReceptionBufferLength, pBindingContext->nReceptionCounter);
}

/**
 * @See NFC HAL Binding header file
 **/
tNALVoidContext* static_PNALBindingCreate(
         void* pPortingConfig,
         void* pCallbackContext,
         uint8_t* pReceptionBuffer,
         uint32_t nReceptionBufferLength,
         tNALBindingReadCompleted* pReadCallbackFunction,
         void* pCallbackParameter,
         uint32_t nAutoStandbyTimeout,
         uint32_t nStandbyTimeout,
         tNALBindingTimerHandler* pTimerHandlerFunction,
         tNALBindingAntropySourceHandler* pAntropySourceHandlerFunction)
{
   uint32_t nComType;
   tNALBindingContext* pBindingContext = & g_sNALBindingContext;

   CNALMemoryFill(pBindingContext, 0, sizeof(tNALBindingContext));

   CNALSyncCreateCriticalSection(&pBindingContext->hCriticalSection);

   /* store NFC HAL read Function and parameters*/
   pBindingContext->pNALReadCallbackFunction = pReadCallbackFunction;
   pBindingContext->pNALReadCallbackParameter = pCallbackParameter;
   pBindingContext->pNALReceptionBuffer = pReceptionBuffer;
   pBindingContext->nNALReceptionBufferLengthMax = nReceptionBufferLength;
   pBindingContext->pPortingConfig = pPortingConfig;
   pBindingContext->pCallbackContext = pCallbackContext;
   pBindingContext->pNALTimerHandlerFunction = pTimerHandlerFunction;
   pBindingContext->pNALAntropySourceHandlerFunction = pAntropySourceHandlerFunction;

   pBindingContext->nAutoStandbyTimeout = nAutoStandbyTimeout;
   pBindingContext->nStandbyTimeout = nStandbyTimeout;

   if ((pBindingContext->pNALInstance = CNALPreCreate(pPortingConfig)) == null)
   {
      PNALDebugError("static_PNALBindingCreate: CNALPreCreate failed");
      goto return_error;
   }

   if((pBindingContext->pComPort = CNALComCreate(pPortingConfig, &nComType)) == null)
   {
      PNALDebugError("static_PNALBindingCreate: Cannot create the com port");
      goto return_error;
   }

   PSHDLCFrameCreate(&pBindingContext->sSHDLCFrame, pBindingContext->pComPort, nComType);

   if((pBindingContext->pTimer = CNALTimerCreate(pPortingConfig)) == null)
   {
      PNALDebugError("static_PNALBindingCreate: Cannot create the timer");
      goto return_error;
   }

   PNALMultiTimerCreate(&pBindingContext->sNALMultiTimer, pBindingContext->pTimer);

   PSHDLCCreate(&pBindingContext->sSHDLCInstance);

   PHCIFrameCreate(&pBindingContext->sHCIFrameInstance);

   PHCIServiceCreate(&pBindingContext->sHCIServiceInstance);

   pBindingContext->nMode = W_NFCC_MODE_BOOT_PENDING;

   if(PNALDFCCreate(&pBindingContext->sDFCQueue) == W_FALSE)
   {
      PNALDebugError("static_PNALBindingCreate: Cannot create the DFC Queue");
      goto return_error;
   }

   if (CNALPostCreate(pBindingContext->pNALInstance, pBindingContext) == W_FALSE)
   {
      PNALDebugError("CNALPostCreate failed");
      goto return_error;
   }

   return (tNALVoidContext*)pBindingContext;

return_error:

   static_PNALBindingDestroy((tNALVoidContext*)pBindingContext);

   return null;
}

/**
 * @See NFC HAL Binding header file
 **/
static void static_PNALBindingDestroy(
         tNALVoidContext* pNALContext)
{
   tNALBindingContext* pBindingContext = (tNALBindingContext*)pNALContext;

   if(pBindingContext == null)
   {
      return;
   }

   CNALPreDestroy(pBindingContext->pNALInstance);

   PHCIServiceDestroy(&pBindingContext->sHCIServiceInstance);

   PHCIFrameDestroy(&pBindingContext->sHCIFrameInstance);

   PSHDLCDestroy(&pBindingContext->sSHDLCInstance);

   PSHDLCFrameDestroy(&pBindingContext->sSHDLCFrame);

   CNALComDestroy(pBindingContext->pComPort);

   PNALDFCDestroy(&pBindingContext->sDFCQueue);

   PNALMultiTimerDestroy(&pBindingContext->sNALMultiTimer);

   CNALTimerDestroy(pBindingContext->pTimer);

   CNALSyncDestroyCriticalSection(&pBindingContext->hCriticalSection);

   CNALPostDestroy(pBindingContext->pNALInstance);

   CNALMemoryFill(pBindingContext, 0, sizeof(tNALBindingContext));
}

/**
 * @See NFC HAL Binding header file
 **/
static void static_PNALBindingReset(
         tNALVoidContext* pNALContext)
{
   tNALBindingContext* pBindingContext = (tNALBindingContext*)pNALContext;
   uint32_t i;
   uintptr_t nLengthToReset;

   if( pNALContext == null )
   {
      PNALDebugWarning( "static_PNALBindingReset() called with null context: error in creation of the Driver ?" );
      return;
   }

   CNALSyncEnterCriticalSection(&pBindingContext->hCriticalSection);

   nLengthToReset = ((uint8_t*)&(pBindingContext->pCallbackContext)) - ((uint8_t*)pBindingContext);

   CNALDebugAssert(nLengthToReset <= (uintptr_t)((uint32_t)-1));
   CNALMemoryFill(pBindingContext, 0x00, (uint32_t)nLengthToReset);

   /* initialize parameters */
   for (i=0; i< sizeof(pBindingContext->aHCIServiceOperations) / sizeof(pBindingContext->aHCIServiceOperations[0]); i++)
   {
      pBindingContext->aHCIServiceOperations[i].nState = P_HCI_OPERATION_STATE_COMPLETED;
   }

   CNALSyncLeaveCriticalSection(&pBindingContext->hCriticalSection);
}

/**
 * @See Definition of PHCIServiceConnect().
 **/
static void static_PNALBindingServiceConnectCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter )
{
   if (pBindingContext->nResetType == NAL_SIGNAL_RESET) {
      PNALBindingBootMachine(
                              pBindingContext,
                              P_NFC_HAL_BINDING_BOOT_START);
   }
   else
   {
      pBindingContext->nNextWakeUpState = P_NFC_HAL_BINDING_WAKE_UP_START_FROM_RESET;

      PNALBindingWakeUpMachine(
                              pBindingContext);
   }
}

/**
 * @See NFC HAL Binding header file
 **/
static void static_PNALBindingConnect(
         tNALVoidContext* pNALContext,
         uint32_t nType,
         tNALBindingConnectCompleted* pCallbackFunction,
         void* pCallbackParameter)
{
   uint32_t nResetType = P_RESET_BOOT;
   tNALBindingContext *pBindingContext = (tNALBindingContext*)pNALContext;

   CNALSyncEnterCriticalSection(&pBindingContext->hCriticalSection);

   pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_INIT_STATUS;
   pBindingContext->pCallbackConnectionFunction = pCallbackFunction;
   pBindingContext->pInternalCallbackConnectionFunction = null;
   pBindingContext->pCallbackConnectionParameter = pCallbackParameter;
   pBindingContext->bRawMode = W_FALSE;

   CNALDebugAssert((nType == NAL_SIGNAL_RESET) || (nType == NAL_SIGNAL_WAKEUP));
   if(nType != NAL_SIGNAL_RESET)
   {
      nResetType = P_RESET_WAKEUP;
   }

   PHCIServicePreReset(pBindingContext);

   CNALResetNFCController(pBindingContext->pPortingConfig, nResetType);

   pBindingContext->bIsResetPending = W_TRUE;
   pBindingContext->nResetType = nResetType;

   CNALSyncLeaveCriticalSection(&pBindingContext->hCriticalSection);
}

/**
  * Converts a detect pulse duration into the corresponding card detect parameter value
  */

static void static_PNALBindingPulseDurationToCardDetectConfig(
   tNALBindingContext          * pBindingContext,
   const uint16_t                nNALDetectPulse,
   uint8_t                      * pBuffer)
{
   /* The MR supports only 4 durations : 110 ms, 220 ms, 430 ms and 740 ms
      [000-165[ -> 110 ms   [165-325[ -> 220 ms   [325-585[ -> 430 ms  [585-INF[ -> 740 ms */

   if (nNALDetectPulse < 165)
   {
      CNALMemoryCopy(pBuffer, g_aCardDetectRFconfig[0], 2);
      pBindingContext->nCardDetectPulse = 110;
   }
   else if (nNALDetectPulse < 325)
   {
      CNALMemoryCopy(pBuffer, g_aCardDetectRFconfig[1], 2);
      pBindingContext->nCardDetectPulse = 220;
   }
   else if (nNALDetectPulse < 585)
   {
      CNALMemoryCopy(pBuffer, g_aCardDetectRFconfig[2], 2);
      pBindingContext->nCardDetectPulse = 430;
   }
   else
   {
      CNALMemoryCopy(pBuffer, g_aCardDetectRFconfig[3], 2);
      pBindingContext->nCardDetectPulse = 740;
   }
}

static void static_PNALBindingBuildGetParameterAnswer(
   tNALBindingContext * pBindingContext,
   uint8_t              nServiceIdentifier,
   uint8_t              nMessageParam)
{
   uint32_t  nNALReceptionBufferLength;

   pBindingContext->pNALReceptionBuffer[0] = nServiceIdentifier;

   g_aNALBindingParameterEntryArray[nMessageParam].sGetParameter.pBuildGetParameterAnswer(
                                                                                pBindingContext,
                                                                                nServiceIdentifier,
                                                                                pBindingContext->pNALReceptionBuffer,
                                                                                &nNALReceptionBufferLength,
                                                                                null,
                                                                                0x00);

   PNALBindingCallReadCallback(pBindingContext, nNALReceptionBufferLength, pBindingContext->nReceptionCounter);
}

/**
 * @See NFC HAL Binding header file
 **/
static void static_PNALBindingWrite(
         tNALVoidContext* pNALContext,
         uint8_t* pBuffer,
         uint32_t nLength,
         tNALBindingWriteCompleted* pCallbackFunction,
         void* pCallbackParameter )
{
   tNALBindingContext * pBindingContext = (tNALBindingContext *) pNALContext;

   CNALSyncEnterCriticalSection(&pBindingContext->hCriticalSection);

   PNALBindingWrite(
         (tNALBindingContext*)pNALContext,
         pBuffer, nLength,
         pCallbackFunction, pCallbackParameter );

   CNALSyncLeaveCriticalSection(&pBindingContext->hCriticalSection);
}

static void static_PNALBindingGenericReadCallback(
      tNALBindingContext* pBindingContext,
      uint8_t             nNALServiceIdentifier,
      uint8_t             nResult)

{
   pBindingContext->pNALReceptionBuffer[0] = nNALServiceIdentifier;
   pBindingContext->pNALReceptionBuffer[1] = nResult;

   PNALBindingCallReadCallback(pBindingContext, 2, pBindingContext->nReceptionCounter);
}

static void static_PNALBindingRawWriteCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint32_t nReceptionCounter )
{
   /* Post the write callback */

   PNALDFCPost0(pBindingContext, P_DFC_TYPE_NFC_HAL_BINDING, static_PNALBindingCallWriteCallback);
}

/**
 * @See NFC HAL Binding header file
 **/
NFC_HAL_INTERNAL void PNALBindingWrite(
         tNALBindingContext* pBindingContext,
         uint8_t* pBuffer,
         uint32_t nLength,
         tNALBindingWriteCompleted* pCallbackFunction,
         void* pCallbackParameter )
{
   uint8_t nNALServiceIdentifier = pBuffer[0];
   uint8_t nMessageCode       = pBuffer[1];
   uint8_t nMessageParam;
   uint32_t nCmdBufferLength = 0x00;
   bool_t bImmediateResponse = W_FALSE;
   uint8_t nResult = NAL_RES_OK;
   uint8_t nHCIServiceIdentifier;

   uint8_t* pHCISendBuffer = pBindingContext->aNALBindingNALService[nNALServiceIdentifier].aNALOperationBuffer;

   /* some sanity checks */

   CNALDebugAssert(pBindingContext->pNALWriteCallbackFunction == null);

   /* store the current operation */
   pBindingContext->pNALWriteBuffer = pBuffer;
   pBindingContext->nNALWriteBufferLength = nLength;
   pBindingContext->pNALWriteCallbackFunction = pCallbackFunction;
   pBindingContext->pNALWriteCallbackParameter = pCallbackParameter;

   switch (pBindingContext->nHCIMode)
   {
      case W_NFCC_MODE_STANDBY :
         /* In low power mode, initiate the wake up */
         PNALBindingWakeUp(pBindingContext, null);

      case W_NFCC_MODE_SWITCH_TO_ACTIVE :
      case W_NFCC_MODE_SWITCH_TO_STANDBY :

         /* In these states, the command will be processed at the end of the procedure */
         return;

      default :
         /* other states : go on processing */
         break;
   }

   if (pBindingContext->bRawMode == W_FALSE)
   {
      /* check if the message shall be accepted*/
      if(static_PNALBindingMsgAuthorization(
                           nMessageCode,
                           pBuffer[2],
                           pBindingContext->nNALBindingStatus) == W_FALSE)
      {
         PNALDebugError("PNALBindingWrite:The NFC HAL Binding State (0x%2x)does not allow this message", pBindingContext->nNALBindingStatus);
         nResult = NAL_RES_BAD_STATE;

         /* Post the write callback */
         PNALDFCPost0(pBindingContext, P_DFC_TYPE_NFC_HAL_BINDING, static_PNALBindingCallWriteCallback);

         bImmediateResponse = W_TRUE;
         goto post_result;
      }

      switch(nMessageCode)
      {
         case NAL_CMD_SET_PARAMETER:

            /* upper layer ensure this pipe will not be used until the command has been acknowleded,
               we can call the write callback without waiting for effective command write completion  */
            PNALDFCPost0(pBindingContext, P_DFC_TYPE_NFC_HAL_BINDING, static_PNALBindingCallWriteCallback);

            nMessageParam  = pBuffer[2];
            pBindingContext->aNALBindingNALService[nNALServiceIdentifier].nMessageParam = nMessageParam;

            if (g_aNALBindingParameterEntryArray[nMessageParam].sSetParameter.pBuildSetParameterCommand)
            {
               nResult = g_aNALBindingParameterEntryArray[nMessageParam].sSetParameter.pBuildSetParameterCommand(
                                    pBindingContext,
                                    nNALServiceIdentifier,
                                    pBuffer,
                                    nLength,
                                    pHCISendBuffer,
                                    &nCmdBufferLength);

               if (nResult == NAL_RES_OK)
               {
                  if (nMessageParam != NAL_PAR_READER_CONFIG)
                  {
                     if (nCmdBufferLength !=0)
                     {
                        PHCIServiceExecuteMultiServiceCommand(
                           pBindingContext,
                           PNALBindingGetOperation(pBindingContext),
                           P_HCI_SERVICE_CMD_SET_PROPERTIES,
                           pHCISendBuffer,
                           nCmdBufferLength,
                           pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
                           static_PNALBindingSetParameterCompleted, PNALUtilConvertUint32ToPointer(nNALServiceIdentifier), W_FALSE);
                     }
                     else
                     {
                        /* nothing to do */
                        nResult = NAL_RES_OK;
                        bImmediateResponse = W_TRUE;
                     }
                  }
                  else
                  {
                     /* Reader parameters are configured using a SUBSCRIBE command */
                     nHCIServiceIdentifier = static_PNALBindingGetHCIServiceIdentifier(pBindingContext, nNALServiceIdentifier, 0);

                     PHCIServiceExecuteCommand(
                        pBindingContext,
                        nHCIServiceIdentifier,
                        PNALBindingGetOperation(pBindingContext),
                        HCI2_CMD_MREAD_SUBSCRIBE,
                        pHCISendBuffer,
                        nCmdBufferLength,
                        pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
                        static_PNALBindingSetParameterCompleted, PNALUtilConvertUint32ToPointer(nNALServiceIdentifier), W_FALSE);
                  }
               }
               else
               {
                  bImmediateResponse = W_TRUE;
               }
            }
            else
            {
               /* this parameter is read only */
               nResult = NAL_RES_UNKNOWN_PARAM;
               bImmediateResponse = W_TRUE;
            }
            break;

         case NAL_CMD_GET_PARAMETER:

            /* upper layer ensure this pipe will not be used until the command has been acknowleded,
               we can call the write callback without waiting for effective command write completion  */
            PNALDFCPost0(pBindingContext, P_DFC_TYPE_NFC_HAL_BINDING, static_PNALBindingCallWriteCallback);

            nMessageParam  = pBuffer[2];

            pBindingContext->aNALBindingNALService[nNALServiceIdentifier].nMessageParam = nMessageParam;

            if (g_aNALBindingParameterEntryArray[nMessageParam].sGetParameter.pGetParameterCommand)
            {
               CNALMemoryFill(pBindingContext->aHCIResponseBuffer, 0xCA, sizeof(pBindingContext->aHCIResponseBuffer));

               PHCIServiceExecuteMultiServiceCommand(
                  pBindingContext,
                  PNALBindingGetOperation(pBindingContext),
                  P_HCI_SERVICE_CMD_GET_PROPERTIES,
                  g_aNALBindingParameterEntryArray[nMessageParam].sGetParameter.pGetParameterCommand,
                  g_aNALBindingParameterEntryArray[nMessageParam].sGetParameter.nGetParameterCommandSize,
                  pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
                  static_PNALBindingGetParameterCompleted, PNALUtilConvertUint32ToPointer(nNALServiceIdentifier), W_FALSE);
            }
            else
            {
                 /* May be the requested infos are available in NFC HAL binding instance*/

               if(g_aNALBindingParameterEntryArray[nMessageParam].sGetParameter.pBuildGetParameterAnswer != null)
               {
                  PNALDFCPost2(pBindingContext, P_DFC_TYPE_NFC_HAL_BINDING,  static_PNALBindingBuildGetParameterAnswer, PNALUtilConvertUint32ToPointer((uint32_t)nNALServiceIdentifier), PNALUtilConvertUint32ToPointer((uint32_t)nMessageParam));
               }
               else
               {
                  /* this parameter is write-only */
                  nResult = NAL_RES_UNKNOWN_PARAM;
                  bImmediateResponse = W_TRUE;
               }
            }
            break;

         case NAL_CMD_DETECTION:

            /* upper layer ensure this pipe will not be used until the command has been acknowleded,
               we can call the write callback without waiting for effective command write completion  */
            PNALDFCPost0(pBindingContext, P_DFC_TYPE_NFC_HAL_BINDING, static_PNALBindingCallWriteCallback);

            /* copy the reader and card protocol capabilities*/
            pBindingContext->nNALCardProtocol   = static_PNALBindingReadUint16FromNALBuffer(&pBuffer[2]);
            pBindingContext->nNALReaderProtocol = static_PNALBindingReadUint16FromNALBuffer(&pBuffer[4]);

           /* Reset the  Reception Counters */
            pBindingContext->nLastStartSomeReceptionCounter = 0;

            /* Start the new detection */
            static_PNALBindingStartNewDetection(pBindingContext, nNALServiceIdentifier, pHCISendBuffer);
            break;

         case NAL_CMD_READER_XCHG_DATA:

            /* upper layer ensure this pipe will not be used until the command has been acknowleded,
               we can call the write callback without waiting for effective command write completion  */
            PNALDFCPost0(pBindingContext, P_DFC_TYPE_NFC_HAL_BINDING, static_PNALBindingCallWriteCallback);

            nHCIServiceIdentifier = static_PNALBindingGetHCIServiceIdentifier(pBindingContext, nNALServiceIdentifier, 0);

#ifdef P_INCLUDE_MIFARE_CLASSIC

            /* Special case for MIFARE */
            if( (nHCIServiceIdentifier == P_HCI_SERVICE_MREAD_ISO_14_A_3)
             && ( (pBuffer[2] & NAL_ISO_14_A_3_USE_MIFARE) == NAL_ISO_14_A_3_USE_MIFARE))
            {
               uint8_t nStatus = NAL_RES_OK;
               uint8_t nNALControlByte = pBuffer[2];
               uint8_t nTimeout = 0;

               if(nNALControlByte & NAL_TIMEOUT_READER_XCHG_DATA_ENABLE)
               {
                  nTimeout = (nNALControlByte & NAL_TIMEOUT_READER_XCHG_DATA_BITS_MASK)>> NAL_TIMEOUT_READER_XCHG_DATA_BITS_OFFSET;
               }

               pBindingContext->nCurrentMifareTimeout = nTimeout;

               nStatus = static_PNALBindingBuildHCIForMifareXchangeDataCmd(
                                         pBindingContext,
                                         nHCIServiceIdentifier,
                                         pHCISendBuffer,
                                         &nCmdBufferLength,
                                         &pBuffer[3],
                                         (nLength - 3),
                                         NAL_RES_OK);

               /* if an error occurred during the build of the HCI Command, the CmdBufferLength is set to 0 and an
                  internal callback indicating the error is called.*/
               if(  ( nCmdBufferLength == 0 )
                 || ( nStatus != NAL_RES_OK ) )
               {
                  static_PNALBindingMifareExchangeCompleted(pBindingContext, nStatus);
                  return;
               }
            }else
#endif
            {
               static_PNALBindingBuildHCIXchangeDataCmd(
                                         nHCIServiceIdentifier,
                                         pHCISendBuffer,
                                         &nCmdBufferLength,
                                         &pBuffer[2],
                                         (nLength - 2));
            }

            /* Exchange bit */
            if( ( nHCIServiceIdentifier == P_HCI_SERVICE_MREAD_ISO_14_A_3 )
             && ( ( pBuffer[2] & NAL_ISO_14_A_3_ADD_FIXED_BIT_NUMBER) != 0) )
            {
               /* Set the expected byte number */
               pBindingContext->nExpectedBitBeforeByte = ( 0x07 & (pHCISendBuffer[1] >> 4));

               if(pBindingContext->bBitMode == W_FALSE)
               {
                  pBindingContext->nHCICmdBufferLength = nCmdBufferLength;

                  PHCIServiceExecuteCommand(
                        pBindingContext,
                        P_HCI_SERVICE_MREAD,
                        PNALBindingGetOperation(pBindingContext),
                        P_HCI_SERVICE_CMD_SET_PROPERTIES,
                        aSetPicoreadParameterToRemoveParityBit,
                        sizeof(aSetPicoreadParameterToRemoveParityBit),
                        pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
                        static_PNALBindingSetPicoreadOptionParameterCompleted, PNALUtilConvertUint32ToPointer(nNALServiceIdentifier), W_FALSE);

                  return;
               }
            }

            PHCIServiceExecuteCommand(
                              pBindingContext,
                              nHCIServiceIdentifier,
                              PNALBindingGetOperation(pBindingContext),
                              ETSI_CMD_WR_XCHG_DATA,
                              pHCISendBuffer,
                              nCmdBufferLength,
                              pBindingContext->aHCIResponseBuffer,
                              sizeof(pBindingContext->aHCIResponseBuffer),
                              static_PNALBindingExchangeDataExecuteCommandCompleted,
                              PNALUtilConvertUint32ToPointer(nNALServiceIdentifier),
                              W_FALSE );
            break;

         case NAL_CMD_COM_TRANSFER:

            /* upper layer ensure this pipe will not be used until the command has been acknowleded,
               we can call the write callback without waiting for effective command write completion  */
            PNALDFCPost0(pBindingContext, P_DFC_TYPE_NFC_HAL_BINDING, static_PNALBindingCallWriteCallback);

            /* Reader sharing is not implemented */
            nResult = NAL_RES_UNKNOWN_COMMAND;
            bImmediateResponse = W_TRUE;
            break;

         case NAL_EVT_P2P_SEND_DATA:

            /* The write callback will be done only when the write has been performed */

            nHCIServiceIdentifier = static_PNALBindingGetHCIServiceIdentifier(pBindingContext, nNALServiceIdentifier, 0);
            {
               uint8_t nIndex = 0x00;
               uint8_t nEvent;
               uint32_t nLen;
               if(nHCIServiceIdentifier == P_HCI_SERVICE_P2P_INITIATOR)
               {
                  pHCISendBuffer[nIndex++] = 0x00;
                  nEvent = HCI2_EVT_P2P_INITIATOR_EXCHANGE_TO_RF;
                  nLen = (nLength - 2) + 1;
               }
               else /* P2P target Service */
               {
                  nEvent = ETSI_EVT_SEND_DATA;
                  nLen = (nLength - 2);
               }
               /*copy the data*/
               CNALMemoryCopy(
                   &pHCISendBuffer[nIndex],
                   &pBuffer[2],
                   (nLength - 2));

               PHCIServiceSendEvent(
                                pBindingContext,
                                nHCIServiceIdentifier,
                                PNALBindingGetOperation(pBindingContext),
                                nEvent,
                                pHCISendBuffer,
                                nLen,
                                static_PNALBindingSendEventCompleted,
                                PNALUtilConvertUint32ToPointer(nNALServiceIdentifier),
                                W_FALSE);
            }
            break;

         case NAL_CMD_UPDATE_FIRMWARE:

            /* upper layer ensure this pipe will not be used until the command has been acknowleded,
               we can call the write callback without waiting for effective command write completion  */
             PNALDFCPost0(pBindingContext, P_DFC_TYPE_NFC_HAL_BINDING, static_PNALBindingCallWriteCallback);

             if (pBindingContext->bIsFirmwareUpdateStarted == W_FALSE)
             {
                uint8_t * pConfigBuffer       = (uint8_t *) PNALUtilReadAddressFromBigEndianBuffer(pBuffer + 2);
                uint32_t  nConfigBufferLength = PNALUtilReadUint32FromBigEndianBuffer(pBuffer + 2 + sizeof(uint8_t*));

                pBindingContext->bIsFirmwareUpdateStarted = W_TRUE;
                PNALBindingConfigStart(pBindingContext, pConfigBuffer, nConfigBufferLength);
             }
             else
             {
                bImmediateResponse = W_TRUE;
                nResult = NAL_RES_BAD_DATA;
                goto post_result;
             }
            return;

         case NAL_CMD_UICC_START_SWP:

            if((nLength != 0x02)
             ||(nNALServiceIdentifier != NAL_SERVICE_UICC))
            {
               /* upper layer ensure this pipe will not be used until the command has been acknowleded,
               we can call the write callback without waiting for effective command write completion  */
               PNALDFCPost0(pBindingContext, P_DFC_TYPE_NFC_HAL_BINDING, static_PNALBindingCallWriteCallback);

               /* the format of this message is not correct*/
               nResult = NAL_RES_BAD_LENGTH;
               bImmediateResponse = W_TRUE;
            }
            else
            {
               /* The write callback will be done only when the write has been performed */
               PHCIServiceSendEvent(
                                   pBindingContext,
                                   P_HCI_SERVICE_PIPE_MANAGEMENT,
                                   PNALBindingGetOperation(pBindingContext),
                                   HCI2_EVT_MGT_HDS_SET_UP_SWPLINE,
                                   null,
                                   0,
                                   static_PNALBindingSendEventCompleted,
                                   PNALUtilConvertUint32ToPointer(nNALServiceIdentifier),
                                   W_FALSE );
            }
            break;

         case NAL_EVT_STANDBY_MODE:

            /* There's no write operation, so we can call the write callback now */
             PNALDFCPost0(pBindingContext, P_DFC_TYPE_NFC_HAL_BINDING, static_PNALBindingCallWriteCallback);

            if ((nLength == 0x03) && (nNALServiceIdentifier == NAL_SERVICE_ADMIN))
            {
               pBindingContext->bIsLowPowerRequested = (pBuffer[2] == 0x01) ? W_TRUE : W_FALSE;

               nResult = NAL_RES_OK;

               PHCIServiceKick(pBindingContext);
            }
            else
            {
               nResult = NAL_RES_BAD_DATA;
            }
            bImmediateResponse = W_TRUE;

            break;

         case NAL_EVT_CARD_SEND_DATA:

            /* The write callback will be done only when the write has been performed */

            CNALMemoryCopy(pHCISendBuffer, &pBuffer[2], (nLength - 2));

            nCmdBufferLength = (nLength - 2);
            nHCIServiceIdentifier = static_PNALBindingGetHCIServiceIdentifier(pBindingContext, nNALServiceIdentifier, 0);

            /* Send the card event data*/
            PHCIServiceSendEvent(
                             pBindingContext,
                             nHCIServiceIdentifier,
                             PNALBindingGetOperation(pBindingContext),
                             ETSI_EVT_SEND_DATA,
                             pHCISendBuffer,
                             nCmdBufferLength,
                             static_PNALBindingCardSendEventCompleted,
                             null,
                             W_FALSE );
            break;

         case NAL_CMD_PRODUCTION_TEST:

            /* upper layer ensure this pipe will not be used until the command has been acknowleded,
               we can call the write callback without waiting for effective command write completion  */
            PNALDFCPost0(pBindingContext, P_DFC_TYPE_NFC_HAL_BINDING, static_PNALBindingCallWriteCallback);

            if(nLength > 0x02)
            {
               bool_t bResult = static_PNALBindingSendHCIProductTestCommand(
                                  pBindingContext,
                                  &pBuffer[2],
                                  (nLength - 0x02),
                                  pHCISendBuffer);
               if(bResult == W_FALSE)
               {
                  nResult = NAL_RES_BAD_STATE;
                  bImmediateResponse = W_TRUE;
               }
            }
            else
            {
               nResult = NAL_RES_BAD_LENGTH;
               bImmediateResponse = W_TRUE;
            }

            break;

         case NAL_CMD_SELF_TEST:

            /* upper layer ensure this pipe will not be used until the command has been acknowleded,
               we can call the write callback without waiting for effective command write completion  */
            PNALDFCPost0(pBindingContext, P_DFC_TYPE_NFC_HAL_BINDING, static_PNALBindingCallWriteCallback);

            PHCIServiceExecuteCommand(
                           pBindingContext,
                           P_HCI_SERVICE_PIPE_MANAGEMENT,
                           PNALBindingGetOperation(pBindingContext),
                           HCI2_CMD_MGT_TEST_SELF_CHIP,
                           null, 0x00,
                           pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
                           static_PNALBindingSelfTestExecuteCmdCompleted, null,
                           W_FALSE );
            break;

         default:

            /* upper layer ensure this pipe will not be used until the command has been acknowleded,
               we can call the write callback without waiting for effective command write completion  */
            PNALDFCPost0(pBindingContext, P_DFC_TYPE_NFC_HAL_BINDING, static_PNALBindingCallWriteCallback);

            nResult = NAL_RES_UNKNOWN_COMMAND;
            bImmediateResponse = W_TRUE;
            break;
      }
   }
   else
   {
      /* We are in RAW mode */

      if ((nNALServiceIdentifier == NAL_SERVICE_ADMIN) && (nMessageCode == NAL_EVT_RAW_MESSAGE) && (nLength >= 3))
      {
         PHCIFrameWritePrepareContext(pBindingContext, &pBindingContext->sRawWriteContext, pBuffer[2], nLength - 3, static_PNALBindingRawWriteCompleted, null);

         PHCIFrameWrite(pBindingContext, &pBindingContext->sRawWriteContext, pBuffer + 3);
      }
      else
      {
         PNALDebugError("PNALBindingWrite : bad HCI command format");
      }
   }

 post_result:

   if(bImmediateResponse != W_FALSE)
   {
      /* post the read callback */
      PNALDFCPost2(pBindingContext, P_DFC_TYPE_NFC_HAL_BINDING, static_PNALBindingGenericReadCallback, PNALUtilConvertUint32ToPointer((uint32_t) nNALServiceIdentifier), PNALUtilConvertUint32ToPointer((uint32_t) nResult));
   }
}

/**
 * @See NFC HAL Binding header file
 **/
static void static_PNALBindingPoll(tNALVoidContext* pNALContext)
{
   tNALBindingContext *pBindingContext = (tNALBindingContext*)pNALContext;

   CNALSyncEnterCriticalSection(&pBindingContext->hCriticalSection);
   pBindingContext->bInPoll = W_TRUE;

   if(CNALTimerIsTimerElapsed( pBindingContext->pTimer ))
   {
      PNALMultiTimerPoll( pBindingContext, W_TRUE );
   }

   PSHDLCFramePoll(pBindingContext);

   if(pBindingContext->bIsResetPending != W_FALSE)
   {
      if(CNALResetIsPending(pBindingContext->pPortingConfig) == W_FALSE)
      {
         pBindingContext->bIsResetPending = W_FALSE;

         PHCIServiceConnect(
               pBindingContext,
               static_PNALBindingServiceConnectCompleted,
               null);
      }
   }

   PNALDFCPump(pBindingContext);

   /* during state transitions phases (boot, enter/exit from standby)
      received events and commands are stored in an internal fifo and are processed at the end of the transition

      The corresponding resources must be freed here, when all DFC callbacks have been processed */

   PHCIServiceFlushProcessedFrames(pBindingContext);

   pBindingContext->bInPoll = W_FALSE;
   CNALSyncLeaveCriticalSection(&pBindingContext->hCriticalSection);
}

/**
 * @See NFC HAL Binding header file
 **/
static uint32_t static_PNALBindingGetVariable(
         tNALVoidContext* pNALContext,
         uint32_t nType)
{
   tNALBindingContext *pBindingContext = (tNALBindingContext*)pNALContext;
   uint32_t nValue = 0;

   CNALSyncEnterCriticalSection(&pBindingContext->hCriticalSection);

   switch(nType)
   {
   case NAL_PARAM_SUB_MODE:
      nValue = pBindingContext->nHCIMode;
      break;
   case NAL_PARAM_MODE:
      nValue = pBindingContext->nMode;
      break;
   case NAL_PARAM_FIRMWARE_UPDATE:
      if (pBindingContext->nConfigLength != 0)
      {
         nValue = pBindingContext->nConfigProgression;
      }
      break;
   case NAL_PARAM_CURRENT_TIME:
      nValue = CNALTimerGetCurrentTime(pBindingContext->pTimer);
      break;
   }

   CNALSyncLeaveCriticalSection(&pBindingContext->hCriticalSection);

   return nValue;
}

/**
 * Receives the timeout completion for the upper layer.
 *
 * @param[in] pBindingContext  The context.
 *
 * @param[in] pCallbakParameter  The callback parameters.
 **/
static void static_PNALBindingTimeoutCompletion(
               tNALBindingContext* pBindingContext,
               void* pCallbakParameter )
{
   pBindingContext->bInPoll = W_FALSE;
   CNALSyncLeaveCriticalSection(&pBindingContext->hCriticalSection);

   pBindingContext->pNALTimerHandlerFunction(
      pBindingContext->pCallbackContext);

   CNALSyncEnterCriticalSection(&pBindingContext->hCriticalSection);
   pBindingContext->bInPoll = W_TRUE;
}

/**
 * @See NFC HAL Binding header file
 **/
static void static_PNALBindingSetVariable(
         tNALVoidContext* pNALContext,
         uint32_t nType,
         uint32_t nValue)
{
   tNALBindingContext* pBindingContext = (tNALBindingContext*)pNALContext;

   CNALSyncEnterCriticalSection(&pBindingContext->hCriticalSection);

   switch(nType)
   {
   case NAL_PARAM_MODE:
      pBindingContext->nMode = nValue;
      break;
   case NAL_PARAM_STATISTICS:
      /* Reset the four protocol statistics */
      PHCIFrameResetStatistics( pBindingContext );
      PHCIServiceResetStatistics( pBindingContext );
      PSHDLCResetStatistics( pBindingContext );
      PSHDLCFrameResetStatistics( pBindingContext );
      break;
   case NAL_PARAM_CURRENT_TIMER:
      if(nValue == 0)
      {
         PNALMultiTimerCancel(pBindingContext, TIMER_T9_UPPER_TIMER);
      }
      else
      {
         PNALMultiTimerSet(
            pBindingContext, TIMER_T9_UPPER_TIMER,
            nValue,
            static_PNALBindingTimeoutCompletion,
            null );
      }
      break;
   }

   CNALSyncLeaveCriticalSection(&pBindingContext->hCriticalSection);
}

/**
 * @See NFC HAL Binding header file
 **/
static void static_PNALBindingGetStatistics(
         tNALVoidContext* pNALContext,
         tNALProtocolStatistics* pStatistics)
{
   tNALBindingContext* pBindingContext = (tNALBindingContext*)pNALContext;

   CNALSyncEnterCriticalSection(&pBindingContext->hCriticalSection);

   /* Get the HCI frame statistics */
   PHCIFrameGetStatistics(
      pBindingContext,
      &pStatistics->nOSI5ReadMessageLost,
      &pStatistics->nOSI5ReadByteErrorCount );

   /* Get the HCI service statistics */
   PHCIServiceGetStatistics(
      pBindingContext,
      &pStatistics->nOSI6ReadMessageLost,
      &pStatistics->nOSI6ReadByteErrorCount );

   /* Get the SHDLC statistics */
   PSHDLCGetStatistics(
      pBindingContext,
      &pStatistics->nOSI4WindowSize,
      &pStatistics->nOSI4ReadPayload,
      &pStatistics->nOSI4ReadFrameLost,
      &pStatistics->nOSI4ReadByteErrorCount,
      &pStatistics->nOSI4WritePayload,
      &pStatistics->nOSI4WriteFrameLost,
      &pStatistics->nOSI4WriteByteErrorCount );

   /* Get the SHDLC frame statistics */
   PSHDLCFrameGetStatistics(
      pBindingContext,
      &pStatistics->nOSI2FrameReadByteErrorCount,
      &pStatistics->nOSI2FrameReadByteTotalCount,
      &pStatistics->nOSI2FrameWriteByteTotalCount);

   CNALSyncLeaveCriticalSection(&pBindingContext->hCriticalSection);
}

/**
 * The static NFC HAL Binding structure
 **/
static const tNALBinding g_sNALBinding =
{
   NAL_BINDING_MAGIC_WORD,
   static_PNALBindingCreate,
   static_PNALBindingDestroy,
   static_PNALBindingReset,
   static_PNALBindingConnect,
   static_PNALBindingWrite,
   static_PNALBindingPoll,
   static_PNALBindingGetVariable,
   static_PNALBindingSetVariable,
   static_PNALBindingGetStatistics
};

const tNALBinding * const g_pNALBinding = &g_sNALBinding;

/**
 * @See definition of tPHCIServiceExecuteCommandCompleted
 **/
static void static_PNALBindingGetParameterCompleted(
          tNALBindingContext* pBindingContext,
          void* pCallbackParameter,
          uint8_t* pBuffer,
          uint32_t nLength,
          W_ERROR nError,
          uint8_t nStatusCode,
          uint32_t nReceptionCounter)
{
   uint32_t nNALReceptionBufferLength = 0;
   uint32_t nOffset = 0;
   uint8_t nNALServiceIdentifier = (uint8_t)PNALUtilConvertPointerToUint32(pCallbackParameter);
   uint8_t nMessageParam = pBindingContext->aNALBindingNALService[nNALServiceIdentifier].nMessageParam;

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

    /* Check the result */

   if ( nError != W_SUCCESS )
   {
      PNALDebugError( "static_PNALBindingGetParameterCompleted: nError %s", PNALUtilTraceError(nError));
      pBindingContext->pNALReceptionBuffer[0] = nNALServiceIdentifier;

      if(nError == W_ERROR_TIMEOUT)
      {
         pBindingContext->pNALReceptionBuffer[1] = NAL_RES_TIMEOUT;
      }
      else if (nError == W_ERROR_ITEM_NOT_FOUND)
      {
         pBindingContext->pNALReceptionBuffer[1] = NAL_RES_FEATURE_NOT_SUPPORTED;
      }
      else
      {
         pBindingContext->pNALReceptionBuffer[1] = NAL_RES_PROTOCOL_ERROR;
      }
      nNALReceptionBufferLength = 0x02;
      goto post_result;
   }
   else if ( nStatusCode != ETSI_ERR_ANY_OK )
   {
      PNALDebugError( "static_PNALBindingGetParameterCompleted: nStatusCode 0x%08X", nStatusCode );

      pBindingContext->pNALReceptionBuffer[0] = nNALServiceIdentifier;
      if(nStatusCode == ETSI_ERR_ANY_E_TIMEOUT)
      {
         pBindingContext->pNALReceptionBuffer[1] = NAL_RES_TIMEOUT;
      }
      else
      {
         pBindingContext->pNALReceptionBuffer[1] = NAL_RES_PROTOCOL_ERROR;
      }
      goto post_result;
   }

   /* Here nError == W_SUCCESS && nStatus == ETSI_ERR_ANY_OK) */

   if((pBuffer != null) &&(nLength != 0))
   {
      /* copy the response to HCI Get command */
      CNALMemoryCopy(
         &pBindingContext->aHCIReceptionBuffer[nOffset],
         pBuffer,
         nLength);
      nOffset += nLength;
   }
   else
   {
      pBindingContext->pNALReceptionBuffer[0] = nNALServiceIdentifier;
      pBindingContext->pNALReceptionBuffer[1] = NAL_RES_BAD_DATA;
      nNALReceptionBufferLength = 0x02;
      goto post_result;
   }

   /* Build NFC HAL message Reponse */

   if(g_aNALBindingParameterEntryArray[nMessageParam].sGetParameter.pBuildGetParameterAnswer != null)
   {
      g_aNALBindingParameterEntryArray[nMessageParam].sGetParameter.pBuildGetParameterAnswer(
                                                         pBindingContext,
                                                         nNALServiceIdentifier,
                                                         pBindingContext->pNALReceptionBuffer,
                                                         &nNALReceptionBufferLength,
                                                         pBindingContext->aHCIReceptionBuffer,
                                                         nOffset);
   }
   else
   {
      /* This parameter is write only */
      pBindingContext->pNALReceptionBuffer[0] = nNALServiceIdentifier;
      pBindingContext->pNALReceptionBuffer[1] = NAL_RES_BAD_DATA;
      nNALReceptionBufferLength = 0x02;
      goto post_result;
   }

post_result:

   /* post the result*/
   PNALBindingCallReadCallback(pBindingContext, nNALReceptionBufferLength, pBindingContext->nReceptionCounter);
}

/**
 * @See definition of tPHCIServiceExecuteCommandCompleted
 **/
static void static_PNALBindingSetParameterCompleted(
            tNALBindingContext* pBindingContext,
            void* pCallbackParameter,
            uint8_t* pBuffer,
            uint32_t nLength,
            W_ERROR nError,
            uint8_t nStatusCode,
            uint32_t nReceptionCounter)
{
   uint8_t nNALServiceIdentifier = (uint8_t)PNALUtilConvertPointerToUint32(pCallbackParameter);
   uint8_t nMessageParam;

   PNALDebugTrace("static_PNALBindingSetParameterCompleted()");

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   nMessageParam = pBindingContext->aNALBindingNALService[nNALServiceIdentifier].nMessageParam;

   PNALDebugTrace("static_PNALBindingSetParameterCompleted : nMessageParam %d", nMessageParam);

   /* Check the result */
   if ( nError == W_SUCCESS )
   {
      /* Check the result */
      if ( nStatusCode == ETSI_ERR_ANY_OK )
      {
         switch (nMessageParam)
         {
            case NAL_PAR_PERSISTENT_POLICY :
               CNALMemoryCopy(pBindingContext->aParam_NAL_PAR_PERSISTENT_POLICY, pBindingContext->aSetParam_NAL_PAR_POLICY_or_NAL_PAR_PERSISTENT_POLICY, NAL_POLICY_SIZE);
               break;

            case  NAL_PAR_POLICY:
               /* store the current policy parameters set to be able to restore them after a wake up from standby */
               CNALMemoryCopy(pBindingContext->aParam_NAL_PAR_POLICY, pBindingContext->aSetParam_NAL_PAR_POLICY_or_NAL_PAR_PERSISTENT_POLICY, NAL_POLICY_SIZE);
               break;

            case NAL_PAR_RAW_MODE:
               pBindingContext->bRawMode = W_TRUE;
               break;
         }

         pBindingContext->pNALReceptionBuffer[1] = NAL_RES_OK;
      }
      else
      {
         PNALDebugError( "static_PNALBindingSetParameterCompleted: HCI Error 0x%02X", nStatusCode);

         if(nStatusCode == ETSI_ERR_ANY_E_TIMEOUT)
         {
            pBindingContext->pNALReceptionBuffer[1] = NAL_RES_TIMEOUT;
         }
         else
         {
            pBindingContext->pNALReceptionBuffer[1] = NAL_RES_PROTOCOL_ERROR;
         }
      }
   }
   else
   {
      PNALDebugError( "static_PNALBindingSetParameterCompleted: nError %s", PNALUtilTraceError(nError));

      if(nError == W_ERROR_TIMEOUT)
      {
         pBindingContext->pNALReceptionBuffer[1] = NAL_RES_TIMEOUT;
      }
      else if (nError == W_ERROR_ITEM_NOT_FOUND)
      {
         pBindingContext->pNALReceptionBuffer[1] = NAL_RES_FEATURE_NOT_SUPPORTED;
      }
      else
      {
         pBindingContext->pNALReceptionBuffer[1] = NAL_RES_PROTOCOL_ERROR;
      }
   }

   pBindingContext->pNALReceptionBuffer[0] = nNALServiceIdentifier;

   /* post the result */
   PNALBindingCallReadCallback(pBindingContext, 0x02, pBindingContext->nReceptionCounter);
 }

/**
 * @See definition of tPHCIServiceExecuteCommandCompleted
 **/
static void static_PNALBindingSetPicoreadOptionParameterCompleted(
            tNALBindingContext* pBindingContext,
            void* pCallbackParameter,
            uint8_t* pBuffer,
            uint32_t nLength,
            W_ERROR nError,
            uint8_t nStatusCode,
            uint32_t nReceptionCounter)
{
   uint8_t nNALServiceIdentifier = (uint8_t)PNALUtilConvertPointerToUint32(pCallbackParameter);
   uint8_t nHCIServiceIdentifier = static_PNALBindingGetHCIServiceIdentifier(pBindingContext, nNALServiceIdentifier, 0);
   uint8_t * pHCISendBuffer = pBindingContext->aNALBindingNALService[nNALServiceIdentifier].aNALOperationBuffer;

   PNALDebugTrace("static_PNALBindingSetPicoreadOptionParameterCompleted()");

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   /* Check the result */
   if ( nError == W_SUCCESS)
   {
      /* Check the result */
      if ( nStatusCode == ETSI_ERR_ANY_OK )
      {
         pBindingContext->bBitMode = W_TRUE;
         PHCIServiceExecuteCommand(
                           pBindingContext,
                           nHCIServiceIdentifier,
                           PNALBindingGetOperation(pBindingContext),
                           ETSI_CMD_WR_XCHG_DATA,
                           pHCISendBuffer,
                           pBindingContext->nHCICmdBufferLength,
                           pBindingContext->aHCIResponseBuffer,
                           sizeof(pBindingContext->aHCIResponseBuffer),
                           static_PNALBindingExchangeDataExecuteCommandCompleted,
                           PNALUtilConvertUint32ToPointer(nNALServiceIdentifier),
                           W_FALSE );
         return;
      }
      else
      {
         PNALDebugError( "static_PNALBindingSetParameterCompleted: HCI Error 0x%02X", nStatusCode);

         if(nStatusCode == ETSI_ERR_ANY_E_TIMEOUT)
         {
            pBindingContext->pNALReceptionBuffer[1] = NAL_RES_TIMEOUT;
         }
         else
         {
            pBindingContext->pNALReceptionBuffer[1] = NAL_RES_PROTOCOL_ERROR;
         }
      }
   }
   else
   {
      PNALDebugError( "static_PNALBindingSetParameterCompleted: nError %s", PNALUtilTraceError(nError));

      if(nError == W_ERROR_TIMEOUT)
      {
         pBindingContext->pNALReceptionBuffer[1] = NAL_RES_TIMEOUT;
      }
      else
      {
         pBindingContext->pNALReceptionBuffer[1] = NAL_RES_PROTOCOL_ERROR;
      }
   }

   pBindingContext->pNALReceptionBuffer[0] = nNALServiceIdentifier;

   /* post the result */
   PNALBindingCallReadCallback(pBindingContext, 0x02, pBindingContext->nReceptionCounter);
 }

/**
 * @brief   Adapts the HCI card found payload to NFC HAL discovered card structure.
 *
 *
 * @param[in]  nReaderServiceIdentifier  The ID of the Reader Service.
 *
 * @param[in]  pNALOutputBuffer  The out put buffer containing the NFC HAL Card discovered structure.
 *
 * @param[in]  pNALOutputBufferLength  The out put buffer size.
 *
 * @param[in]  pBuffer  The buffer containing the payload of discovered card.
 *
 * @param[in]  nLength  The payload size.
 **/
static W_ERROR static_PNALBindingCardDiscovered(
                                 uint8_t nHCIReaderServiceIdentifier,
                                 uint8_t* pNALOutputBuffer,
                                 uint32_t* pNALOutputBufferLength,
                                 uint8_t* pBuffer,
                                 uint32_t nLength
                                 )
{
   uint32_t nIndex = 0;

   /* check the input and out put buffers */
   if((pNALOutputBuffer == null)||
      (pNALOutputBufferLength == null)||
      (pBuffer == null))
   {
       PNALDebugError("static_PNALBindingCardDiscovered:incorrect input or output buffer format");
       return W_ERROR_BAD_PARAMETER;
   }
   /* set the type Event */
   pNALOutputBuffer[1] = NAL_EVT_READER_TARGET_DISCOVERED;

   switch(nHCIReaderServiceIdentifier)
   {
      case P_HCI_SERVICE_MREAD_ISO_14_A_3:
      {
#define P_NFC_HAL_BINDING_CARD_FOUND_SIZE_ISO_14443_3_A        14
#define HCI2_CMD_MREAD_CARD_FOUND_14_3_A_ATQA_OFFSET           0
#define HCI2_CMD_MREAD_CARD_FOUND_14_3_A_SAK_OFFSET            2
#define HCI2_CMD_MREAD_CARD_FOUND_14_3_A_UID_LENGTH_OFFSET     3
#define HCI2_CMD_MREAD_CARD_FOUND_14_3_A_UID_OFFSET            4

         if (nLength == P_NFC_HAL_BINDING_CARD_FOUND_SIZE_ISO_14443_3_A)
         {
            pNALOutputBuffer[nIndex++] = NAL_SERVICE_READER_14_A_3;
            pNALOutputBuffer[nIndex++] = NAL_EVT_READER_TARGET_DISCOVERED;

            nIndex += PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_14_A_3(
                           pNALOutputBuffer + 2,
                           static_PNALBindingReadUint16FromHCIBuffer(pBuffer + HCI2_CMD_MREAD_CARD_FOUND_14_3_A_ATQA_OFFSET),
                           pBuffer[HCI2_CMD_MREAD_CARD_FOUND_14_3_A_SAK_OFFSET],
                           &pBuffer[HCI2_CMD_MREAD_CARD_FOUND_14_3_A_UID_OFFSET],
                           pBuffer[HCI2_CMD_MREAD_CARD_FOUND_14_3_A_UID_LENGTH_OFFSET]);

            *pNALOutputBufferLength = nIndex;
         }
         else
         {
            PNALDebugError("static_PNALBindingCardDiscovered : invalid CARD_FOUND size %d", nLength);
            return W_ERROR_BAD_PARAMETER;
         }
      }
      break;

      case P_HCI_SERVICE_MREAD_ISO_14_A_4:
      {

#define P_NFC_HAL_BINDING_CARD_FOUND_MIN_SIZE_ISO_14443_4_A    18
#define HCI2_EVT_MREAD_CARD_FOUND_14_4_A_ATQA_OFFSET           0
#define HCI2_EVT_MREAD_CARD_FOUND_14_4_A_SAK_OFFSET            2
#define HCI2_EVT_MREAD_CARD_FOUND_14_4_A_UID_LENGTH_OFFSET     3
#define HCI2_EVT_MREAD_CARD_FOUND_14_4_A_UID_OFFSET            4
#define HCI2_EVT_MREAD_CARD_FOUND_14_4_A_OLDIES               14
#define HCI2_EVT_MREAD_CARD_FOUND_14_4_A_ATS_LENGTH_OFFSET    16
#define HCI2_EVT_MREAD_CARD_FOUND_14_4_A_ATS_OFFSET           17

         if (nLength >= P_NFC_HAL_BINDING_CARD_FOUND_MIN_SIZE_ISO_14443_4_A)
         {
            uint32_t nUIDLength = pBuffer[HCI2_EVT_MREAD_CARD_FOUND_14_4_A_UID_LENGTH_OFFSET];
            uint32_t nATSLength = pBuffer[HCI2_EVT_MREAD_CARD_FOUND_14_4_A_ATS_LENGTH_OFFSET];
            uint32_t nOptionalBytes = 0;

            /* Set the TL length of the ATS to the length of the ATS given by the MR (may be truncated) */
            pBuffer[HCI2_EVT_MREAD_CARD_FOUND_14_4_A_ATS_OFFSET] = pBuffer[HCI2_EVT_MREAD_CARD_FOUND_14_4_A_ATS_LENGTH_OFFSET];
            pNALOutputBuffer[nIndex++] = NAL_SERVICE_READER_14_A_4;
            pNALOutputBuffer[nIndex++] = NAL_EVT_READER_TARGET_DISCOVERED;

            if (nATSLength > 1)
            {
               /* TO is present */
               nOptionalBytes++;

               if (pBuffer[HCI2_EVT_MREAD_CARD_FOUND_14_4_A_ATS_OFFSET + 1] & 0x10)
               {
                  /* TA is present */
                  nOptionalBytes++;
               }

               if (pBuffer[HCI2_EVT_MREAD_CARD_FOUND_14_4_A_ATS_OFFSET + 1] & 0x20)
               {
                  /* TB is present */
                  nOptionalBytes++;
               }

               if (pBuffer[HCI2_EVT_MREAD_CARD_FOUND_14_4_A_ATS_OFFSET + 1] & 0x40)
               {
                  /* TB is present */
                  nOptionalBytes++;
               }
            }

            nIndex += PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_14_A_4(
                           pNALOutputBuffer + 2,
                           static_PNALBindingReadUint16FromHCIBuffer(pBuffer + HCI2_EVT_MREAD_CARD_FOUND_14_4_A_ATQA_OFFSET),
                           pBuffer[HCI2_EVT_MREAD_CARD_FOUND_14_4_A_SAK_OFFSET],
                           &pBuffer[HCI2_EVT_MREAD_CARD_FOUND_14_4_A_ATS_OFFSET],
                           1 + nOptionalBytes,
                           &pBuffer[HCI2_EVT_MREAD_CARD_FOUND_14_4_A_ATS_OFFSET + 1 + nOptionalBytes],
                           nATSLength - (1 + nOptionalBytes),
                           &pBuffer[HCI2_EVT_MREAD_CARD_FOUND_14_4_A_UID_OFFSET],
                           nUIDLength);

            *pNALOutputBufferLength = nIndex;
         }
         else
         {
            PNALDebugError("static_PNALBindingCardDiscovered : invalid CARD_FOUND size %d", nLength);
            return W_ERROR_BAD_PARAMETER;
         }

      }
      break;

      case P_HCI_SERVICE_MREAD_ISO_14_B_3:
      case P_HCI_SERVICE_MREAD_ISO_14_B_4:
      {

#define P_NFC_HAL_BINDING_CARD_FOUND_MIN_SIZE_ISO_14443_3_B                   12
#define HCI2_CMD_MREAD_CARD_FOUND_14_3_B_UID_OFFSET                           0
#define HCI2_CMD_MREAD_CARD_FOUND_14_3_B_APPLI_DATA_OFFSET                    4
#define HCI2_CMD_MREAD_CARD_FOUND_14_3_B_PROT_INFO_BLI_CID_OFFSET             8
#define HCI2_CMD_MREAD_CARD_FOUND_14_3_B_ATTRIB_HL_RESP_OFFSET                12

         if (nLength >= P_NFC_HAL_BINDING_CARD_FOUND_MIN_SIZE_ISO_14443_3_B)
         {
            /* set the equivalent NFC HAL Reader service Id */
            pNALOutputBuffer[nIndex++] =
                  (nHCIReaderServiceIdentifier == P_HCI_SERVICE_MREAD_ISO_14_B_3)? NAL_SERVICE_READER_14_B_3 : NAL_SERVICE_READER_14_B_4;

            pNALOutputBuffer[nIndex++] = NAL_EVT_READER_TARGET_DISCOVERED;

            nIndex += PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_14_B_3(
                           pNALOutputBuffer + 2,
                           &pBuffer[HCI2_CMD_MREAD_CARD_FOUND_14_3_B_UID_OFFSET],
                           &pBuffer[HCI2_CMD_MREAD_CARD_FOUND_14_3_B_ATTRIB_HL_RESP_OFFSET],
                           nLength -  HCI2_CMD_MREAD_CARD_FOUND_14_3_B_ATTRIB_HL_RESP_OFFSET);

            *pNALOutputBufferLength = nIndex;
         }
         else
         {
            PNALDebugError("static_PNALBindingCardDiscovered : invalid CARD_FOUND size %d", nLength);
            return W_ERROR_BAD_PARAMETER;
         }
      }
      break;

      case P_HCI_SERVICE_MREAD_ISO_15_3:
      {
#define P_NFC_HAL_BINDING_CARD_FOUND_SIZE_ISO_15_3                10
#define HCI2_CMD_MREAD_CARD_FOUND_15_3_FLAG_OFFSET                0
#define HCI2_CMD_MREAD_CARD_FOUND_15_3_DSFID_OFFSET               1
#define HCI2_CMD_MREAD_CARD_FOUND_15_3_UID_OFFSET                 2

         if (nLength == P_NFC_HAL_BINDING_CARD_FOUND_SIZE_ISO_15_3)
         {
            pNALOutputBuffer[nIndex++] = NAL_SERVICE_READER_15_3;
            pNALOutputBuffer[nIndex++] = NAL_EVT_READER_TARGET_DISCOVERED;

            nIndex += PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_15_3(
                           pNALOutputBuffer + 2,
                           pBuffer[HCI2_CMD_MREAD_CARD_FOUND_15_3_FLAG_OFFSET],
                           pBuffer[HCI2_CMD_MREAD_CARD_FOUND_15_3_DSFID_OFFSET],
                           &pBuffer[HCI2_CMD_MREAD_CARD_FOUND_15_3_UID_OFFSET]);

            *pNALOutputBufferLength = nIndex;
         }
         else
         {
            PNALDebugError("static_PNALBindingCardDiscovered : invalid CARD_FOUND size %d", nLength);
            return W_ERROR_BAD_PARAMETER;
         }
      }
      break;

      case P_HCI_SERVICE_MREAD_ISO_15_2:
         PNALDebugError("static_PNALBindingCardDiscovered: error protocol, ISO_15_2 is not implemented");
      break;

      case P_HCI_SERVICE_MREAD_NFC_T1:
      {
#define P_NFC_HAL_BINDING_CARD_FOUND_SIZE_TYPE_1                  8
#define HCI2_CMD_MREAD_CARD_FOUND_TYPE_1_ATQA_OFFSET              0
#define HCI2_CMD_MREAD_CARD_FOUND_TYPE_1_HR_OFFSET                2
#define HCI2_CMD_MREAD_CARD_FOUND_TYPE_1_UID_OFFSET               4

         if (nLength == P_NFC_HAL_BINDING_CARD_FOUND_SIZE_TYPE_1)
         {
            pNALOutputBuffer[nIndex++] = NAL_SERVICE_READER_TYPE_1;
            pNALOutputBuffer[nIndex++] = NAL_EVT_READER_TARGET_DISCOVERED;

            nIndex += PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_TYPE1(
                        pNALOutputBuffer + 2,
                        pBuffer + HCI2_CMD_MREAD_CARD_FOUND_TYPE_1_UID_OFFSET,
                        pBuffer + HCI2_CMD_MREAD_CARD_FOUND_TYPE_1_HR_OFFSET,
                        static_PNALBindingReadUint16FromHCIBuffer(pBuffer + HCI2_CMD_MREAD_CARD_FOUND_TYPE_1_ATQA_OFFSET));

            *pNALOutputBufferLength = nIndex;
          }
         else
         {
            PNALDebugError("static_PNALBindingCardDiscovered : invalid CARD_FOUND size %d", nLength);
            return W_ERROR_BAD_PARAMETER;
         }
      }
      break;
      case P_HCI_SERVICE_MREAD_FELICA:
      {
#define P_NFC_HAL_BINDING_CARD_FOUND_SIZE_FELICA                  18
#define HCI2_CMD_MREAD_CARD_FOUND_TYPE_FELICA_ID_PM_OFFSET        0
#define HCI2_CMD_MREAD_CARD_FOUND_TYPE_FELICA_SYSTEM_CODE         16

         if (nLength == P_NFC_HAL_BINDING_CARD_FOUND_SIZE_FELICA)
         {

            pNALOutputBuffer[nIndex++] = NAL_SERVICE_READER_FELICA;
            pNALOutputBuffer[nIndex++] = NAL_EVT_READER_TARGET_DISCOVERED;

            nIndex += PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_FELICA(
                           pNALOutputBuffer + 2,
                           pBuffer + HCI2_CMD_MREAD_CARD_FOUND_TYPE_FELICA_ID_PM_OFFSET,
                           static_PNALBindingReadUint16FromHCIBuffer(pBuffer + HCI2_CMD_MREAD_CARD_FOUND_TYPE_FELICA_SYSTEM_CODE));

            *pNALOutputBufferLength = nIndex;
         }
         else
         {
            PNALDebugError("static_PNALBindingCardDiscovered : invalid CARD_FOUND size %d", nLength);
            return W_ERROR_BAD_PARAMETER;
         }
      }
      break;
      case P_HCI_SERVICE_MREAD_BPRIME:
      {
         pNALOutputBuffer[nIndex++] = NAL_SERVICE_READER_B_PRIME;
         pNALOutputBuffer[nIndex++] = NAL_EVT_READER_TARGET_DISCOVERED;

         nIndex += (uint32_t)PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_BPRIME(
                        (uint8_t *)(pNALOutputBuffer + nIndex),
                        (uint8_t *)pBuffer + 1,
                        (uint32_t)nLength - 1);

         *pNALOutputBufferLength = nIndex;
      }
      break;

      case P_HCI_SERVICE_MREAD_KOVIO:
      {
#define P_NFC_HAL_BINDING_CARD_FOUND_SIZE_KOVIO                   16
         if (nLength == P_NFC_HAL_BINDING_CARD_FOUND_SIZE_KOVIO)
         {
            uint8_t nCrcFirst, nCrcSecond;

            pNALOutputBuffer[nIndex++] = NAL_SERVICE_READER_KOVIO;
            pNALOutputBuffer[nIndex++] = NAL_EVT_READER_TARGET_DISCOVERED;

            nIndex += (uint32_t)PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_KOVIO(
                           (uint8_t *)(pNALOutputBuffer + nIndex),
                           (uint8_t *)pBuffer,
                           (uint32_t)nLength);

            static_PNALBindingComputeCrc(
                                         P_NFC_HAL_BINDING_CRC_A,
                                         pNALOutputBuffer + nIndex - P_NFC_HAL_BINDING_CARD_FOUND_SIZE_KOVIO,
                                         (P_NFC_HAL_BINDING_CARD_FOUND_SIZE_KOVIO - 2),
                                         &nCrcFirst,
                                         &nCrcSecond);

            if( (nCrcFirst  != pNALOutputBuffer[nIndex - 1]) ||
                (nCrcSecond != pNALOutputBuffer[nIndex - 2]))
            {
               *pNALOutputBufferLength = 0;
               PNALDebugError("static_PNALBindingCardDiscovered : invalid CRC");
               return W_ERROR_RF_COMMUNICATION;
            }

            *pNALOutputBufferLength = nIndex;
         }
         else
         {
            PNALDebugError("static_PNALBindingCardDiscovered : invalid CARD_FOUND size %d", nLength);
            return W_ERROR_BAD_PARAMETER;
         }
      }
      break;
      default:
         PNALDebugError("static_PNALBindingCardDiscovered:unknow Reader protocol mode: 0x%2x", nHCIReaderServiceIdentifier);
         return W_ERROR_RF_PROTOCOL_NOT_SUPPORTED;
      break;
   }
   return W_SUCCESS;
}

/**
 * @brief   Adapts the HCI card selected payload to NFC HAL selected card structure.
 *
 * @param[in]  nHCICardServiceIdentifier  The ID of the Reader Service.
 *
 * @param[in]  pNALOutputBuffer  The out put buffer containing the NFC HAL Card selected structure.
 *
 * @param[in]  pNALOutputBufferLength  The out put buffer size.
 *
 * @param[in]  pBuffer  The buffer containing the payload of selected card.
 *
 * @param[in]  nLength  The payload size.
 **/
static W_ERROR static_PNALBindingCardSelected(
                                 tNALBindingContext *pBindingContext,
                                 uint8_t nHCICardServiceIdentifier,
                                 uint8_t* pNALOutputBuffer,
                                 uint32_t* pNALOutputBufferLength,
                                 uint8_t* pBuffer,
                                 uint32_t nLength
                                 )
{
    uint32_t nIndex = 0x02;
    /* check the input and output buffers */
   if((pNALOutputBuffer == null)||
      (pNALOutputBufferLength == null)||
      (pBuffer == null))
   {
       PNALDebugError("static_PNALBindingCardSelected:incorrect input or output buffer format");
       return W_ERROR_BAD_PARAMETER;
   }
   *pNALOutputBufferLength = 0x02;

   switch(nHCICardServiceIdentifier)
   {
      case P_HCI_SERVICE_MCARD_ISO_14_A_4:

         pNALOutputBuffer[0] = NAL_SERVICE_CARD_14_A_4;
         /* set the type Event */
         pNALOutputBuffer[1] = NAL_EVT_CARD_SELECTED;
         /* Set the data rate max */
         pNALOutputBuffer[nIndex++] = P_NFC_HAL_BINDING_DATA_RATE_MAX_DEFAULT;
         /* set the CID */
         pNALOutputBuffer[nIndex++] = 0x00;
         /* Restore UID */
         if(pBindingContext->nUIDCardLength != 0)
         {
            CNALMemoryCopy(&pNALOutputBuffer[nIndex], pBindingContext->aUIDCard, pBindingContext->nUIDCardLength);
         }
         nIndex += 10;
         pNALOutputBuffer[nIndex++] = pBindingContext->nUIDCardLength;
         *pNALOutputBufferLength += nIndex;
      break;

      case P_HCI_SERVICE_MCARD_ISO_14_B_4:

         pNALOutputBuffer[0] = NAL_SERVICE_CARD_14_B_4;
         /* set the type Event */
         pNALOutputBuffer[1] = NAL_EVT_CARD_SELECTED;
        /* set the default payload response */
        CNALMemoryCopy(&pNALOutputBuffer[nIndex], g_aIsoB4TypeBSelectedCard, sizeof(g_aIsoB4TypeBSelectedCard));
         /* Restore UID */
         if(pBindingContext->nUIDCardLength != 0)
         {
            CNALMemoryCopy(
                 &pNALOutputBuffer[6],
                  pBindingContext->aUIDCard,
                 pBindingContext->nUIDCardLength);
         }
         *pNALOutputBufferLength += sizeof(g_aIsoB4TypeBSelectedCard);
         break;

      case P_HCI_SERVICE_P2P_TARGET:
          pNALOutputBuffer[0] = NAL_SERVICE_P2P_TARGET;
          /* set the type Event */
          pNALOutputBuffer[1] = NAL_EVT_P2P_INITIATOR_DISCOVERED;
          static_PNALBindingWriteUint32ToNALBuffer(424000, &pNALOutputBuffer[2]);
          *pNALOutputBufferLength += 4;
          CNALMemoryCopy(&pNALOutputBuffer[6], pBuffer, nLength);
          *pNALOutputBufferLength += nLength;
          break;

      default:
         PNALDebugError("static_PNALBindingCardSelected:unknow Card protocol mode: 0x%2x", nHCICardServiceIdentifier);
         return W_ERROR_RF_PROTOCOL_NOT_SUPPORTED;
   }

   return W_SUCCESS;
}

/**
* @See definition of tPHCIServiceCommandDataReceived
**/
NFC_HAL_INTERNAL void PNALBindingCardEventDataReceived(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t nEventIdentifier,
         uint32_t nOffset,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nHCIMessageReceptionCounter)
{
   uint32_t nNALLength = 0x02;
   W_ERROR nError;
   uint8_t nHCIServiceIdentifier = 0;
   uint8_t nProtocolIndex;
   uint8_t nNALServiceIdentifier = (uint8_t)PNALUtilConvertPointerToUint32(pCallbackParameter);

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nHCIMessageReceptionCounter);

   for(nProtocolIndex = 0; nProtocolIndex < sizeof(g_aNALBindingCardProtocolArray)/sizeof(tNALBindingProtocolEntry); nProtocolIndex++)
   {
      if(g_aNALBindingCardProtocolArray[nProtocolIndex].nNALServiceIdentifier == nNALServiceIdentifier)
      {
         nHCIServiceIdentifier = g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIServiceIdentifier;
         break;
      }
   }

   if(pBuffer == null)
   {
      PNALDebugError("PNALBindingCardEventDataReceived: HCI Buffer is null");
      return;
   }
   if(nOffset == 0)
   {
      pBindingContext->nHCIReceptionBufferLength = 0;
   }
   if(nOffset + nLength > P_HCI_RECEIVED_FRAME_MAX_LENGTH)
   {
      PNALDebugError("PNALBindingCardEventDataReceived: HCI Buffer too short");
      pBindingContext->nHCIReceptionBufferLength = 0;
      return;
   }
   if(nLength != 0)
   {
      CNALMemoryCopy(
         &pBindingContext->aHCIReceptionBuffer[nOffset],
         pBuffer,
         nLength);
      pBindingContext->nHCIReceptionBufferLength += nLength;
   }

   if(nHCIMessageReceptionCounter == 0)
   {
      PNALDebugTrace("PNALBindingCardEventDataReceived: HCI event is chained");
      return;
   }

   switch(nEventIdentifier)
   {
      case ETSI_EVT_CARD_ACTIVATED:

         if (pBindingContext->sActivatedCard.nHCIProtocol != 0)
         {
            PNALDebugWarning("PNALBindingCardEventDataReceived : received ACTIVATED without previous deactivation");
         }

         pBindingContext->sActivatedCard.nHCIProtocol =  static_PNALBindingGetHCICardProtocolFromServiceID(nHCIServiceIdentifier);
         pBindingContext->sActivatedCard.nHCIServiceIdentifier = nHCIServiceIdentifier;
         pBindingContext->sActivatedCard.nNALServiceIdentifier = nNALServiceIdentifier;
         pBindingContext->sActivatedCard.nHCIReceptionCounter = nHCIMessageReceptionCounter;

         /* Get the Parameters:
          * Default parameters are used: NFCC does not provide the parameters for read operation
          **/
         nError = static_PNALBindingCardSelected(
                             pBindingContext,
                             pBindingContext->sActivatedCard.nHCIServiceIdentifier,
                             pBindingContext->pNALReceptionBuffer,
                             &nNALLength,
                             pBindingContext->aHCIReceptionBuffer,
                             pBindingContext->nHCIReceptionBufferLength);

         if(nError != W_SUCCESS)
         {
            PNALDebugError("PNALBindingCardEventDataReceived: static_PNALBindingCardSelected returns 0x%x", nError);
            return;
         }
       break;

       case ETSI_EVT_CARD_DEACTIVATED:

         if (pBindingContext->sActivatedCard.nHCIProtocol != 0)
         {
            pBindingContext->pNALReceptionBuffer[0] = nNALServiceIdentifier;
            pBindingContext->pNALReceptionBuffer[1] = NAL_EVT_CARD_END_OF_TRANSACTION;
            /* The reader has sent a de-selected command */
            pBindingContext->pNALReceptionBuffer[2] = 0x00;
            nNALLength += 0x01;

            CNALMemoryFill(& pBindingContext->sActivatedCard, 0, sizeof(pBindingContext->sActivatedCard));
         }
         else
         {
            PNALDebugWarning("PNALBindingCardEventDataReceived : received DEACTIVATED whereas we are not in ACTIVATED mode");
            return;
         }


         break;

       case ETSI_EVT_SEND_DATA:

         if (pBindingContext->sActivatedCard.nHCIProtocol == 0)
         {
            PNALDebugWarning("PNALBindingCardEventDataReceived : received DATA whereas we are not in ACTIVATED mode");
         }

         /* Check the RF error */
         if ( pBindingContext->aHCIReceptionBuffer[pBindingContext->nHCIReceptionBufferLength - 1] == 0 )
         {
            pBindingContext->pNALReceptionBuffer[0] = nNALServiceIdentifier;
            pBindingContext->pNALReceptionBuffer[1] = (nNALServiceIdentifier != NAL_SERVICE_P2P_TARGET)? NAL_EVT_CARD_SEND_DATA :NAL_EVT_P2P_SEND_DATA;
            CNALMemoryCopy(
               &pBindingContext->pNALReceptionBuffer[2],
               pBindingContext->aHCIReceptionBuffer,
               (pBindingContext->nHCIReceptionBufferLength - 1));
            nNALLength += (pBindingContext->nHCIReceptionBufferLength - 1);
         }
         else
         {
            PNALDebugError("PNALBindingCardEventDataReceived: RF Error");
            return;
         }
         break;

      case ETSI_EVT_FIELD_ON:

         pBindingContext->pNALReceptionBuffer[0] = NAL_SERVICE_ADMIN;
         pBindingContext->pNALReceptionBuffer[1] = NAL_EVT_RF_FIELD;
         pBindingContext->pNALReceptionBuffer[2] = 0x01;
         nNALLength += 0x01;
         break;

      case ETSI_EVT_FIELD_OFF:

         if (pBindingContext->sActivatedCard.nHCIProtocol != 0)
         {
            pBindingContext->pNALReceptionBuffer[0] = pBindingContext->sActivatedCard.nNALServiceIdentifier;
            pBindingContext->pNALReceptionBuffer[1] = NAL_EVT_CARD_END_OF_TRANSACTION;
             /* The reader did not send a de-selected command */
            pBindingContext->pNALReceptionBuffer[2] = 0x01;
            nNALLength += 0x01;
         }
         else
         {
            pBindingContext->pNALReceptionBuffer[0] = NAL_SERVICE_ADMIN;
            pBindingContext->pNALReceptionBuffer[1] = NAL_EVT_RF_FIELD;
            pBindingContext->pNALReceptionBuffer[2] = 0x00;
            nNALLength += 0x01;
         }

         CNALMemoryFill(& pBindingContext->sActivatedCard, 0, sizeof(pBindingContext->sActivatedCard));
         break;

      default:
         PNALDebugError("PNALBindingCardEventDataReceived: unknow event ID 0x%2x", nEventIdentifier);
         return;
   }

   PNALBindingCallReadCallback(pBindingContext, nNALLength, pBindingContext->nReceptionCounter);
}

static void static_PNALBindingGetCollisionCompleted(
                          tNALBindingContext* pBindingContext,
                          void* pCallbackParameter,
                          uint8_t* pBuffer,
                          uint32_t nLength,
                          W_ERROR nError,
                          uint8_t nStatusCode,
                          uint32_t nReceptionCounter)
{
   uint16_t aCollision[8];
   uint32_t i;
   uint32_t nIndex;
   uint32_t nNALLength = 0;
   uint8_t nService = NAL_SERVICE_READER_14_A_4;

   if ((nError == W_SUCCESS) && (nStatusCode == ETSI_ERR_ANY_OK) && (pBindingContext->aHCIResponseBuffer[0] == HCI2_PAR_MREAD_GEN_COLLISION_FOUND))
   {
      pBindingContext->nHCIReceptionBufferLength = nLength;
      if (pBindingContext->nHCIReceptionBufferLength > 10)
      {
         PNALDebugError("static_PNALBindingCollisionCompleted : received buffer too long");
         pBindingContext->nHCIReceptionBufferLength = 10;
      }

      for (nIndex = 0,  i=2 ; i<pBindingContext->nHCIReceptionBufferLength; i++)
      {
         switch (pBindingContext->aHCIResponseBuffer[i])
         {
            /* ISO 14443 A family */
            case 0x01 : /* T1 */
            case 0x02 : /* A4 */
            case 0x08 : /* A3 */
            case 0x0B : /* KOVIO */
               nService = NAL_SERVICE_READER_14_A_4;
               aCollision[nIndex++] = NAL_PROTOCOL_READER_ISO_14443_4_A;
               break;

            /* ISO 14443 B family */
            case 0x00 : /* B4 */
            case 0x06 : /* B3 */
               nService = NAL_SERVICE_READER_14_B_4;
               aCollision[nIndex++] = NAL_PROTOCOL_READER_ISO_14443_4_B;
               break;

            /* ISO 18092 family */
            case 0x05 :  /* T3 */
            case 0x0A :  /* P2P INIT F */
               nService = NAL_SERVICE_READER_FELICA;
               aCollision[nIndex++] = NAL_PROTOCOL_READER_FELICA;
               break;

            /* ISO 15693-3 family */
            case 0x03 : /* 15-3 */
               nService = NAL_SERVICE_READER_15_3;
               aCollision[nIndex++] = NAL_PROTOCOL_READER_ISO_15693_3;
               break;

            /* B Prime */

            case 0x07 : /* BP */
               nService = NAL_SERVICE_READER_B_PRIME;
               aCollision[nIndex++] = NAL_PROTOCOL_READER_BPRIME;
               break;

            default:
               /* Other values are not supported */
               break;
         }
      }

      pBindingContext->pNALReceptionBuffer[nNALLength++] = nService;
      pBindingContext->pNALReceptionBuffer[nNALLength++] = NAL_EVT_READER_TARGET_COLLISION;
      nNALLength += PNALProtocolFormat_NAL_EVT_READER_TARGET_COLLISION( pBindingContext->pNALReceptionBuffer + 2,  aCollision, nIndex);

      PNALBindingCallReadCallback(pBindingContext, nNALLength, pBindingContext->nReceptionCounter);
   }
   else
   {
      PNALDebugError("static_PNALBindingCollisionCompleted : unable to retrieve HCI2_PAR_MREAD_GEN_COLLISION_FOUND");
   }
}


static void static_PNALBindingCardFoundErrorDetectionReaderSendEventCompleted(
            tNALBindingContext* pBindingContext,
            void* pCallbackParameter,
            W_ERROR nError,
            uint32_t nHCIMessageReceptionCounter )
{
   PNALDebugTrace("static_PNALBindingCardFoundErrorDetectionReaderSendEventCompleted nError %d", nError);

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nHCIMessageReceptionCounter);

   if (nError != W_SUCCESS)
   {
      PNALDebugTrace("static_PNALBindingCardFoundErrorDetectionReaderSendEventCompleted nError %d", nError);
   }
}


/**
* @See definition of tPHCIServiceCommandDataReceived
**/
NFC_HAL_INTERNAL void PNALBindingReaderEventDataReceived(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t nEventIdentifier,
         uint32_t nOffset,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nHCIMessageReceptionCounter)
{
   uint32_t nNALLength = 0x02;
   uint8_t nHCIReaderServiceIdentifier = (uint8_t)PNALUtilConvertPointerToUint32(pCallbackParameter);

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nHCIMessageReceptionCounter);

   if(pBuffer == null)
   {
      PNALDebugError("PNALBindingReaderEventDataReceived: HCI Buffer is null");
      return;
   }
   if(nOffset == 0)
   {
      pBindingContext->nHCIReceptionBufferLength = 0;
   }
   if(nOffset + nLength > P_HCI_RECEIVED_FRAME_MAX_LENGTH)
   {
      PNALDebugError("PNALBindingReaderEventDataReceived: HCI Buffer to short");
      pBindingContext->nHCIReceptionBufferLength = 0;
      return;
   }
   CNALMemoryCopy(
      &pBindingContext->aHCIReceptionBuffer[nOffset],
      pBuffer,
      nLength);
   pBindingContext->nHCIReceptionBufferLength += nLength;

   if(nHCIMessageReceptionCounter == 0)
   {
      PNALDebugTrace("PNALBindingReaderEventDataReceived: HCI event is chained");
      return;
   }

   if ((pBindingContext->nLastStartSomeReceptionCounter == 0) &&
      ( (nEventIdentifier == HCI2_EVT_MREAD_CARD_FOUND) ||
        (nEventIdentifier == HCI2_EVT_MREADER_DISCOVERY_OCCURED)))
   {
      /* we are in the detection restart automaton, and EVT_START_SOME have not yet been sent,
         the current RF communication will be broken when EVT_START_SOME will be sent */

      PNALDebugWarning("PNALBindingReaderEventDataReceived: EVT_START_SOME is pending");
      return;
   }

   switch(nEventIdentifier)
   {
      case HCI2_EVT_MREAD_CARD_FOUND:
         if (static_PNALBindingCardDiscovered(
                        nHCIReaderServiceIdentifier,
                        pBindingContext->pNALReceptionBuffer,
                        &pBindingContext->nNALReceptionBufferLength,
                        pBindingContext->aHCIReceptionBuffer,
                        pBindingContext->nHCIReceptionBufferLength) != W_SUCCESS)
         {
            uint16_t nHCIReaderProtocolCapa;

            PNALDebugError("PNALBindingReaderEventDataReceived: Protocol error");

            /* restart polling to avoid to remain stuck in RF ON */

            static_PNALBindingGetHCIReaderProtocolCapabilities(
                                       &nHCIReaderProtocolCapa,
                                       pBindingContext->nLastNALReaderProtocol);

            static_PNALBindingWriteUint16ToHCIBuffer(nHCIReaderProtocolCapa, pBindingContext->aHCISendBuffer);

            /* Enable all selected reader protocol */
            PHCIServiceSendEvent(
                        pBindingContext,
                        pBindingContext->nOneOfOpenedReaderServiceId,
                      & pBindingContext->sNALBindingWakeUpOperation,
                        HCI2_EVT_MREADER_DISCOVERY_START_SOME,
                        pBindingContext->aHCISendBuffer,
                        0x02,
                        static_PNALBindingCardFoundErrorDetectionReaderSendEventCompleted,
                        0,
                        W_TRUE);
            return;
         }

#ifdef P_INCLUDE_MIFARE_CLASSIC
         /* Special initialisation of Mifare 1K 4K */
         if(   ( nHCIReaderServiceIdentifier == P_HCI_SERVICE_MREAD_ISO_14_A_3)
            && (  ((pBindingContext->aHCIReceptionBuffer[HCI2_CMD_MREAD_CARD_FOUND_14_3_A_SAK_OFFSET]
                                    & P_NFC_HAL_BINDING_SAK_MIFARE_4K) == P_NFC_HAL_BINDING_SAK_MIFARE_4K)
               || ((pBindingContext->aHCIReceptionBuffer[HCI2_CMD_MREAD_CARD_FOUND_14_3_A_SAK_OFFSET]
                                    & P_NFC_HAL_BINDING_SAK_MIFARE_1K) == P_NFC_HAL_BINDING_SAK_MIFARE_1K)))
         {
            pBindingContext->pMifareContext = CNALMifareInit(&pBindingContext->aHCIReceptionBuffer[HCI2_CMD_MREAD_CARD_FOUND_14_3_A_UID_OFFSET],
                                                             pBindingContext->aHCIReceptionBuffer[HCI2_CMD_MREAD_CARD_FOUND_14_3_A_UID_LENGTH_OFFSET]);
         }
#endif

         nNALLength = pBindingContext->nNALReceptionBufferLength;

         pBindingContext->nCurrentHCIReaderServiceIdentifier = nHCIReaderServiceIdentifier;


         break;


      case HCI2_EVT_MREADER_DISCOVERY_OCCURED:

         if(nLength != 0x01)
         {
            PNALDebugError("PNALBindingReaderEventDataReceived: size of EVT-HCI2_EVT_MREADER_DISCOVERY_OCCURED is wrong:0x%x", nLength);
            return;
         }

         if (pBuffer[0] == 0x00)
         {
            /* Ok, a discovery occured */

            if (nHCIReaderServiceIdentifier == P_HCI_SERVICE_P2P_INITIATOR)
            {
               uint8_t* pHCISendBuffer;

               /* The NFCC has not implemented the P2P initiator in quick mode */
                pHCISendBuffer = pBindingContext->aNALBindingNALService[NAL_SERVICE_P2P_INITIATOR].aNALOperationBuffer;
                pHCISendBuffer[0] = HCI2_PAR_P2P_INITIATOR_GT;
                /* Get GT bytes of discovered target*/
                PHCIServiceExecuteCommand(
                                 pBindingContext,
                                 P_HCI_SERVICE_P2P_INITIATOR,
                                 PNALBindingGetOperation(pBindingContext),
                                 P_HCI_SERVICE_CMD_GET_PROPERTIES,
                                 pHCISendBuffer,
                                 0x01,
                                 pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
                                 static_PNALBindingGetInitiatorGTbytesCompleted,
                                 null,
                                 W_FALSE);
               return;
         }
            else
            {
               /* should not occur, since other detections use quick commands */
               PNALDebugError("PNALBindingReaderEventDataReceived: HCI2_EVT_MREADER_DISCOVERY_OCCURED in READER Mode");
               return;
            }
         }
         else if (pBuffer[0] == 0x03)
         {
            /* Ok, a collision occured */
            uint8_t* pHCISendBuffer;

            pHCISendBuffer = pBindingContext->aNALBindingNALService[NAL_SERVICE_READER_14_A_4].aNALOperationBuffer;
            pHCISendBuffer[0] = HCI2_PAR_MREAD_GEN_COLLISION_FOUND;

            PHCIServiceExecuteCommand(
                                 pBindingContext,
                                 P_HCI_SERVICE_MREAD,
                                 PNALBindingGetOperation(pBindingContext),
                                 P_HCI_SERVICE_CMD_GET_PROPERTIES,
                                 pHCISendBuffer,
                                 0x01,
                                 pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
                                 static_PNALBindingGetCollisionCompleted,
                                 null,
                                 W_FALSE);

            return;
         }
         else
         {
            /* Unsupported value */
            PNALDebugError("HCI2_EVT_MREADER_DISCOVERY_OCCURED : unsupported status value");
            return;
         }

         break;

      case HCI2_EVT_P2P_INITIATOR_EXCHANGE_FROM_RF:

         pBindingContext->pNALReceptionBuffer[0] = NAL_SERVICE_P2P_INITIATOR;
         pBindingContext->pNALReceptionBuffer[1] = NAL_EVT_P2P_SEND_DATA;

         CNALMemoryCopy(
               &pBindingContext->pNALReceptionBuffer[2],
               pBindingContext->aHCIReceptionBuffer,
               (pBindingContext->nHCIReceptionBufferLength));
            nNALLength = 0x02 + pBindingContext->nHCIReceptionBufferLength - 1;       /* -1 to remove supplemental byte introduced in MR F/W v 6.11a */

         break;

      case HCI2_EVT_MREADER_SIM_REQUEST:

         if(nLength != 0x02)
         {
            PNALDebugError("PNALBindingReaderEventDataReceived: size of EVT-SIM_REQUEST is wrong:0x%x", nLength);
         }

         if (pBindingContext->bSendDetectionAnswerOnNextSimRequest != W_FALSE)
         {
            pBindingContext->bSendDetectionAnswerOnNextSimRequest = W_FALSE;

            pBindingContext->pNALReceptionBuffer[0] = NAL_SERVICE_ADMIN;
            pBindingContext->pNALReceptionBuffer[1] = NAL_RES_OK;

            if (pBindingContext->sActivatedCard.nHCIProtocol != 0)
            {
               PNALBindingCallReadCallback(pBindingContext, 0x02, pBindingContext->sActivatedCard.nHCIReceptionCounter);
            }
            else
            {
               PNALBindingCallReadCallback(pBindingContext, 0x02, pBindingContext->nLastStartSomeReceptionCounter);
            }
         }

         return;

      default:

         PNALDebugError("PNALBindingReaderEventDataReceived: unknow event ID 0x%2x", nEventIdentifier);
         return;
   }

   PNALBindingCallReadCallback(pBindingContext, nNALLength, pBindingContext->nReceptionCounter);
 }

static void static_PNALBindingCardSendEventCompleted(
            tNALBindingContext* pBindingContext,
            void* pCallbackParameter,
            W_ERROR nError,
            uint32_t nReceptionCounter )
{
   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   /* Check the result */
   if ( nError != W_SUCCESS )
   {
      PNALDebugError(
         "static_PNALBindingCardSendEventCompleted: nError = %s", PNALUtilTraceError(nError) );
   }

   static_PNALBindingCallWriteCallback(pBindingContext);
}

/**
 *See Definition of PHCISendAnswerCompleted
 **/
static void static_PNALBindingSendEventCompleted(
            tNALBindingContext* pBindingContext,
            void* pCallbackParameter,
            W_ERROR nError,
            uint32_t nReceptionCounter )
{
   pBindingContext->pNALReceptionBuffer[0] = (uint8_t)PNALUtilConvertPointerToUint32(pCallbackParameter);

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   /* Check the result */
   if ( nError != W_SUCCESS )
   {
      PNALDebugError(
         "static_PNALBindingSendEventCompleted: nError = %s", PNALUtilTraceError(nError) );
      pBindingContext->pNALReceptionBuffer[1] = NAL_RES_PROTOCOL_ERROR;
   }
   else
   {
      pBindingContext->pNALReceptionBuffer[1] = NAL_RES_OK;
   }

   /* call the write callback */
   static_PNALBindingCallWriteCallback(pBindingContext);

   /* post the result */
   PNALBindingCallReadCallback(pBindingContext, 0x02, pBindingContext->nReceptionCounter);
}

static void static_PNALBindingConfigureLLCPTimeoutCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         W_ERROR nError,
         uint8_t nStatusCode,
         uint32_t nReceptionCounter)
{
   PNALDebugTrace("static_PNALBindingConfigureLLCPTimeoutCompleted()");

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   if ((nError != W_SUCCESS) || (nStatusCode != ETSI_ERR_ANY_OK))
   {
      pBindingContext->bLLCPTimeoutSupported = W_FALSE;
   }
}

static void static_PNALBindingConfigureLLCPTimeout(
      tNALBindingContext* pBindingContext,
      uint8_t * pBuffer,
      uint32_t nLength)
{

   if (pBindingContext->bLLCPTimeoutSupported == W_TRUE)
   {
      #define LLCP_PARAM_LTO_TYPE               0x04
      #define LLCP_PARAM_LTO_LENGTH             0x01
      #define LLCP_PARAM_LTO_DEFAULT_VALUE      100000

      uint32_t nIndex;
      uint32_t nLTOValue = 0;
      uint8_t  nTimeout;

      if ((nLength < 3) || (pBuffer[0] != 0x46) || (pBuffer[1] != 0x66) || (pBuffer[2] != 0x6D))
      {
         return;
      }

      nIndex = 3;

      while ((nIndex + 2) < nLength)
      {
         uint8_t nType   = pBuffer[nIndex];
         uint8_t nTLVLength = pBuffer[nIndex+1];

         if ((nType == LLCP_PARAM_LTO_TYPE) && (nTLVLength == LLCP_PARAM_LTO_LENGTH))
         {
            nLTOValue = pBuffer[nIndex+2] * 10000;    /* 10 ms -> us conversion */
            break;
         }

         /* skip TLV */
         nIndex += 2 + nTLVLength;
      }

      if (nLTOValue == 0)
      {
         nLTOValue = LLCP_PARAM_LTO_DEFAULT_VALUE;
      }

      if (nLTOValue      >= 2475000)       /* 2.475 s */
         nTimeout = 0x0D;
      else if (nLTOValue >= 1237000)       /* 1.237s */
         nTimeout = 0x0C;
      else if (nLTOValue >=  618000)       /* 618 ms */
         nTimeout = 0x0B;
      else if (nLTOValue >=  309000)       /* 309 ms */
         nTimeout = 0x0A;
      else if (nLTOValue >=  154000)       /* 154 ms */
         nTimeout = 0x09;
      else if (nLTOValue >=   77300)       /* 77.3 ms */
         nTimeout = 0x08;
      else if (nLTOValue >=   38700)       /* 38.7 ms */
         nTimeout = 0x07;
      else if (nLTOValue >=   19300)       /* 19.3 ms */
         nTimeout = 0x06;
      else
         nTimeout = 0x05;             /* 9.67 ms */

      nIndex = 0;
      pBindingContext->aHCISendBuffer[nIndex++] = HCI2_PAR_MREAD_GEN_NFC_TCL_TIMEOUT_MAX;
      pBindingContext->aHCISendBuffer[nIndex++] = 1;
      pBindingContext->aHCISendBuffer[nIndex++] = nTimeout;

      PHCIServiceExecuteCommand(
                           pBindingContext,
                           P_HCI_SERVICE_MREAD,
                           PNALBindingGetOperation(pBindingContext),
                           P_HCI_SERVICE_CMD_SET_PROPERTIES,
                           pBindingContext->aHCISendBuffer,
                           nIndex,
                           pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
                           static_PNALBindingConfigureLLCPTimeoutCompleted, PNALUtilConvertUint32ToPointer(NAL_SERVICE_P2P_INITIATOR), W_FALSE);
   }
}

/**
 * @see  PHCIServiceSendEventCompleted()
 **/
static void static_PNALBindingGetInitiatorGTbytesCompleted(
                          tNALBindingContext* pBindingContext,
                          void* pCallbackParameter,
                          uint8_t* pBuffer,
                          uint32_t nLength,
                          W_ERROR nError,
                          uint8_t nStatusCode,
                          uint32_t nReceptionCounter)
{
   uint32_t nNALLength = 2;

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   pBindingContext->pNALReceptionBuffer[0] = NAL_SERVICE_P2P_INITIATOR;
   pBindingContext->pNALReceptionBuffer[1] = NAL_EVT_P2P_TARGET_DISCOVERED;
   static_PNALBindingWriteUint32ToNALBuffer(424000, &pBindingContext->pNALReceptionBuffer[2]);
   nNALLength += 4;

   if (( nError != W_SUCCESS )||
      (nStatusCode != ETSI_ERR_ANY_OK)||
      (pBuffer[0]!= HCI2_PAR_P2P_INITIATOR_GT))
   {
      CNALMemoryCopy(
                  &pBindingContext->pNALReceptionBuffer[6],
                  g_aDefaultLinkParameters,
                  sizeof(g_aDefaultLinkParameters));
             nNALLength += sizeof(g_aDefaultLinkParameters);
   }
   else
   {
       CNALMemoryCopy(
                  &pBindingContext->pNALReceptionBuffer[6],
                  &pBuffer[2],
                  (nLength - 0x02));
             nNALLength += (nLength - 0x02);

      static_PNALBindingConfigureLLCPTimeout(pBindingContext, pBuffer + 2, nLength - 2);
   }

   PNALBindingCallReadCallback(pBindingContext, nNALLength, pBindingContext->nReceptionCounter);

}

/**
 * @brief   Type of the fucntion to implement to be notified of the completion of
 * a send event operation initiated by PHCIServiceSendEvent().
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pCallbackParameter  The callback parameter specified for the call.
 *
 * @param[in]  nError  The error code:
 *               - W_SUCCESS in case of success.
 *               - W_ERROR_PROTOCOL  An error of protocol occured.
 *               - W_ERROR_CANCEL if the operation is cancelled.
 *
 * @param[in]  nHCIMessageReceptionCounter  The reception counter of the frame
 *             acknowledging the event message.
 *
 * @see  PHCIServiceSendEvent()
 **/
static void static_PNALBindingDetectionReaderSendEventCompleted(
            tNALBindingContext* pBindingContext,
            void* pCallbackParameter,
            W_ERROR nError,
            uint32_t nHCIMessageReceptionCounter )
{
   uint8_t nNALServiceIdentifier = (uint8_t)PNALUtilConvertPointerToUint32(pCallbackParameter);

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nHCIMessageReceptionCounter);

   if (nError == W_SUCCESS)
   {
      pBindingContext->nLastStartSomeReceptionCounter = nHCIMessageReceptionCounter;

      /* The HCI2_EVT_MREADER_DISCOVERY_START_SOME event has been succesfully sent,
         remember the reader mode activated to be able to restore them after a wake up from standby mode */

      pBindingContext->nLastNALReaderProtocol = pBindingContext->nNALReaderProtocol;

      pBindingContext->bSendDetectionAnswerOnNextSimRequest = W_TRUE;

      /* The bit mode have been removed by the chip when he received the DISCOVERY START SOME */
      pBindingContext->bBitMode = W_FALSE;

#ifdef P_INCLUDE_MIFARE_CLASSIC
      if(pBindingContext->bMifareExchange == W_TRUE)
      {
         pBindingContext->bMifareExchange = W_FALSE;
      }


      if(pBindingContext->pMifareContext != null)
      {
         CNALMifareDestroy(pBindingContext->pMifareContext);
         pBindingContext->pMifareContext = null;
      }
#endif
   }
   else
   {
      PNALDebugError( "static_PNALBindingDetectionReaderSendEventCompleted: nError = %s", PNALUtilTraceError(nError) );

      /* Post the result */
      pBindingContext->pNALReceptionBuffer[0] = nNALServiceIdentifier;
      pBindingContext->pNALReceptionBuffer[1] = NAL_RES_PROTOCOL_ERROR;
      PNALBindingCallReadCallback(pBindingContext, 0x02, nHCIMessageReceptionCounter);
   }
}

static void static_PNALBindingDeactivateCardMode(
          tNALBindingContext* pBindingContext,
          uint8_t nNALServiceIdentifier,
          uint8_t* pBuffer,
          uint32_t nLength,
          W_ERROR nError,
          uint8_t nStatusCode)
{
   uint8_t nIndex;
   uint16_t  nRequestedHCICardProcotols;

   PNALDebugTrace("static_PNALBindingDeactivateCardMode");

   /* Check the result */
   if ( nError != W_SUCCESS )
   {
      PNALDebugError( "static_PNALBindingDeactivateCardMode: nError %s",
         PNALUtilTraceError(nError));
      pBindingContext->pNALReceptionBuffer[1] = NAL_RES_PROTOCOL_ERROR;
      goto send_result;
   }
   else if ( nStatusCode != ETSI_ERR_ANY_OK )
   {
      PNALDebugError(
         "static_PNALBindingDeactivateCardMode: nStatusCode 0x%08X",
         nStatusCode );
      pBindingContext->pNALReceptionBuffer[0] = nNALServiceIdentifier;
      pBindingContext->pNALReceptionBuffer[1] = NAL_RES_PROTOCOL_ERROR;
      goto send_result;
   }

   static_PNALBindingGetHCICardProtocolCapabilities(
                                       &nRequestedHCICardProcotols,
                                       pBindingContext->nNALCardProtocol);

   for(nIndex = 0; nIndex < P_NFC_HAL_BINDING_NBR_CARD_PROTOCOLS; nIndex++)
   {
      if (  ((pBindingContext->nHCILastCardProtocol & (1 << nIndex)) != 0)                                    /* Card protocol is currently enabled */
         && ((pBindingContext->nHCILastCardProtocol & (1 << nIndex)  & nRequestedHCICardProcotols) == 0 ))    /* Card protocol is no longer requested */
      {
         pBindingContext->nCurrentHCIServiceIdentifier = static_PNALBindingGetHCIServiceCardIdentifierFromHCIProtocol((1 << nIndex));
         /* Reset the bit field protocol */
         pBindingContext->nHCILastCardProtocol &= ~(1 << nIndex);

         /* Disable the protocol */
         static_PNALBindingDetectionStateMachine(
                                       pBindingContext,
                                       nNALServiceIdentifier,
                                       P_NFC_HAL_BINDING_CARD_MODE_DISABLE_STATE);
         return;
      }
   }

   if ((pBindingContext->sActivatedCard.nHCIProtocol & pBindingContext->nHCILastCardProtocol) == 0)
   {
      CNALMemoryFill(&pBindingContext->sActivatedCard, 0, sizeof(pBindingContext->sActivatedCard));
   }

   PNALDebugTrace("static_PNALBindingDeactivateCardMode : all card emulations stopped");
   PNALDebugTrace("static_PNALBindingDeactivateCardMode : reactivate the card emulations");

   static_PNALBindingGetHCICardProtocolCapabilities(
                                    &pBindingContext->nHCITempCardProtocol,
                                    pBindingContext->nNALCardProtocol);
   /* Enable Selected card mode */
   static_PNALBindingActivateCardMode(
                                  pBindingContext,
                                  nNALServiceIdentifier,
                                  null,
                                  0,
                                  W_SUCCESS,
                                  ETSI_ERR_ANY_OK);
   return;

send_result:

   /* Post the result */
   PNALBindingCallReadCallback(pBindingContext, 0x02, pBindingContext->nReceptionCounter);
}

/**
 * @see  PHCIServiceExecuteCommand
 **/
static void static_PNALBindingCardModeDeactivatedCompleted(
          tNALBindingContext* pBindingContext,
          void* pCallbackParameter,
          uint8_t* pBuffer,
          uint32_t nLength,
          W_ERROR nError,
          uint8_t nStatusCode,
          uint32_t nReceptionCounter)
{
   uint8_t nNALServiceIdentifier = (uint8_t)PNALUtilConvertPointerToUint32(pCallbackParameter);

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   static_PNALBindingDeactivateCardMode(
          pBindingContext,
          nNALServiceIdentifier,
          pBuffer,
          nLength,
          nError,
          nStatusCode);
}

static void static_PNALBindingActivateCardMode(
          tNALBindingContext* pBindingContext,
          uint8_t nNALServiceIdentifier,
          uint8_t* pBuffer,
          uint32_t nLength,
          W_ERROR nError,
          uint8_t nStatusCode)
{
   uint8_t nIndex;

   /* Check the result */
   if ( nError != W_SUCCESS )
   {
      PNALDebugError( "static_PNALBindingActivateCardMode: nError %s",
         PNALUtilTraceError(nError));
      pBindingContext->pNALReceptionBuffer[1] = NAL_RES_PROTOCOL_ERROR;
      goto send_result;
   }
   else if ( nStatusCode != ETSI_ERR_ANY_OK )
   {
      PNALDebugError(
         "static_PNALBindingActivateCardMode: nStatusCode 0x%08X",
         nStatusCode );
      pBindingContext->pNALReceptionBuffer[0] = nNALServiceIdentifier;
      pBindingContext->pNALReceptionBuffer[1] = NAL_RES_PROTOCOL_ERROR;
      goto send_result;
   }
   for(nIndex = 0; nIndex < P_NFC_HAL_BINDING_NBR_CARD_PROTOCOLS; nIndex++)
   {
      if ( ((pBindingContext->nHCITempCardProtocol & (1 << nIndex)) != 0)     /* The card emulation is requested */ &&
           ((pBindingContext->nHCILastCardProtocol & (1 << nIndex)) == 0) )   /* the card emulation was not requested */
      {
         pBindingContext->nCurrentHCIServiceIdentifier = static_PNALBindingGetHCIServiceCardIdentifierFromHCIProtocol((1 << nIndex));
         /* Reset the bit field protocol */
         pBindingContext->nHCITempCardProtocol &= ~(1 << nIndex);
         /* Enable the protocol */
         static_PNALBindingDetectionStateMachine(
                                       pBindingContext,
                                       nNALServiceIdentifier,
                                       P_NFC_HAL_BINDING_CARD_MODE_ENABLE_STATE);
         return;
      }
   }

   PNALDebugTrace("static_PNALBindingActivateCardMode : all card emulation restarted");
   PNALDebugTrace("static_PNALBindingActivateCardMode : restarting reader modes");

   static_PNALBindingGetHCICardProtocolCapabilities(
                                       &pBindingContext->nHCILastCardProtocol,
                                       pBindingContext->nNALCardProtocol);
   static_PNALBindingDetectionStateMachine(
                                       pBindingContext,
                                       nNALServiceIdentifier,
                                       P_NFC_HAL_BINDING_READER_MODE_DEACTIVATE_STATE);
   return;
send_result:

   /* Post the result */
   PNALBindingCallReadCallback(pBindingContext, 0x02, pBindingContext->nReceptionCounter);
}

/**
 * @see  PHCIServiceExecuteCommand
 **/
static void static_PNALBindingCardModeActivatedCompleted(
          tNALBindingContext* pBindingContext,
          void* pCallbackParameter,
          uint8_t* pBuffer,
          uint32_t nLength,
          W_ERROR nError,
          uint8_t nStatusCode,
          uint32_t nReceptionCounter)
{
   uint8_t nNALServiceIdentifier = (uint8_t)PNALUtilConvertPointerToUint32(pCallbackParameter);

      /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   if ((nError == W_SUCCESS) && (nStatusCode == ETSI_ERR_ANY_OK) && (pBindingContext->nFirstCardModeActivatedCounter == 0))
   {
      pBindingContext->nFirstCardModeActivatedCounter = nReceptionCounter;
   }

   static_PNALBindingActivateCardMode(
          pBindingContext,
          nNALServiceIdentifier,
          pBuffer,
          nLength,
          nError,
          nStatusCode);
}
/**
 * @brief   Check the detection State.
 *
 * @param[in]  pBindingContext  The NFC HAL Binding Instance.
 *
 * @param[in]  nNALServiceIdentifier  The NFC HAL Service.
**/
static void static_PNALBindingCheckDetectionState(
                                       tNALBindingContext *pBindingContext,
                                       uint8_t nNALServiceIdentifier)
{
   PNALDebugTrace("static_PNALBindingCheckDetectionState : stopping the card emulations");

   /* Disable all card modes previously enabled */
   static_PNALBindingDeactivateCardMode(
                               pBindingContext,
                               nNALServiceIdentifier,
                               null,
                               0,
                               W_SUCCESS,
                               ETSI_ERR_ANY_OK
                               );
}

/**
 * @brief   Start new detection.
 *
 * @param[in]  pBindingContext  The NFC HAL Binding Instance.
 *
 * @param[in]  nNALServiceIdentifier  The NFC HAL Service.
 *
 * @param[in]  pHCISendBuffer  The HCI send buffer.
**/
static void static_PNALBindingStartNewDetection(
                                       tNALBindingContext *pBindingContext,
                                       uint8_t nNALServiceIdentifier,
                                       uint8_t* pHCISendBuffer)
{
   PNALDFCPost1(pBindingContext, P_DFC_TYPE_NFC_HAL_BINDING, static_PNALBindingCheckDetectionState, PNALUtilConvertUint32ToPointer((uint32_t) nNALServiceIdentifier));
}


#ifdef P_NAL_DEACTIVATE_BEFORE_POLL_AGAIN

static void static_PNALBindingReaderDeactivatedCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         W_ERROR nError,
         uint8_t nStatusCode,
         uint32_t nHCIMessageReceptionCounter)
{
   uint8_t nNALServiceIdentifier = (uint8_t)PNALUtilConvertPointerToUint32(pCallbackParameter);

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nHCIMessageReceptionCounter);

   if ((nError != W_SUCCESS) || (nStatusCode != ETSI_ERR_ANY_OK))
   {
      PNALDebugError("static_PNALBindingReaderDeactivatedCompleted nError %d - nStatusCode %d\n", nError, nStatusCode);
   }

   static_PNALBindingDetectionStateMachine(pBindingContext,  nNALServiceIdentifier, P_NFC_HAL_BINDING_READER_MODE_START_STATE);
}

#endif

/**
 * @brief   Set the detection sequence.
 *
 * @param[in]  pBindingContext  The NFC HAL Binding Instance.
 *
 * @param[in]  nNALServiceIdentifier  The NFC HAL Service.
 *
 * @param[in]  nPerformDetectionState the flag indicates if reader or card protocol have to be activated in the current step
**/
static void static_PNALBindingDetectionStateMachine(
                                       tNALBindingContext *pBindingContext,
                                       uint8_t nNALServiceIdentifier,
                                       uint8_t nPerformDetectionState)
{
   uint8_t* pHCISendBuffer = pBindingContext->aNALBindingNALService[nNALServiceIdentifier].aNALOperationBuffer;

   switch(nPerformDetectionState)
   {

      case P_NFC_HAL_BINDING_READER_MODE_DEACTIVATE_STATE:

#ifdef P_NAL_DEACTIVATE_BEFORE_POLL_AGAIN

      /* If the current detected card is a ISO A or ISO B, request deactivation of the card before performing
         the new detection sequence */

      if ( (pBindingContext->nCurrentHCIReaderServiceIdentifier == P_HCI_SERVICE_MREAD_ISO_14_A_4) ||
           (pBindingContext->nCurrentHCIReaderServiceIdentifier == P_HCI_SERVICE_MREAD_ISO_14_B_4) )
      {
         /* Deactivate the card prior cutting the RF */

         pHCISendBuffer[0] = 0x00;

         PHCIServiceExecuteCommand(
                  pBindingContext,
                  pBindingContext->nCurrentHCIReaderServiceIdentifier,
                  PNALBindingGetOperation(pBindingContext),
                  HCI2_CMD_MREAD_DEACTIVATION,
                  pHCISendBuffer,
                  0x01,
                  null, 0,
                  static_PNALBindingReaderDeactivatedCompleted, PNALUtilConvertUint32ToPointer(nNALServiceIdentifier),
                  W_FALSE );

         break;
      }
      else
      {
         PNALDebugTrace("static_PNALBindingDetectionStateMachine : no need to deactivate, go to next state");
      }

#endif
      /* else fall through ... */

      case P_NFC_HAL_BINDING_READER_MODE_START_STATE:
      {
         uint16_t nHCIReaderProtocolCapa;

         PNALDebugTrace("static_PNALBindingDetectionStateMachine : P_NFC_HAL_BINDING_READER_MODE_PERFORM_STATE");

         static_PNALBindingGetHCIReaderProtocolCapabilities(
                                       &nHCIReaderProtocolCapa,
                                       pBindingContext->nNALReaderProtocol);

         static_PNALBindingWriteUint16ToHCIBuffer(
                                          nHCIReaderProtocolCapa,
                                          pHCISendBuffer);

         /* Enable all selected reader protocol */
         PHCIServiceSendEvent(
                     pBindingContext,
                     pBindingContext->nOneOfOpenedReaderServiceId,
                     PNALBindingGetOperation(pBindingContext),
                     HCI2_EVT_MREADER_DISCOVERY_START_SOME,
                     pHCISendBuffer,
                     0x02,
                     static_PNALBindingDetectionReaderSendEventCompleted,
                     PNALUtilConvertUint32ToPointer(nNALServiceIdentifier),
                     W_FALSE);
         break;
      }
      case P_NFC_HAL_BINDING_CARD_MODE_DISABLE_STATE:

         PNALDebugTrace("static_PNALBindingDetectionStateMachine : P_NFC_HAL_BINDING_CARD_MODE_DISABLE_STATE");

         pHCISendBuffer[0] = static_PNALBindingGetCardTypeMode(pBindingContext->nCurrentHCIServiceIdentifier);
         pHCISendBuffer[1] = 0x01;
         pHCISendBuffer[2] = HCI2_VAL_MCARD_MODE_DISABLE;
         /* Disable a card protocol */
         PHCIServiceExecuteCommand(
                  pBindingContext,
                  pBindingContext->nCurrentHCIServiceIdentifier,
                  PNALBindingGetOperation(pBindingContext),
                  P_HCI_SERVICE_CMD_SET_PROPERTIES,
                  pHCISendBuffer,
                  0x03,
                  null, 0,
                  static_PNALBindingCardModeDeactivatedCompleted, PNALUtilConvertUint32ToPointer(nNALServiceIdentifier),
                  W_FALSE );
         break;

      case P_NFC_HAL_BINDING_CARD_MODE_ENABLE_STATE:
      {
         pHCISendBuffer[0] = static_PNALBindingGetCardTypeMode(pBindingContext->nCurrentHCIServiceIdentifier);
         pHCISendBuffer[1] = 0x01;
         pHCISendBuffer[2] = HCI2_VAL_MCARD_MODE_ENABLE;
         /* Enable a card protocol */
         PHCIServiceExecuteCommand(
                  pBindingContext,
                  pBindingContext->nCurrentHCIServiceIdentifier,
                  PNALBindingGetOperation(pBindingContext),
                  P_HCI_SERVICE_CMD_SET_PROPERTIES,
                  pHCISendBuffer,
                  0x03,
                  null, 0,
                  static_PNALBindingCardModeActivatedCompleted, PNALUtilConvertUint32ToPointer(nNALServiceIdentifier),
                  W_FALSE );
         break;
      }

      default:
         CNALDebugAssert(W_TRUE);
         break;
   }
}

/**
 * @See definition of tPNALBindingBuildNALResponse
 **/

static void static_PNALBindingBuildNALResponsePersistentPolicy(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 uint8_t  *pNALReceptionBuffer,
                                 uint32_t *nNALReceptionBufferLength,
                                 const uint8_t  *pBuffer,
                                 uint32_t  nLength)
{
   pNALReceptionBuffer[0] = nNALServiceIdentifier;
   pNALReceptionBuffer[1] = NAL_RES_OK;
   CNALMemoryCopy(&pNALReceptionBuffer[2], pBindingContext->aParam_NAL_PAR_PERSISTENT_POLICY, NAL_POLICY_SIZE);
   *nNALReceptionBufferLength = 0x02 + NAL_POLICY_SIZE;
}

/**
 * @See definition of tPNALBindingBuildNALResponse
 **/
static void static_PNALBindingBuildNALResponseHardwareInfo(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 uint8_t  *pNALReceptionBuffer,
                                 uint32_t *nNALReceptionBufferLength,
                                 const uint8_t  *pBuffer,
                                 uint32_t  nLength)
{
   uint32_t nSize;

   pNALReceptionBuffer[0] = nNALServiceIdentifier;
   pNALReceptionBuffer[1] = NAL_RES_OK;

   nSize = PNALProtocolFormat_NAL_PAR_HARDWARE_INFO(pNALReceptionBuffer + 2, &pBindingContext->sNALParHardwareInfo);

   * nNALReceptionBufferLength = nSize + 2;
}

/**
 * @See definition of tPNALBindingBuildNALResponse
 **/
static void static_PNALBindingBuildNALResponseFirmwareInfo(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 uint8_t  *pNALReceptionBuffer,
                                 uint32_t *nNALReceptionBufferLength,
                                 const uint8_t  *pBuffer,
                                 uint32_t  nLength)
{
   uint32_t nSize;

   pNALReceptionBuffer[0] = nNALServiceIdentifier;
   pNALReceptionBuffer[1] = NAL_RES_OK;

   nSize = PNALProtocolFormat_NAL_PAR_FIRMWARE_INFO(pNALReceptionBuffer + 2, &pBindingContext->sNALParFirmwareInfo, (uint16_t) pBindingContext->nAutoStandbyTimeout);

   * nNALReceptionBufferLength = nSize + 2;
}

/**
 * @See definition of tPNALBindingBuildNALResponse
 **/
static void static_PNALBindingBuildNALResponseGetCardList(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 uint8_t  *pNALReceptionBuffer,
                                 uint32_t *nNALReceptionBufferLength,
                                 const uint8_t  *pBuffer,
                                 uint32_t  nLength)
{
   uint32_t nNbCard = (uint32_t)((nLength - 4) / P_NFC_HAL_BINDING_SIZE_ENTRY_TYPE_3);
   uint32_t nIndex = 0;


   pNALReceptionBuffer[0] = nNALServiceIdentifier;
   if ( (nLength < (P_NFC_HAL_BINDING_SIZE_ENTRY_TYPE_3 + 4 ))
   || ( nLength != ((nNbCard * P_NFC_HAL_BINDING_SIZE_ENTRY_TYPE_3) + 4)))
   {
      pNALReceptionBuffer[1] = NAL_RES_UNKNOWN_PARAM;
      *nNALReceptionBufferLength = 0x02;
      return;
   }

   pNALReceptionBuffer[1] = NAL_RES_OK;

   CNALMemoryCopy(pNALReceptionBuffer +2, pBuffer + 4, (nNbCard * P_NFC_HAL_BINDING_SIZE_ENTRY_TYPE_3));

   for(nIndex = 2 + P_NFC_HAL_BINDING_SIZE_ENTRY_TYPE_3 - P_NFC_HAL_BINDING_SIZE_SYSTEM_CODE_TYPE_3 ;
       nIndex < nLength;
       nIndex += P_NFC_HAL_BINDING_SIZE_ENTRY_TYPE_3)
   {
      static_PNALBindingUint16BufferSwap(pNALReceptionBuffer + nIndex);
   }

   *nNALReceptionBufferLength = 2 + (nNbCard * P_NFC_HAL_BINDING_SIZE_ENTRY_TYPE_3);
}

/**
 * @See definition of tPNALBindingBuildNALResponse
 **/
static void static_PNALBindingBuildNALResponsePersistentMemory(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 uint8_t  *pNALReceptionBuffer,
                                 uint32_t *nNALReceptionBufferLength,
                                 const uint8_t *pBuffer,
                                 uint32_t  nLength)
{
   uint32_t nSize;
   pNALReceptionBuffer[0] = nNALServiceIdentifier;

   if((pBuffer == null) || (nLength == 0))
   {
      pNALReceptionBuffer[1] = NAL_RES_UNKNOWN_PARAM;
      *nNALReceptionBufferLength = 0x02;
      return;
   }

   /* Answer has the following format

         P_HCI_SERVICE_ADMINISTRATION,
         HCI2_PAR_ADM_SESSION_ID_SIZE + 2,
         HCI2_PAR_ADM_SESSION_ID
         HCI2_PAR_ADM_SESSION_ID_SIZE,
         <value0> ... <value7>
     */

   if ( (nLength != 4 + HCI2_PAR_ADM_SESSION_ID_SIZE) ||
        (pBuffer[0] != P_HCI_SERVICE_ADMINISTRATION) ||
        (pBuffer[1] != 2 + HCI2_PAR_ADM_SESSION_ID_SIZE) ||
        (pBuffer[2] != HCI2_PAR_ADM_SESSION_ID) ||
        (pBuffer[3] != HCI2_PAR_ADM_SESSION_ID_SIZE))
   {
      pNALReceptionBuffer[1] = NAL_RES_UNKNOWN_PARAM;
      *nNALReceptionBufferLength = 0x02;
      return;
   }

   pNALReceptionBuffer[1] = NAL_RES_OK;

   nSize = PNALProtocolFormat_NAL_PAR_PERSISTENT_MEMORY(pNALReceptionBuffer + 2, pBuffer + 4);

   *nNALReceptionBufferLength = 2 + nSize;
}

/**
 * @See definition of tPNALBindingBuildNALResponse
 **/
static void static_PNALBindingBuildNALResponseUICCSWP(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 uint8_t  *pNALReceptionBuffer,
                                 uint32_t *nNALReceptionBufferLength,
                                 const uint8_t  *pBuffer,
                                 uint32_t  nLength)
{
   uint32_t nStatus;
   uint32_t nSize;
   pNALReceptionBuffer[0] = nNALServiceIdentifier;

   if((pBuffer == null) || (nLength == 0))
   {
      pNALReceptionBuffer[1] = NAL_RES_UNKNOWN_PARAM;
      *nNALReceptionBufferLength = 0x02;
      return;
   }

   /* Answer has the following format

         P_HCI_SERVICE_PIPE_MANAGEMENT,
         3,
         HCI2_PAR_MGT_SWP_SHDLC_STATUS,
         1,
         <value>
     */

   if ( (nLength != 5) ||
        (pBuffer[0] != P_HCI_SERVICE_PIPE_MANAGEMENT) ||
        (pBuffer[1] != 3) ||
        (pBuffer[2] != HCI2_PAR_MGT_SWP_SHDLC_STATUS) ||
        (pBuffer[3] != 1))
   {
      pNALReceptionBuffer[1] = NAL_RES_UNKNOWN_PARAM;
      *nNALReceptionBufferLength = 0x02;
      return;
   }
   pNALReceptionBuffer[1] = NAL_RES_OK;

   switch(pBuffer[4])
   {
      case 0:
         nStatus = NAL_UICC_SWP_DOWN; /* NAL_UICC_SWP_NO_SE is not supported by NFCC */
         break;
      case 1:
         nStatus = NAL_UICC_SWP_BOOTING;
         break;
      case 3:
         nStatus = NAL_UICC_SWP_ACTIVE;
         break;
      default:
         nStatus = NAL_UICC_SWP_ERROR;
         break;
   }

   nSize =  PNALProtocolFormat_NAL_PAR_UICC_SWP(pNALReceptionBuffer + 2, nStatus);

   *nNALReceptionBufferLength = 2 + nSize;
}

/**
 * @See definition of tPNALBindingBuildNALResponse
 **/
static void static_PNALBindingBuildNALResponseUICCReaderProtocols(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 uint8_t  *pNALReceptionBuffer,
                                 uint32_t *nNALReceptionBufferLength,
                                 const uint8_t  *pBuffer,
                                 uint32_t  nLength)
{
   uint16_t nHCIReaderProtocolOpened = 0;
   uint16_t nNALReaderProtocolCapa = 0;
   uint32_t nSize;

   pNALReceptionBuffer[0] = nNALServiceIdentifier;

   if((pBuffer == null) || (nLength == 0))
   {
      pNALReceptionBuffer[1] = NAL_RES_UNKNOWN_PARAM;
      *nNALReceptionBufferLength = 0x02;
      return;
   }

   /* Answer has the following format

         P_HCI_SERVICE_PIPE_MANAGEMENT,
         13,
         HCI2_PAR_MGT_SIM_OWNER_READER
         11,
         <isob4> <nfct1> <isoa4> <15-3> <15-2> <nfct3> <isob3> <bprime> <Isoa3> <p2p_initiator (2 bytes)>
     */

   if ( (nLength != 16) ||
        (pBuffer[0] != P_HCI_SERVICE_PIPE_MANAGEMENT) ||
        (pBuffer[1] != 14) ||
        (pBuffer[2] != HCI2_PAR_MGT_SIM_OWNER_READER) ||
        (pBuffer[3] != 12))
   {
      pNALReceptionBuffer[1] = NAL_RES_UNKNOWN_PARAM;
      *nNALReceptionBufferLength = 0x02;
      return;
   }

   pNALReceptionBuffer[1] = NAL_RES_OK;

   if (pBuffer[4] != HCI2_PIPE_ID_NULL)
   {
      nHCIReaderProtocolOpened |= NAL_PROTOCOL_READER_ISO_14443_4_B;
   }

   if (pBuffer[5] != HCI2_PIPE_ID_NULL)
   {
      nHCIReaderProtocolOpened |= NAL_PROTOCOL_READER_TYPE_1_CHIP;
   }

   if (pBuffer[6] != HCI2_PIPE_ID_NULL)
   {
      nHCIReaderProtocolOpened |= NAL_PROTOCOL_READER_ISO_14443_4_A;
   }

   if (pBuffer[7] != HCI2_PIPE_ID_NULL)
   {
      nHCIReaderProtocolOpened |= NAL_PROTOCOL_READER_ISO_15693_3;
   }

   if (pBuffer[8] != HCI2_PIPE_ID_NULL)
   {
      nHCIReaderProtocolOpened |= NAL_PROTOCOL_READER_ISO_15693_2;
   }

   if (pBuffer[9] != HCI2_PIPE_ID_NULL)
   {
      nHCIReaderProtocolOpened |= NAL_PROTOCOL_READER_FELICA;
   }

   if (pBuffer[10] != HCI2_PIPE_ID_NULL)
   {
      nHCIReaderProtocolOpened |= NAL_PROTOCOL_READER_ISO_14443_3_B;
   }

   if (pBuffer[11] != HCI2_PIPE_ID_NULL)
   {
      nHCIReaderProtocolOpened |= NAL_PROTOCOL_READER_BPRIME;
   }

   if (pBuffer[12] != HCI2_PIPE_ID_NULL)
   {
      nHCIReaderProtocolOpened |= NAL_PROTOCOL_READER_ISO_14443_3_A;
   }

   if ((pBuffer[13] != HCI2_PIPE_ID_NULL) || (pBuffer[14] != HCI2_PIPE_ID_NULL))
   {
      nHCIReaderProtocolOpened |= NAL_PROTOCOL_READER_P2P_INITIATOR;
   }

   static_PNALBindingGetNALReaderProtocolCapabilities(&nNALReaderProtocolCapa, nHCIReaderProtocolOpened);

   nSize = PNALProtocolFormat_NAL_PAR_UICC_READER_PROTOCOLS(
                  pNALReceptionBuffer + 2,
                  pBindingContext->nUICCDetectionFlag,
                  nNALReaderProtocolCapa,
                  0,
                  0,
                  null,
                  0);

   *nNALReceptionBufferLength = 2 + nSize;
}

/**
 * @See definition of tPNALBindingBuildNALResponse
 **/
static void static_PNALBindingBuildNALResponseUICCCardProtocols(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 uint8_t  *pNALReceptionBuffer,
                                 uint32_t *nNALReceptionBufferLength,
                                 const uint8_t  *pBuffer,
                                 uint32_t  nLength)
{
   uint16_t nHCICardProtocolOpened = 0;
   uint16_t nNALCardProtocolCapa = 0;
   uint32_t nSize;

   pNALReceptionBuffer[0] = nNALServiceIdentifier;

   if((pBuffer == null) || (nLength == 0))
   {
      pNALReceptionBuffer[1] = NAL_RES_UNKNOWN_PARAM;
      *nNALReceptionBufferLength = 0x02;
      return;
   }

   /* Answer has the following format

         P_HCI_SERVICE_PIPE_MANAGEMENT,
         HCI2_PAR_MGT_SIM_OWNER_CARD_MSG_SIZE + 2,
         HCI2_PAR_MGT_SIM_OWNER_CARD
         HCI2_PAR_MGT_SIM_OWNER_CARD_MSG_SIZE
         <Iso B> <B prime> <Iso A> <15-3> <15-2> <nfc_t3> <Iso B2> <custom> <p2p_target>
     */

   if ( (nLength != HCI2_PAR_MGT_SIM_OWNER_CARD_MSG_SIZE + 4) ||
        (pBuffer[0] != P_HCI_SERVICE_PIPE_MANAGEMENT) ||
        (pBuffer[1] != HCI2_PAR_MGT_SIM_OWNER_CARD_MSG_SIZE + 2) ||
        (pBuffer[2] != HCI2_PAR_MGT_SIM_OWNER_CARD) ||
        (pBuffer[3] != HCI2_PAR_MGT_SIM_OWNER_CARD_MSG_SIZE))
   {
      pNALReceptionBuffer[1] = NAL_RES_UNKNOWN_PARAM;
      *nNALReceptionBufferLength = 0x02;
      return;
   }

   pNALReceptionBuffer[1] = NAL_RES_OK;

   CNALMemoryFill(pNALReceptionBuffer + 2, 0, 0x46);

   if (pBuffer[4] != HCI2_PIPE_ID_NULL)
   {
      nHCICardProtocolOpened |= NAL_PROTOCOL_CARD_ISO_14443_4_B;
   }

   if (pBuffer[5] != HCI2_PIPE_ID_NULL)
   {
      nHCICardProtocolOpened |= NAL_PROTOCOL_CARD_BPRIME;
   }

   if (pBuffer[6] != HCI2_PIPE_ID_NULL)
   {
      nHCICardProtocolOpened |= NAL_PROTOCOL_CARD_ISO_14443_4_A;
   }

   if (pBuffer[7] != HCI2_PIPE_ID_NULL)
   {
      nHCICardProtocolOpened |= NAL_PROTOCOL_CARD_ISO_15693_3;
   }

   if (pBuffer[8] != HCI2_PIPE_ID_NULL)
   {
      nHCICardProtocolOpened |= NAL_PROTOCOL_CARD_ISO_15693_2;
   }

   if (pBuffer[9] != HCI2_PIPE_ID_NULL)
   {
      nHCICardProtocolOpened |= NAL_PROTOCOL_CARD_FELICA;
   }

   if (pBuffer[12] != HCI2_PIPE_ID_NULL)
   {
      nHCICardProtocolOpened |= NAL_PROTOCOL_CARD_P2P_TARGET;
   }

   static_PNALBindingGetNALCardProtocolCapabilities(&nNALCardProtocolCapa, nHCICardProtocolOpened);

   nSize = PNALProtocolFormat_NAL_PAR_UICC_CARD_PROTOCOLS(
                        pNALReceptionBuffer + 2,         /* pBuffer */
                        nNALCardProtocolCapa,            /* nOpenProtocols */
                        0,                               /* nCardA4Mode */
                        null,                            /* pnUID */
                        0,                               /* nUIDLength */
                        0x00,                            /* nSAK */
                        0x0000,                          /* nATQA */
                        null,                            /* pnApplicationData */
                        0x00,                            /* nFWI_SFGI */
                        0,                               /* nCIDSupport */
                        0,                               /* nDataRateMax */
                        0,                               /* nCardB4Mode */
                        0x00000000,                      /* nPUPI */
                        0x00,                            /* nAFI */
                        0x00000000,                      /* nATQB */
                        null);                           /* pnHigherLayerResponse */

   *nNALReceptionBufferLength = 2 + nSize;
}



/**
 * @See definition of tPNALBindingBuildNALResponse
 **/

static void static_PNALBindingBuildNALResponseRoutingTableConfig(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 uint8_t  *pNALReceptionBuffer,
                                 uint32_t *pnNALReceptionBufferLength,
                                 const uint8_t  *pBuffer,
                                 uint32_t  nLength)
{
   uint32_t nSize;
   bool_t bIsEnabled;

   pNALReceptionBuffer[0] = nNALServiceIdentifier;

   /* Answer has the following format

         P_HCI_SERVICE_PIPE_MCARD,
         3,
         HCI2_PAR_MCARD_GEN_ROUTING_TABLE_ENABLED,
         1,
         <enable>
     */

   if ((nLength == 5) && (pBuffer[0] == P_HCI_SERVICE_MCARD) && (pBuffer[1] == 3) && (pBuffer[2] == HCI2_PAR_MCARD_GEN_ROUTING_TABLE_ENABLED) && (pBuffer[3] == 1))
   {
      pNALReceptionBuffer[1] = NAL_RES_OK;

      bIsEnabled = pBuffer[4]  ? W_TRUE : W_FALSE;

      nSize = PNALProtocolFormat_NAL_PAR_ROUTING_TABLE_CONFIG(pNALReceptionBuffer + 2, bIsEnabled);

      * pnNALReceptionBufferLength = nSize + 2 ;
   }
   else
   {
      pNALReceptionBuffer[1] = NAL_RES_UNKNOWN_PARAM;
      * pnNALReceptionBufferLength = 2;
   }
}


/**
 * @See definition of tPNALBindingBuildNALResponse
 **/
static void static_PNALBindingBuildNALResponseRoutingTableEntries(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 uint8_t  *pNALReceptionBuffer,
                                 uint32_t *pnNALReceptionBufferLength,
                                 const uint8_t  *pBuffer,
                                 uint32_t  nLength)
{
   uint32_t nSize;
   pNALReceptionBuffer[0] = nNALServiceIdentifier;

   /* Answer has the following format

         P_HCI_SERVICE_PIPE_MCARD,
         2 + n,
         HCI2_PAR_MCARD_GEN_ROUTING_TABLE,
         n
         n * <data>
     */

   if ( (nLength >= 4) && (nLength <= 259) &&
        (pBuffer[0] == P_HCI_SERVICE_MCARD)   &&
        (pBuffer[1] == nLength - 2)                  &&
        (pBuffer[2] == HCI2_PAR_MCARD_GEN_ROUTING_TABLE) &&
        (pBuffer[3] == nLength - 4))
   {
      pNALReceptionBuffer[1] = NAL_RES_OK;

      nSize = PNALProtocolFormat_NAL_PAR_ROUTING_TABLE_ENTRIES(pNALReceptionBuffer + 2, & pBuffer[4], (uint8_t) nLength - 4);

      * pnNALReceptionBufferLength = nSize + 2 ;
   }
   else
   {
      pNALReceptionBuffer[1] = NAL_RES_UNKNOWN_PARAM;
      * pnNALReceptionBufferLength = 2;
   }
}


static W_ERROR static_PNALBindingConvertByteToBit(uint32_t nNumberOfByte, uint32_t * nOutNumberOfBit)
{
   uint32_t nNbByteWithoutParityBits;

   if(nNumberOfByte == 1)
   {
      *nOutNumberOfBit = 4;
      return W_SUCCESS;
   }

   if(nNumberOfByte % 9 == 1)
   {
      *nOutNumberOfBit = 0;
      return W_ERROR_RF_COMMUNICATION;
   }

   nNbByteWithoutParityBits =  ((((8 * ((8 * nNumberOfByte) - 7) ) / 9) + 7) / 8);
   *nOutNumberOfBit = nNbByteWithoutParityBits * 9;
   return W_SUCCESS;
}

/**
 * @See definition of tPHCIServiceCommandDataReceived
 **/
NFC_HAL_INTERNAL void PNALBindingUICCEventDataReceived(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t nEventIdentifier,
         uint32_t nOffset,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nHCIMessageReceptionCounter)
{
   PNALDebugTrace("PNALBindingUICCEventDataReceived");
   PNALDebugTraceBuffer(pBuffer, nLength);

   if((nEventIdentifier != HCI2_EVT_CONNECTIVITY)
    &&(nEventIdentifier != HCI2_EVT_TRANSACTION)
    &&(nEventIdentifier != HCI2_EVT_OPERATION_ENDED))
   {
      PNALDebugError("PNALBindingUICCEventDataReceived:  nEventIdentifier(0x%x) error: ", nEventIdentifier);
      return;
   }
   if(nOffset == 0)
   {
      pBindingContext->nHCIReceptionBufferLength = 0;
   }
   if(nOffset + nLength > P_HCI_RECEIVED_FRAME_MAX_LENGTH)
   {
      PNALDebugError("PNALBindingUICCEventDataReceived: HCI Buffer too short");
      pBindingContext->nHCIReceptionBufferLength = 0;
      return;
   }

   if((nLength > 0) &&(pBuffer != null))
   {
      CNALMemoryCopy(
         &pBindingContext->aHCIReceptionBuffer[nOffset],
         pBuffer,
         nLength);

      pBindingContext->nHCIReceptionBufferLength += nLength;
   }

   if(nHCIMessageReceptionCounter == 0)
   {
      return;
   }

   /* Ok, we received the complete event */

   pBindingContext->pNALReceptionBuffer[0] = NAL_SERVICE_UICC;
   pBindingContext->pNALReceptionBuffer[1] = NAL_EVT_UICC_CONNECTIVITY;
   pBindingContext->pNALReceptionBuffer[2] = nEventIdentifier;
   CNALMemoryCopy(& pBindingContext->pNALReceptionBuffer[3], pBindingContext->aHCIReceptionBuffer, pBindingContext->nHCIReceptionBufferLength);

   pBindingContext->nNALReceptionBufferLength = 3 + pBindingContext->nHCIReceptionBufferLength;

   PNALBindingCallReadCallback(pBindingContext, pBindingContext->nNALReceptionBufferLength , nHCIMessageReceptionCounter);
}

/**
 * @See definition of tPHCIServiceCommandDataReceived
 **/
NFC_HAL_INTERNAL void PNALBindingSEEventDataReceived(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t nEventIdentifier,
         uint32_t nOffset,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nHCIMessageReceptionCounter)
{
   W_ERROR nResult;
   uint16_t nNALCardProtocols;
   uint8_t nHCICardProtocols;
   uint32_t nSESwitchPositionCurrent;
   uint32_t nSESlotIdentifierCurrent;

   PNALDebugTrace("PNALBindingSEEventDataReceived");
   PNALDebugTraceBuffer(pBuffer, nLength);

   if (nEventIdentifier != HCI2_EVT_SE_STATUS)
   {
      PNALDebugError("PNALBindingSEEventDataReceived:  nEventIdentifier(0x%x) error", nEventIdentifier);
      return;
   }

   if(nOffset == 0)
   {
      pBindingContext->nHCIReceptionBufferLength = 0;
   }

   if(nOffset + nLength > P_HCI_RECEIVED_FRAME_MAX_LENGTH)
   {
      PNALDebugError("PNALBindingSEEventDataReceived: HCI Buffer too short");
      pBindingContext->nHCIReceptionBufferLength = 0;
      return;
   }

   if((nLength > 0) &&(pBuffer != null))
   {
      CNALMemoryCopy(
         &pBindingContext->aHCIReceptionBuffer[nOffset],
         pBuffer,
         nLength);

      pBindingContext->nHCIReceptionBufferLength += nLength;
   }

   if(nHCIMessageReceptionCounter == 0)
   {
      return;
   }

   if (pBindingContext->nHCIReceptionBufferLength < 3)
   {
      PNALDebugError("Received event too short");
      PNALDebugErrorBuffer(pBindingContext->aHCIReceptionBuffer, pBindingContext->nHCIReceptionBufferLength);
      return;
   }

   nResult = PNALProtocolParse_NAL_PAR_POLICY(pBindingContext->aParam_NAL_PAR_POLICY, NAL_POLICY_SIZE,
                                                      null, null, null, null,
                                                      &nSESwitchPositionCurrent, &nSESlotIdentifierCurrent, null, null);

   if (nResult != NAL_RES_OK)
   {
      PNALDebugError("Unable to parse the current POLICY");
      return;
   }

   if (pBindingContext->aHCIReceptionBuffer[1] == 0x00)
   {
      nHCICardProtocols = pBindingContext->aHCIReceptionBuffer[0];

      static_PNALBindingGetNALCardProtocolCapabilities(&nNALCardProtocols, nHCICardProtocols);

      pBindingContext->pNALReceptionBuffer[0] = NAL_SERVICE_SECURE_ELEMENT;
      pBindingContext->pNALReceptionBuffer[1] = NAL_EVT_SE_CARD_EOT;
      pBindingContext->pNALReceptionBuffer[2] = (uint8_t) nSESlotIdentifierCurrent;
      pBindingContext->nNALReceptionBufferLength = 3;

      pBindingContext->nNALReceptionBufferLength += PNALProtocolFormat_NAL_EVT_SE_CARD_EOT(& pBindingContext->pNALReceptionBuffer[3], nNALCardProtocols, & pBindingContext->aHCIReceptionBuffer[3], pBindingContext->nHCIReceptionBufferLength - 3);

      PNALBindingCallReadCallback(pBindingContext, pBindingContext->nNALReceptionBufferLength , nHCIMessageReceptionCounter);
   }
}

/*Add comm Operation management function*/
NFC_HAL_INTERNAL tHCIServiceOperation *PNALBindingGetOperation(
      tNALBindingContext* pBindingContext)
{
   uint32_t i;

   for (i=0; i < (sizeof(pBindingContext->aHCIServiceOperations) / sizeof(pBindingContext->aHCIServiceOperations[0])); i++)
   {
      if (pBindingContext->aHCIServiceOperations[i].nState == P_HCI_OPERATION_STATE_COMPLETED)
      {
         pBindingContext->aHCIServiceOperations[i].nState = P_HCI_OPERATION_STATE_RESERVED;
         return & pBindingContext->aHCIServiceOperations[i];
      }
      else
      {
         /* PNALDebugWarning("pBindingContext->aHCIServiceOperations[%d].nState %d", i, pBindingContext->aHCIServiceOperations[i].nState); */
      }
   }

   /* here, no operation found */
   CNALDebugAssert(0);

   return (null);
}

/* See header file */
NFC_HAL_INTERNAL tSHDLCFrameInstance* PNALContextGetSHDLCFrame(
            tNALBindingContext* pBindingContext)
{
   return &pBindingContext->sSHDLCFrame;
}

/* See header file */
NFC_HAL_INTERNAL tSHDLCInstance* PNALContextGetSHDLCInstance(
            tNALBindingContext* pBindingContext)
{
   return &pBindingContext->sSHDLCInstance;
}

/* See header file */
NFC_HAL_INTERNAL tHCIFrameInstance* PNALContextGetHCIFrameInstance(
            tNALBindingContext* pBindingContext)
{
   return &pBindingContext->sHCIFrameInstance;
}

/* See header file */
NFC_HAL_INTERNAL tHCIServiceInstance* PNALContextGetHCIServiceInstance(
            tNALBindingContext* pBindingContext)
{
   return &pBindingContext->sHCIServiceInstance;
}

/* See header file */
NFC_HAL_INTERNAL tNALDFCQueue* PNALContextGetDFCQueue(
            tNALBindingContext* pBindingContext)
{
   return &pBindingContext->sDFCQueue;
}

/* See header file */
NFC_HAL_INTERNAL tNALMultiTimerInstance* PNALContextGetMultiTimer(
            tNALBindingContext* pBindingContext)
{
   return &pBindingContext->sNALMultiTimer;
}

#ifdef P_NAL_TRACE_ACTIVE

/* See header file */
NFC_HAL_INTERNAL tPNALSHDLCTraceContext* PNALContextGetSHDLCTraceContext(
            tNALBindingContext* pBindingContext)
{
   return &pBindingContext->sSHDLCTraceContext;
}

#endif /* P_NAL_TRACE_ACTIVE */

