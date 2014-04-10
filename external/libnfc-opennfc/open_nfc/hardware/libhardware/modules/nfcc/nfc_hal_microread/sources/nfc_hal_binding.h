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

#ifndef __NFC_HAL_BINDING_H
#define __NFC_HAL_BINDING_H

#include "nal_porting_os.h"

#include "nfc_hal.h"

#define  P_OPEN_NFC_CONSTANTS_ONLY /* Includes only the constants */
#include "open_nfc.h"
#include "nal_porting_hal.h"

#ifndef NFC_HAL_INTERNAL
#define NFC_HAL_INTERNAL
#endif /* #ifndef NFC_HAL_INTERNAL */

typedef struct __tNALBindingContext tNALBindingContext;

/**
 * Generic callback function for a completion without error.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pCallbakParameter  the blind paramater.
 **/
typedef void tPNALGenericCompletion(
               tNALBindingContext* pBindingContext,
               void* pCallbakParameter );

#include "nfc_hal_dfc.h"
#include "nfc_hal_multi_timer.h"
#include "nfc_hal_hci_protocol.h"
#include "nfc_hal_shdlc_frame.h"
#include "nfc_hal_shdlc.h"
#include "nfc_hal_hci_frame.h"
#include "nfc_hal_hci_service.h"
#include "nfc_hal_util.h"


/******************************************************************************************************************
*                                         Definition
*******************************************************************************************************************/

/* NFC HAL Binding State*/
#define P_NFC_HAL_BINDING_VALID_STATUS                         0x00
#define P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS                   0x01
#define P_NFC_HAL_BINDING_NO_HARDWARE_STATUS                   0x02
#define P_NFC_HAL_BINDING_INIT_STATUS                          0x0F

/* Default NFC capabilities */
#define P_NFC_HAL_BINDING_INPUT_BUFFER_SIZE_DEFAULT            0x08
#define P_NFC_HAL_BINDING_DATA_RATE_MAX_DEFAULT                0x00
#define P_NFC_HAL_BINDING_DEFAULT_AFI_ISOB4                    0x00
#define P_NFC_HAL_BINDING_DEFAULT_AFI_ISO15_3                  0x00
#define P_NFC_HAL_BINDING_DEFAULT_SYSTEM_CODE_FELICA           0xFF
#define P_NFC_HAL_BINDING_HCI_BATTERY_LOW_SUPPORTED            0x01

/* Protocols attributes */
#define P_NFC_HAL_BINDING_PROTOCOL_ATTR_OPEN                   0x80
#define P_NFC_HAL_BINDING_PROTOCOL_ATTR_SUBSCRIBE              0x40
#define P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_EVT                0x20
#define P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_CMD                0x10
#define P_NFC_HAL_BINDING_PROTOCOL_ATTR_CLOSE                  0x08

/* Open NFC interface */
#define P_NFC_HAL_BINDING_INTERFACE                            0x31  /* New interface in microread firmware 7.13 */

/* NFCC boot state machine */
#define P_NFC_HAL_BINDING_BOOT_START                           0

#define P_NFC_HAL_BINDING_PROTOCOL_CAPABILITIES_SIZE    2
#define P_NFC_HAL_BINDING_FIRMWARE_CAPABILITIES_SIZE    2
#define P_NFC_HAL_BINDING_FIRMARE_TRACKING_ID_SIZE      6

#define P_NFC_HAL_BINDING_SE_IDENTIFICATION_SIZE        2
#define P_NFC_HAL_BINDING_SE_PROTOCOL_SIZE              2

#define P_NFC_HAL_BINDING_UID_SIZE_MAX                  10
/* the NFCC page size */
#define P_NFC_HAL_BINDING_NFCC_LOAD_BLOCK_SIZE          0x1A

#define P_NFC_HAL_BINDING_UICC_PROTOCOL_NUMBER          0x02

#define P_NFC_HAL_SE_NONE 0xFFFF
#define P_NFC_HAL_SE_JUPITER 0x6182

/* NFC Controller Error event causes */

#define NAL_EVT_NFCC_ERROR_SPONTANEOUS_RESET      0
#define NAL_EVT_NFCC_ERROR_NAL_PROTOCOL_ERROR     1      /* same as NAL_EVT_NFCC_ERROR_SPONTANEOUS_RESET */
#define NAL_EVT_NFCC_ERROR_HCI_PROTOCOL_ERROR     2

/*******************************************************************************
   Information Section
*******************************************************************************/

#define P_NFC_HAL_VERSION_SIZE  3

#define P_NFC_HAL_HARDWARE_SERIAL_NUMBER_SIZE   8

/**
 * NAL_PAR_FIRMWARE_INFO structure:
 * Do not use directly
 * Use uint8_t type to avoid mis-alignement
 * -------------------------------/
 **/
