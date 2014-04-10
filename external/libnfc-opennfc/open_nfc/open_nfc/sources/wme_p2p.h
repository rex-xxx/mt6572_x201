/*
 * Copyright (c) 2008-2012 Inside Secure, All Rights Reserved.
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

#ifndef __WME_P2P_H
#define __WME_P2P_H

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)


typedef struct __tP2PLink    tP2PLink;
typedef struct __tP2PSocket  tP2PSocket;

#include "wme_p2p_llcp.h"

struct __tP2PLink
{
   tHandleObjectHeader sObjectHeader;              /* header common to all handle objects */
   tP2PLink  * pNext;

   W_HANDLE hLink;                                 /* handle of the P2P link */
   W_HANDLE hEstablishOperation;                   /* handle of the establish operation */
   tDFCCallbackContext sEstablishmentCC;           /* Establishment callback */
   bool_t bEstablishedCalled;                      /* If W_TRUE, the establishment callback has been called */

   tDFCCallbackContext sReleaseCC;                 /* Release callback */
   bool_t bReleasedCalled;                         /* If W_TRUE, the release callback has been called */

   bool_t   bIsRegistered;
   bool_t   bIsDestroyPending;
};


struct __tP2PSocket
{

   tHandleObjectHeader sObjectHeader;     /**< header common to all handle objects */
   tP2PSocket * pNext;

   struct {

      W_HANDLE    hSocket;                /**< handle associated to the socket */
      uint8_t     nType;                  /**< type of socket : client, server, any, connectionless */

      uint8_t     nLocalSap;              /**< local SAP allocated to the socket */
      bool_t        bServiceRegistered;     /**< If set to W_TRUE, the socket is associated to a local service name */

      uint8_t   * pRemoteURI;             /**< Remote URI, used for connection establishement */
      uint32_t    nRemoteURILength;       /**< Remote URI length */
      uint8_t     nRemoteSAP;             /**< Remote SAP, used for connection establishment */

      uint16_t    nLocalMIU;              /**< Local socket MIU, 128 - 256 */
      uint8_t     nLocalRW;               /**< local RWND, 0-15 */

   } sConfig;

   bool_t         bXmitInProgress;          /**< a send operation is in progress */
   tDFCCallbackContext sXmitCC;             /**< send complete callback */
   W_HANDLE     hXmitOperation;             /**< send operation */
   uint8_t *    pXmitBuffer;                /**< current xmit buffer */
   uint32_t     nXmitBufferLength;          /**< current xmit buffer length */

   bool_t         bRecvInProgress;          /**< a receive operation is in progress */
   tDFCCallbackContext sRecvCC;             /**< receive complete callback */
   W_HANDLE     hRecvOperation;             /**< receive operation */
   uint8_t *    pRecvBuffer;                /**< current receive buffer */
   uint32_t     nRecvBufferLength;          /**< current receive buffer length */

   tLLCPPDUHeader * pFirstRecvPDU;
   tLLCPPDUHeader * pLastRecvPDU;
   uint32_t         nRecvPDU;


   struct {
      bool_t bIsConnected;                  /**< connection is established */
      bool_t bIsServer;

      bool_t bConnectInProgress;            /**< a connect operation is in progress */
      tDFCCallbackContext sConnectCC;       /**< connection complete callback */

      bool_t bShutdownInProgress;
      tDFCCallbackContext sShutdownCC;      /**< connection complete callback */

      tLLCPConnection * pConnection;        /**< LLCP connection associated to the socket */
      tLLCPConnection * pPendingConnection; /**< LLCP connection associated to the socket */

   } sConnection;
};

