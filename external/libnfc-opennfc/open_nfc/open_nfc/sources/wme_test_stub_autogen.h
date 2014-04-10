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
 File auto-generated with the autogen.exe tool - Do not modify manually
 The autogen.exe binary tool, the generation scripts and the files used
 for the source of the generation are available under Apache License, Version 2.0
 ******************************************************************************/

#ifndef __WME_TEST_STUB_AUTOGEN_H
#define __WME_TEST_STUB_AUTOGEN_H


#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
typedef void tW14Part3ExchangeData(W_HANDLE hConnection, tWBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pReaderToCardBuffer, uint32_t nReaderToCardBufferLength, uint8_t * pCardToReaderBuffer, uint32_t nCardToReaderBufferMaxLength, W_HANDLE * phOperation);
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

typedef void tW14Part3ExchangeRawBits(W_HANDLE hConnection, tWBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pReaderToCardBuffer, uint32_t nReaderToCardBufferLength, uint8_t nReaderToCardBufferLastByteBitNumber, uint8_t * pCardToReaderBuffer, uint32_t nCardToReaderBufferMaxLength, uint8_t nExpectedBits, W_HANDLE * phOperation);
typedef W_ERROR tW14Part3GetConnectionInfo(W_HANDLE hConnection, tW14Part3ConnectionInfo * p14Part3ConnectionInfo);
typedef W_ERROR tW14Part3ListenToCardDetectionTypeB(tWReaderCardDetectionHandler * pHandler, void * pHandlerParameter, uint8_t nPriority, uint8_t nAFI, bool_t bUseCID, uint8_t nCID, uint32_t nBaudRate, const uint8_t * pHigherLayerDataBuffer, uint8_t nHigherLayerDataLength, W_HANDLE * phEventRegistry);
typedef W_ERROR tW14Part3SetTimeout(W_HANDLE hConnection, uint32_t nTimeout);

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
typedef void tW14Part4ExchangeData(W_HANDLE hConnection, tWBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pReaderToCardBuffer, uint32_t nReaderToCardBufferLength, uint8_t * pCardToReaderBuffer, uint32_t nCardToReaderBufferMaxLength, W_HANDLE * phOperation);
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

typedef W_ERROR tW14Part4GetConnectionInfo(W_HANDLE hConnection, tW14Part4ConnectionInfo * p14Part4ConnectionInfo);
typedef W_ERROR tW14Part4ListenToCardDetectionTypeA(tWReaderCardDetectionHandler * pHandler, void * pHandlerParameter, uint8_t nPriority, bool_t bUseCID, uint8_t nCID, uint32_t nBaudRate, W_HANDLE * phEventRegistry);
typedef W_ERROR tW14Part4ListenToCardDetectionTypeB(tWReaderCardDetectionHandler * pHandler, void * pHandlerParameter, uint8_t nPriority, uint8_t nAFI, bool_t bUseCID, uint8_t nCID, uint32_t nBaudRate, const uint8_t * pHigherLayerDataBuffer, uint8_t nHigherLayerDataLength, W_HANDLE * phEventRegistry);
typedef W_ERROR tW14Part4SetNAD(W_HANDLE hConnection, uint8_t nNAD);
typedef W_ERROR tW15GetConnectionInfo(W_HANDLE hConnection, tW15ConnectionInfo * pConnectionInfo);
typedef W_ERROR tW15IsWritable(W_HANDLE hConnection, uint8_t nSectorIndex);
typedef W_ERROR tW15ListenToCardDetection(tWReaderCardDetectionHandler * pHandler, void * pHandlerParameter, uint8_t nPriority, uint8_t nAFI, W_HANDLE * phEventRegistry);
typedef void tW15Read(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, W_HANDLE * phOperation);
typedef void tW15SetAttribute(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nActions, uint8_t nAFI, uint8_t nDSFID, W_HANDLE * phOperation);
typedef W_ERROR tW15SetTagSize(W_HANDLE hConnection, uint16_t nSectorNumber, uint8_t nSectorSize);
typedef void tW15Write(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, bool_t bLockSectors, W_HANDLE * phOperation);
typedef void tW7816ExchangeAPDU(W_HANDLE hChannel, tWBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pSendAPDUBuffer, uint32_t nSendAPDUBufferLength, uint8_t * pReceivedAPDUBuffer, uint32_t nReceivedAPDUBufferMaxLength, W_HANDLE * phOperation);
typedef W_ERROR tW7816GetAid(W_HANDLE hChannel, uint8_t * pBuffer, uint32_t nBufferMaxLength, uint32_t * pnActualLength);
typedef W_ERROR tW7816GetATR(W_HANDLE hConnection, uint8_t * pBuffer, uint32_t nBufferMaxLength, uint32_t * pnActualLength);
typedef W_ERROR tW7816GetATRSize(W_HANDLE hConnection, uint32_t * pnSize);
typedef W_ERROR tW7816GetResponseAPDUData(W_HANDLE hChannel, uint8_t * pReceivedAPDUBuffer, uint32_t nReceivedAPDUBufferMaxLength, uint32_t * pnReceivedAPDUActualLength);
typedef void tW7816OpenChannel(W_HANDLE hConnection, tWBasicGenericHandleCallbackFunction * pCallback, void * pCallbackParameter, uint32_t nType, const uint8_t * pAID, uint32_t nAIDLength, W_HANDLE * phOperation);

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
typedef void tW7816OpenLogicalChannel(W_HANDLE hConnection, tWBasicGenericHandleCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pAID, uint32_t nAIDLength, W_HANDLE * phOperation);
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