typedef struct __tNALParFirmwareInfo
{
   uint8_t aFirmwareVersion[P_NFC_HAL_VERSION_SIZE];
   uint8_t aFirmwareTrackingId[P_NFC_HAL_BINDING_FIRMARE_TRACKING_ID_SIZE];
   uint8_t aCardProtocolCapabilities[P_NFC_HAL_BINDING_PROTOCOL_CAPABILITIES_SIZE];
   uint8_t aReaderProtocolCapabilities[P_NFC_HAL_BINDING_PROTOCOL_CAPABILITIES_SIZE];
   uint8_t aFirmwareCapabilities[P_NFC_HAL_BINDING_FIRMWARE_CAPABILITIES_SIZE];
   uint8_t nReaderISOADataRateMax;
   uint8_t nReaderISOAInputBufferSize;
   uint8_t nReaderISOBDataRateMax;
   uint8_t nReaderISOBInputBufferSize;
   uint8_t nCardISOADataRateMax;
   uint8_t nCardISOBDataRateMax;
 }tNALParFirmwareInfo;

/**
 * NAL_PAR_HARDWARE_INFO structure
 * Do not use directly
 * -------------------------------/
 **/
typedef struct __tNALParHardwareInfo
{
   uint8_t aHardwareVersion[P_NFC_HAL_VERSION_SIZE];
   uint8_t aHardwareSerialNumber[P_NFC_HAL_HARDWARE_SERIAL_NUMBER_SIZE];
   uint8_t aLoaderVersion[P_NFC_HAL_VERSION_SIZE];
   uint16_t nSEType;
   uint8_t aSEProtocol[P_NFC_HAL_BINDING_SE_PROTOCOL_SIZE];
   uint8_t nFirmwareStatus;
}tNALParHardwareInfo;

typedef struct __tNALService
{
   uint8_t                      aNALOperationBuffer[P_HCI_RECEIVED_FRAME_MAX_LENGTH];
   uint8_t                      nMessageParam;
}tNALService;

typedef struct sNALBindingConfigOperation tNALBindingConfigOperation;

#define P_NFC_HAL_BINDING_MAX_OPERATIONS                128

#define P_NFC_HAL_BINDING_CONFIG_NULL                    0
#define P_NFC_HAL_BINDING_CONFIG_LOADER_ENTER            1
#define P_NFC_HAL_BINDING_CONFIG_LOADER_EXIT             2
#define P_NFC_HAL_BINDING_CONFIG_SET_PARAMETER           3
#define P_NFC_HAL_BINDING_CONFIG_MODIFY_PARAMETER        4
#define P_NFC_HAL_BINDING_CONFIG_FIRMWARE_UPDATE         5
#define P_NFC_HAL_BINDING_CONFIG_LOADER_UPDATE           6
#define P_NFC_HAL_BINDING_CONFIG_BOOT                    7

struct sNALBindingConfigOperation
{
   uint8_t     nOperation;                /**< Current operation type */
   tNALBindingConfigOperation * pNext;    /**< next operation pointer */

   union {
      /* set operation descriptor */
      struct
      {
         uint8_t   nService;
         uint8_t   nParameter;
         uint32_t  nLength;
         uint8_t * pValue;
      } sSetOperation;

      /* modify operation descriptor */
      struct
      {
         uint8_t   nService;
         uint8_t   nParameter;
         uint32_t  nLength;
         uint8_t * pOrMask;
         uint8_t * pAndMask;
      } sModifyOperation;

      /* firmware update operation descriptor */
      struct
      {
         uint8_t * pBuffer;
         uint32_t  nLength;
      } sFirmwareUpdateOperation;

      /* NFC HAL boot operation descriptor */
      struct
      {
         uint32_t nExpectedResult;
      } sNALBootOperation;
   } Operation;
};

typedef void tNALBindingInternalConnectCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint32_t nResultCode );

/**
 * Returns the SHDLC frame instance.
 *
 * @param[in]  pBindingContext  The NFC HAL Binding context.
 *
 * @return  The SHDLC frame instance.
 **/
NFC_HAL_INTERNAL tSHDLCFrameInstance* PNALContextGetSHDLCFrame(
            tNALBindingContext* pBindingContext);

/**
 * Returns the SHDLC instance.
 *
 * @param[in]  pBindingContext  The NFC HAL Binding context.
 *
 * @return  The SHDLC instance.
 **/
NFC_HAL_INTERNAL tSHDLCInstance* PNALContextGetSHDLCInstance(
            tNALBindingContext* pBindingContext);

/**
 * Returns the HCI frame instance.
 *
 * @param[in]  pBindingContext  The NFC HAL Binding context.
 *
 * @return  The HCI frame instance.
 **/
NFC_HAL_INTERNAL tHCIFrameInstance* PNALContextGetHCIFrameInstance(
            tNALBindingContext* pBindingContext);

