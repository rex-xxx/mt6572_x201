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
   Contains the declaration of the functions for the NFC Controller
*******************************************************************************/

#ifndef __WME_NFCC_H
#define __WME_NFCC_H


/* Define a mask with all the card protocols */
#define W_NFCC_PROTOCOL_CARD_ALL (\
      W_NFCC_PROTOCOL_CARD_ISO_14443_4_A | \
      W_NFCC_PROTOCOL_CARD_ISO_14443_4_B | \
      W_NFCC_PROTOCOL_CARD_ISO_14443_3_A | \
      W_NFCC_PROTOCOL_CARD_ISO_14443_3_B | \
      W_NFCC_PROTOCOL_CARD_ISO_15693_3 | \
      W_NFCC_PROTOCOL_CARD_ISO_15693_2 | \
      W_NFCC_PROTOCOL_CARD_FELICA | \
      W_NFCC_PROTOCOL_CARD_P2P_TARGET | \
      W_NFCC_PROTOCOL_CARD_TYPE_1_CHIP | \
      W_NFCC_PROTOCOL_CARD_MIFARE_CLASSIC | \
      W_NFCC_PROTOCOL_CARD_BPRIME |\
      W_NFCC_PROTOCOL_CARD_KOVIO |\
      W_NFCC_PROTOCOL_CARD_MIFARE_PLUS)

/* Define a mask with all the reader protocols */
#define W_NFCC_PROTOCOL_READER_ALL (\
      W_NFCC_PROTOCOL_READER_ISO_14443_4_A | \
      W_NFCC_PROTOCOL_READER_ISO_14443_4_B | \
      W_NFCC_PROTOCOL_READER_ISO_14443_3_A | \
      W_NFCC_PROTOCOL_READER_ISO_14443_3_B | \
      W_NFCC_PROTOCOL_READER_ISO_15693_3 | \
      W_NFCC_PROTOCOL_READER_ISO_15693_2 | \
      W_NFCC_PROTOCOL_READER_FELICA | \
      W_NFCC_PROTOCOL_READER_P2P_INITIATOR | \
      W_NFCC_PROTOCOL_READER_TYPE_1_CHIP | \
      W_NFCC_PROTOCOL_READER_MIFARE_CLASSIC | \
      W_NFCC_PROTOCOL_READER_BPRIME |\
      W_NFCC_PROTOCOL_READER_KOVIO |\
      W_NFCC_PROTOCOL_READER_MIFARE_PLUS)

typedef struct __tNFCControllerSeInfo
{
   uint8_t aDescription[NAL_SE_DESCRIPTION_STRING_SIZE];
   uint32_t nCapabilities;
   uint32_t nSwpTimeout;
   uint32_t nProtocols;
   uint32_t nHalSlotIdentifier;
} tNFCControllerSeInfo;

typedef struct __tNFCControllerInfo
{
   uint32_t nProtocolCapabilities;
   uint8_t aHardwareVersion[NAL_HARDWARE_TYPE_STRING_SIZE];
   uint8_t aHardwareSerialNumber[NAL_HARDWARE_SERIAL_NUMBER_STRING_SIZE];
   uint8_t aLoaderVersion[NAL_LOADER_DESCRIPTION_STRING_SIZE];
   uint8_t aFirmwareVersion[NAL_FIRMWARE_DESCRIPTION_STRING_SIZE];
   uint8_t nNALVersion;

   /* The total number of SE */
   uint32_t nSeNumber;
   tNFCControllerSeInfo aSEInfoArray[P_SE_HAL_MAXIMUM_SE_NUMBER];

   uint32_t nFirmwareCapabilities;

   uint32_t nReaderISO14443_A_MaxRate;
   uint32_t nReaderISO14443_A_InputSize;
   uint32_t nReaderISO14443_B_MaxRate;
   uint32_t nReaderISO14443_B_InputSize;
   uint32_t nCardISO14443_A_MaxRate;
   uint32_t nCardISO14443_B_MaxRate;
   uint32_t nAutoStandbyTimeout;

} tNFCControllerInfo;

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* The global policy parameters */
typedef struct __tNFCControllerSEPolicy
{
   uint32_t nSESwitchPosition;
   uint32_t nSlotIdentifier;
   uint32_t nSEProtocolPolicy;

} tNFCControllerSEPolicy;