/* Declare a Peer2Peer instance */
typedef struct __tP2PInstance
{
   tLLCPInstance   sLLCPInstance;

   /* link management */
   uint32_t        nNbEstablishRequests;
   tP2PLink      * pFirstP2PLink;

   /* socket management */
   tP2PSocket    * pFirstSocket;

   uint8_t     aBuffer[LLCP_NUMBERED_PDU_DATA_OFFSET + LLCP_DEFAULT_MIU + LLCP_MAX_LOCAL_MIUX];

   W_HANDLE    hP2PInitiatorListener;     /**< Handle of the P2P initiator listener   */

   W_HANDLE    hP2PInitiatorConnection;   /**< Handle of the P2P initiator connection */
   W_HANDLE    hP2PTargetConnection;      /**< Handle of the P2P target connection */

   uint8_t     nState;
   uint8_t     nTargetState;

#define     P_LLCP_STATE_STOPPED    0
#define     P_LLCP_STATE_STARTING   1
#define     P_LLCP_STATE_STARTED    2
#define     P_LLCP_STATE_STOPPING   3
#define     P_LLCP_STATE_ERROR      4

   W_ERROR     nLinkReleaseCause;

} tP2PInstance;

/**
 * @brief   Creates a peer 2 peer instance.
 *
 * @pre  Only one P2P instance is created at a given time.
 *
 * @param[in]  pP2PInstance The card emulation instance to initialize.
 **/
void PP2PCreate(
      tP2PInstance* pP2PInstance );

/**
 * @brief   Destroyes a P2P instance.
 *
 * If the instance is already destroyed, the function does nothing and returns.
 *
 * @post  PEmulDestroy() does not return any error. The caller should always
 *        assume that the peer 2 peer instance is destroyed after this call.
 *
 * @post  The caller should never re-use the peer 2 peer instance value.
 *
 * @param[in]  pP2PInstance  The card emulation instance to destroy.
 **/
void PP2PDestroy(
      tP2PInstance* pP2PInstance );


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
uint8_t PP2PGetRFActivity(
      tContext* pContext);


/**
  * @brief Returns W_TRUE if the NFC controller supports the P2P protocol
  *
  * @param[in] pContext The context
  *
  * @return W_TRUE if the P2P is supported
  */
bool_t PP2PCheckP2PSupport(
      tContext * pContext);

/**
  * @brief Retrieves the link context from its handle
  *
  * @param[in] pContext The context
  *
  * @param[in] hLink    The link handle
  *
  * @param[out] ppLink  The link context address
  *
  * @return W_SUCCESS if the link context has been found
  */
W_ERROR  PP2PGetLinkObject(
      tContext * pContext,
      W_HANDLE hLink,
      tP2PLink ** ppLink);

/**
  * @brief Retrieves the socket context from its handle
  *
  * @param[in] pContext The context
  *
  * @param[in] hSocket    The socket handle
  *
  * @param[out] ppSocket  The socket context address
  *
  * @return W_SUCCESS if the socket context has been found
  */
W_ERROR  PP2PGetSocketObject(
      tContext * pContext,
      W_HANDLE hSocket,
      tP2PSocket ** ppSocket);

/**
 * Outgoing data link establishment callback,
 *
 * called by LLCP at the end of outgoing connection phase
 *
 * @param[in] pContext    The context
 *
 * @param[in] pConnection The LLCP connection
 *
 * @param[in] nError      The error code
 *
 */
void PP2PConnectCompleteCallback(
      tContext         * pContext,
      tLLCPConnection  * pConnection,
      W_ERROR            nError);

/**
 * Outgoing data link disconnection callback,
 *
 * called by LLCP at the end of disconnection phase
 *
 * @param[in] pContext    The context
 *
 * @param[in] pConnection The LLCP connection
 *
 * @param[in] nError      The error code
 *
 */
void PP2PDisconnectCompleteCallback(
                  tContext         * pContext,
                  tLLCPConnection  * pConnection,
                  W_ERROR            nError);

/**
 * Incoming data link establishment callback,
 * called by LLCP at the end of incoming data link connect phase
 *
 * @param[in] pContext    The context
 *
 * @param[in] pConnection The connection
 */
void PP2PConnectIndicationCallback(
                  tContext         * pContext,
                  tLLCPConnection  * pConnection);

/**
 * Incoming data link release callback,
 * called by LLCP when a data connection is released (DISC or DM) or when an error occured
 *
 * @param[in] pContext       The context
 *
 * @param[in] pConnection    The connection
 */
