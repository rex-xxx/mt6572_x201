/*
 * Copyright (c) 2007-2011 Inside Secure, All Rights Reserved.
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

#ifndef __NFC_HAL_PROTOCOL_H
#define __NFC_HAL_PROTOCOL_H

/**
 * Parses the parameter NAL_PAR_POLICY.
 *
 * @param[in]  pBuffer  The parameter buffer.
 *
 * @param[in]  nLength  The buffer length
 *
 * @param[out] pbIsReaderLocked  The reader lock. W_TRUE if locked.
 *
 * @param[out] pbIsCardLocked  The card lock. W_TRUE if locked.
 *
 * @param[out] pnUICCCardPolicy  The UICC card policy.
 *
 * @param[out] pnUICCReaderPolicy  The UICC card policy.
 *
 * @param[out] pnSESwitchPosition  The SE switch posisiton.
 *
 * @param[out] pnSESlotIdentifier  The SE Slot identifier.
 *
 * @param[out] pnSECardPolicy  The SE card policy.
 **/
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_POLICY(
               const uint8_t* pBuffer,
               uint32_t nLength,
               bool_t* pbIsReaderLocked,
               bool_t* pbIsCardLocked,
               uint16_t* pnUICCCardPolicy,
               uint16_t* pnUICCReaderPolicy,
               uint32_t* pnSESwitchPosition,
               uint32_t* pnSESlotIdentifier,
               uint16_t* pnSECardPolicy,
               uint32_t* pnBattOff);

/**
 * Formats the parameter NAL_PAR_POLICY.
 *
 * @param[in] pBuffer  The parameter buffer.
 *
 * @param[in] bIsReaderLocked  The reader lock. W_TRUE if locked.
 *
 * @param[in] bIsCardLocked  The card lock. W_TRUE if locked.
 *
 * @param[in] nUICCCardPolicy  The UICC card policy.
 *
 * @param[in] nUICCReaderPolicy  The UICC card policy.
 *
 * @param[in] nSESwitchPosition  The SE switch posisiton.
 *
 * @param[in] nSESlotIdentifier  The SE Slot identifier.
 *
 * @param[in] nSECardPolicy  The SE card policy.
 *
 * @return The length of the NAL buffer
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_PAR_POLICY(
               uint8_t* pBuffer,
               bool_t bIsReaderLocked,
               bool_t bIsCardLocked,
               uint16_t nUICCCardPolicy,
               uint16_t nUICCReaderPolicy,
               uint32_t nSESwitchPosition,
               uint32_t nSESlotIdentifier,
               uint16_t nSECardPolicy);

/**
 * Formats the parameter NAL_PAR_HARDWARE_INFO.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] pNALParHardwareInfo  NAL_PAR_HARDWARE_INFO structure.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_PAR_HARDWARE_INFO(
               uint8_t* pBuffer,
               const tNALParHardwareInfo* pNALParHardwareInfo);

/**
 * Formats the parameter NAL_PAR_FIRMWARE_INFO.
 *
 * @param[in] pBuffer  The parameter buffer.
 *
 * @param[in] pNALParHardwareInfo  NAL_PAR_FIRMWARE_INFO structure.
 *
 * @param[in] nAutoStandbyTimeout  The timeout values for the standby mode.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_PAR_FIRMWARE_INFO(
               uint8_t* pBuffer,
               const tNALParFirmwareInfo* pNALParFirmwareInfo,
               uint16_t nAutoStandbyTimeout);

/**
 * Formats the parameter NAL_PAR_DETECT_PULSE.
 *
 * @param[in] pBuffer  The parameter buffer.
 *
 * @param[in] nCardDetectPulse  Card detect pulse.
 *
 **/
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_DETECT_PULSE(
               const uint8_t* pBuffer,
               uint32_t nLength,
               uint16_t *pnCardDetectPulse);