typedef void tWBasicCancelOperation(W_HANDLE hOperation);
typedef W_ERROR tWBasicCheckConnectionProperty(W_HANDLE hConnection, uint8_t nPropertyIdentifier);
typedef void tWBasicCloseHandle(W_HANDLE hHandle);
typedef void tWBasicCloseHandleSafe(W_HANDLE hHandle, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);
typedef W_ERROR tWBasicGetConnectionProperties(W_HANDLE hConnection, uint8_t * pPropertyArray, uint32_t nArrayLength);
typedef const char * tWBasicGetConnectionPropertyName(uint8_t nPropertyIdentifier);
typedef W_ERROR tWBasicGetConnectionPropertyNumber(W_HANDLE hConnection, uint32_t * pnPropertyNumber);
typedef const char * tWBasicGetErrorString(W_ERROR nError);
typedef W_ERROR tWBPrimeGetConnectionInfo(W_HANDLE hConnection, tWBPrimeConnectionInfo * pBPrimeConnectionInfo);
typedef W_ERROR tWBPrimeListenToCardDetection(tWReaderCardDetectionHandler * pHandler, void * pHandlerParameter, const uint8_t * pAPGENBuffer, uint32_t nAPGENLength, W_HANDLE * phEventRegistry);
typedef W_ERROR tWBPrimeSetTimeout(W_HANDLE hConnection, uint32_t nTimeout);
typedef void tWEmulClose(W_HANDLE hHandle, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);
typedef W_ERROR tWEmulGetMessageData(W_HANDLE hHandle, uint8_t * pDataBuffer, uint32_t nDataLength, uint32_t * pnActualDataLength);
typedef bool_t tWEmulIsPropertySupported(uint8_t nPropertyIdentifier);
typedef void tWEmulOpenConnection(tWBasicGenericCallbackFunction * pOpenCallback, void * pOpenCallbackParameter, tWBasicGenericEventHandler * pEventCallback, void * pEventCallbackParameter, tWEmulCommandReceived * pCommandCallback, void * pCommandCallbackParameter, tWEmulConnectionInfo * pEmulConnectionInfo, W_HANDLE * phHandle);
typedef W_ERROR tWEmulSendAnswer(W_HANDLE hDriverConnection, const uint8_t * pDataBuffer, uint32_t nDataLength);
typedef W_ERROR tWFeliCaGetCardList(W_HANDLE hConnection, tWFeliCaConnectionInfo * aFeliCaConnectionInfos, const uint32_t nArraySize);
typedef W_ERROR tWFeliCaGetCardNumber(W_HANDLE hConnection, uint32_t * pnCardNumber);

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
typedef W_ERROR tWFeliCaGetConnectionInfo(W_HANDLE hConnection, tWFeliCaConnectionInfo * pConnectionInfo);
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

typedef W_ERROR tWFeliCaListenToCardDetection(tWReaderCardDetectionHandler * pHandler, void * pHandlerParameter, uint8_t nPriority, uint16_t nSystemCode, W_HANDLE * phEventRegistry);
typedef void tWFeliCaRead(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nLength, uint8_t nNumberOfService, const uint16_t * pServiceCodeList, uint8_t nNumberOfBlocks, const uint8_t * pBlockList, W_HANDLE * phOperation);
typedef void tWFeliCaSelectCard(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const tWFeliCaConnectionInfo * pFeliCaConnectionInfo);
typedef void tWFeliCaSelectSystem(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t nIndexSubSystem);
typedef void tWFeliCaWrite(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nLength, uint8_t nNumberOfService, const uint16_t * pServiceCodeList, uint8_t nNumberOfBlocks, const uint8_t * pBlockList, W_HANDLE * phOperation);
typedef W_ERROR tWHandoverAddBluetoothCarrier(W_HANDLE hConnectionHandover, tWBTPairingInfo * pBluetoothInfo, uint8_t nCarrierPowerState);
typedef W_ERROR tWHandoverAddWiFiCarrier(W_HANDLE hConnectionHandover, tWWiFiPairingInfo * pWiFiInfo, uint8_t nCarrierPowerState);
typedef W_ERROR tWHandoverCreate(W_HANDLE * phMessage);
typedef void tWHandoverFormatTag(W_HANDLE hConnectionHandover, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint32_t nActionMask, W_HANDLE * phOperation);
typedef W_ERROR tWHandoverGetBluetoothInfo(W_HANDLE hConnectionHandover, tWBTPairingInfo * pRemoteInfo);
typedef W_ERROR tWHandoverGetPairingInfo(W_HANDLE hConnectionHandover, tWHandoverPairingInfo * pPairingInfo);
typedef W_ERROR tWHandoverGetPairingInfoLength(W_HANDLE hConnectionHandover, uint32_t * pnLength);
typedef W_ERROR tWHandoverGetWiFiInfo(W_HANDLE hOperation, tWWiFiPairingInfo * pWiFiInfo);
typedef void tWHandoverPairingCompletion(W_HANDLE hHandoverConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, W_HANDLE * phOperation);
typedef void tWHandoverPairingStart(W_HANDLE hConnectionHandover, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint32_t nMode, W_HANDLE * phOperation);
typedef W_ERROR tWHandoverRemoveAllCarrier(W_HANDLE hHandoverConnection);
typedef void tWMifareClassicAuthenticate(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint16_t nSectorNumber, bool_t bWithKeyA, const uint8_t * pKey, uint8_t nKeyLength);
typedef W_ERROR tWMifareClassicGetConnectionInfo(W_HANDLE hConnection, tWMifareClassicConnectionInfo * pConnectionInfo);
typedef W_ERROR tWMifareClassicGetSectorInfo(W_HANDLE hConnection, uint16_t nSectorNumber, tWMifareClassicSectorInfo * pSectorInfo);
typedef void tWMifareClassicReadBlock(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint16_t nBlockNumber, uint8_t * pBuffer, uint8_t nBufferLength);
typedef void tWMifareClassicWriteBlock(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint16_t nBlockNumber, const uint8_t * pBuffer, uint8_t nBufferLength);
typedef W_ERROR tWMifareGetConnectionInfo(W_HANDLE hConnection, tWMifareConnectionInfo * pConnectionInfo);
typedef void tWMifareRead(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, W_HANDLE * phOperation);
typedef void tWMifareULCAuthenticate(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pKey, uint32_t nKeyLength);
typedef void tWMifareULCSetAccessRights(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pKey, uint32_t nKeyLength, uint8_t nThreshold, uint32_t nRights, bool_t bLockConfiguration);
typedef W_ERROR tWMifareULForceULC(W_HANDLE hConnection);
typedef void tWMifareULFreezeDataLockConfiguration(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);
typedef W_ERROR tWMifareULGetAccessRights(W_HANDLE hConnection, uint32_t nOffset, uint32_t nLength, uint32_t * pnRights);
typedef void tWMifareULRetrieveAccessRights(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);
typedef void tWMifareWrite(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, bool_t bLockSectors, W_HANDLE * phOperation);
typedef W_ERROR tWMyDGetConnectionInfo(W_HANDLE hConnection, tWMyDConnectionInfo * pConnectionInfo);
typedef void tWMyDMoveAuthenticate(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint32_t nPassword);
typedef void tWMyDMoveFreezeDataLockConfiguration(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);
typedef void tWMyDMoveGetConfiguration(W_HANDLE hConnection, tWMyDMoveGetConfigurationCompleted * pCallback, void * pCallbackParameter);
typedef void tWMyDMoveSetConfiguration(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nStatusByte, uint8_t nPasswordRetryCounter, uint32_t nPassword, bool_t bLockConfiguration);
typedef void tWMyDRead(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, W_HANDLE * phOperation);
typedef void tWMyDWrite(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, bool_t bLockSectors, W_HANDLE * phOperation);
typedef W_ERROR tWNDEFAppendRecord(W_HANDLE hMessage, W_HANDLE hRecord);
typedef W_ERROR tWNDEFBuildMessage(const uint8_t * pBuffer, uint32_t nBufferLength, W_HANDLE * phMessage);
typedef W_ERROR tWNDEFBuildRecord(const uint8_t * pBuffer, uint32_t nBufferLength, W_HANDLE * phRecord);
typedef bool_t tWNDEFCheckIdentifier(W_HANDLE hRecord, const char16_t * pIdentifierString);
typedef bool_t tWNDEFCheckRecordType(uint8_t nTNF, const char16_t * pTypeString);
typedef bool_t tWNDEFCheckType(W_HANDLE hRecord, uint8_t nTNF, const char16_t * pTypeString);
typedef bool_t tWNDEFCompareRecordType(uint8_t nTNF1, const char16_t * pTypeString1, uint8_t nTNF2, const char16_t * pTypeString2);
typedef W_ERROR tWNDEFCreateNestedMessageRecord(uint8_t nTNF, const char16_t * pTypeString, W_HANDLE hNestedMessage, W_HANDLE * phRecord);
typedef W_ERROR tWNDEFCreateNewMessage(W_HANDLE * phMessage);
typedef W_ERROR tWNDEFCreateRecord(uint8_t nTNF, const char16_t * pTypeString, const uint8_t * pPayloadBuffer, uint32_t nPayloadLength, W_HANDLE * phRecord);
typedef W_ERROR tWNDEFGetEnclosedMessage(W_HANDLE hRecord, W_HANDLE * phMessage);
typedef W_ERROR tWNDEFGetIdentifierString(W_HANDLE hRecord, char16_t * pIdentifierStringBuffer, uint32_t nIdentifierStringBufferLength, uint32_t * pnActualLength);
typedef W_ERROR tWNDEFGetMessageContent(W_HANDLE hMessage, uint8_t * pMessageBuffer, uint32_t nMessageBufferLength, uint32_t * pnActualLength);
typedef uint32_t tWNDEFGetMessageLength(W_HANDLE hMessage);
typedef W_HANDLE tWNDEFGetNextMessage(W_HANDLE hMessage);
typedef W_ERROR tWNDEFGetPayloadPointer(W_HANDLE hRecord, uint8_t ** ppBuffer);
typedef W_HANDLE tWNDEFGetRecord(W_HANDLE hMessage, uint32_t nIndex);
typedef W_ERROR tWNDEFGetRecordInfo(W_HANDLE hRecord, uint32_t nInfoType, uint32_t * pnValue);
typedef W_ERROR tWNDEFGetRecordInfoBuffer(W_HANDLE hRecord, uint32_t nInfoType, uint8_t * pBuffer, uint32_t nBufferLength, uint32_t * pnActualLength);
typedef uint32_t tWNDEFGetRecordNumber(W_HANDLE hMessage);
typedef W_ERROR tWNDEFGetTagInfo(W_HANDLE hConnection, tNDEFTagInfo * pTagInfo);
typedef W_ERROR tWNDEFGetTypeString(W_HANDLE hRecord, char16_t * pTypeStringBuffer, uint32_t nTypeStringBufferLength, uint32_t * pnActualLength);
typedef void tWNDEFHandlerWorkPerformed(bool_t bGiveToNextListener, bool_t bMessageMatch);
typedef W_ERROR tWNDEFInsertRecord(W_HANDLE hMessage, uint32_t nIndex, W_HANDLE hRecord);
typedef void tWNDEFReadMessage(W_HANDLE hConnection, tWBasicGenericHandleCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nTNF, const char16_t * pTypeString, W_HANDLE * phOperation);

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
typedef void tWNDEFReadMessageOnAnyTag(tWBasicGenericHandleCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nPriority, uint8_t nTNF, const char16_t * pTypeString, W_HANDLE * phRegistry);
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

