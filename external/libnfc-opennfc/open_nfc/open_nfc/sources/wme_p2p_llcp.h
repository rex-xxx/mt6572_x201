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
   Contains the declaration of the LLCP SAP component
*******************************************************************************/

#ifndef __WME_P2P_LLCP_H
#define __WME_P2P_LLCP_H

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

typedef struct __tLLCPInstance   tLLCPInstance;

typedef struct __tDecodedPDU     tDecodedPDU;

#include "wme_p2p_sap.h"
#include "wme_p2p_sdp.h"
#include "wme_p2p_llcp_pdu.h"
#include "wme_p2p_llcp_connection.h"
#include "wme_p2p_socket.h"
#include "wme_p2p_llcp_state_machine.h"

/* LLCP 1.0 */
#define LLCP_VERSION_10_MAJOR                 1
#define LLCP_VERSION_10_MINOR                 0

/* LLCP 1.1 */
#define LLCP_VERSION_11_MAJOR                 1
#define LLCP_VERSION_11_MINOR                 1

#define LLCP_VERSION_MAJOR                    LLCP_VERSION_11_MAJOR
#define LLCP_VERSION_MINOR                    LLCP_VERSION_11_MINOR

#define LLCP_DEFAULT_MIU                      128  //128
#define LLCP_MAX_LOCAL_MIUX                   64  //128
#define LLCP_MAX_REMOTE_MIUX                  64  //128
#define LLCP_LOCAL_RW                          15

#define LLCP_ROLE_NONE                       0
#define LLCP_ROLE_INITIATOR                  1
#define LLCP_ROLE_TARGET                     2


   /* PDU desription */

#define LLCP_UNUMBERED_PDU_MIN_LENGTH        2
#define LLCP_UNUMBERED_PDU_DATA_OFFSET       2

#define LLCP_NUMBERED_PDU_MIN_LENGTH         3
#define LLCP_NUMBERED_PDU_SEQUENCE_OFFSET    2
#define LLCP_NUMBERED_PDU_DATA_OFFSET        3

   /* PDU types */

#define LLCP_PDU_SYMM                  0x00
#define LLCP_PDU_PAX                   0x01
#define LLCP_PDU_AGF                   0x02
#define LLCP_PDU_UI                    0x03
#define LLCP_PDU_CONNECT               0x04
#define LLCP_PDU_DISC                  0x05
#define LLCP_PDU_CC                    0x06
#define LLCP_PDU_DM                    0x07
#define LLCP_PDU_FRMR                  0x08
#define LLCP_PDU_SNL                   0x09
#define LLCP_PDU_I                     0x0C
#define LLCP_PDU_RR                    0x0D
#define LLCP_PDU_RNR                   0x0E


struct __tDecodedPDU
{
      /* fields common to all PDUs */

   uint8_t     nType;               /**< Type of PDU */
   uint8_t     nDSAP;               /**< Destination Service Access Point */
   uint8_t     nSSAP;               /**< Source Service Access Point */

      /* fields specific to numbered PDUs */

   uint8_t     nNS;                 /**< Send Sequence Number for numbered PDUs */
   uint8_t     nNR;                 /**< Receive Sequence Number for numbered PDUs */


   union
   {
      struct
      {
         uint16_t    nInformationLength;  /**< Length of the information part of the PDU (if any) */
         uint8_t   * pInformation;        /**< Address of PDU information part (if any) */

      } sSDU;     /**< Service Data Unit present in UI and I PDUs */

      struct
      {
         uint8_t  nFlag;
         uint8_t  nType;
         uint8_t  nNS;
         uint8_t  nNR;
         uint8_t  nVS;
         uint8_t  nVR;
         uint8_t  nVSA;
         uint8_t  nVRA;

      } sFRMR;                /**< FRMR description */

      struct
      {
         uint8_t  nReason;

      } sDM;                  /**< DM description */


      struct
      {
         struct
         {
            bool_t bIsPresent;              /**< Indicates if the VERSION parameter is present */

            uint8_t  nMajor;              /**< LLCP VERSION major */
            uint8_t  nMinor;              /**< LLCP VERSION minor */

         } sVersion;

         struct
         {
            bool_t        bIsPresent;       /**< Indicates if the MIUX parameter is present */
            uint16_t    nMIUX;            /**< Maximum Information Unit Extension */

         } sMIUX;

         struct
         {
            bool_t        bIsPresent;       /**< Indicates if the WKS parameter is present */
            uint16_t    nWKS;             /**< Well known services bitfield */

         } sWKS;

         struct
         {
            bool_t        bIsPresent;       /**< Indicates if the LTO parameter is present */
            uint8_t     nLTO;             /**< Receive Window size */

         } sLTO;

         struct
         {
            bool_t        bIsPresent;       /**< Indicates if the RW parameter is present */
            uint8_t     nRW;              /**< Receive Window size */
         } sRW;