/**
 * Parses the parameter NAL_PAR_PERSISTENT_MEMORY.
 *
 * @param[in] pBuffer  The parameter buffer.
 *
 * @param[in] nLength  The buffer length.
 *
 * @param[out] pPersistentStorage  The parameter buffer.
 *
 * @return NAL_RES_OK if the mapping is successfull.
 **/
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_PERSISTENT_MEMORY(
               const uint8_t* pBuffer,
               uint32_t nLength,
               uint8_t** ppPersistentStorage);

/**
 * Formats the parameter NAL_PAR_PERSISTENT_MEMORY.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] pPersistentStorage  The parameter buffer.
 *
 * @param[in] nPersistentStorageLength  The parameter buffer length
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_PAR_PERSISTENT_MEMORY(
               uint8_t* pBuffer,
               const uint8_t  aPersistentStorage[NAL_PERSISTENT_MEMORY_SIZE]);

/**
 * Parses the parameter NAL_PAR_READER_CONFIG for Reader ISO 14443 A Part 4.
 *
 * @param[in] pBuffer  The parameter buffer.
 *
 * @param[in] nLength  The buffer length.
 *
 * @param[out] pnMaxDataRate  The maximum data rate usable for the transaction.
 *
 * @param[out] pbIsCID  The flag for the CID usage see.
 *
 * @param[out] pnCID  The CID value.
 *
 * @param[out] pnFSD  The FSD code is the size in bytes of the Reader input buffer.
 *
 * @return NAL_RES_OK if the mapping is successfull.
 **/
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_READER_CONFIG_14_A_4(
               const uint8_t* pBuffer,
               uint32_t nLength,
               uint8_t* pnMaxDataRate,
               bool_t* pbIsCID,
               uint8_t* pnCID,
               uint8_t* pnFSD);

/**
 * Parses the parameter NAL_PAR_READER_CONFIG for Reader ISO 14443 B Part 3.
 *
 * @param[in] pBuffer  The parameter buffer.
 *
 * @param[in] nLength  The buffer length.
 *
 * @param[out] pnMaxDataRate  The maximum data rate usable for the transaction.
 *
 * @param[out] pnAFI  The Application Family Identifier.
 *
 * @param[out] pbIsCID  The flag for the CID usage see.
 *
 * @param[out] pnCID  The CID value.
 *
 * @param[out] pnFSD  The FSD code is the size in bytes of the Reader input buffer.
 *
 * @param[out] pHigherLayerData  The HIGHER_LAYER_DATA.
 *
 * @param[out] nHigherLayerDataLength  The pHigherLayerData length.
 *
 * @return NAL_RES_OK if the mapping is successfull.
 **/
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_READER_CONFIG_14_B_3(
               const uint8_t* pBuffer,
               uint32_t nLength,
               uint8_t* pnMaxDataRate,
               uint8_t* pnAFI,
               bool_t* pbIsCID,
               uint8_t* pnCID,
               uint8_t* pnFSD,
               uint8_t** pHigherLayerData,
               uint32_t* pnHigherLayerDataLength);

/**
 * Parses the parameter NAL_PAR_READER_CONFIG for Reader ISO 15693 Part 3.
 *
 * @param[in] pBuffer  The parameter buffer.
 *
 * @param[in] nLength  The buffer length.
 *
 * @param[out] pnAFI  The Application Family Identifier.
 *
 * @return NAL_RES_OK if the mapping is successfull.
 **/
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_READER_CONFIG_15_3(
               const uint8_t* pBuffer,
               uint32_t nLength,
               uint8_t* pnAFI);

/**
 * Parses the parameter NAL_PAR_READER_CONFIG for Reader FeliCa.
 *
 * @param[in] pBuffer  The parameter buffer.
 *
 * @param[in] nLength  The buffer length.
 *
 * @param[out] pnSystemCode  The system code bytes to use for the polling command.
 *
 * @return NAL_RES_OK if the mapping is successfull.
 **/
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_READER_CONFIG_FELICA(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        uint16_t* pnSystemCode);

