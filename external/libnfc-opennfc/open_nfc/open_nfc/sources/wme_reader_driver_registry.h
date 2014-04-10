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

#ifndef __WME_READER_DRIVER_REGISTRY_H
#define __WME_READER_DRIVER_REGISTRY_H

/*******************************************************************************
  Contains the declaration of the reader registry implementation
*******************************************************************************/

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* The maximum length in bytes of a card UID */
#define P_READER_MAX_UID_LENGTH  32

typedef struct __tPReaderDriverCardInfo
{
   /* UID/PUPI & Protocol */
   uint8_t  nUIDLength;
   uint8_t  nAFI;

   uint8_t  aUID[P_READER_MAX_UID_LENGTH];

   /* The card protocol bit field */
   uint32_t  nProtocolBF;
   
} tPReaderDriverCardInfo;

/**
 * @brief   Creates the connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nServiceIdentifier  The service identifier.
 *
 * @param[in]  phDriverConnection  A pointer on a variable valued with
 *             the driver connection handle.
 *
 * @return  W_SUCCESS if the buffer was successfully created, an error otherwise.
 **/
typedef W_ERROR tPReaderDriverCreateConnection(
            tContext* pContext,
            uint8_t nServiceIdentifier,
            W_HANDLE* phDriverConnection );

/**
 * @brief   Creates a set command corresponding to the detection configuration data.
 *
 * If the length of the configuration is zero, the implementation should set the
 * command reseting the configuration to its default value.
 *
 * @param[in]  pContext  The context.
 *
 * @param[out] pCommandBuffer  The buffer receiving the command an parameters.
 *
 * @param[out] pnCommandBufferLength  A pointer on avariable valued with
 *             the length in bytes of the command buffer
 *
 * @param[in]  pDetectionConfigurationBuffer  The buffer containing
 *             the detection configuration data.
 *
 * @param[in]  nDetectionConfigurationLength  The length in bytes of
 *             the detection configuration data.
 *
 * @return  W_SUCCESS if the parameters are valid, an error otherwise.
 **/
typedef W_ERROR tPReaderDriverSetDetectionConfiguration(
            tContext* pContext,
            uint8_t* pCommandBuffer,
            uint32_t* nCommandBufferLength,
            const uint8_t* pDetectionConfigurationBuffer,
            uint32_t nDetectionConfigurationLength );

/**
 * @brief   Parses the detection message data.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pBuffer  The detection message data.
 *
 * @param[in]  nLength  The length in bytes of the detection message data.
 *
 * @param[out] pCardInfo  The card information to fill.
 *             The protocol index and the protocol bit field are already set.
 *
 * @return  W_SUCCESS if the detection message is valid, an error otherwise.
 **/
typedef W_ERROR tPReaderDriverParseDetectionMessage(
            tContext* pContext,
            const uint8_t* pBuffer,
            uint32_t nLength,
            tPReaderDriverCardInfo* pCardInfo );

/**
 * @brief  Checks if a card matches the detection configuration.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  nProtocolBF  The protocol bit field.
 *
 * @param[in]  pDetectionConfigurationBuffer  The buffer containing
 *             the detection configuration data.
 *
 * @param[in]  nDetectionConfigurationLength  The length in bytes of
 *             the detection configuration data.
 *
 * @param(in]  pCardInfo  The card information to check.
 *
 * @return  W_TRUE if the card matches the configuration, W_FALSE otherwise.
 **/
typedef bool_t tPReaderDriverCheckCardMatchConfiguration(
            tContext* pContext,
            uint32_t nProtocolBF,
            const uint8_t* pDetectionConfigurationBuffer,
            uint32_t nDetectionConfigurationLength,
            tPReaderDriverCardInfo* pCardInfo);

/* The constant structure describing a reader protocol */
typedef const struct __tPRegistryDriverReaderProtocolConstant
{
   uint32_t nProtocolBF;
   uint8_t nServiceIdentifier;
   tPReaderDriverCreateConnection* pCreateConnection;
   tPReaderDriverSetDetectionConfiguration* pSetDetectionConfiguration;
   tPReaderDriverParseDetectionMessage* pParseDetectionMessage;
   tPReaderDriverCheckCardMatchConfiguration* pCheckCardMatchConfiguration;
} tPRegistryDriverReaderProtocolConstant;