typedef W_ERROR tWNDEFRegisterMessageHandler(tWBasicGenericHandleCallbackFunction * pHandler, void * pHandlerParameter, const uint8_t * pPropertyArray, uint32_t nPropertyNumber, uint8_t nPriority, uint8_t nTNF, const char16_t * pTypeString, W_HANDLE * phRegistry);
typedef W_ERROR tWNDEFRemoveRecord(W_HANDLE hMessage, uint32_t nIndex);
typedef void tWNDEFSendMessage(tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pPropertyArray, uint32_t nPropertyNumber, W_HANDLE hMessage, W_HANDLE * phOperation);
typedef W_ERROR tWNDEFSetIdentifierString(W_HANDLE hRecord, const char16_t * pIdentifierString);
typedef W_ERROR tWNDEFSetRecord(W_HANDLE hMessage, uint32_t nIndex, W_HANDLE hRecord);
typedef W_ERROR tWNDEFSetRecordInfo(W_HANDLE hRecord, uint32_t nInfoType, const uint8_t * pBuffer, uint32_t nBufferLength);
typedef void tWNDEFWriteMessage(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, W_HANDLE hMessage, uint32_t nActionMask, W_HANDLE * phOperation);
typedef void tWNDEFWriteMessageOnAnyTag(tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nPriority, W_HANDLE hMessage, uint32_t nActionMask, W_HANDLE * phOperation);
typedef W_ERROR tWNFCControllerActivateSwpLine(uint32_t nSlotIdentifier);
typedef void tWNFCControllerFirmwareUpdate(tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pUpdateBuffer, uint32_t nUpdateBufferLength, uint32_t nMode);
typedef uint32_t tWNFCControllerFirmwareUpdateState(void);
typedef W_ERROR tWNFCControllerGetBooleanProperty(uint8_t nPropertyIdentifier, bool_t * pbValue);
typedef W_ERROR tWNFCControllerGetFirmwareProperty(const uint8_t * pUpdateBuffer, uint32_t nUpdateBufferLength, uint8_t nPropertyIdentifier, char16_t * pValueBuffer, uint32_t nBufferLength, uint32_t * pnValueLength);
typedef W_ERROR tWNFCControllerGetIntegerProperty(uint8_t nPropertyIdentifier, uint32_t * pnValue);
typedef uint32_t tWNFCControllerGetMode(void);
typedef W_ERROR tWNFCControllerGetProperty(uint8_t nPropertyIdentifier, char16_t * pValueBuffer, uint32_t nBufferLength, uint32_t * pnValueLength);
typedef W_ERROR tWNFCControllerGetRawMessageData(uint8_t * pBuffer, uint32_t nBufferLength, uint32_t * pnActualLength);
typedef void tWNFCControllerGetRFActivity(uint8_t * pnReaderState, uint8_t * pnCardState, uint8_t * pnP2PState);
typedef void tWNFCControllerGetRFLock(uint32_t nLockSet, bool_t * pbReaderLock, bool_t * pbCardLock);
typedef W_ERROR tWNFCControllerMonitorException(tWBasicGenericEventHandler * pHandler, void * pHandlerParameter, W_HANDLE * phEventRegistry);
typedef W_ERROR tWNFCControllerMonitorFieldEvents(tWBasicGenericEventHandler * pHandler, void * pHandlerParameter, W_HANDLE * phEventRegistry);
typedef void tWNFCControllerProductionTest(const uint8_t * pParameterBuffer, uint32_t nParameterBufferLength, uint8_t * pResultBuffer, uint32_t nResultBufferLength, tWBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter);
typedef W_ERROR tWNFCControllerRegisterRawListener(tWBasicGenericDataCallbackFunction * pReceiveMessageEventHandler, void * pHandlerParameter);
typedef void tWNFCControllerReset(tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint32_t nMode);
typedef void tWNFCControllerSelfTest(tWNFCControllerSelfTestCompleted * pCallback, void * pCallbackParameter);
typedef void tWNFCControllerSetRFLock(uint32_t nLockSet, bool_t bReaderLock, bool_t bCardLock, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);
typedef W_ERROR tWNFCControllerSwitchStandbyMode(bool_t bStandbyOn);
typedef void tWNFCControllerSwitchToRawMode(tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);
typedef void tWNFCControllerWriteRawMessage(tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nLength);
typedef void tWP2PConnect(W_HANDLE hSocket, W_HANDLE hLink, tWBasicGenericCallbackFunction * pEstablishmentCallback, void * pEstablishmentCallbackParameter);
typedef W_ERROR tWP2PCreateSocket(uint8_t nType, const char16_t * pServiceURI, uint8_t nSAP, W_HANDLE * phSocket);
typedef void tWP2PEstablishLink(tWBasicGenericHandleCallbackFunction * pEstablishmentCallback, void * pEstablishmentCallbackParameter, tWBasicGenericCallbackFunction * pReleaseCallback, void * pReleaseCallbackParameter, W_HANDLE * phOperation);
typedef W_ERROR tWP2PGetConfiguration(tWP2PConfiguration * pConfiguration);
typedef W_ERROR tWP2PGetLinkProperties(W_HANDLE hLink, tWP2PLinkProperties * pProperties);
typedef W_ERROR tWP2PGetSocketParameter(W_HANDLE hSocket, uint32_t nParameter, uint32_t * pnValue);
typedef void tWP2PRead(W_HANDLE hSocket, tWBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pReceptionBuffer, uint32_t nReceptionBufferLength, W_HANDLE * phOperation);
typedef void tWP2PRecvFrom(W_HANDLE hSocket, tWP2PRecvFromCompleted * pCallback, void * pCallbackParameter, uint8_t * pReceptionBuffer, uint32_t nReceptionBufferLength, W_HANDLE * phOperation);
typedef void tWP2PSendTo(W_HANDLE hSocket, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nSAP, const uint8_t * pSendBuffer, uint32_t nSendBufferLength, W_HANDLE * phOperation);
typedef W_ERROR tWP2PSetConfiguration(const tWP2PConfiguration * pConfiguration);
typedef W_ERROR tWP2PSetSocketParameter(W_HANDLE hSocket, uint32_t nParameter, uint32_t nValue);
typedef void tWP2PShutdown(W_HANDLE hSocket, tWBasicGenericCallbackFunction * pReleaseCallback, void * pReleaseCallbackParameter);
typedef void tWP2PURILookup(W_HANDLE hLink, tWP2PURILookupCompleted * pCallback, void * pCallbackParameter, const char16_t * pServiceURI);
typedef void tWP2PWrite(W_HANDLE hSocket, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pSendBuffer, uint32_t nSendBufferLength, W_HANDLE * phOperation);
typedef W_ERROR tWPicoGetConnectionInfo(W_HANDLE hConnection, tWPicoConnectionInfo * pConnectionInfo);
typedef W_ERROR tWPicoIsWritable(W_HANDLE hConnection);
typedef void tWPicoRead(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, W_HANDLE * phOperation);
typedef void tWPicoWrite(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, bool_t bLockCard, W_HANDLE * phOperation);
typedef W_ERROR tWReaderErrorEventRegister(tWBasicGenericEventHandler * pHandler, void * pHandlerParameter, uint8_t nEventType, bool_t bCardDetectionRequested, W_HANDLE * phRegistryHandle);
typedef void tWReaderExchangeData(W_HANDLE hConnection, tWBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pReaderToCardBuffer, uint32_t nReaderToCardBufferLength, uint8_t * pCardToReaderBuffer, uint32_t nCardToReaderBufferMaxLength, W_HANDLE * phOperation);
typedef void tWReaderExchangeDataEx(W_HANDLE hConnection, uint8_t nPropertyIdentifier, tWBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pReaderToCardBuffer, uint32_t nReaderToCardBufferLength, uint8_t * pCardToReaderBuffer, uint32_t nCardToReaderBufferMaxLength, W_HANDLE * phOperation);
typedef W_ERROR tWReaderGetIdentifier(W_HANDLE hConnection, uint8_t * pIdentifierBuffer, uint32_t nIdentifierBufferMaxLength, uint32_t * pnIdentifierActualLength);
typedef W_ERROR tWReaderGetPulsePeriod(uint32_t * pnTimeout);
typedef void tWReaderHandlerWorkPerformed(W_HANDLE hConnection, bool_t bGiveToNextListener, bool_t bCardApplicationMatch);
typedef bool_t tWReaderIsPropertySupported(uint8_t nPropertyIdentifier);
typedef W_ERROR tWReaderListenToCardDetection(tWReaderCardDetectionHandler * pHandler, void * pHandlerParameter, uint8_t nPriority, const uint8_t * pConnectionPropertyArray, uint32_t nPropertyNumber, W_HANDLE * phEventRegistry);
typedef W_ERROR tWReaderListenToCardRemovalDetection(W_HANDLE hConnection, tWBasicGenericEventHandler * pEventHandler, void * pCallbackParameter, W_HANDLE * phEventRegistry);
typedef bool_t tWReaderPreviousApplicationMatch(W_HANDLE hConnection);
typedef void tWReaderSetPulsePeriod(tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint32_t nPulsePeriod);
typedef void tWRoutingTableApply(W_HANDLE hRoutingTable, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);
typedef W_ERROR tWRoutingTableCreate(W_HANDLE * phRoutingTable);
typedef void tWRoutingTableEnable(bool_t bIsEnabled, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);
typedef W_ERROR tWRoutingTableGetEntry(W_HANDLE hRoutingTable, uint16_t nEntryIndex, tWRoutingTableEntry * pRoutingTableEntry);
typedef W_ERROR tWRoutingTableGetEntryCount(W_HANDLE hRoutingTable, uint16_t * pnEntryCount);
typedef W_ERROR tWRoutingTableIsEnabled(bool_t * pbIsEnabled);
typedef W_ERROR tWRoutingTableModify(W_HANDLE hRoutingTable, uint32_t nOperation, uint16_t nEntryIndex, const tWRoutingTableEntry * pRoutingTableEntry);
typedef void tWRoutingTableRead(tWBasicGenericHandleCallbackFunction * pCallback, void * pCallbackParameter);
typedef bool_t tWRTDIsTextRecord(W_HANDLE hRecord);
typedef bool_t tWRTDIsURIRecord(W_HANDLE hRecord);
typedef W_ERROR tWRTDTextAddRecord(W_HANDLE hMessage, const char16_t * pLanguage, bool_t bUseUtf8, const char16_t * pText, uint32_t nTextLength);
typedef W_ERROR tWRTDTextCreateRecord(const char16_t * pLanguage, bool_t bUseUtf8, const char16_t * pText, uint32_t nTextLength, W_HANDLE * phRecord);
typedef W_ERROR tWRTDTextFind(W_HANDLE hMessage, const char16_t * pLanguage1, const char16_t * pLanguage2, W_HANDLE * phRecord, uint8_t * pnMatch);
typedef W_ERROR tWRTDTextGetLanguage(W_HANDLE hRecord, char16_t * pLanguageBuffer, uint32_t nBufferLength);
typedef uint32_t tWRTDTextGetLength(W_HANDLE hRecord);
typedef W_ERROR tWRTDTextGetValue(W_HANDLE hRecord, char16_t * pBuffer, uint32_t nBufferLength);
typedef uint8_t tWRTDTextLanguageMatch(W_HANDLE hRecord, const char16_t * pLanguage1, const char16_t * pLanguage2);
typedef W_ERROR tWRTDURIAddRecord(W_HANDLE hMessage, const char16_t * pURI);
typedef W_ERROR tWRTDURICreateRecord(const char16_t * pURI, W_HANDLE * phRecord);
typedef uint32_t tWRTDURIGetLength(W_HANDLE hRecord);
typedef W_ERROR tWRTDURIGetValue(W_HANDLE hRecord, char16_t * pBuffer, uint32_t nBufferLength);
typedef W_ERROR tWSECheckAIDAccess(uint32_t nSlotIdentifier, const uint8_t * pAIDBuffer, uint32_t nAIDLength, const uint8_t * pImpersonationDataBuffer, uint32_t nImpersonationDataBufferLength);
typedef W_ERROR tWSecurityAuthenticate(const uint8_t * pApplicationDataBuffer, uint32_t nApplicationDataBufferLength);
typedef W_ERROR tWSEGetConnectivityEventParameter(uint32_t nSlotIdentifier, uint8_t * pDataBuffer, uint32_t nBufferLength, uint32_t * pnActualDataLength);
typedef W_ERROR tWSEGetInfoEx(uint32_t nSlotIdentifier, tWSEInfoEx * pSEInfo);
typedef void tWSEGetStatus(uint32_t nSlotIdentifier, tWSEGetStatusCompleted * pCallback, void * pCallbackParameter);
typedef uint32_t tWSEGetTransactionAID(uint32_t nSlotIdentifier, uint8_t * pBuffer, uint32_t nBufferLength);
typedef W_ERROR tWSEMonitorConnectivityEvent(uint32_t nSlotIdentifier, tWBasicGenericEventHandler2 * pHandler, void * pHandlerParameter, W_HANDLE * phEventRegistry);
typedef W_ERROR tWSEMonitorEndOfTransaction(uint32_t nSlotIdentifier, tWBasicGenericEventHandler2 * pHandler, void * pHandlerParameter, W_HANDLE * phEventRegistry);
typedef W_ERROR tWSEMonitorHotPlugEvents(uint32_t nSlotIdentifier, tWBasicGenericEventHandler2 * pHandler, void * pHandlerParameter, W_HANDLE * phEventRegistry);
typedef void tWSEOpenConnection(uint32_t nSlotIdentifier, bool_t bForce, tWBasicGenericHandleCallbackFunction * pCallback, void * pCallbackParameter);
typedef void tWSESetPolicy(uint32_t nSlotIdentifier, uint32_t nStorageType, uint32_t nProtocols, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);