/**
 * Parses the parameter NAL_PAR_READER_CONFIG for Reader B Prime.
 *
 * @param[in] pBuffer  The parameter buffer.
 *
 * @param[in] nLength  The buffer length.
 *
 * @return NAL_RES_OK if the mapping is successfull.
 **/
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_READER_CONFIG_B_PRIME(
                        const uint8_t* pBuffer,
                        uint32_t nLength);

/**
 * Parses the parameter NAL_PAR_CARD_CONFIG for Card Emulation ISO 14443 A Part 4.
 *
 * @param[in] pBuffer  The parameter buffer.
 *
 * @param[in] nLength  The buffer length.
 *
 * @param[out] pnUID
 *
 * @param[out] pnUIDLen  The UID length in bytes.
 *
 * @param[out] pnATQAMsb  The MSB of the ATQA.
 *
 * @param[out] pnTO
 *
 * @param[out] pnTA
 *
 * @param[out] pnTB
 *
 * @param[out] pnTC
 *
 * @param[out] ApplicationData
 *
 * @param[out] pnApplicationDataLength  The ApplicationData length.
 *
 * @return NAL_RES_OK if the mapping is successfull.
 **/
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_CARD_CONFIG_14_A_4(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        uint8_t* pnUID,
                        uint8_t* pnUIDLen,
                        uint8_t* pnATQAMsb,
                        uint8_t* pnTO,
                        uint8_t* pnTA,
                        uint8_t* pnTB,
                        uint8_t* pnTC,
                        uint8_t** pnApplicationData,
                        uint32_t* pnApplicationDataLength);

/**
 * Parses the parameter NAL_PAR_CARD_CONFIG for Card Emulation ISO 14443 B Part 4.
 *
 * @param[in] pBuffer  The parameter buffer.
 *
 * @param[in] nLength  The buffer length.
 *
 * @param[out] pnATQB
 *
 * @param[out] pnATTRIB
 *
 * @param[out] pnATTRIBLength  The pnATTRIB length.
 *
 * @return NAL_RES_OK if the mapping is successfull.
 **/
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_CARD_CONFIG_14_B_4(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        uint8_t* pATQB,
                        uint8_t** pATTRIB,
                        uint32_t* pnATTRIBLength);

/**
 * Parses the parameter NAL_PAR_P2P_INITIATOR_LINK_PARAMETERS.
 *
 * @param[in] pBuffer  The parameter buffer.
 *
 * @param[in] nLength  The buffer length.
 *
 * @param[out] pParameters  The parameter buffer.
 *
 * @param[out] pnParametersLength  The pointer to the length of the parameter buffer.
 *
 * @return NAL_RES_OK if the mapping is successfull.
 **/
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_P2P_INITIATOR_LINK_PARAMETERS(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        uint8_t **pParameters,
                        uint32_t *pnPamaetersLength);

/**
 * Parses the parameter NAL_PAR_CARD_CONFIG for P2P target.
 *
 * @param[in] pBuffer  The parameter buffer.
 *
 * @param[in] nLength  The buffer length.
 *
 * @param[out] pnRTX  The pointer to the reception timeout.
 *
 * @param[out] pbAllowTypeATargetProtocol  The pointer to a boolean indicating if Active P2P is enabled.
 *
 * @param[out] pParameters  The pointer to a boolean indicating if TypeA is enabled for P2P target.
 *
 * @param[out] pParameters  The parameter buffer.
 *
 * @param[out] pnParametersLength  The pointer to the length of the parameter buffer.
 *
 * @return NAL_RES_OK if the mapping is successfull.
 **/
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_CARD_CONFIG_P2P_TARGET(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        uint32_t* pnRTX,
                        bool_t* pbAllowTypeATargetProtocol,
                        bool_t* pbAllowActiveMode,
                        uint8_t** pParameters,
                        uint32_t * pnParametersLength);