void PP2PDisconnectIndicationCallback(
                  tContext         * pContext,
                  tLLCPConnection  * pConnection);


/**
 * Call all the data link release callbacks
 *
 * @param[in] pContext       The context
 */
void PP2PCallAllDisconnectIndicationCallback(
                  tContext         * pContext);

/**
  * Calls the P2P link establishment callbacks
  *
  * @param[in] pContext       The context
  *
  * @param[in] nError         The result
  */
void PP2PCallAllLinkEstablishmentCallbacks(
                  tContext * pContext,
                  W_ERROR    nError);


void PP2PCallAllLinkReleaseCallbacks(
                  tContext * pContext,
                  W_ERROR    nError);
/**
 *  Called when a link deactivation has been detected by the LLCP (reception of a DISC SSAP/0 - DSAP 0)
 *
 * @param[in] pContext                The context
 *
 * @param[in] pCallbackParameter1       The parameter
 */
void PP2PLinkDeactivationCallback(
                  tContext * pContext,
                  void * pCallbackParameter);



void PP2PCallURILookUpCallback(
                  tContext * pContext,
                  W_ERROR nError);


/**
 * General failure.
 *
 * The P2P link is broken, destroy it
 *
 * @param[in] pContext       The context
 *
 * @param[in] nError         The cause of the failure
 */
void PP2PLinkGeneralFailure(
                  tContext * pContext,
                  W_ERROR    nError);


/**
 * LLCP remote timeout expiry callback
 *
 * The peer did not answered in a timely manner
 * we consider the LLCP connection as broken
 *
 * @param[in] pContext       The context
 *
 * @param[in] pCallbackParameter The callback parameter
 */

void PP2PRemoteTimeoutExpiry(
                  tContext * pContext,
                  void     * pCallbackParameter);



/* Following functions are used for access to P2P from DRIVER part of the Open NFC stack */

W_HANDLE PP2PEstablishLinkDriver1Internal(
      tContext * pContext,
      tPBasicGenericHandleCallbackFunction* pEstablishmentCallback,
      void* pEstablishmentCallbackParameter);

void PP2PEstablishLinkDriver2Internal(
      tContext * pContext,
      W_HANDLE hLink,
      tPBasicGenericCallbackFunction* pReleaseCallback,
      void* pReleaseCallbackParameter,
      W_HANDLE * phOperation);

void PP2PConnectDriverInternal(
      tContext * pContext,
      W_HANDLE hSocket,
      W_HANDLE hLink,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter);

void PP2PShutdownDriverInternal(
      tContext * pContext,
      W_HANDLE  hSocket,
      tPBasicGenericCallbackFunction * pCallback,
      void * pCallbackParameter);

void PP2PReadDriverInternal (
      tContext * pContext,
      W_HANDLE hSocket,
      tPBasicGenericDataCallbackFunction * pCallback,
      void * pCallbackParameter,
      uint8_t * pReceptionBuffer,
      uint32_t nReceptionBufferLength,
      W_HANDLE             * phOperation);

void PP2PWriteDriverInternal(
      tContext * pContext,
      W_HANDLE hSocket,
      tPBasicGenericCallbackFunction* pCallback,
      void* pCallbackParameter,
      const uint8_t* pSendBuffer,
      uint32_t nSendBufferLength,
      W_HANDLE* phOperation);

void PP2PRecvFromDriverInternal(
      tContext * pContext,
      W_HANDLE hSocket,
      tPP2PRecvFromCompleted* pCallback,
      void* pCallbackParameter,
      uint8_t * pReceptionBuffer,
      uint32_t nReceptionBufferLength,
      W_HANDLE* phOperation);

void PP2PSendToDriverInternal(
      tContext * pContext,
      W_HANDLE hSocket,
      tPBasicGenericCallbackFunction* pCallback,
      void* pCallbackParameter,
      uint8_t nSAP,
      const uint8_t* pSendBuffer,
      uint32_t nSendBufferLength,
      W_HANDLE* phOperation);

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

#endif /* __WME_P2P_H */
