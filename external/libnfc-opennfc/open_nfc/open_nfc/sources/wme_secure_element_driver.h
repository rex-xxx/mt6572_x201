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

#ifndef __WME_SECURE_ELEMENT_DRIVER_H
#define __WME_SECURE_ELEMENT_DRIVER_H

/* The maximum length of the SE detection data */
#define P_SE_DETECTION_BUFFER_LENGTH      50

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/*******************************************************************************
   Contains the declaration of the secure element functions
*******************************************************************************/

/* Internal flags */
#ifdef P_INCLUDE_SE_SECURITY
#define W_SE_FLAG_SE_HAL                           0x0100
#endif /* P_INCLUDE_SE_SECURITY */
#define W_SE_FLAG_COMMUNICATION_VIA_RF             0x0200
#define W_SE_FLAG_COMMUNICATION_VIA_SWP            0x0400
#define W_SE_FLAG_STK_REFRESH_SUPPORT              0x0800

/* SE switch position values */
#define P_SE_SWITCH_OFF                      0
#define P_SE_SWITCH_RF_INTERFACE             1
#define P_SE_SWITCH_FORCED_HOST_INTERFACE    2
#define P_SE_SWITCH_HOST_INTERFACE           3

/* The maximum length of the AID list in bytes */
#define P_SE_MAX_AID_LIST_LENGTH   280

/*******************************************************************************
   The STK REFRESH command codes from ETSI 10 134 ETSI TS 102 223 V10.4.0 (2011-06)
*******************************************************************************/

/* NAA Initialization and Full File Change Notification */
#define P_SE_STK_REFRESH_CODE_NAA_INIT_FULL_FILE_CHANGE_NOTIFICATION    0x00
/* File Change Notification */
#define P_SE_STK_REFRESH_CODE_FILE_CHANGE_NOTIFICATION                  0x01
/* NAA Initialization and File Change Notification */
#define P_SE_STK_REFRESH_CODE_NAA_INIT_FILE_CHANGE_NOTIFICATION         0x02
/* NAA Initialization */
#define P_SE_STK_REFRESH_CODE_NAA_INIT                                  0x03
/* UICC Reset */
#define P_SE_STK_REFRESH_CODE_UICC_RESET                                0x04
/* NAA Application Reset, only applicable for a 3G platform */
#define P_SE_STK_REFRESH_CODE_NAA_APP_RESET                             0x05
/* NAA Session Reset, only applicable for a 3G platform */
#define P_SE_STK_REFRESH_CODE_NAA_SESSION_RESET                         0x06
/* Reserved by 3GPP ("Steering of Roaming" REFRESH support) */
#define P_SE_STK_REFRESH_CODE_ROAMING                                   0x07
/* Reserved by 3GPP (Steering of Roaming for I-WLAN) */
#define P_SE_STK_REFRESH_CODE_ROAMING_I_WLAN                            0x08

/**
 * @brief Type of the function performing an operation on an applet of the Secure Element.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pInstance  The operation instance.
 *
 * @param[in]  pExchangeApduFunction  The exchange APDU function.
 *
 * @param[in]  hConnection  The secure element connection.
 *
 * @param[in]  pCardToReaderBuffer  The data received after the select AID APDU.
 *
 * @param[in]  nCardToReaderDataLength  The length in byte of the data.
 *
 * @param[in]  pCallback  A pointer on the callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter transmitted to the callback function.
 **/
typedef void tPSEPerformAppletOperation(
            tContext* pContext,
            void* pInstance,
            tPBasicExchangeData* pExchangeApduFunction,
            W_HANDLE hConnection,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderDataLength,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter );

/* The current operation variables */
typedef struct __tSEOperation
{
   /** The channel reference of the opened logical channel */
   uint32_t nChannelReference;
   /** The AID of the application to be selected */
   const uint8_t* pAidBuffer;
   /** The length of the AID */
   uint32_t nAidLength;
   /** The buffer containing the APDU command sent to the SE */
   uint8_t aReaderToCardBuffer[(255+5)+2]; /* 2 padding bytes */
   /** The buffer containing the APDU response returned by the SE */
   uint8_t aCardToReaderBuffer[(256+2)+2]; /* 2 padding bytes */

   tPSEPerformAppletOperation* pFunction;
   void* pInstance;

   tDFCCallbackContext sOperationCallback;

   /** Whether the connection should be closed after the current operation completion */
   bool_t bCloseConnection;

   W_ERROR nCurrentError;
} tSEOperation;

/* The WSEMonitorEndOfTransaction operation */
typedef struct __tSEMonitorEndOfTransaction
{
   tHandleObjectHeader sObjectHeaderRegistry;
   tDFCDriverCCReference pHandlerDriverCC;
   uint32_t nCardProtocols;
   tUserInstance* pUserInstance;

   /* The WSEGetTransactionAID operation */
   uint32_t nLastAidListLength;
   uint8_t aLastAidList[P_SE_MAX_AID_LIST_LENGTH];

} tSEMonitorEndOfTransaction;