/**
 * Formats the parameter NAL_PAR_UICC_SWP.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] pParameter  The parameter buffer.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_PAR_UICC_SWP(
                        uint8_t* pBuffer,
                        uint32_t nStatus);

/**
 * Formats the parameter NAL_PAR_UICC_READER_PROTOCOLS.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] bDetect  The buffer length.
 *
 * @param[in] nOpenProtocols  The parameter buffer.
 *
 * @param[in] nDataRateMax  The parameter buffer.
 *
 * @param[in] nAFI  The parameter buffer.
 *
 * @param[in] pnHigherLayerData  The parameter buffer.
 *
 * @param[in] nHigherLayerDataLength  The parameter buffer.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_PAR_UICC_READER_PROTOCOLS(
                        uint8_t* pBuffer,
                        bool_t bDetect,
                        uint16_t nOpenProtocols,
                        uint8_t nDataRateMax,
                        uint8_t nAFI,
                        const uint8_t* pnHigherLayerData,
                        uint32_t nHigherLayerDataLength);

/**
 * Formats the parameter NAL_PAR_UICC_CARD_PROTOCOLS.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] nOpenProtocols
 *
 * @param[in] pnUID
 *
 * @param[in] nUIDLength
 *
 * @param[in] nSAK
 *
 * @param[in] nATQA
 *
 * @param[in] pnApplicationData
 *
 * @param[in] nFWI_SFGI
 *
 * @param[in] nCIDSupport
 *
 * @param[in] nDataRateMax
 *
 * @param[in] nMode
 *
 * @param[in] nPUPI
 *
 * @param[in] nAFI
 *
 * @param[in] nATQB
 *
 * @param[in] pnHigherLayerResponse
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_PAR_UICC_CARD_PROTOCOLS(
                        uint8_t* pBuffer,
                        uint16_t nOpenProtocols,
                        uint8_t nCardA4Mode,
                        const uint8_t* pnUID,
                        uint8_t nUIDLength,
                        uint8_t nSAK,
                        uint16_t nATQA,
                        const uint8_t* pnApplicationData,
                        uint8_t nFWI_SFGI,
                        uint8_t nCIDSupport,
                        uint8_t nDataRateMax,
                        uint8_t nCardB4Mode,
                        uint32_t nPUPI,
                        uint8_t nAFI,
                        uint32_t nATQB,
                        const uint8_t* pnHigherLayerResponse);


/**
 * Parses the parameter NAL_PAR_ROUTING_TABLE_CONFIG
 *
 * @param[in] pBuffer  The parameter buffer.
 *
 * @param[in] nLength  The buffer length.
 *
 * @param[out] pbIsEnabled  The pointer to the enable flag
 * 
 * @return NAL_RES_OK if the mapping is successfull.
 **/
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_ROUTING_TABLE_CONFIG(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        bool_t* pbIsEnabled);

/**
 * Formats the parameter NAL_PAR_ROUTING_TABLE_CONFIG.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] bIsEnabled 
 *
 * @return The length in bytes of the data formatted.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_PAR_ROUTING_TABLE_CONFIG(
                        uint8_t* pBuffer,
                        bool_t	bIsEnabled);


/**
 * Parses the parameter NAL_PAR_ROUTING_TABLE_ENTRIES
 *
 * @param[in] pBuffer  The parameter buffer.
 *
 * @param[in] nLength  The buffer length.
 *
 * @param[out] pRoutingTable  The pointer routing table entries

 * @param[out] pnLength  The length of the routing table entries
 * 
 * @return NAL_RES_OK if the mapping is successfull.
 **/
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_PAR_ROUTING_TABLE_ENTRIES(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        uint8_t* pRoutingTable,
								uint8_t * pnLength);

/**
 * Formats the parameter NAL_PAR_ROUTING_TABLE_CONFIG.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] pRoutingTable The routing table 
 *
 * @return The length in bytes of the data formatted.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_PAR_ROUTING_TABLE_ENTRIES(
                        uint8_t* pBuffer,
								const uint8_t* pRoutingTable,
								uint8_t  nRoutingTableLength);

								 
/* ************************************************************************** */
/*                             Event Codes                                    */
/* ************************************************************************** */