/**
 * Returns the HCI service instance.
 *
 * @param[in]  pBindingContext  The NFC HAL Binding context.
 *
 * @return  The HCI service instance.
 **/
NFC_HAL_INTERNAL tHCIServiceInstance* PNALContextGetHCIServiceInstance(
            tNALBindingContext* pBindingContext);

/**
 * Returns the DFC Queue instance.
 *
 * @param[in]  pBindingContext  The NFC HAL Binding context.
 *
 * @return  The DFC queue instance.
 **/
NFC_HAL_INTERNAL tNALDFCQueue* PNALContextGetDFCQueue(
            tNALBindingContext* pBindingContext);

/**
 * Returns the multi-timer instance.
 *
 * @param[in]  pBindingContext  The NFC HAL Binding context.
 *
 * @return  The multi-timer instance.
 **/
NFC_HAL_INTERNAL tNALMultiTimerInstance* PNALContextGetMultiTimer(
            tNALBindingContext* pBindingContext);

#ifdef P_NAL_TRACE_ACTIVE

/* The SHDLC Context for the traces */
typedef struct __tPNALSHDLCTraceContext
{
   char aTraceBuffer[384];
   bool_t bTraceChainedFrame;
   bool_t bFromNFCC;
   uint8_t nLastHeader;
   uint32_t nLastLength;
} tPNALSHDLCTraceContext;

/**
 * Returns the trace context.
 *
 * @param[in]  pBindingContext  The NFC HAL Binding context.
 *
 * @return  The pointer on the context.
 **/
NFC_HAL_INTERNAL tPNALSHDLCTraceContext* PNALContextGetSHDLCTraceContext(
            tNALBindingContext* pBindingContext);

#endif /* P_NAL_TRACE_ACTIVE */

/**
 * Describes a NFC HAL Binding instance
 * Do not use directly
 **/
struct __tNALBindingContext
{
   /* NFC HAL Binding state*/
   uint32_t                    nNALBindingStatus;
   /*loader version*/
   tNALParHardwareInfo         sNALParHardwareInfo;
   /*Firmware version*/
   tNALParFirmwareInfo         sNALParFirmwareInfo;
   /* the Reset State*/
   bool_t                      bIsResetPending;
   /* The reset type */
   uint32_t                    nResetType;
   /* Initial boot */
   bool_t                      bInitialBoot;
   /* Policy */
   uint8_t                     aSetParam_NAL_PAR_POLICY_or_NAL_PAR_PERSISTENT_POLICY[NAL_POLICY_SIZE];

   /* The HCI Reader and Card protocols capabilities*/
   uint16_t                    nHCICardProtocolToOpen;
   uint16_t                    nHCICardProtocolOpened;
   uint16_t                    nHCIReaderProtocolToOpen;
   uint16_t                    nHCIReaderProtocolOpened;
   uint16_t                    nHCIAvailableCardProtocol;
   uint16_t                    nHCIAvailableReaderProtocol;
   uint32_t                    nProtocolCounter;
   /*Reader Sharing*/
   uint8_t                     nReaderSharingMode;
   uint8_t                     nUICCDetectionFlag;
   uint8_t                     aBackupCardFoundBuffer[P_NFC_HAL_BINDING_UICC_PROTOCOL_NUMBER][P_HCI_RECEIVED_FRAME_MAX_LENGTH];
   /* Card mode */
   uint8_t                     aUIDCard[P_NFC_HAL_BINDING_UID_SIZE_MAX];
   uint8_t                     nUIDCardLength;
   /* Card detect pulse */
   uint16_t                    nCardDetectPulse;
   /* HCI buffers*/
   uint8_t                     aHCIResponseBuffer[P_HCI_RECEIVED_FRAME_MAX_LENGTH];
   uint8_t                     aHCISendBuffer[P_HCI_RECEIVED_FRAME_MAX_LENGTH];
   uint8_t                     aHCIReceptionBuffer[1024];
   uint32_t                    nHCIReceptionBufferLength;
   uint8_t*                    pHCISendBuffer;
   /* Connection Callback and parameter*/
   tNALBindingConnectCompleted * pCallbackConnectionFunction;
   tNALBindingInternalConnectCompleted* pInternalCallbackConnectionFunction;
   void                        *pCallbackConnectionParameter;
   /* Read callback and params*/
   uint32_t                     nNALReceptionBufferLength;
   uint32_t                     nNALCardFoundLength;
   uint8_t                      aNALCardFoundBuffer[P_HCI_RECEIVED_FRAME_MAX_LENGTH];

   /* offset of the reception buffer*/
   uint32_t                     nOffset;

   uint32_t                     nReceptionCounter;
   uint32_t                     nFirstCardModeActivatedCounter;