         struct
         {
            bool_t        bIsPresent;       /**< Indicates if the SN parameter is present */
            uint8_t *   pSN;              /**< Start address of the Service Name */
            uint8_t     nSNLength;        /**< Length of the Service Name */
         } sSN;

         struct
         {
            bool_t        bIsPresent;       /**< Indicates if the OPT parameter is present */
            uint8_t     nLSC;             /**< Link Service Class */
         } sOPT;

         struct
         {
            bool_t        bIsPresent;       /**< Indicates if the SDREQ parameter is present */
            uint8_t     nTID;             /**< The transaction Id */
            uint8_t *   pSN;              /**< Start address of the Service Name */
            uint8_t     nSNLength;        /**< Length of the Service Name */
         } sSDREQ;

         struct
         {
            bool_t        bIsPresent;       /**< Indicates if the SDRES parameter is present */
            uint8_t     nTID;             /**< The transaction Id */
            uint8_t     nSAP;             /**< The SAP value */

         } sSDRES;

      } sParams;               /**< Parameters description (PAX, CONNECT, CC PDUs only)*/

   } sPayload;
};

#define LLCP_DM_PDU_LENGTH                   3

#define LLCP_DM_REASON_DISC_CONFIRMED             0x00
#define LLCP_DM_REASON_NO_ACTIVE_CONNECTION       0x01
#define LLCP_DM_REASON_NO_SERVICE                 0x02
#define LLCP_DM_REASON_REJECTED                   0x03
#define LLCP_DM_REASON_PERMANENT_DSAP_REJECT      0x10
#define LLCP_DM_REASON_PERMANENT_REJECT           0x11
#define LLCP_DM_REASON_TEMPORARY_DSAP_REJECT      0x20
#define LLCP_DM_REASON_TEMPORARY_REJECT           0x21


#define LLCP_FRMR_PDU_LENGTH                 6

#define LLCP_FRMR_FLAG_W                     0x08
#define LLCP_FRMR_FLAG_I                     0x04
#define LLCP_FRMR_FLAG_R                     0x02
#define LLCP_FRMR_FLAG_S                     0x01

   /* LLCP parameters */

#define LLCP_PARAM_TYPE_OFFSET                  0
#define LLCP_PARAM_LENGTH_OFFSET                1
#define LLCP_PARAM_DATA_OFFSET                  2

#define LLCP_PARAM_VERSION_ID                0x01
#define LLCP_PARAM_VERSION_LENGTH            0x01

#define LLCP_PARAM_MIUX_ID                   0x02
#define LLCP_PARAM_MIUX_LENGTH               0x02
#define LLCP_PARAM_MIUX_DEFAULT_VALUE           0
#define LLCP_PARAM_MIUX_MIN_VALUE               0
#define LLCP_PARAM_MIUX_MAX_VALUE            2047

#define LLCP_PARAM_WKS_ID                    0x03
#define LLCP_PARAM_WKS_LENGTH                0x02

#define LLCP_PARAM_LTO_ID                    0x04
#define LLCP_PARAM_LTO_LENGTH                0x01
#define LLCP_PARAM_LTO_DEFAULT_VALUE         100      /* 100 ms */

#define LLCP_PARAM_RW_ID                     0x05
#define LLCP_PARAM_RW_LENGTH                 0x01
#define LLCP_PARAM_RW_DEFAULT                   1

#define LLCP_PARAM_SN_ID                     0x06
#define LLCP_PARAM_SN_MIN_LENGTH             0x01
#define LLCP_MAX_SN_LENGTH                   255

#define LLCP_PARAM_OPT_ID                    0x07
#define LLCP_PARAM_OPT_LENGTH                0x01

#define LLCP_PARAM_SDREQ_ID                  0x08
#define LLCP_PARAM_SDREQ_MIN_LENGTH          0x01

#define LLCP_PARAM_SDRES_ID                  0x09
#define LLCP_PARAM_SDRES_LENGTH              0x02

typedef void  tLinkDeactivationCallback (tContext * pContext, void * pCallbackParameter);

struct __tLLCPInstance
{
   /* LLCP configuration */

   bool_t          bAllowInitiatorMode;   /**< request activation of the P2P initiator */
   bool_t          bAllowActiveMode;       /**< request activation of the active mode */
   bool_t          bAllowTypeATargetProtocol;    /**< request activation of the type A protocol */
   uint16_t      nLocalLinkMIU;        /**< local link MIU */
   uint16_t      nLocalTimeoutMs;      /**< Local link timeout */
   uint16_t      nLocalWKS;

   uint8_t       nRole;
   uint8_t       nAgreedVersionMajor;
   uint8_t       nAgreedVersionMinor;
   uint8_t       nRemoteVersionMajor;
   uint8_t       nRemoteVersionMinor;

   uint16_t      nRemoteLinkMIU;
   uint16_t      nRemoteWKS;

   uint16_t      nRemoteTimeoutMs;     /**< Remote link timeout */
   uint32_t      nBaudRate;