/**
 * @brief  Receive a card emulation event.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pCallbackParameter  The callback parameter.
 *
 * @param(in]  nProtocolBF  The protocol BF.
 *
 * @param[in]  nEventIdentifier  The event identifier.
 *
 * @param[in]  pBuffer  The event data buffer.
 *
 * @param[in]  nLength  The length in bytes of the event data buffer.
 **/
typedef void tPRegistryDriverCardEventDataReceived(
            tContext* pContext,
            void* pCallbackParameter,
            uint32_t nProtocolBF,
            uint8_t nEventIdentifier,
            const uint8_t* pBuffer,
            uint32_t nLength );

/**
 * @brief  Receive a P2P data event
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pCallbackParameter  The callback parameter.
 *
 * @param[in]  pBuffer  The data buffer.
 *
 * @param[in]  nLength  The length in bytes of the data buffer.
 **/

typedef void tP2PDataIndicationCallback(
            tContext* pContext,
            void* pCallbackParameter,
            const uint8_t* pBuffer,
            uint32_t nLength );

/* The constant structure describing a card protocol */
typedef const struct __tPRegistryDriverCardProtocolConstant
{
   uint32_t nProtocolBF;
   uint8_t nServiceIdentifier;
   tPRegistryDriverCardEventDataReceived* pEventReceived;

} tPRegistryDriverCardProtocolConstant;

/**
 * Returns the pointer on the connection object corresponding to a handle value.
 *
 * This function should be called to check the validity of a handle value and
 * to obtain the pointer on the object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  hDriverConnection  The driver connection handle.
 *
 * @param[in]  pExpectedType  The expected object type.
 *
 * @param[out] ppObject  A pointer on a variable valued with the pointer on the
 *             corresponding object is the handle is valid and the object is of
 *             the expected type. Otherwise, the value is set to null.
 *
 * @return     One of the following value:
 *              - W_SUCCESS The handle is valid and of the specified type.
 *              - W_ERROR_BAD_HANDLE  The handle is not valid, the handle is
 *                W_NULL_HANDLE or the object is not of the specified type.
 **/
W_ERROR PReaderDriverGetConnectionObject(
            tContext* pContext,
            W_HANDLE hDriverConnection,
            tHandleType* pExpectedType,
            void** ppObject);

/**
 * Sets the anti-replay time reference.
 *
 * This function should be called each time a successful exchange is performed
 * with a card.
 *
 * @param[in]  pContext  The context.
 **/
void PReaderDriverSetAntiReplayReference(
            tContext* pContext);

/* Declare a reader protocol information */
struct __tPRegistryDriverReaderProtocol;

/* Declare a card protocol information */
struct __tPRegistryDriverCardProtocol;

/* Declare a reader listener */
struct __tPRegistryDriverReaderListener;

/* Declare an error registry structure */
typedef struct __tErrorRegistry
{
   /* Error registry handle */
   tHandleObjectHeader sObjectHeader;

   /* Registry present or not */
   bool_t bIsRegistered;

   /* Polling Reqested flag */
   bool_t bCardDetectionRequested;

   /* Callback information */
   tDFCDriverCCReference pErrorListenerDriverCC;

} tErrorRegistry;

/* The pulse monitor structure */
typedef struct __tNFCControllerPulsePeriodMonitor
{
   uint16_t nRegisterValue;
   uint16_t nNewRegisterValue;
   tDFCDriverCCReference pDriverCC;
   tNALServiceOperation sServiceOperation;
   bool_t bOperationPending;
   uint8_t aSetParameterData[2];

} tNFCControllerPulsePeriodMonitor;

/* Target discovered message */
typedef struct __tPReaderDriverTargetDetectionMessage
{
   uint32_t nMessageCounter;
   uint32_t nProtocolBF;
   bool_t bCollision;
   uint8_t aBuffer[NAL_MESSAGE_MAX_LENGTH];
   uint32_t nBufferLength;

} tPReaderDriverTargetDetectionMessage;

/* Card Emulation message */
typedef struct __tPReaderDriverCardEmulationMessage
{
   uint32_t nMessageCounter;
   struct __tPRegistryDriverCardProtocol* pCardProtocol;
   uint8_t nEventIdentifier;
   uint8_t aBuffer[NAL_MESSAGE_MAX_LENGTH];
   uint32_t nBufferLength;

} tPReaderDriverCardEmulationMessage;