/*
 * The maximum length in bytes of the connectivity data,
 * as defined in the ETSI specification.
 */
#define P_SE_MAX_CONNECTIVITY_DATA_LENGTH  255

/* The connectivity operation */
typedef struct __tSEConnectivity
{
   /* The SEMonitorConnectivityEvent() operation */
   tHandleObjectHeader sObjectHeaderRegistry;
   tDFCDriverCCReference pMonitorConnectivityDriverCC;
   tNALServiceOperation sMonitorConnectivityOperation;
   uint32_t nDataLength;
   uint8_t aDataBuffer[P_SE_MAX_CONNECTIVITY_DATA_LENGTH];

} tSEConnectivity;

/* The WSEMonitorHotPlugEvents operation */
typedef struct __tSEMonitorHotPlugEvents
{
   tHandleObjectHeader sObjectHeaderRegistry;
   tDFCDriverCCReference pHandlerDriverCC;

} tSEMonitorHotPlugEvents;

/* The Initialization Operations */
typedef struct __tSEInitialization
{
   W_ERROR nLastError;

   uint32_t nPersistentPolicy;
   uint32_t nVolatilePolicy;

} tSEInitialization;

/* The get status operation */
typedef struct __tSEGetStatus
{
   tDFCDriverCCReference pCallbackDriverCC;

   /* The ATR buffer */
   uint8_t aAtrBuffer[50];
   uint32_t nAtrLength;

   /* The presence flag */
   bool_t bIsPresent;

   tNALServiceOperation sNalOperation;
   uint8_t aNalParameterBuffer[1];

} tSEGetStatus;

/* The 7816 SM structure */
typedef struct __tSE7816Sm
{
   tDFCDriverCCReference pCallbackDriverCC;
   tP7816SmInstance* pSmInstance;
   tP7816SmInterface* pSmInterface;
   tP7816SmInstance* pLowLevelSmInstance;
   tP7816SmInterface* pLowLevelSmInterface;

} tSE7816Sm;

/* The exchange operation */
typedef struct __tSEExchange
{
   tDFCCallbackContext sCallbackContext;

} tSEExchange;

/* Declare a secure element slot */
typedef struct __tSESlot
{
   tHandleObjectHeader sObjectHeader;

   /* Second object header for the security stack destroy hook */
   tHandleObjectHeader sObjectHeaderSecurityStackDestroyHook;

   /* The Secure Element information */
   tWSEInfoEx sConstantInfo;

   /* The get status operation */
   tSEGetStatus sStatus;

   /* Swp policy operation */
   uint32_t nNewSwpPolicy;
   tNFCControllerSEPolicy sNewSePolicy;
   uint32_t nNewSePolicyStorageType;

   /* The HAL identifiers */
   uint32_t nHalSlotIdentifier;
   uint32_t nHalSessionIdentifier;

   /* The secure element protocol */
   uint32_t nConstantProtocols;

   /* The pending operation flags */
   uint32_t nPendingFlags;

   /* Internal communication operation */
   tDFCDriverCCReference pOpenConnectionDriverCC;
   tDFCCallbackContext sOpenConnectionCC;
   uint8_t aOpenConnectionDetectionBuffer[P_SE_DETECTION_BUFFER_LENGTH]; /* Receive the detection frame for the buffer */
   uint32_t nOpenConnectionDetectionBufferLength;
   uint32_t nDriverProtocol;
   W_HANDLE hOpenConnectionListenner;
   W_ERROR nOpenConnectionFirstError;
   bool_t bFirstDestroyCalled;
   bool_t bOpenConnectionForce;
   tDFCCallbackContext sDestroyCallback;
   W_HANDLE hDriverConnection;
   tUserInstance* pUserInstance;

   /* The Security Stack instance */
#ifdef P_INCLUDE_SE_SECURITY
   tSecurityStackInstance* pSecurityStackInstance;
#endif /* P_INCLUDE_SE_SECURITY */

   /* The WSESetPolicy operations */
   tDFCDriverCCReference pSetPolicyDriverCC;

   /* The current operation variables */
   tSEOperation sOperation;

   /* The SetPolicy callback and error are queued when GetTransactionAID is queued after SetPolicy */
   tDFCCallbackContext sOperationQueuedCallback;
   W_ERROR nOperationQueuedError;
   bool_t bOperationQueuedCloseConnection;

   /* The WSEMonitorEndOfTransaction operation */
   tSEMonitorEndOfTransaction sMonitorEndOfTransaction;

   /* The connectivity operation */
   tSEConnectivity sConnectivity;

   /* The WSEMonitorHotPlugEvents operation */
   tSEMonitorHotPlugEvents sMonitorHotPlugEvents;

   /* The initialization operations */
   tSEInitialization sIntialization;

   /* The activate SWP line operation */
   tNALServiceOperation  sActivateSWPLineOperation;

   /* The exchange operation */
   tSEExchange sExchange;

   /* The 7816 SM structure */
   tSE7816Sm s7816Sm;

} tSESlot;