/**
 * The event NAL_EVT_STANDBY_MODE.
 *
 * @param[in] pBuffer  The parameter buffer.
 *
 * @param[out] bStandbyFlag 0: if the NFC Controller should only enter standby
 *                         mode if no reader mode is active.
 *                         1: if the NFC Controller can enter standby mode even
 *                         if the reader mode is active.
 *
 *
 * @return NAL_RES_OK if the mapping is successfull.
 *
 **/
NFC_HAL_INTERNAL uint8_t PNALProtocolParse_NAL_EVT_STANDBY_MODE(
                        const uint8_t* pBuffer,
                        uint32_t nLength,
                        bool_t* bStandbyFlag);

/**
 * Formats the event NAL_EVT_READER_TARGET_COLLISION.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] pnParameters
 *
 * @param[in] nParametersLength  The buffer length.
 *
 * @return The length in bytes of the data formatted.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_READER_TARGET_COLLISION(
                        uint8_t* pBuffer,
                        const uint16_t *pReaderProtocols,
                        uint32_t  nReaderProtocols);

/**
 * Formats the event NAL_EVT_READER_TARGET_DISCOVERED for Reader ISO 14443 A Part 3.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] nATQA  The ATQA parameter.
 *
 * @param[in] nSAK  The SAK parameter.
 *
 * @param[in] pnUID  The UID parameter.
 *
 * @param[in] nUIDLength The UID parameter length.
 *
 * @return The length in bytes of the data formatted.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_14_A_3(
                        uint8_t* pBuffer,
                        uint16_t nATQA,
                        uint8_t nSAK,
                        const uint8_t* pnUID,
                        uint32_t nUIDLength);

/**
 * Formats the event NAL_EVT_READER_TARGET_DISCOVERED for Reader ISO 14443 A Part 4.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] nATQA  The ATQA parameter.
 *
 * @param[in] nSAK  The SAK parameter.
 *
 * @param[in] pnATS  The ATS frame.
 *
 * @param[in] nATSLength The ATS frame length.
 *
 * @param[in] nUID The UID parameter.
 *
 * @param[in] nUIDLength The UID parameter length.
 *
 * @return The length in bytes of the data formatted.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_14_A_4(
                        uint8_t* pBuffer,
                        uint16_t nATQA,
                        uint8_t nSAK,
                        const uint8_t* pATS,
                        uint32_t nATSLength,
                        const uint8_t * pApplicationData,
                        uint32_t nApplicationDataLength,
                        const uint8_t* pUID,
                        uint32_t nUIDLength);

/**
 * Formats the event NAL_EVT_READER_TARGET_DISCOVERED for Reader ISO 14443 B Part 3.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] pnATQB  The ATQB frame from byte 2 to byte 12.
 *
 * @param[in] pnATTRIB  The answer to ATTRIB frame, excluding the CRC.
 *
 * @param[in] nATTRIBLength  The pnATTRIB length.
 *
 * @return The length in bytes of the data formatted.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_14_B_3(
                        uint8_t* pBuffer,
                        uint8_t  aATQB[11],
                        const uint8_t* pATTRIB,
                        uint32_t nATTRIBLength);

/**
 * Formats the event NAL_EVT_READER_TARGET_DISCOVERED for Reader ISO 15693 Part 3.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] nFlag  The flags.
 *
 * @param[in] nDSFID  The Data storage format identifier.
 *
 * @param[in] nUID  The UID.
 *
 * @return The length in bytes of the data formatted.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_15_3(
                        uint8_t* pBuffer,
                        uint8_t nFlag,
                        uint8_t nDSFID,
                        uint8_t pnUID[8]);

/**
 * Formats the event NAL_EVT_READER_TARGET_DISCOVERED for Reader Type 1.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] pnUID  The UID of the selected card.
 *
 * @param[in] nHR  The HR bytes of the selected card.
 *
 * @param[in] nATQA  The ATQA of the selected card.
 *
 * @return The length in bytes of the data formatted.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_TYPE1(
                        uint8_t* pBuffer,
                        const uint8_t* pUID,
                        const uint8_t* pHR,
                        uint16_t nATQA);

/**
 * Formats the event NAL_EVT_READER_TARGET_DISCOVERED for 4Reader FeliCa.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] pnIdPm  The ID and PM included in the polling response of the selected card.
 *
 * @param[in] nSystemCode  The system code returned by the card.
 *
 * @return The length in bytes of the data formatted.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_FELICA(
                        uint8_t* pBuffer,
                        uint8_t pnIdPm[16],
                        uint16_t nSystemCode);

/**
 * Formats the event NAL_EVT_READER_TARGET_DISCOVERED for Reader B PRIME.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] pAPGENBuffer buffer containing payload of APGEN
 *
 * @param[in] nAPGENLength  The APPGEN payload Length
 *
 * @return The length in bytes of the data formatted.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_BPRIME(
                        uint8_t* pBuffer,
                        uint8_t* pAPGENBuffer,
                        uint32_t nAPGENLength
                        );

/**
 * Formats the event NAL_EVT_READER_TARGET_DISCOVERED for Reader KOVIO.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] pDataBuffer buffer containing data coming from tag kovio
 *
 * @param[in] nDataLength  The data Length
 *
 * @return The length in bytes of the data formatted.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_READER_TARGET_DISCOVERED_KOVIO(
                        uint8_t* pBuffer,
                        uint8_t* pDataBuffer,
                        uint32_t nDataLength
                        );

/**
 * Formats the event NAL_EVT_UICC_DETECTION_REQUEST.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] bUICC
 *
 * @param[in] nReaderProtocolOpened
 *
 * @param[in] nDataRateMax
 *
 * @param[in] nAFI
 *
 * @param[in] pnHigherLayerDara
 *
 * @param[in] nHigherLayerDaraLength
 *
 * @return The length in bytes of the data formatted.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_UICC_DETECTION_REQUEST (
                        uint8_t* pBuffer,
                        bool_t bUICC,
                        uint16_t nReaderProtocolOpened,
                        uint8_t nDataRateMax,
                        uint8_t nAFI,
                        const uint8_t* pnHigherLayerDara,
                        uint32_t nHigherLayerDaraLength);

/**
 * Formats the event NAL_EVT_CARD_SELECTED for Card Emulation ISO 14443 A Part 4.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] nDataRate  The data rate used for the communication with the remote reader.
 *
 * @param[in] nCID  The CID Parameter.
 *
 * @param[in] pnUID  The UID value.
 *
 * @param[in] nUIDLength  The UID length in bytes. This value may be 4, 7 or 10.
 *
 * @return The length in bytes of the data formatted.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_CARD_SELECTED_14_A_4(
                        uint8_t* pBuffer,
                        uint8_t  nDataRate,
                        uint8_t  nCID,
                        uint8_t  aUID[10],
                        uint32_t nUIDLength);

/**
 * Formats the event NAL_EVT_CARD_SELECTED for Card Emulation ISO 14443 B Part 4.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] nAFI  The Application Family Identifier.
 *
 * @param[in] nDataRate  The data rate used for the communication.
 *
 * @param[in] nCID  The CID Parameter.
 *
 * @param[in] nFrameSize  The Frame Size parameter.
 *
 * @param[in] pnPUPI  The PUPI sent in the ATQB.
 *
 * @param[in] pnHigherLayerDara The HIGHER_LAYER_DATA.
 *
 * @param[in] nHigherLayerDaraLength The HIGHER_LAYER_DATA length.
 *
 * @return The length in bytes of the data formatted.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_CARD_SELECTED_14_B_4(
                        uint8_t* pBuffer,
                        uint8_t nAFI,
                        uint8_t nDataRate,
                        uint8_t nCID,
                        uint8_t nFrameSize,
                        uint8_t aPUPI[4],
                        const uint8_t* pnHigherLayerData,
                        uint32_t nHigherLayerDataLength);

/**
 * Parses/Formats the parameter NAL_EVT_CARD_SEND_DATA.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] pSendData  The parameter buffer.
 *
 * @param[in] nSendDataLength  The pSendData length.
 *
 * @return The length in bytes of the data formatted.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocol_NAL_EVT_CARD_SEND_DATA(
                        uint8_t* pBuffer,
                        const uint8_t* pSendData,
                        uint32_t nSendDataLength);

/**
 * Formats the parameter NAL_EVT_CARD_END_OF_TRANSACTION.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] bDeSelection  0 if the reader sent a de-selection command, 1 for
 *                            any other reason.
 *
 **/