  /* Write callback and params*/
   tNALBindingWriteCompleted*   pNALWriteCallbackFunction;
   void*                        pNALWriteCallbackParameter;
   uint8_t*                     pNALWriteBuffer;
   uint32_t                     nNALWriteBufferLength;
   /*HCI infos*/
   uint8_t                      nCurrentHCIServiceIdentifier;
   uint8_t                      nOneOfOpenedReaderServiceId;

   /*detection sequence*/   
   uint16_t                     nNALCardProtocol;                       /* Contains the requested card protocols in the CMD_DETECTION */
   uint16_t                     nHCITempCardProtocol;
   uint16_t                     nHCILastCardProtocol;

   struct {      
      uint16_t                  nHCIProtocol;
      uint8_t                   nHCIServiceIdentifier;
      uint8_t                   nNALServiceIdentifier;
      uint32_t                  nHCIReceptionCounter;

   } sActivatedCard;

	uint8_t							  nCurrentHCIReaderServiceIdentifier;

   uint16_t                     nNALReaderProtocol;                     /* Contains the requested reader protocols in the CMD_DETECTION */
   uint32_t                     nLastStartSomeReceptionCounter;
   uint8_t                      nHCIPendingReaderServiceIdentifier;
   /*Firmware update*/
   uint8_t                      nUpdateStep;
   uint32_t                     nUpdateState;
   uint32_t                     nNextUpdateState;
   uint8_t                      nPatchStatus;
   uint8_t                      nInitStatus;
   uint8_t*                     pUpdateBuffer;
   uint8_t                      aUpdateBuffer[P_NFC_HAL_BINDING_NFCC_LOAD_BLOCK_SIZE];
   uint32_t                     nUpdateBufferLength;
   bool_t                       bIsFirmwareUpdateStarted;
   uint8_t                      aCheckCrcBuffer[4];
   uint8_t                      aInitConfig[5];
   uint32_t                     nInitConfigLen;

   tNALBindingConfigOperation   aConfigOperations[P_NFC_HAL_BINDING_MAX_OPERATIONS];

  /* Service Operation */
   tHCIServiceOperation         sFirmwareUpdateOperation;

   tNALService                  aNALBindingNALService[NAL_SERVICE_NUMBER];
   tHCIServiceOperation         aHCIServiceOperations[P_HCI_SERVICE_MAX_ID + 1 + P_NBR_MAX_READER_PROTOCOLS + NAL_SERVICE_NUMBER];

   /* standby management */
   uint16_t                     nLastNALReaderProtocol;                    /**< reader protocols currently activated in the NFCC */
   bool_t                         bIsLowPowerRequested;                      /**< low power mode has been requested by user ? */
   uint8_t                      aParam_NAL_PAR_POLICY[NAL_POLICY_SIZE];    /**< volatile policy parameters currently set in the NFCC */
   uint8_t                      aParam_NAL_PAR_PERSISTENT_POLICY[NAL_POLICY_SIZE];    /**< persistent policy parameters currently set in the NFCC */
   uint8_t                      aParam_NAL_PAR_DETECT_PULSE[3 + NAL_PAR_DETECT_PULSE_SIZE];

   tHCIServiceOperation         sNALBindingWakeUpOperation;
   uint32_t                     nCurrentWakeUpState;
   uint32_t                     nNextWakeUpState;

   /* configuration management */
   tNALBindingConfigOperation * pFirstConfigOperation;
   tNALBindingConfigOperation * pLastConfigOperation;

   tNALBindingConfigOperation * pOperation;
   uint32_t                     nConfigProgression;
   uint32_t                     nConfigLength;

   bool_t                         bInLoader;
   bool_t                         bRegisterDone;

   /* The current HCI mode */
   uint32_t                     nHCIMode;

   bool_t                         bInPoll;

   bool_t                         bSendDetectionAnswerOnNextSimRequest;

   /* Card detect management */
   bool_t                         bCardDetectEnabled;

   bool_t                         bLLCPTimeoutSupported;
   /* **************************************************************************
    *  The following fields SHALL be below this line to be preserved
    *  during a reset. DO NOT MOVE THEM.
    * *************************************************************************/
   void* pCallbackContext; /* MUST BE THE FIRST FIELD */

   void* pPortingConfig;

   tNALInstance            * pNALInstance;
   tNALComInstance         * pComPort;
   tNALTimerInstance       * pTimer;

   tNALMultiTimerInstance sNALMultiTimer;

   tNALBindingReadCompleted* pNALReadCallbackFunction;
   void*                     pNALReadCallbackParameter;
   uint8_t*                  pNALReceptionBuffer;
   uint32_t                  nNALReceptionBufferLengthMax;

   tNALBindingTimerHandler*  pNALTimerHandlerFunction;
   tNALBindingAntropySourceHandler* pNALAntropySourceHandlerFunction;

   /* The timeout values for the standby mode */
   uint32_t nAutoStandbyTimeout;
   uint32_t nStandbyTimeout;

   /* The current mode */
   uint32_t nMode;