/* The global policy parameters */
typedef struct __tNFCControllerPolicyParameters
{
   bool_t bReaderRFLock;
   bool_t bCardRFLock;

   uint32_t nUICCProtocolPolicy;

   tNFCControllerSEPolicy sSEPolicy;

} tNFCControllerPolicyParameters;

/* The operation codes */
#define P_NFCC_OPERATION_SET_RF_LOCK_VOLATILE         0x01
#define P_NFCC_OPERATION_SET_RF_LOCK_PERSISTENT       0x02
#define P_NFCC_OPERATION_SET_UICC_ACCESS_VOLATILE     0x03
#define P_NFCC_OPERATION_SET_UICC_ACCESS_PERSISTENT   0x04
#define P_NFCC_OPERATION_SET_UICC_READER_PERSISTENT   0x05
#define P_NFCC_OPERATION_SET_SE_SWITCH_VOLATILE       0x06
#define P_NFCC_OPERATION_SET_SE_SWITCH_PERSISTENT     0x07

/* The maximum number of pending operations */
#define P_NFCC_MAX_NUMBER_PENDING_OPERATIONS          0x07

/* The NOP flag for the intermediate operations without callback function */
#define P_NFCC_OPERATION_NOP                          0x80

/* The Policy monitor for the persistent and hte volatile policy */
typedef struct __tNFCControllerPolicyMonitor
{
   uint8_t aPendingOperations[P_NFCC_MAX_NUMBER_PENDING_OPERATIONS];
   uint32_t nPendingOperations;

   tNFCControllerPolicyParameters sPersistent;
   tNFCControllerPolicyParameters sNewPersistent;
   tNFCControllerPolicyParameters sVolatile;
   tNFCControllerPolicyParameters sNewVolatile;

   /* Is there already an operation in progress ? */
   bool_t bOperationInProgress;

   /* Set RF Lock operations */
   tDFCDriverCCReference pSetVolatileRFLockDriverCC;
   tNALServiceOperation sSetVolatileRFLockOperation;
   uint8_t aSetVolatileRFLockOperationBuffer[NAL_POLICY_SIZE];

   tDFCDriverCCReference pSetPersistentRFLockDriverCC;
   tNALServiceOperation sSetPersistentRFLockOperation;
   uint8_t aSetPersistentRFLockOperationBuffer[NAL_POLICY_SIZE];

   /* Set UICC Policy operations */
   tDFCCallbackContext sSetVolatileUICCPolicyCC;
   tNALServiceOperation sSetVolatileUICCPolicyOperation;
   uint8_t aSetVolatileUICCPolicyOperationBuffer[NAL_POLICY_SIZE];

   tDFCCallbackContext sSetPersistentUICCPolicyCC;
   tNALServiceOperation sSetPersistentUICCPolicyOperation;
   uint8_t aSetPersistentUICCPolicyOperationBuffer[NAL_PERSISTENT_MEMORY_SIZE];
   /* NAL_PERSISTENT_MEMORY_SIZE bytes for the policy parameter and the persistent storage */

   /* Set SE Switch Position operations */
   tDFCCallbackContext sSetVolatileSESwitchCC;
   tNALServiceOperation sSetVolatileSESwitchOperation;
   uint8_t aSetVolatileSESwitchOperationBuffer[NAL_POLICY_SIZE];

   tDFCCallbackContext sSetPersistentSESwitchCC;
   tNALServiceOperation sSetPersistentSESwitchOperation;
   uint8_t aSetPersistentSESwitchOperationBuffer[NAL_POLICY_SIZE];

} tNFCControllerPolicyMonitor;

/* The exception monitor */
typedef struct __tNFCControllerExceptionMonitor
{
   tHandleObjectHeader sObjectHeader;
   bool_t bRegistered;
   tDFCDriverCCReference pDriverCC;

   tNALServiceOperation sServiceOperation;

} tNFCControllerExceptionMonitor;

/* The RF field monitor */
typedef struct __tNFCControllerRFFieldMonitor
{
   tHandleObjectHeader sObjectHeader;
   bool_t bRegistered;
   tDFCDriverCCReference pDriverCC;

   tNALServiceOperation sServiceOperation;

   uint8_t aEventMessageBuffer[5];

} tNFCControllerRFFieldMonitor;