#ifdef P_INCLUDE_TEST_ENGINE
typedef void * tWTestAlloc(uint32_t nSize);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef int32_t tWTestCompare(const void * pBuffer1, const void * pBuffer2, uint32_t nLength);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef uint32_t tWTestConvertUTF16ToUTF8(uint8_t * pDestUtf8, const char16_t * pSourceUtf16, uint32_t nSourceCharLength);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef void * tWTestCopy(void * pDestination, void * pSource, uint32_t nLength);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef void tWTestExecuteRemoteFunction(const char * pFunctionIdentifier, uint32_t nParameter, const uint8_t * pParameterBuffer, uint32_t nParameterBufferLength, uint8_t * pResultBuffer, uint32_t nResultBufferLength, tWBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef void tWTestFill(void * pBuffer, uint8_t nValue, uint32_t nLength);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef void tWTestFree(void * pBuffer);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef const void * tWTestGetConstAddress(const void * pConstData);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef uint32_t tWTestGetCurrentTime(void);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef tTestExecuteContext * tWTestGetExecuteContext(void);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef bool_t tWTestIsInAutomaticMode(void);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef void tWTestMessageBox(uint32_t nFlags, const char * pMessage, uint32_t nAutomaticResult, tWTestMessageBoxCompleted * pCallback, void * pCallbackParameter);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef void * tWTestMove(void * pDestination, void * pSource, uint32_t nLength);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef void tWTestNotifyEnd(void);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef void tWTestPresentObject(const char * pObjectName, const char * pOperatorMessage, uint32_t nDistance, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef void tWTestRemoveObject(const char * pOperatorMessage, bool_t bSaveState, bool_t bCheckUnmodifiedState, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef void tWTestSetErrorResult(uint32_t nResult, const char * pMessage);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef void tWTestSetResult(uint32_t nResult, const void * pResultData, uint32_t nResultDataLength);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef void tWTestSetTimer(uint32_t nTimeout, tWBasicGenericCompletionFunction * pCallback, void * pCallbackParameter);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef int32_t tWTestStringCompare(const char16_t * pString1, const char16_t * pString2);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef char16_t * tWTestStringCopy(char16_t * pBuffer, uint32_t * pnPos, const char16_t * pString);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef uint32_t tWTestStringLength(const char16_t * pString);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef void tWTestTraceBuffer(const uint8_t * pBuffer, uint32_t nLength);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef void tWTestTraceError(const char * pMessage, ...);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef void tWTestTraceInfo(const char * pMessage, ...);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef void tWTestTraceWarning(const char * pMessage, ...);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef uint32_t tWTestWriteDecimalUint32(char16_t * pStringBuffer, uint32_t nValue);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef uint32_t tWTestWriteHexaUint32(char16_t * pStringBuffer, uint32_t nValue);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
typedef uint32_t tWTestWriteHexaUint8(char16_t * pStringBuffer, uint8_t nValue);
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */

typedef W_ERROR tWType1ChipGetConnectionInfo(W_HANDLE hConnection, tWType1ChipConnectionInfo * pConnectionInfo);
typedef W_ERROR tWType1ChipIsWritable(W_HANDLE hConnection);
typedef void tWType1ChipRead(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, W_HANDLE * phOperation);
typedef void tWType1ChipWrite(W_HANDLE hConnection, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, bool_t bLockBlocks, W_HANDLE * phOperation);

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
typedef void tWUICCGetSlotInfo(tWUICCGetSlotInfoCompleted * pCallback, void * pCallbackParameter);
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */


#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
typedef void tWUICCSetAccessPolicy(uint32_t nStorageType, const tWUICCAccessPolicy * pAccessPolicy, tWBasicGenericCallbackFunction * pCallback, void * pCallbackParameter);
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

typedef W_ERROR tWVirtualTagCreate(uint8_t nTagType, const uint8_t * pIdentifier, uint32_t nIdentifierLength, uint32_t nMaximumMessageLength, W_HANDLE * phVirtualTag);
typedef void tWVirtualTagStart(W_HANDLE hVirtualTag, tWBasicGenericCallbackFunction * pCompletionCallback, void * pCompletionCallbackParameter, tWBasicGenericEventHandler * pEventCallback, void * pEventCallbackParameter, bool_t bReadOnly);
typedef void tWVirtualTagStop(W_HANDLE hVirtualTag, tWBasicGenericCallbackFunction * pCompletionCallback, void * pCallbackParameter);


typedef struct __tTestAPI
{

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   tW14Part3ExchangeData* W14Part3ExchangeData;
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

   tW14Part3ExchangeRawBits* W14Part3ExchangeRawBits;
   tW14Part3GetConnectionInfo* W14Part3GetConnectionInfo;
   tW14Part3ListenToCardDetectionTypeB* W14Part3ListenToCardDetectionTypeB;
   tW14Part3SetTimeout* W14Part3SetTimeout;

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   tW14Part4ExchangeData* W14Part4ExchangeData;
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

   tW14Part4GetConnectionInfo* W14Part4GetConnectionInfo;
   tW14Part4ListenToCardDetectionTypeA* W14Part4ListenToCardDetectionTypeA;
   tW14Part4ListenToCardDetectionTypeB* W14Part4ListenToCardDetectionTypeB;
   tW14Part4SetNAD* W14Part4SetNAD;
   tW15GetConnectionInfo* W15GetConnectionInfo;
   tW15IsWritable* W15IsWritable;
   tW15ListenToCardDetection* W15ListenToCardDetection;
   tW15Read* W15Read;
   tW15SetAttribute* W15SetAttribute;
   tW15SetTagSize* W15SetTagSize;
   tW15Write* W15Write;
   tW7816ExchangeAPDU* W7816ExchangeAPDU;
   tW7816GetAid* W7816GetAid;
   tW7816GetATR* W7816GetATR;
   tW7816GetATRSize* W7816GetATRSize;
   tW7816GetResponseAPDUData* W7816GetResponseAPDUData;
   tW7816OpenChannel* W7816OpenChannel;

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   tW7816OpenLogicalChannel* W7816OpenLogicalChannel;
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

   tWBasicCancelOperation* WBasicCancelOperation;
   tWBasicCheckConnectionProperty* WBasicCheckConnectionProperty;
   tWBasicCloseHandle* WBasicCloseHandle;
   tWBasicCloseHandleSafe* WBasicCloseHandleSafe;
   tWBasicGetConnectionProperties* WBasicGetConnectionProperties;
   tWBasicGetConnectionPropertyName* WBasicGetConnectionPropertyName;
   tWBasicGetConnectionPropertyNumber* WBasicGetConnectionPropertyNumber;
   tWBasicGetErrorString* WBasicGetErrorString;
   tWBPrimeGetConnectionInfo* WBPrimeGetConnectionInfo;
   tWBPrimeListenToCardDetection* WBPrimeListenToCardDetection;
   tWBPrimeSetTimeout* WBPrimeSetTimeout;
   tWEmulClose* WEmulClose;
   tWEmulGetMessageData* WEmulGetMessageData;
   tWEmulIsPropertySupported* WEmulIsPropertySupported;
   tWEmulOpenConnection* WEmulOpenConnection;
   tWEmulSendAnswer* WEmulSendAnswer;
   tWFeliCaGetCardList* WFeliCaGetCardList;
   tWFeliCaGetCardNumber* WFeliCaGetCardNumber;

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   tWFeliCaGetConnectionInfo* WFeliCaGetConnectionInfo;
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

   tWFeliCaListenToCardDetection* WFeliCaListenToCardDetection;
   tWFeliCaRead* WFeliCaRead;
   tWFeliCaSelectCard* WFeliCaSelectCard;
   tWFeliCaSelectSystem* WFeliCaSelectSystem;
   tWFeliCaWrite* WFeliCaWrite;
   tWHandoverAddBluetoothCarrier* WHandoverAddBluetoothCarrier;
   tWHandoverAddWiFiCarrier* WHandoverAddWiFiCarrier;
   tWHandoverCreate* WHandoverCreate;
   tWHandoverFormatTag* WHandoverFormatTag;
   tWHandoverGetBluetoothInfo* WHandoverGetBluetoothInfo;
   tWHandoverGetPairingInfo* WHandoverGetPairingInfo;
   tWHandoverGetPairingInfoLength* WHandoverGetPairingInfoLength;
   tWHandoverGetWiFiInfo* WHandoverGetWiFiInfo;
   tWHandoverPairingCompletion* WHandoverPairingCompletion;
   tWHandoverPairingStart* WHandoverPairingStart;
   tWHandoverRemoveAllCarrier* WHandoverRemoveAllCarrier;
   tWMifareClassicAuthenticate* WMifareClassicAuthenticate;
   tWMifareClassicGetConnectionInfo* WMifareClassicGetConnectionInfo;
   tWMifareClassicGetSectorInfo* WMifareClassicGetSectorInfo;
   tWMifareClassicReadBlock* WMifareClassicReadBlock;
   tWMifareClassicWriteBlock* WMifareClassicWriteBlock;
   tWMifareGetConnectionInfo* WMifareGetConnectionInfo;
   tWMifareRead* WMifareRead;
   tWMifareULCAuthenticate* WMifareULCAuthenticate;
   tWMifareULCSetAccessRights* WMifareULCSetAccessRights;
   tWMifareULForceULC* WMifareULForceULC;
   tWMifareULFreezeDataLockConfiguration* WMifareULFreezeDataLockConfiguration;
   tWMifareULGetAccessRights* WMifareULGetAccessRights;
   tWMifareULRetrieveAccessRights* WMifareULRetrieveAccessRights;
   tWMifareWrite* WMifareWrite;
   tWMyDGetConnectionInfo* WMyDGetConnectionInfo;
   tWMyDMoveAuthenticate* WMyDMoveAuthenticate;
   tWMyDMoveFreezeDataLockConfiguration* WMyDMoveFreezeDataLockConfiguration;
   tWMyDMoveGetConfiguration* WMyDMoveGetConfiguration;
   tWMyDMoveSetConfiguration* WMyDMoveSetConfiguration;
   tWMyDRead* WMyDRead;
   tWMyDWrite* WMyDWrite;
   tWNDEFAppendRecord* WNDEFAppendRecord;
   tWNDEFBuildMessage* WNDEFBuildMessage;
   tWNDEFBuildRecord* WNDEFBuildRecord;
   tWNDEFCheckIdentifier* WNDEFCheckIdentifier;
   tWNDEFCheckRecordType* WNDEFCheckRecordType;
   tWNDEFCheckType* WNDEFCheckType;
   tWNDEFCompareRecordType* WNDEFCompareRecordType;
   tWNDEFCreateNestedMessageRecord* WNDEFCreateNestedMessageRecord;
   tWNDEFCreateNewMessage* WNDEFCreateNewMessage;
   tWNDEFCreateRecord* WNDEFCreateRecord;
   tWNDEFGetEnclosedMessage* WNDEFGetEnclosedMessage;
   tWNDEFGetIdentifierString* WNDEFGetIdentifierString;
   tWNDEFGetMessageContent* WNDEFGetMessageContent;
   tWNDEFGetMessageLength* WNDEFGetMessageLength;
   tWNDEFGetNextMessage* WNDEFGetNextMessage;
   tWNDEFGetPayloadPointer* WNDEFGetPayloadPointer;
   tWNDEFGetRecord* WNDEFGetRecord;
   tWNDEFGetRecordInfo* WNDEFGetRecordInfo;
   tWNDEFGetRecordInfoBuffer* WNDEFGetRecordInfoBuffer;
   tWNDEFGetRecordNumber* WNDEFGetRecordNumber;
   tWNDEFGetTagInfo* WNDEFGetTagInfo;
   tWNDEFGetTypeString* WNDEFGetTypeString;
   tWNDEFHandlerWorkPerformed* WNDEFHandlerWorkPerformed;
   tWNDEFInsertRecord* WNDEFInsertRecord;
   tWNDEFReadMessage* WNDEFReadMessage;

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   tWNDEFReadMessageOnAnyTag* WNDEFReadMessageOnAnyTag;
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

   tWNDEFRegisterMessageHandler* WNDEFRegisterMessageHandler;
   tWNDEFRemoveRecord* WNDEFRemoveRecord;
   tWNDEFSendMessage* WNDEFSendMessage;
   tWNDEFSetIdentifierString* WNDEFSetIdentifierString;
   tWNDEFSetRecord* WNDEFSetRecord;
   tWNDEFSetRecordInfo* WNDEFSetRecordInfo;
   tWNDEFWriteMessage* WNDEFWriteMessage;
   tWNDEFWriteMessageOnAnyTag* WNDEFWriteMessageOnAnyTag;
   tWNFCControllerActivateSwpLine* WNFCControllerActivateSwpLine;
   tWNFCControllerFirmwareUpdate* WNFCControllerFirmwareUpdate;
   tWNFCControllerFirmwareUpdateState* WNFCControllerFirmwareUpdateState;
   tWNFCControllerGetBooleanProperty* WNFCControllerGetBooleanProperty;
   tWNFCControllerGetFirmwareProperty* WNFCControllerGetFirmwareProperty;
   tWNFCControllerGetIntegerProperty* WNFCControllerGetIntegerProperty;
   tWNFCControllerGetMode* WNFCControllerGetMode;
   tWNFCControllerGetProperty* WNFCControllerGetProperty;
   tWNFCControllerGetRawMessageData* WNFCControllerGetRawMessageData;
   tWNFCControllerGetRFActivity* WNFCControllerGetRFActivity;
   tWNFCControllerGetRFLock* WNFCControllerGetRFLock;
   tWNFCControllerMonitorException* WNFCControllerMonitorException;
   tWNFCControllerMonitorFieldEvents* WNFCControllerMonitorFieldEvents;
   tWNFCControllerProductionTest* WNFCControllerProductionTest;
   tWNFCControllerRegisterRawListener* WNFCControllerRegisterRawListener;
   tWNFCControllerReset* WNFCControllerReset;
   tWNFCControllerSelfTest* WNFCControllerSelfTest;
   tWNFCControllerSetRFLock* WNFCControllerSetRFLock;
   tWNFCControllerSwitchStandbyMode* WNFCControllerSwitchStandbyMode;
   tWNFCControllerSwitchToRawMode* WNFCControllerSwitchToRawMode;
   tWNFCControllerWriteRawMessage* WNFCControllerWriteRawMessage;
   tWP2PConnect* WP2PConnect;
   tWP2PCreateSocket* WP2PCreateSocket;
   tWP2PEstablishLink* WP2PEstablishLink;
   tWP2PGetConfiguration* WP2PGetConfiguration;
   tWP2PGetLinkProperties* WP2PGetLinkProperties;
   tWP2PGetSocketParameter* WP2PGetSocketParameter;
   tWP2PRead* WP2PRead;
   tWP2PRecvFrom* WP2PRecvFrom;
   tWP2PSendTo* WP2PSendTo;
   tWP2PSetConfiguration* WP2PSetConfiguration;
   tWP2PSetSocketParameter* WP2PSetSocketParameter;
   tWP2PShutdown* WP2PShutdown;
   tWP2PURILookup* WP2PURILookup;
   tWP2PWrite* WP2PWrite;
   tWPicoGetConnectionInfo* WPicoGetConnectionInfo;
   tWPicoIsWritable* WPicoIsWritable;
   tWPicoRead* WPicoRead;
   tWPicoWrite* WPicoWrite;
   tWReaderErrorEventRegister* WReaderErrorEventRegister;
   tWReaderExchangeData* WReaderExchangeData;
   tWReaderExchangeDataEx* WReaderExchangeDataEx;
   tWReaderGetIdentifier* WReaderGetIdentifier;
   tWReaderGetPulsePeriod* WReaderGetPulsePeriod;
   tWReaderHandlerWorkPerformed* WReaderHandlerWorkPerformed;
   tWReaderIsPropertySupported* WReaderIsPropertySupported;
   tWReaderListenToCardDetection* WReaderListenToCardDetection;
   tWReaderListenToCardRemovalDetection* WReaderListenToCardRemovalDetection;
   tWReaderPreviousApplicationMatch* WReaderPreviousApplicationMatch;
   tWReaderSetPulsePeriod* WReaderSetPulsePeriod;
   tWRoutingTableApply* WRoutingTableApply;
   tWRoutingTableCreate* WRoutingTableCreate;
   tWRoutingTableEnable* WRoutingTableEnable;
   tWRoutingTableGetEntry* WRoutingTableGetEntry;
   tWRoutingTableGetEntryCount* WRoutingTableGetEntryCount;
   tWRoutingTableIsEnabled* WRoutingTableIsEnabled;
   tWRoutingTableModify* WRoutingTableModify;
   tWRoutingTableRead* WRoutingTableRead;
   tWRTDIsTextRecord* WRTDIsTextRecord;
   tWRTDIsURIRecord* WRTDIsURIRecord;
   tWRTDTextAddRecord* WRTDTextAddRecord;
   tWRTDTextCreateRecord* WRTDTextCreateRecord;
   tWRTDTextFind* WRTDTextFind;
   tWRTDTextGetLanguage* WRTDTextGetLanguage;
   tWRTDTextGetLength* WRTDTextGetLength;
   tWRTDTextGetValue* WRTDTextGetValue;
   tWRTDTextLanguageMatch* WRTDTextLanguageMatch;
   tWRTDURIAddRecord* WRTDURIAddRecord;
   tWRTDURICreateRecord* WRTDURICreateRecord;
   tWRTDURIGetLength* WRTDURIGetLength;
   tWRTDURIGetValue* WRTDURIGetValue;
   tWSECheckAIDAccess* WSECheckAIDAccess;
   tWSecurityAuthenticate* WSecurityAuthenticate;
   tWSEGetConnectivityEventParameter* WSEGetConnectivityEventParameter;
   tWSEGetInfoEx* WSEGetInfoEx;
   tWSEGetStatus* WSEGetStatus;
   tWSEGetTransactionAID* WSEGetTransactionAID;
   tWSEMonitorConnectivityEvent* WSEMonitorConnectivityEvent;
   tWSEMonitorEndOfTransaction* WSEMonitorEndOfTransaction;
   tWSEMonitorHotPlugEvents* WSEMonitorHotPlugEvents;
   tWSEOpenConnection* WSEOpenConnection;
   tWSESetPolicy* WSESetPolicy;

#ifdef P_INCLUDE_TEST_ENGINE
   tWTestAlloc* WTestAlloc;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestCompare* WTestCompare;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestConvertUTF16ToUTF8* WTestConvertUTF16ToUTF8;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestCopy* WTestCopy;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestExecuteRemoteFunction* WTestExecuteRemoteFunction;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestFill* WTestFill;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestFree* WTestFree;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestGetConstAddress* WTestGetConstAddress;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestGetCurrentTime* WTestGetCurrentTime;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestGetExecuteContext* WTestGetExecuteContext;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestIsInAutomaticMode* WTestIsInAutomaticMode;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestMessageBox* WTestMessageBox;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestMove* WTestMove;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestNotifyEnd* WTestNotifyEnd;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestPresentObject* WTestPresentObject;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestRemoveObject* WTestRemoveObject;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestSetErrorResult* WTestSetErrorResult;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestSetResult* WTestSetResult;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestSetTimer* WTestSetTimer;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestStringCompare* WTestStringCompare;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestStringCopy* WTestStringCopy;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestStringLength* WTestStringLength;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestTraceBuffer* WTestTraceBuffer;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestTraceError* WTestTraceError;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestTraceInfo* WTestTraceInfo;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestTraceWarning* WTestTraceWarning;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestWriteDecimalUint32* WTestWriteDecimalUint32;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestWriteHexaUint32* WTestWriteHexaUint32;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   tWTestWriteHexaUint8* WTestWriteHexaUint8;
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */

   tWType1ChipGetConnectionInfo* WType1ChipGetConnectionInfo;
   tWType1ChipIsWritable* WType1ChipIsWritable;
   tWType1ChipRead* WType1ChipRead;
   tWType1ChipWrite* WType1ChipWrite;

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   tWUICCGetSlotInfo* WUICCGetSlotInfo;
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */


#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   tWUICCSetAccessPolicy* WUICCSetAccessPolicy;
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

   tWVirtualTagCreate* WVirtualTagCreate;
   tWVirtualTagStart* WVirtualTagStart;
   tWVirtualTagStop* WVirtualTagStop;
} tTestAPI;

#define P_TEST_INTERFACE_UUID_LENGTH  16
#define P_TEST_INTERFACE_UUID { 0xBD, 0x10, 0xB7, 0xE7, 0x16, 0xCB, 0x48, 0x1B, 0xB3, 0x90, 0xE6, 0x96, 0x0A, 0x90, 0x39, 0x86, }
#ifndef OPEN_NFC_BUILD_NUMBER
#define OPEN_NFC_BUILD_NUMBER 0
#define OPEN_NFC_BUILD_NUMBER_S  { '0',  0 }
#endif /* #ifndef OPEN_NFC_BUILD_NUMBER */
#ifndef OPEN_NFC_PRODUCT_SIMPLE_VERSION
#define OPEN_NFC_PRODUCT_SIMPLE_VERSION 4.4.3
#define OPEN_NFC_PRODUCT_SIMPLE_VERSION_S  { '4', '.', '4', '.', '3',  0 }
#endif /* #ifndef OPEN_NFC_PRODUCT_SIMPLE_VERSION */
#ifndef OPEN_NFC_PRODUCT_VERSION
#define OPEN_NFC_PRODUCT_VERSION v4.4.3
#define OPEN_NFC_PRODUCT_VERSION_S  { 'v', '4', '.', '4', '.', '3',  0 }
#endif /* #ifndef OPEN_NFC_PRODUCT_VERSION */
#ifndef OPEN_NFC_PRODUCT_VERSION_BUILD
#define OPEN_NFC_PRODUCT_VERSION_BUILD v4.4.3 (Build 0)
#define OPEN_NFC_PRODUCT_VERSION_BUILD_S  { 'v', '4', '.', '4', '.', '3', ' ', '(', 'B', 'u', 'i', 'l', 'd', ' ', '0', ')',  0 }
#endif /* #ifndef OPEN_NFC_PRODUCT_VERSION_BUILD */

#endif /* #ifdef __WME_TEST_STUB_AUTOGEN_H */