/* Declare a secure element instance */
typedef struct __tSEInstance
{
   tNALServiceOperation sEndOfTransactionOperation;

   /* The policy to set after the host communication */
   tNFCControllerSEPolicy sAfterComPolicy;

   /* The policy currently requested */
   tNFCControllerSEPolicy sNewPolicy;

   /* The inialize callback parameters */
   tDFCCallbackContext sInitializeCallback;

   /* The current SE identifier used during initialization */
   uint32_t nSEIndentifierBeingInitialized;

   /* Flag indicating if the Secure Element information is built */
   bool_t bSEInfoIsBuilt;
   uint32_t nSeNumber;
   tSESlot* pSlotArray;
} tSEInstance;

/**
 * @brief   Creates a secure element instance.
 *
 * @pre  Only one secure element instance is created at a given time.
 *
 * @param[in]  pSEInstance The secure element instance to initialize.
 **/
void PSEDriverCreate(
      tSEInstance* pSEInstance );

/**
 * @brief   Destroyes a secure element instance.
 *
 * If the instance is already destroyed, the function does nothing and returns.
 *
 * @post  PSEDriverDestroy() does not return any error. The caller should always
 *        assume that the secure element instance is destroyed after this call.
 *
 * @post  The caller should never re-use the secure element instance value.
 *
 * @param[in]  pSEInstance  The secure element instance to destroy.
 **/
void PSEDriverDestroy(
      tSEInstance* pSEInstance );

/**
 * @brief   Resets the data in the secure element instance.
 *
 * @param[in]  pContext  The current context.
 **/
void PSEDriverResetData(
      tContext* pContext );

/**
 * @brief  Initializes the Secure Element(s)
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pCallback  A pointer on the callback function.
 *
 * @param[in]  pCallbackParameter  A blind parameter transmitted to the callback function.
 **/
void PSEDriverInitialize(
            tContext* pContext,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter);

/**
 * @brief Performs an applet operation on the SE.
 *
 * This function:
 *   - Creates a connection with the SE
 *   - Creates a logical channel (if requested)
 *   - Selects the application
 *   - Send the connection to the operation function
 *   - When the operation callback is called, the logical channel is closed
 *   - Closes the connection with the SE
 *   - Then the callback function is called
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nSlotIdentifier  The SE identifier.
 *
 * @param[in]  bCreateLogicalChannel  Request the creation of a logical channel.
 *
 * @param[in]  pAidBuffer  The buffer containing the applet AID.
 *
 * @param[in]  nAidLength  The AID length in bytes.
 *
 * @param[in]  pFunction  The operation function.
 *
 * @param[in]  pInstance  The operation instance returned to the operation function.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter  The callback parameter function.
 **/
void PSEDriverOperation(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            bool_t bCreateLogicalChannel,
            const uint8_t* pAidBuffer,
            uint32_t nAidLength,
            tPSEPerformAppletOperation* pFunction,
            void* pInstance,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter );

/**
 * Notifies the reception of a STK ACTIVATE (SWP) command.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nHalSlotIdentifier  The slot identifier.
 **/
void PSEDriverNotifyStkActivateSwp(
            tContext* pContext,
            uint32_t nHalSlotIdentifier);

/**
 * Notifies the reception of a STK REFRESH command.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nHalSlotIdentifier  The HAL slot identifier.
 *
 * @param[in]  nCommand The STK REFRESH command code.
 *
 * @param[in]  pRefreshFileList  The buffer containing the refresh file list of the STK REFRESH command.
 *
 * @param[in]  nRefreshFileListLength  The file list length in bytes. May be null if no file list is included in the command.
 **/
void PSEDriverNotifyStkRefresh(
         tContext* pContext,
         uint32_t nHalSlotIdentifier,
         uint32_t nCommand,
         const uint8_t* pRefreshFileList,
         uint32_t nRefreshFileListLength);

/**
 * Notifies a hot-plug event.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nHalSlotIdentifier  The slot identifier.
 *
 * @param[in]  bIsPresent  The tag presence
 **/
void PSEDriverNotifyHotPlug(
            tContext* pContext,
            uint32_t nHalSlotIdentifier,
            bool_t bIsPresent);

/**
 * Returns the slot with the specified HAL identifier.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nHalSlotIdentifier  The slot identifier.
 *
 * @return  The slot with the specified HAL identifier, null if not found
 */
tSESlot* PSEDriverGetSlotFromHalIdentifier(
            tContext* pContext,
            uint32_t nHalSlotIdentifier);

#endif /* P_CONFIG_DRIVER ||P_CONFIG_MONOLITHIC */

#endif /* __WME_SECURE_ELEMENT_DRIVER_H */