   tSHDLCFrameInstance sSHDLCFrame;
   tSHDLCInstance sSHDLCInstance;
   tHCIFrameInstance sHCIFrameInstance;
   tHCIServiceInstance sHCIServiceInstance;

   tNALDFCQueue sDFCQueue;

#ifdef P_NAL_TRACE_ACTIVE

   tPNALSHDLCTraceContext sSHDLCTraceContext;

#endif /* P_NAL_TRACE_ACTIVE */

   P_NAL_SYNC_CS           hCriticalSection;

   /* RAW mode management */
   bool_t bRawMode;
   tHCIFrameWriteContext   sRawWriteContext;

   /* Bit mode mangement*/
   bool_t                  bBitMode;
   uint8_t                 nExpectedBitBeforeByte; /* Used only in waiting incomplete byte */
   uint32_t                nHCICmdBufferLength;


#ifdef P_INCLUDE_MIFARE_CLASSIC
   /* Used for mifare card */
   void *                  pMifareContext;
   bool_t                  bMifareExchange;
   uint8_t                 aTmpMifareBuffer[16]; /* 16 is the maximum size we can received from a card */
   uint8_t                 nCurrentMifareTimeout;
#endif
};

typedef struct __tNALBindingProtocolEntry
{
   uint16_t       nHCIProtocolCapability;
   uint8_t        nHCIServiceIdentifier;
   uint8_t        nHCIPipeIdentifier;
   const uint8_t* pHCIProtocolConfig;
   uint32_t       nHCIProtocolConfigLength;
   const uint8_t* pHCIGetParameters;
   uint32_t       nHCIGetParametersLength;
   uint16_t       nNALProtocolCapability;
   uint8_t        nNALServiceIdentifier;
   uint8_t        nProtocolAttributes;
}tNALBindingProtocolEntry;

/* Card Protocol Array */
extern const tNALBindingProtocolEntry g_aNALBindingCardProtocolArray[];
extern const uint32_t                 g_nNALBindingCardProtocolArraySize;

extern const tNALBindingProtocolEntry g_aNALBindingReaderProtocolArray[];
extern const uint32_t                 g_nNALBindingReaderProtocolArraySize;

/*******************************************************************************
*                     Functions declaration
*******************************************************************************/

NFC_HAL_INTERNAL void PNALBindingReaderEventDataReceived(
         tNALBindingContext *pBindingContext,
         void* pCallbackParameter,
         uint8_t nEventIdentifier,
         uint32_t nOffset,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nHCIMessageReceptionCounter);

NFC_HAL_INTERNAL void PNALBindingReaderCommandDataReceived(
         tNALBindingContext *pBindingContext,
         void* pCallbackParameter,
         uint8_t nCommandIdentifier,
         uint32_t nOffset,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nHCIMessageReceptionCounter);

NFC_HAL_INTERNAL void PNALBindingCardEventDataReceived(
         tNALBindingContext *pBindingContext,
         void* pCallbackParameter,
         uint8_t nEventIdentifier,
         uint32_t nOffset,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nHCIMessageReceptionCounter);

NFC_HAL_INTERNAL void PNALBindingUICCEventDataReceived(
         tNALBindingContext *pBindingContext,
         void* pCallbackParameter,
         uint8_t neventIdentifier,
         uint32_t nOffset,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nHCIMessageReceptionCounter);

NFC_HAL_INTERNAL void PNALBindingSEEventDataReceived(
         tNALBindingContext *pBindingContext,
         void* pCallbackParameter,
         uint8_t neventIdentifier,
         uint32_t nOffset,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nHCIMessageReceptionCounter);

NFC_HAL_INTERNAL void PNALBindingChipManagementEventDataReceived(
         tNALBindingContext *pBindingContext,
         void* pCallbackParameter,
         uint8_t neventIdentifier,
         uint32_t nOffset,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nHCIMessageReceptionCounter);

NFC_HAL_INTERNAL void PNALBindingBootMachine(
         tNALBindingContext *pBindingContext,
         uint8_t nState);

NFC_HAL_INTERNAL void PNALBindingUpdateMachine(
         tNALBindingContext *pBindingContext,
         uint32_t  nEvent,
         uint8_t*  pBuffer,
         uint32_t  nLength,
         W_ERROR  nError,
         uint8_t  nStatusCode );

NFC_HAL_INTERNAL void PNALBindingWakeUp(
      tNALBindingContext *pBindingContext,
      void * pCallbackParameter);

NFC_HAL_INTERNAL void PNALBindingWakeUpMachine(
      tNALBindingContext *pBindingContext);

NFC_HAL_INTERNAL void PNALBindingUpdateReceptionCounter(
      tNALBindingContext * pBindingContext,
      uint32_t             nReceptionCounter);

NFC_HAL_INTERNAL void PNALBindingWrite(
      tNALBindingContext * pBindingContext,
      uint8_t* pBuffer,
      uint32_t nLength,
      tNALBindingWriteCompleted* pCallbackFunction,
      void* pCallbackParameter );