#define MAX_STORED_EMUL_MESSAGE_NB       5

/* Declare a reader driver registry */
typedef struct __tPReaderDriverRegistry
{
   /* The current detection bit field */
   uint32_t nCurrentDetectionProtocolBF;

   /* The detection state */
   uint32_t nDetectionState;

   /* The card application match flag */
   bool_t bCardApplicationMatch;

   /* Check if we are in collision resolution */
   bool_t bInCollisionResolution;
   uint32_t aCollisionProtocols[5];
   uint32_t nCollisionProtocolsSize;
   uint32_t nCurrentCollisionProtocolIdx;

   /* nb Card detected */
   uint8_t  nNbCardDetected;

   /* The reader detection message counter for the last detection message
      with at least one reader protocol.
      0 means no valid last message */
   uint32_t nReaderDetectionCommandMessageCounter;

   /* The card emulation detection message counter for the last detection message
      with at least one card protocol.
      0 means no valid last message */
   uint32_t nCardEmulationDetectionCommandMessageCounter;

   /* The target discovered message stored in case of message crossing.
      If the message counter is 0, no message is stored */
   tPReaderDriverTargetDetectionMessage sStoredTargetDetectionMessage;

   /* The card emulation message stored in case of message crossing.
      If the message counter is 0, no message is stored */

   uint32_t  nStoredEmulMessageNb;
   tPReaderDriverCardEmulationMessage sStoredCardEmulationMessage[MAX_STORED_EMUL_MESSAGE_NB];

   /* Detection Command Operation */
   tNALServiceOperation sDetectionCommandOperation;

   /* The detection command buffer */
   uint8_t aDetectionCommandData[4];

   /* The last card information, including the last card protocol BF */
   tPReaderDriverCardInfo  * pLastCard;

   /* Time reference */
   uint32_t nTimeReference;

   /* The reader protocol list */
   struct __tPRegistryDriverReaderProtocol* pReaderProtocolListHead;

   /* The card protocol list */
   struct __tPRegistryDriverCardProtocol* pCardProtocolListHead;

   /* The connection flag */
   bool_t bIsConnected;

   /* Reader connection object header */
   tHandleObjectHeader              sReaderConnectionObjectHeader;

   /* Unknown callback registry */
   tErrorRegistry                   sUnknownRegistry;

   /* Collision callback registry */
   tErrorRegistry                   sCollisionRegistry;

   /* Multiple detection callback registry */
   tErrorRegistry                   sMultipleDetectionRegistry;

   /* Pointer on the current listener */
   struct __tPRegistryDriverReaderListener*  pCurrentDriverListener;

   /* Handle of the current connection */
   W_HANDLE                         hCurrentDriverConnection;

   /* Pointer on the first listener */
   struct __tPRegistryDriverReaderListener*  pDriverListenerListHead;

   /* The monitor for the pulse period */
   tNFCControllerPulsePeriodMonitor sPulsePeriodMonitor;

   /* callback function dealing with data reception for P2P */
   uint8_t                           aDataBuffer[NAL_MESSAGE_MAX_LENGTH];
   tDFCCallbackContext               sReceptionCC;

   bool_t                              bStopAllActiveDetection;
   bool_t                              bReducedDetection;
   bool_t                              bReducedDetectionExpired;

   tPBasicGenericCompletionFunction* pStopCallback;
   void*                             pStopCallbackParameter;
} tPReaderDriverRegistry;

/**
 * @brief   Creates a registry instance.
 *
 * @pre  Several registry instance can be created at a given time.
 *
 * @param[in]  pReaderDriverRegistry The reader driver registry to initialize.
 **/
void PReaderDriverRegistryCreate(
         tPReaderDriverRegistry* pReaderDriverRegistry );

/**
 * @brief   Destroyes a registry instance.
 *
 * If the instance is already destroyed, the function does nothing and returns.
 *
 * @post  PReaderDriverRegistryDestroy() does not return any error. The caller should always
 *        assume that the registry instance is destroyed after this call.
 *
 * @post  The caller should never re-use the registry instance value.
 *
 * @param[in]  pReaderDriverRegistry  The reader driver registry to destroy.
 **/
void PReaderDriverRegistryDestroy(
         tPReaderDriverRegistry* pReaderDriverRegistry );

/**
 * @brief  Connects the reader registry from the NFC Controller.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  nProtocolCapabilitiesBF  The protocol capabilities.
 *
 * @return  The result code.
 **/