NFC_HAL_INTERNAL void PNALProtocolFormat_NAL_EVT_CARD_END_OF_TRANSACTION(
                        uint8_t* pBuffer,
                        bool_t bDeSelection);

/**
 * Formats the parameter NAL_EVT_SE_CARD_EOT.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] nCardProtocolBitField  A card protocol bit field with one and only
 *                                   one bit set for the card protocol used by
 *                                   the SE.
 *
 * @param[in] pnAID The AID of N bytes used for the transaction.
 *
 * @param[in] nAIDLength The pnAID length.
 *
 * @return The length in bytes of the data formatted.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_SE_CARD_EOT(
                        uint8_t* pBuffer,
                        uint16_t nCardProtocolBitField,
                        const uint8_t* pAID,
                        uint32_t nAIDLength);

/**
 * Formats the parameter NAL_EVT_P2P_TARGET_DISCOVERED.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] pnLink  The link configuration parameters sent by the target in the
 *                    answer to the link activation message.
 *
 * @param[in] nLinkLength The pnLink length.
 *
 * @return The length in bytes of the data formatted.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_P2P_TARGET_DISCOVERED(
                        uint8_t* pBuffer,
                        const uint8_t* pLink,
                        uint32_t nLinkLength);

/**
 * Formats the parameter NAL_EVT_P2P_INITIATOR_DISCOVERED.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] pnLink  The link configuration parameters sent by the Initiator in
 *                    the link activation message.
 *
 * @param[in] nLinkLength The pnLink length.
 *
 * @return The length in bytes of the data formatted.
 *
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_P2P_INITIATOR_DISCOVERED(
                        uint8_t* pBuffer,
                        const uint8_t* pLink,
                        uint32_t nLinkLength);

/**
 * Formats the parameter NAL_EVT_UICC_CONNECTIVITY.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] pnPayload  The payload of the connectivity message.
 *
 * @param[in] nPayloadLength The pnPayload length.
 *
 * @return The length in bytes of the data formatted.
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_UICC_CONNECTIVITY(
                        uint8_t* pBuffer,
                        const uint8_t* pPayload,
                        uint32_t nPayloadLength);

/**
 * Formats the parameter NAL_EVT_RF_FIELD.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] bExternalRfField  1 if the external RF field is ON, 0 if the RF
 *                              field is OFF.
 **/
NFC_HAL_INTERNAL void PNALProtocolFormat_NAL_EVT_RF_FIELD(
                        uint8_t* pBuffer,
                        bool_t bExternalRfField);

/**
 * Formats the parameter NAL_EVT_NFCC_ERROR.
 *
 * @param[out] pBuffer  The parameter buffer.
 *
 * @param[in] nCause  The error cause.
 *
 * @return The length in bytes of the data formatted.
 **/
NFC_HAL_INTERNAL uint32_t PNALProtocolFormat_NAL_EVT_NFCC_ERROR(
                        uint8_t* pBuffer,
                        uint32_t nCause);

#endif /* __NFC_HAL_PROTOCOL_H */