NFC_HAL_INTERNAL tHCIServiceOperation* PNALBindingGetOperation(
      tNALBindingContext * pBindingContext);

/**
 * @brief Starts configuration process
 *
 * @param[in] pBindingContext  The context
 *
 * @param[in] pConfigBuffer  The configuration buffer
 *
 * @param[in] nConfigLength The configuration buffer length
 **/
NFC_HAL_INTERNAL void PNALBindingConfigStart(
   tNALBindingContext* pBindingContext,
   uint8_t  * pConfigBuffer,
   uint32_t   nConfigLength);

/*******************************************************************************
*                   static functions to be inlined                             *
*******************************************************************************/

/**
 * @brief  Writes an uint32_t value into a  buffer.
 *
 * @param[out]  pBuffer  The buffer of at least 4 bytes receiving the value.
 *
 * @param[in]   nValue  The value to store.
 **/
P_NAL_INLINE static void static_PNALBindingWriteUint32ToNALBuffer(
                                          uint32_t nValue,
                                          uint8_t* pBuffer)
{
   pBuffer[0] = (uint8_t)((nValue >> 24) & 0xFF);
   pBuffer[1] = (uint8_t)((nValue >> 16) & 0xFF);
   pBuffer[2] = (uint8_t)((nValue >> 8) & 0xFF);
   pBuffer[3] = (uint8_t)(nValue & 0xFF);
}

/**
 * @brief  Reads a uint32_t value from a buffer.
 *
 * @param[out]  pBuffer  The buffer of at least 4 bytes containing the value.
 *
 * @return  The corresponding value.
 **/
P_NAL_INLINE static uint32_t static_PNALBindingReadUint32FromNALBuffer(
                                             const uint8_t* pBuffer)
{
   return (uint32_t)((((uint32_t)pBuffer[0]) << 24) | (((uint32_t)pBuffer[1]) << 16) | (((uint32_t)pBuffer[2]) << 8) | ((uint32_t)pBuffer[3]));
}

/**
 * @brief  Writes an uint16_t value into a  buffer.
 *
 * @param[out]  pBuffer  The buffer of at least 2 bytes receiving the value.
 *
 * @param[in]   nValue  The value to store.
 **/
P_NAL_INLINE static void static_PNALBindingWriteUint16ToNALBuffer(
                                          uint16_t nValue,
                                          uint8_t* pBuffer)
{
   pBuffer[0] = (uint8_t)((nValue >> 8) & 0xFF);
   pBuffer[1] = (uint8_t)(nValue & 0xFF);
}

/**
 * @brief  Reads a uint16_t value from a buffer.
 *
 * @param[out]  pBuffer  The buffer of at least 2 bytes containing the value.
 *
 * @return  The corresponding value.
 **/
P_NAL_INLINE static uint16_t static_PNALBindingReadUint16FromNALBuffer(
                                             const uint8_t* pBuffer)
{
   return (uint16_t)((((uint16_t)pBuffer[0]) << 8) | ((uint16_t)pBuffer[1]));
}

/**
 * @brief  Writes an uint16_t value into a  buffer.
 *
 * @param[out]  pBuffer  The buffer of at least 2 bytes receiving the value.
 *
 * @param[in]   nValue  The value to store.
 **/
P_NAL_INLINE static void static_PNALBindingWriteUint16ToHCIBuffer(
                                          uint16_t nValue,
                                          uint8_t* pBuffer)
{
   pBuffer[1] = (uint8_t)((nValue >> 8) & 0xFF);
   pBuffer[0] = (uint8_t)(nValue & 0xFF);
}

/**
 * @brief  Reads a uint16_t value from a buffer and swap the value.
 *
 * @param[out]  pBuffer  The buffer of at least 2 bytes containing the value.
 *
 * @return  The corresponding value.
 **/
P_NAL_INLINE static uint16_t static_PNALBindingReadUint16FromHCIBuffer(
                                             const uint8_t* pBuffer)
{
   return (uint16_t)((((uint16_t)pBuffer[1]) << 8) | ((uint16_t)pBuffer[0]));
}

/**
 * @brief  swap two bytes of uint8_t buffer.
 *
 * @param[out]  pBuffer  The buffer of at least 2 bytes containing the value.
 *
 * @return  The corresponding value.
 **/
P_NAL_INLINE static  void static_PNALBindingUint16BufferSwap(
                                              uint8_t* pBuffer)
{
   uint8_t nTemp = pBuffer[0];

   pBuffer[0] = pBuffer[1];
   pBuffer[1] = nTemp;
}

