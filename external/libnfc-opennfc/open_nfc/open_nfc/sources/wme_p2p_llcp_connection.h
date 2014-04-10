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
   Contains the interface of the LLCP connection manager
*******************************************************************************/

#ifndef __WME_P2P_CONNECTION_H
#define __WME_P2P_CONNECTION_H

   /* LLCP connection state */

enum {
   LLCP_CONNECTION_STATE_IDLE,
   LLCP_CONNECTION_STATE_CONNECT_SENT,
   LLCP_CONNECTION_STATE_ESTABLISHED,
   LLCP_CONNECTION_STATE_DISC_SENT,
   LLCP_CONNECTION_STATE_DISCONNECTED
};

   /* Structure of a LLCP Connection */

typedef struct __tLLCPConnection tLLCPConnection;

struct __tLLCPConnection
{
   tLLCPConnection * pNext;      /**< address of the next connection context */

   uint8_t           nLocalSAP;     /**< local SAP related to this connection */
   uint8_t           nRemoteSAP;    /**< remote SAP related to this connection */

   uint32_t          nState;        /**< connection state */

   uint8_t           nVS;           /**< Send State variable : NS value to be used in next I PDU*/
   uint8_t           nVSA;          /**< Send Acknowledged State : last received NR value */
   uint8_t           nVR;           /**< Receive State : next NS value expected */
   uint8_t           nVRA;          /**< Receive Acknowledged State  : most recently sent NR value */

   uint16_t          nMIUL;         /**< Local MIU */
   uint16_t          nMIUR;         /**< Remote MIU */
   uint8_t           nRWL;          /**< Local Receive Window size */
   uint8_t           nRWR;          /**< Remote Receive Window size */

   bool_t              bShutdownRequested;     /**< connection release has been requested */
   bool_t              bIsPeerBusy;   /**< Remote peer busy or not */
   bool_t              bIsBusy;       /**< If W_TRUE, we are in local congestion, (RNR has been sent) */

   bool_t              bAtLeastOneIPDUSent;       /**< Set to W_TRUE if at least one I PDU has been sent, used for NS/NR validation */
   bool_t              bAtLeastOneIPDUReceived;   /**< Set to W_TRUE if at least one I PDU has been received, used for NS/NR validation */

   tP2PSocket     * pP2PSocket;  /**< socket owning this connection */

   tDFCCallbackContext sDisconnectCC;

   tLLCPPDUHeader * pConnectPDU;
   tLLCPPDUHeader * pDiscPDU;

};

typedef struct __tLLCPCONNInstance
{
   tLLCPConnection * pConnectionList;

} tLLCPCONNInstance;


/**
 * Creates LLCP connection component

 * @param[in]   pLLCPCONNInstance The CONN instance
 *
 * @return
 *         - W_SUCCESS
 *
 **/

W_ERROR PLLCPCreateCONN(
   tLLCPCONNInstance * pLLCPCONNInstance
);

/**
 * Destroys LLCP connection component

 * @param[in]   pLLCPCONNInstance The CONN instance
 *
 * @return
 *         - W_SUCCESS
 *
 **/

W_ERROR PLLCPDestroyCONN(
   tLLCPCONNInstance * pLLCPCONNInstance
);

/**
 * Allocates a LLCP connection component
 *
 * @param[in]   pContext   The context
 * @param[in]   nLocalSAP  The local SAP associated to this connection
 * @param[in]   nRemoteSAP The remote SAP associated to this connection
 * @param[in]   pP2PSocket The P2P socket associated to this connection

 * @return
 *         - W_SUCCESS
 *         - W_ERROR_OUT_OF_RESOURCES
 *
 **/

tLLCPConnection * PLLCPConnectionAllocContext(
   tContext          *  pContext,
   uint8_t              nLocalSAP,
   uint8_t              nRemoteSAP,
   tP2PSocket       *  pP2PSocket
   );


/**
 * Frees a LLCP connection component
 *
 * @param[in]  pContext   The context
 * @param[in]  pConnection The connection to be freeed
 *
 * @return
 *         - W_SUCCESS
 *         - W_ERROR_BAD_PARAMETER
 *
 **/
W_ERROR PLLCPConnectionFreeContext(
   tContext        * pContext,
   tLLCPConnection * pConnection
   );


/**
 * Accesses a LLCP connection from local and remote SAP
 *
 * @param[in]   pContext    The context
 * @param[in]   nLocalSAP   The local SAP
 * @param[in]   nRemoteSAP  The remote SAP
 *
 * @return
 *         - Address of the connection context if found
 *         - null
 **/

tLLCPConnection * PLLCPConnectionAccessContext(
      tContext  * pContext,
      uint8_t nLocalSAP,
      uint8_t nRemoteSAP
      );

/**
 * Start transmission of SDU in the LLCP connection xmit fifo
 *
 * @param[in] pContext        The context
 * @param[in] pConnection     The LLCP connection
 *
 * @return - W_TRUE if a PDU has been sent, otherwise W_FALSE
 *
 */

bool_t PLLCPConnectionStartXmitSDU(
   tContext          * pContext,
   tLLCPConnection   * pConnection
   );

void PLLCPConnectionStartRecvSDU(
   tContext   * pContext,
   tLLCPConnection * pP2PSocket
   );

#endif /* __WME_P2P_CONNECTION_H */