W_ERROR PReaderDriverConnect(
         tContext* pContext,
         uint32_t nProtocolCapabilitiesBF );

/**
 * @brief  Disconnects the reader registry from the NFC Controller.
 *
 * @param[in]  pContext  The current context.
 **/
void PReaderDriverDisconnect(
         tContext* pContext );

/**
 * @brief Gets the current RF activity state.
 *
 * @param[in]  pContext  The current context.
 *
 * @return  The RF activity state:
 *               - W_NFCC_RF_ACTIVITY_INACTIVE
 *               - W_NFCC_RF_ACTIVITY_DETECTION
 *               - W_NFCC_RF_ACTIVITY_ACTIVE
 **/
uint8_t PReaderDriverGetRFActivity(
                  tContext* pContext);

/**
 * Registers a card emulation.
 *
 * @param[in]  nProtocolBF  The protocol BF.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter  The callback parameter.
 *
 * @param[in]  pParameterBuffer  The parameter buffer.
 *
 * @param[in]  nParameterLength  The length in bytes of the parameter buffer.
 **/
void PReaderDriverRegisterCardEmulation(
                  tContext* pContext,
                  uint32_t nProtocolBF,
                  tPBasicGenericCallbackFunction* pCallback,
                  void* pCallbackParameter,
                  const uint8_t* pParameterBuffer,
                  uint32_t nParameterLength);

/**
 * Unregisters a card emulation.
 *
 * @param[in]  nProtocolBF  The protocol BF.
 *
 * @param[in]  pCallback  The callback function.
 *
 * @param[in]  pCallbackParameter  The callback parameter.
 **/
void PReaderDriverUnregisterCardEmulation(
                  tContext* pContext,
                  uint32_t nProtocolBF,
                  tPBasicGenericCallbackFunction* pCallback,
                  void* pCallbackParameter );


/**
  * Register P2P data indication callback
  *
  * @param[in] pContext The context
  *
  * @param[in] pCallback The callback
  *
  * @param[in] pCallbackParameter The callback parameter
  *
  */

void PReaderDriverRegisterP2PDataIndicationCallback(
                  tContext * pContext,
                  tDFCCallback * pCallback,
                  void * pCallbackParameter);

/**
  * Unregister P2P data indication callback
  *
  * @param[in] pContext The context
  */

void PReaderDriverUnregisterP2PDataIndicationCallback(
                  tContext * pContext);

/*
 * Register a listener (to be called internally by the driver/server part of the stack)
 */

W_ERROR PReaderDriverRegisterInternal(
            tContext* pContext,
            tPReaderDriverRegisterCompleted* pCallback,
            void* pCallbackParameter,
            uint8_t nPriority,
            uint32_t nRequestedProtocolsBF,
            uint32_t nDetectionConfigurationLength,
            uint8_t* pBuffer,
            uint32_t nBufferMaxLength,
            W_HANDLE* phListenerHandle,
            bool_t      bFromUser);

/**
  * Request the deactivation of the non-SE listeners.
  * This does not cut the current connection with an external card. If no connection was
  * established, a new detection without any reader mode is requested.
  *
  * This function is called before putting the SE in dialog mode
  *
  * @param[in] pContext The context
  *
  * @param[in] pCallback Callback function called when it is safe to switch the SE in dialog mode
  *
  * @param[in] pCallback pCallbackParameter The callback parameter
  */
void PReaderDriverDisableAllNonSEListeners(
            tContext* pContext,
            tPBasicGenericCompletionFunction * pCallback,
            void * pCallbackParameter);

/**
  * Request the reactivation of the non-SE listeners.
  *
  * This function is called after restoring the SE in RF or HighZ mode.
  *
  * @param[in] pContext The context
  *
  * @param[in] pCallback Callback function called when it is safe to switch the SE in dialog mode
  *
  * @param[in] pCallback pCallbackParameter The callback parameter
  */
void PReaderDriverEnableAllNonSEListeners(
            tContext* pContext);

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

/* Declare a reader link information by jimmy */
typedef struct __tGerenalByte
{
   /* Detection configuration */
   uint8_t aGerenalByte[NAL_MESSAGE_MAX_LENGTH];
   uint8_t nGerenalByteLength;
} tGerenalByte;

#endif /* __WME_READER_DRIVER_REGISTRY_H */