/**
 * @brief   Get the NFC HAL Reader Protocol Capabilities and returns the number
 * of available  reader protocols .
 *
 * @param[in]   nHCIReaderProtocolCapa The HCI Reader Protocol Capabilities.
 *
 * @param[out]  pNALReaderProtocolCapa The NFC HAL Reader Protocol Capabilities.
 *
 *@return      The number of available  reader protocols.
 **/
P_NAL_INLINE static uint32_t static_PNALBindingGetNALReaderProtocolCapabilities(
                                       uint16_t *pNALReaderProtocolCapa,
                                       uint16_t nHCIReaderProtocolCapa)
{
    uint16_t nNbrReaderProtocols = 0;
    uint8_t nProtocolIndex;

   *pNALReaderProtocolCapa = 0x0000;
   for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingReaderProtocolArraySize; nProtocolIndex++)
   {
      if((g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIProtocolCapability & nHCIReaderProtocolCapa) != 0)
      {
         *pNALReaderProtocolCapa |=  g_aNALBindingReaderProtocolArray[nProtocolIndex].nNALProtocolCapability;
         nNbrReaderProtocols++;
      }
   }
   return nNbrReaderProtocols;
}

/**
 * @brief   Get the NFC HAL Card Protocol Capabilities and returns the number
 * of available  Card protocols .
 *
 * @param[in]  nHCICardProtocolCapa The HCI Card Protocol Capabilities.
 *
 * @param[out]  pNALCardProtocolCapa The NFC HAL Card Protocol Capabilities.
 *
 * @return    The number of available  Card protocols .
 **/
P_NAL_INLINE static uint32_t static_PNALBindingGetNALCardProtocolCapabilities(
                                       uint16_t *pNALCardProtocolCapa,
                                       uint16_t nHCICardProtocolCapa)
{
   uint16_t nNbrCardProtocols = 0;
   uint8_t nProtocolIndex;

   *pNALCardProtocolCapa = 0x0000;
   for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingCardProtocolArraySize; nProtocolIndex++)
   {
      if((g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIProtocolCapability & nHCICardProtocolCapa) != 0)
      {
         *pNALCardProtocolCapa |=  g_aNALBindingCardProtocolArray[nProtocolIndex].nNALProtocolCapability;
         nNbrCardProtocols++;
      }
   }
   return nNbrCardProtocols;
}

/**
 * @brief   Get the HCI Reader Protocol Capabilities and returns the number
 * of available  reader protocols .
 *
 * @param[out]  pHCIReaderProtocolCapa The pointer to HCI Reader Protocol Capabilities.
 *
 * @param[in]   nNALReaderProtocolCapa The NFC HAL Reader Protocol Capabilities.
 *
 *@return      The number of available  reader protocols.
 **/
P_NAL_INLINE static uint32_t static_PNALBindingGetHCIReaderProtocolCapabilities(
                                       uint16_t *pHCIReaderProtocolCapa,
                                       uint16_t nNALReaderProtocolCapa)
{
    uint16_t nNbrReaderProtocols = 0;
    uint8_t nProtocolIndex;

   *pHCIReaderProtocolCapa = 0x0000;
   for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingReaderProtocolArraySize; nProtocolIndex++)
   {
      if((g_aNALBindingReaderProtocolArray[nProtocolIndex].nNALProtocolCapability & nNALReaderProtocolCapa) != 0)
      {
         *pHCIReaderProtocolCapa |=  g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIProtocolCapability;
         nNbrReaderProtocols++;
      }
   }
   return nNbrReaderProtocols;
}

/**
 * @brief   Get the HCI Card Protocol Capabilities and returns the number
 * of available  Card protocols .
 *
 * @param[out]  pHCICardProtocolCapa The pointer to HCI Card Protocol Capabilities.
 *
 * @param[out]  nNALCardProtocolCapa The NFC HAL Card Protocol Capabilities.
 *
 * @return    The number of card protocols .
 **/
P_NAL_INLINE static uint32_t static_PNALBindingGetHCICardProtocolCapabilities(
                                       uint16_t *pHCICardProtocolCapa,
                                       uint16_t nNALCardProtocolCapa)
{
   uint16_t nNbrCardProtocols = 0;
   uint8_t nProtocolIndex;

   *pHCICardProtocolCapa = 0x0000;
   for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingCardProtocolArraySize; nProtocolIndex++)
   {
      if((g_aNALBindingCardProtocolArray[nProtocolIndex].nNALProtocolCapability & nNALCardProtocolCapa) != 0)
      {
         *pHCICardProtocolCapa |=  g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIProtocolCapability;
         nNbrCardProtocols++;
      }
   }
   return nNbrCardProtocols;
}

/**
 * @brief   returns the HCI Reader Protocol.
 *
 * @param[in]  nHCIPipe The HCI Reader Pipe ID Value.
 *
 *@return  The HCI Reader Protocol
 **/