/* The test monitor */
typedef struct __tNFCControllerTestMonitor
{
   tDFCDriverCCReference pDriverCC;
   tNALServiceOperation sServiceOperation;

   bool_t bOperationPending;

   uint8_t aCommandAndResponseBuffer[5];

} tNFCControllerTestMonitor;

/** The type of the completion callback function */
typedef void tPNFCControllerInitialResetCompletionCallback(
               void* pPortingConfig,
               uint32_t nMode);

/* The NFC Controller structure */
typedef struct __tNFCController
{
   /* The policy monitor */
   tNFCControllerPolicyMonitor sPolicyMonitor;

   /* The exception monitor */
   tNFCControllerExceptionMonitor sExceptionMonitor;

   /* RF field monitor */
   tNFCControllerRFFieldMonitor sRFFieldMonitor;

   /* The test monitor */
   tNFCControllerTestMonitor sTestMonitor;

   /* The current mode */
   /* The current mode is stored in the NFC HAL */

   /* The target mode */
   uint32_t nTargetMode;

   /* The service operation */
   tNALServiceOperation sBootServiceOperation;

   /* The service operation */
   tNALServiceOperation sBootServiceOperation2;

   /* The standby operation */
   tNALServiceOperation sStandbyOperation;

   /* The current standby value */
   bool_t bCurrentStandbyOn;

   /* The pending standby value */
   bool_t bPendingStandby;

   /* The standby operation progress */
   bool_t bStandbyOperationInProgress;

   /* The current state */
   uint32_t nState;

   /* The number of reset already done */
   uint32_t nResetCount;

   /* The buffer containing the new firmware */
   uint8_t* pUpdateBuffer;

   /* The length of the firmware buffer */
   uint32_t nUpdateBufferLength;

   /* The current index for the firmware update */
   uint32_t nUpdateBufferIndex;

   /* Callback for the boot procedure */
   tDFCDriverCCReference pBootDriverCC;

   /* The udpate error */
   W_ERROR nUpdateError;

   /* Flag indicating if the state machine is in the initial reset */
   bool_t bInitialReset;

   /* Flag indicating if the NFCC is ready for the SE & UICC boot during the boot procedure */
   bool_t bBootedForSEandUICC;

   /* flag used to force the reset of the persistent data */
   bool_t bForceReset;

   /* The NFC HAL data buffer */
   uint8_t aNALDataBuffer[NAL_MESSAGE_MAX_LENGTH];

   /* The completion callback function and parameter */
   tPNFCControllerInitialResetCompletionCallback* pResetCallback;
   void* pCompletionCallbackParameter;

   /* The raw mode parameters */
   uint32_t nRawMode;
   bool_t bRawListenerRegistered;
   tDFCDriverCCReference pRawOperationDriverCC;
   tDFCDriverCCReference pListenerDriverCC;
   tNALServiceOperation sRawModeOperation;
   tNALServiceOperation sRawMessageOperation;
   void* pMessageQueue;
   uint32_t nNextIndexToEnqueue;
   uint32_t nNextIndexToDequeue;

   bool_t bPulsePeriodSupported;

} tNFCController;

/** Creates the NFC Controller instance */
void PNFCControllerCreate(
         tNFCController* pNFCController );

/** Destroyes the NFC Controller instance */
void PNFCControllerDestroy(
         tNFCController* pNFCController );

/** Performs the initial reset of the NFC Controller */
void PNFCControllerPerformInitialReset(
               tContext* pContext,
               tPNFCControllerInitialResetCompletionCallback* pCompletionCallback,
               void* pCompletionCallbackParameter,
               bool_t bForceReset );

/**
 * Returns the SWP access rights.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nStorageType  The storage type.
 *
 * @param[out] pnSwpPolicyProtocols  A variable valued with the bit field
 *             representing the SWP access rights.
 *
 * @return  W_SUCCESS or W_ERROR_BAD_STATE if the information is not available.
 **/
W_ERROR PNFCControllerGetSwpAccessPolicy(
            tContext* pContext,
            uint32_t nStorageType,
            uint32_t* pnSwpPolicyProtocols);

/**
 * Sets the SWP access rights.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nStorageType  The stroage type.
 *
 * @param[in]  nPolicyProtocols  The bit-field representing the SWP access rights.
 *
 * @param[in]  pCallback  The callback function receiving the result.
 *
 * @param[in]  pCallbackParameter  The blind parameter for the callback function.
 **/
