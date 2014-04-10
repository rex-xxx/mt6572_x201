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
   Contains the declaration of the LLCP socket component
*******************************************************************************/

#ifndef __WME_P2P_SOCKET_H
#define __WME_P2P_SOCKET_H

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)


/**
 * @brief Clean the context of the socket
 *
 * @param[in]  pContext     The context
 *
 * @param[in]  pP2PSocket   The socket;
 **/

void PP2PCleanSocketContext(
                  tContext * pContext,
                  tP2PSocket   * pP2PSocket);

/**
 * @brief Clean the contexts of all the sockets
 *
 * @param[in]  pContext     The context
 */

void PP2PCleanAllSocketContexts(
                  tContext * pContext);

/**
 * @brief Calls the receive callback
 *
 * @param[in]  pContext     The context
 *
 * @param[in]  pP2PSocket   The socket
 *
 * @param[in]  nRecvLength  The amount of data received
 *
 * @param[in]  nError       The result of the operation
 *
 * @param[in]  nSAP         The source SAP (connectionless only )
 *
 **/
void PP2PCallSocketReadCallback(
                  tContext    * pContext,
                  tP2PSocket  * pP2PSocket,
                  uint32_t      nRecvLength,
                  W_ERROR       nError,
                  uint8_t       nSAP);

/**
 * @brief Calls the xmit callback
 *
 * @param[in]  pContext     The context
 *
 * @param[in]  pP2PSocket   The socket
 *
 * @param[in]  nError       The result of the operation
 **/
void PP2PCallSocketWriteCallback(
                  tContext    * pContext,
                  tP2PSocket  * pP2PSocket,
                  W_ERROR       nError);

/**
 * @brief Calls the read and write callback
 *
 * @param[in]  pContext     The context
 *
 * @param[in]  pP2PSocket   The socket
 *
 * @param[in]  nError       The result of the operation
 **/

void PP2PCallSocketReadAndWriteCallbacks(
   tContext     * pContext,
   tP2PSocket   * pP2PSocket,
   W_ERROR        nError);

/**
 * @brief Calls the read and write callback of all sockets
 *
 * @param[in]  pContext     The context
 *
 * @param[in]  nError       The result of the operation
 **/
void  PP2PCallAllSocketReadAndWriteCallbacks(
   tContext * pContext,
   W_ERROR nError);


/**
 * @brief Calls the connect callbacks of all sockets
 *
 * @param[in]  pContext     The context
 *
 * @param[in]  nError       The result of the operation
 **/
void PP2PCallAllSocketConnectCallbacks(
   tContext * pContext,
   W_ERROR nError);


#endif /* (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */
#endif /* __WME_P2P_SOCKET_H */