P_NAL_INLINE static uint16_t static_PNALBindingGetHCIReaderProtocolFromPipeID(
                                                             uint8_t nHCIPipe)
{
   uint8_t nProtocolIndex;

   for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingReaderProtocolArraySize; nProtocolIndex++)
   {
      if(g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIPipeIdentifier == nHCIPipe)
      {
         return  g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIProtocolCapability;
      }
   }
   return 0;
}

/**
 * @brief   returns the HCI Card Protocol.
 *
 * @param[in]  nHCIPipe The HCI card Pipe ID Value.
 *
 *@return  The HCI Card Protocol
 **/
P_NAL_INLINE static uint16_t static_PNALBindingGetHCICardProtocolFromPipeID(
                                                               uint8_t nHCIPipe)
{
   uint8_t nProtocolIndex;

   for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingCardProtocolArraySize; nProtocolIndex++)
   {
      if(g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIPipeIdentifier == nHCIPipe)
      {
         return g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIProtocolCapability;
      }
  }
  return 0;
}

/**
 * @brief   returns the HCI card Service Protocol.
 *
 * @param[in]  nHCIServiceCardIdentifier The HCI card Service Identifier.
 *
 * @return  The HCI card Service Protocol.
 **/
P_NAL_INLINE static uint16_t static_PNALBindingGetHCICardProtocolFromServiceID(
                        uint8_t nHCIServiceCardIdentifier)
{
   uint8_t nProtocolIndex;
   for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingCardProtocolArraySize; nProtocolIndex++)
     {
        if(g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIServiceIdentifier == nHCIServiceCardIdentifier)
        {
           return g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIProtocolCapability;
        }
     }
     return 0;
}

/**
 * @brief   returns the NFC HAL Card Service Protocol.
 *
 * @param[in]  nNALServiceCardIdentifier The NFC HAL Card Service Identifier.
 *
 * @return  The NFC HAL Card Service Protocol.
 **/
P_NAL_INLINE static uint16_t static_PNALBindingGetNALCardProtocolFromServiceID(
                        uint8_t nNALServiceCardIdentifier)
{
   uint8_t nProtocolIndex;
   for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingCardProtocolArraySize; nProtocolIndex++)
     {
        if(g_aNALBindingCardProtocolArray[nProtocolIndex].nNALServiceIdentifier == nNALServiceCardIdentifier)
        {
           return g_aNALBindingCardProtocolArray[nProtocolIndex].nNALProtocolCapability;
        }
     }
     return 0;
}

/**
 * Returns the NFC HAL Reader Service Protocol.
 *
 * @param[in]  nNALServiceReaderIdentifier The NFC HAL Reader Service Identifier.
 *
 * @return  The NFC HAL Reader Service Protocol.
 **/
P_NAL_INLINE static uint16_t static_PNALBindingGetNALReaderProtocolFromServiceID(
                        uint8_t nNALServiceReaderIdentifier)
{
   uint8_t nProtocolIndex;
   for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingReaderProtocolArraySize; nProtocolIndex++)
     {
        if(g_aNALBindingReaderProtocolArray[nProtocolIndex].nNALServiceIdentifier == nNALServiceReaderIdentifier)
        {
           return g_aNALBindingReaderProtocolArray[nProtocolIndex].nNALProtocolCapability;
        }
     }
     return 0;
}

#define static_PNALBindingUpdateMachineDefault(pBindingContext, nEvent)     \
   PNALBindingUpdateMachine(pBindingContext, nEvent, null, 0, W_SUCCESS, ETSI_ERR_ANY_OK);

/**
 * @brief   Build the NFC HCI Command corresponding to the NAL_PAR_POLICY/NAL_PAR_PERSISTENT_POLICY parameters
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
 *
 * @param[in]  bPersistent Build the conmmand to configure the persistent/current policy
 *
 * @param[in]  bFromPersistent Use the persistent/current policy as a reference
 **/

uint8_t PNALBindingBuildHCICommandGenericPolicy(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 const uint8_t  *pNALReceptionBuffer,
                                 uint32_t  nNALReceptionBufferLength,
                                 uint8_t  *pBuffer,
                                 uint32_t *pnLength,
                                 bool_t       bPersistent,
                                 bool_t       bFromPersistent);

/**
 * @brief   Build the NFC HCI Command corresponding to the NAL_PAR_DETECT_PULSE parameter
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

uint8_t PNALBindingBuildHCICommandPulse(
                                 tNALBindingContext * pBindingContext,
                                 uint8_t   nNALServiceIdentifier,
                                 const uint8_t  *pNALReceptionBuffer,
                                 uint32_t  nNALReceptionBufferLength,
                                 uint8_t  *pBuffer,
                                 uint32_t *pnLength);

void PNALBindingCallReadCallback(
            tNALBindingContext * pBindingContext,
            uint32_t nReadLength,
            uint32_t nReceptionCounter);

#include "nfc_hal_protocol.h"

#endif   /* __NFC_HAL_BINDING_H */