void PNFCControllerSetSwpAccessPolicy(
            tContext* pContext,
            uint32_t nStorageType,
            uint32_t nPolicyProtocols,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter);

/**
 * Checks the Device card emulation policy.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nProtocol  The Device card emulation protocol to check.
 *
 * @return  The error code.
 **/
W_ERROR PNFCControllerCheckCardEmulPolicy(
            tContext* pContext,
            uint32_t nProtocol);

/**
 * Checks the persistent policy.
 *
 * @param[in]  pContext  The current context.
 *
 * @return  The error code.
 **/
W_ERROR PNFCControllerCheckPersistentPolicy(
            tContext* pContext);

/**
 * Returns the Secure Element number.
 *
 * @param[in]  pContext  The current context.
 *
 * @return  The secure element number.
 **/
uint32_t PNFCControllerGetSecureElementNumber(
            tContext* pContext);

/**
 * Returns the Secure Element hardware information.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nSlotIdentifier  The zero-based Secure Element slot identifier.
 *
 * @param[out] pDescription  A pointer on a variable array to fill with the
 *             zero-ended name of the Secure Element.
 *
 * @param[out] pnCapabilities  A variable valued with the capabilities of the Secure Element.
 *
 * @param[out) pnProtocols  A protocol bit field representing the Secure Element protocols.
 *
 * @param[out] pnHalSlotIdentifier  The SE HAL identifier.
 *
 * @return  W_SUCCESS or W_ERROR_BAD_STATE if the information is not available.
 **/
W_ERROR PNFCControllerGetSecureElementHardwareInfo(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            char16_t* pDescription,
            uint32_t* pnCapabilities,
            uint32_t* pnProtocols,
            uint32_t* pnHalSlotIdentifier);

/**
 * Returns the SE switch positions.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[out] pPersistentPolicy  A structure valued with the persistent SE policy.
 *
 * @param[out) pVolatilePolicy  A structure valued with the volatile SE policy.
 *
 * @param[out] pNewPersistentPolicy  A structure valued with the new persistent SE policy.
 *
 * @param[out) pNewVolatilePolicy  A structure valued with the new volatile SE policy.
 *
 * @return  W_SUCCESS or W_ERROR_BAD_STATE if the information is not available.
 **/
W_ERROR PNFCControllerGetSESwitchPosition(
            tContext* pContext,
            tNFCControllerSEPolicy* pPersistentPolicy,
            tNFCControllerSEPolicy* pVolatilePolicy,
            tNFCControllerSEPolicy* pNewPersistentPolicy,
            tNFCControllerSEPolicy* pNewVolatilePolicy);

/**
 * Checks the new SE policy.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nStorageType  The storage type.
 *
 * @param[in]  pSEPolicy  The new SE policy to set.
 **/
W_ERROR PNFCControllerCheckSESwitchPosition(
            tContext* pContext,
            uint32_t nStorageType,
            const tNFCControllerSEPolicy* pSEPolicy);

/**
 * Sets the SE switch position.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nStorageType  The storage type.
 *
 * @param[in]  pSEPolicy  The SE policy to set.
 *
 * @param[in]  pCallback  The callback function receiving the result.
 *
 * @param[in]  pCallbackParameter  The blind parameter for the callback function.
 **/
void PNFCControllerSetSESwitchPosition(
            tContext* pContext,
            uint32_t nStorageType,
            const tNFCControllerSEPolicy* pSEPolicy,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter);

/**
 * Notifies a NFC Controller exception if a listenner is registered for shuch event.
 *
 * Does nothing if no listenner is registered.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nCause  The cause of the exception.
 **/
void PNFCControllerNotifyException(
            tContext* pContext,
            uint32_t nCause);

/**
 * Calls the NFC field monitor
 *
 * Does nothing if no listenner is registered.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nEvent    The field event
 **/

void PNFCControllerCallMonitorFieldCallback(
            tContext* pContext,
            uint32_t nEvent);

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#if (P_BUILD_CONFIG == P_CONFIG_USER)

W_ERROR PNFCControllerUserReadInfo(
            tContext* pContext);

#endif /* P_CONFIG_USER */

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* See header file */
W_ERROR PNFCControllerInternalGetProperty(
         tContext * pContext,
         uint8_t nPropertyIdentifier,
         char16_t* pValueBuffer,
         uint32_t nBufferLength,
         uint32_t* pnValueLength );

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#endif /* #ifdef __WME_NFCC_H */