   tLLCPSAPInstance sSAPInstance;      /**< SAP component */
   tLLCPSDPInstance sSDPInstance;      /**< SDP component */
   tLLCPCONNInstance sCONNInstance;    /**< CONN component */

   tLLCPPDUHeader * pRcvPDU;
   tDecodedPDU      sRcvPDUDescriptor;
   tDecodedPDU      sXmitPDUDescriptor;

   tLLCPPDUHeader * pFirstXmitPDU;
   tLLCPPDUHeader * pLastXmitPDU;
   uint16_t         nXmitPDU;

   uint32_t        nAllocatedPDU;     /**< Number of PDU allocated */

   tLinkDeactivationCallback * pLinkDeactivationCallback;
   void *          pCallbackParameter;

   bool_t bURILookupInProgress;
   tDFCDriverCCReference pURILookupCC;
   uint8_t nCurrentTID;

};

/**
 * @brief   Intiializes a LLCP instance.
 *
 * @param[in]  pLLCPInstance The LLCP instance to initialize.
 **/

void PLLCPCreate(
      tLLCPInstance* pLLCPInstance);

/**
 * @brief   Destroyes a LLCP instance.
 *
 * If the instance is already destroyed, the function does nothing and returns.
 *
 * @param[in]  pLLCPInstance  The LLCP instance to destroy.
 **/

void PLLCPDestroy(
      tLLCPInstance* pLLCPInstance );

/**
 * @brief   Builds LLCP configuration buffer, used for card emeul and reader registry
 *
 * @param[in]  pContext  The context
 *
 * @param[in]  pBuffer  The destination buffer.
 **/

uint8_t PLLCPBuildConfigurationBuffer(
      tContext * pContext,
      uint8_t * pBuffer);

/*
 * Processes MAC link activation event
 *
 * @param[in] pLLCPInstance  LLCP instance
 *
 * @param[in] nRole role of the local LLC
 *
 * @param[in] pRemoteParameters address of negociated parameters during Link activation procedure
 *
 * @param[in] nRemoteParametersLength  length of negociated parameters
 *
 * @param[in] pLinkDeactivationCallback the address of the link deactivation callback
 *
 * @param[in] pCallbackParameter the parameter of the callback
 *
 * @return W_SUCCESS if link activation event has been successfully processed
 */

W_ERROR PLLCPProcessLinkActivation (
  tContext                  * pContext,
  uint8_t                     nRole,
  uint8_t                   * pRemoteParameters,
  uint16_t                    nRemoteParametersLength,
  tLinkDeactivationCallback * pLinkDeactivationCallback,
  void *                      pCallbackParameter
);

/*
 * Request MAC link deactivation
 *
 * @param[in] pLLCPInstance  LLCP instance
 *
 * @return W_SUCCESS if the operation has been successfully completed
 */

void PLLCPRequestLinkDeactivation (
  tContext                  * pContext,
  tPBasicGenericCallbackFunction * pCallback,
  void * pCallbackParameter
);

/*
 * Processes MAC link deactivation event
 *
 * @param[in] pLLCPInstance  LLCP instance
 *
 * @return W_SUCCESS if the operation has been successfully completed
 */

W_ERROR PLLCPProcessLinkDeactivation (
   tContext                  * pContext
);


/*
 * Processes Incoming PDU
 *
 * @param[in] pContext       The context
 * @param[in] pRcvPDU        The received PDU
 *
 * @return    void
 */

void PLLCPProcessIncomingPDU(
   tContext        * pContext,
   tLLCPPDUHeader  * pRcvPDU
   );

#define IncMod16(nValue)                  (nValue) = (((nValue) + 1) & 0x0F)

void PLLCPFramerEnterXmitPacket(
   tContext         * pContext,
   tLLCPPDUHeader   * pPDU
   );

void PLLCPFramerEnterXmitPacketWithAck(
   tContext         * pContext,
   tLLCPPDUHeader   * pPDU,
   tPBasicGenericCallbackFunction * pCallback,
   void * pCallbackParameter
   );

tLLCPPDUHeader * PLLCPFramerGetNextXmitPDU(
   tContext         * pContext,
   bool_t               bPostCallback
);

void PLLCPFramerRemoveDiscardedPDU(
      tContext         * pContext);


/**
 * Initiates an outgoing connection
 * @param[in]   pContext        The context
 * @param[in]   pConnection     The connection
 * @param[in]   pURI            The destination URI
 * @param[in]   nURILength      The destination URI length
 * @param[in]   nSAP            The destination SAP
 *
 * @return
 * - W_SUCCESS on success
 * - W_ERROR_OUT_OF_RESOURCE
 **/

W_ERROR PLLCPConnect(
   tContext        * pContext,
   tLLCPConnection * pConnection,
   uint8_t         * pURI,
   uint32_t          nURILength,
   uint8_t           nSAP
   );

void PLLCPDisconnect(
   tContext        * pContext,
   tLLCPConnection * pConnection
   );

#endif /* (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) */

#endif /* __WME_P2P_LLCP_H */
