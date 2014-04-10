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

#ifndef __WME_CLIENT_API_AUTOGEN_H
#define __WME_CLIENT_API_AUTOGEN_H

typedef void tPBasicGenericCallbackFunction( tContext* pContext, void * pCallbackParameter, W_ERROR nResult );

typedef void tPBasicGenericCompletionFunction( tContext* pContext, void * pCallbakParameter );

typedef void tPBasicGenericDataCallbackFunction( tContext* pContext, void * pCallbackParameter, uint32_t nDataLength, W_ERROR nResult );

typedef void tPBasicGenericEventHandler( tContext* pContext, void * pHandlerParameter, W_ERROR nEventCode );

typedef void tPBasicGenericEventHandler2( tContext* pContext, void * pHandlerParameter, W_ERROR nEventCode1, W_ERROR nEventCode2 );

typedef void tPBasicGenericHandleCallbackFunction( tContext* pContext, void * pCallbackParameter, W_HANDLE hHandle, W_ERROR nResult );

typedef void tPEmulCommandReceived( tContext* pContext, void * pCallbackParameter, uint32_t nDataLength );

typedef void tPEmulDriverCommandReceived( tContext* pContext, void * pCallbackParameter, uint32_t nDataLength );

typedef void tPEmulDriverEventReceived( tContext* pContext, void * pCallbackParameter, uint32_t nEventCode );

typedef void tPMyDMoveGetConfigurationCompleted( tContext* pContext, void * pCallbackParameter, W_ERROR nError, uint8_t nStatusByte, uint8_t nPasswordRetryCounter );

typedef void tPNFCControllerSelfTestCompleted( tContext* pContext, void * pCallbackParameter, W_ERROR nError, uint32_t nResult );

typedef void tPP2PRecvFromCompleted( tContext* pContext, void * pCallbackParameter, uint32_t nDataLength, W_ERROR nError, uint8_t nSAP );

typedef void tPP2PURILookupCompleted( tContext* pContext, void * pCallbackParameter, uint8_t nDSAP, W_ERROR nError );

typedef void tPReaderCardDetectionHandler( tContext* pContext, void * pHandlerParameter, W_HANDLE hConnection, W_ERROR nError );

typedef void tPSEGetStatusCompleted( tContext* pContext, void * pCallbackParameter, bool_t bIsPresent, uint32_t nSWPLinkStatus, W_ERROR nError );

typedef void tPTestEntryPoint( tContext* pContext, tTestAPI * pAPI );

typedef void tPTestMessageBoxCompleted( tContext* pContext, void * pCallbackParameter, uint32_t nResult );

typedef void tPUICCGetSlotInfoCompleted( tContext* pContext, void * pCallbackParameter, uint32_t nSWPLinkStatus, W_ERROR nError );

typedef void tPUICCMonitorConnectivityEventHandler( tContext* pContext, void * pHandlerParameter, uint8_t nMessageCode, uint32_t nDataLength );

typedef void tPUICCMonitorTransactionEventHandler( tContext* pContext, void * pHandlerParameter );

void P14Part3ExchangeRawBits( tContext* pContext, W_HANDLE hConnection, tPBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pReaderToCardBuffer, uint32_t nReaderToCardBufferLength, uint8_t nReaderToCardBufferLastByteBitNumber, uint8_t * pCardToReaderBuffer, uint32_t nCardToReaderBufferMaxLength, uint8_t nExpectedBits, W_HANDLE * phOperation );

W_ERROR P14Part3GetConnectionInfo( tContext* pContext, W_HANDLE hConnection, tW14Part3ConnectionInfo * p14Part3ConnectionInfo );


#ifdef P_INCLUDE_JAVA_API
W_ERROR P14Part3GetConnectionInfoBuffer( tContext* pContext, W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR P14Part3ListenToCardDetectionTypeB( tContext* pContext, tPReaderCardDetectionHandler * pHandler, void * pHandlerParameter, uint8_t nPriority, uint8_t nAFI, bool_t bUseCID, uint8_t nCID, uint32_t nBaudRate, const uint8_t * pHigherLayerDataBuffer, uint8_t nHigherLayerDataLength, W_HANDLE * phEventRegistry );

W_ERROR P14Part3SetTimeout( tContext* pContext, W_HANDLE hConnection, uint32_t nTimeout );

W_ERROR P14Part4GetConnectionInfo( tContext* pContext, W_HANDLE hConnection, tW14Part4ConnectionInfo * p14Part4ConnectionInfo );


#ifdef P_INCLUDE_JAVA_API
W_ERROR P14Part4GetConnectionInfoBuffer( tContext* pContext, W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR P14Part4ListenToCardDetectionTypeA( tContext* pContext, tPReaderCardDetectionHandler * pHandler, void * pHandlerParameter, uint8_t nPriority, bool_t bUseCID, uint8_t nCID, uint32_t nBaudRate, W_HANDLE * phEventRegistry );

W_ERROR P14Part4ListenToCardDetectionTypeB( tContext* pContext, tPReaderCardDetectionHandler * pHandler, void * pHandlerParameter, uint8_t nPriority, uint8_t nAFI, bool_t bUseCID, uint8_t nCID, uint32_t nBaudRate, const uint8_t * pHigherLayerDataBuffer, uint8_t nHigherLayerDataLength, W_HANDLE * phEventRegistry );

W_ERROR P14Part4SetNAD( tContext* pContext, W_HANDLE hConnection, uint8_t nNAD );

W_ERROR P15GetConnectionInfo( tContext* pContext, W_HANDLE hConnection, tW15ConnectionInfo * pConnectionInfo );


#ifdef P_INCLUDE_JAVA_API
W_ERROR P15GetConnectionInfoBuffer( tContext* pContext, W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR P15IsWritable( tContext* pContext, W_HANDLE hConnection, uint8_t nSectorIndex );

W_ERROR P15ListenToCardDetection( tContext* pContext, tPReaderCardDetectionHandler * pHandler, void * pHandlerParameter, uint8_t nPriority, uint8_t nAFI, W_HANDLE * phEventRegistry );

void P15Read( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, W_HANDLE * phOperation );

void P15SetAttribute( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nActions, uint8_t nAFI, uint8_t nDSFID, W_HANDLE * phOperation );

W_ERROR P15SetTagSize( tContext* pContext, W_HANDLE hConnection, uint16_t nSectorNumber, uint8_t nSectorSize );

void P15Write( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, bool_t bLockSectors, W_HANDLE * phOperation );

void P7816ExchangeAPDU( tContext* pContext, W_HANDLE hChannel, tPBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pSendAPDUBuffer, uint32_t nSendAPDUBufferLength, uint8_t * pReceivedAPDUBuffer, uint32_t nReceivedAPDUBufferMaxLength, W_HANDLE * phOperation );

W_ERROR P7816GetAid( tContext* pContext, W_HANDLE hChannel, uint8_t * pBuffer, uint32_t nBufferMaxLength, uint32_t * pnActualLength );

W_ERROR P7816GetATR( tContext* pContext, W_HANDLE hConnection, uint8_t * pBuffer, uint32_t nBufferMaxLength, uint32_t * pnActualLength );

W_ERROR P7816GetATRSize( tContext* pContext, W_HANDLE hConnection, uint32_t * pnSize );

W_ERROR P7816GetResponseAPDUData( tContext* pContext, W_HANDLE hChannel, uint8_t * pReceivedAPDUBuffer, uint32_t nReceivedAPDUBufferMaxLength, uint32_t * pnReceivedAPDUActualLength );

void P7816OpenChannel( tContext* pContext, W_HANDLE hConnection, tPBasicGenericHandleCallbackFunction * pCallback, void * pCallbackParameter, uint32_t nType, const uint8_t * pAID, uint32_t nAIDLength, W_HANDLE * phOperation );

void PBasicCancelOperation( tContext* pContext, W_HANDLE hOperation );

W_ERROR PBasicCheckConnectionProperty( tContext* pContext, W_HANDLE hConnection, uint8_t nPropertyIdentifier );

void PBasicCloseHandle( tContext* pContext, W_HANDLE hHandle );

void PBasicCloseHandleSafe( tContext* pContext, W_HANDLE hHandle, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter );

void PBasicExecuteEventLoop( tContext* pContext );

bool_t PBasicGenericSyncPrepare( tContext* pContext, void * param );

W_ERROR PBasicGetConnectionProperties( tContext* pContext, W_HANDLE hConnection, uint8_t * pPropertyArray, uint32_t nArrayLength );

const char * PBasicGetConnectionPropertyName( tContext* pContext, uint8_t nPropertyIdentifier );

W_ERROR PBasicGetConnectionPropertyNumber( tContext* pContext, W_HANDLE hConnection, uint32_t * pnPropertyNumber );

const char * PBasicGetErrorString( tContext* pContext, W_ERROR nError );

W_ERROR PBasicInit( tContext* pContext, const char16_t * pVersionString );

W_ERROR PBasicPumpEvent( tContext* pContext, bool_t bWait );

void PBasicStopEventLoop( tContext* pContext );

void PBasicTerminate( tContext* pContext );

void PBasicTestExchangeMessage( tContext* pContext, tPBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pSendMessageBuffer, uint32_t nSendMessageBufferLength, uint8_t * pReceivedMessageBuffer );

W_ERROR PBPrimeGetConnectionInfo( tContext* pContext, W_HANDLE hConnection, tWBPrimeConnectionInfo * pBPrimeConnectionInfo );


#ifdef P_INCLUDE_JAVA_API
W_ERROR PBPrimeGetConnectionInfoBuffer( tContext* pContext, W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR PBPrimeListenToCardDetection( tContext* pContext, tPReaderCardDetectionHandler * pHandler, void * pHandlerParameter, const uint8_t * pAPGENBuffer, uint32_t nAPGENLength, W_HANDLE * phEventRegistry );

W_ERROR PBPrimeSetTimeout( tContext* pContext, W_HANDLE hConnection, uint32_t nTimeout );


#ifdef P_INCLUDE_JAVA_API
bool_t PDFCPumpJNICallback( tContext* pContext, uint32_t * pArgs, uint32_t nArgsSize );

#endif /* #ifdef P_INCLUDE_JAVA_API */

void PEmulClose( tContext* pContext, W_HANDLE hHandle, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter );

W_ERROR PEmulGetMessageData( tContext* pContext, W_HANDLE hHandle, uint8_t * pDataBuffer, uint32_t nDataLength, uint32_t * pnActualDataLength );

bool_t PEmulIsPropertySupported( tContext* pContext, uint8_t nPropertyIdentifier );

void PEmulOpenConnectionDriver1( tContext* pContext, tPBasicGenericCallbackFunction * pOpenCallback, void * pOpenCallbackParameter, const tWEmulConnectionInfo * pEmulConnectionInfo, uint32_t nSize, W_HANDLE * phHandle );

void PEmulOpenConnectionDriver2( tContext* pContext, W_HANDLE hHandle, tPEmulDriverEventReceived * pEventCallback, void * pEventCallbackParameter );

void PEmulOpenConnectionDriver3( tContext* pContext, W_HANDLE hHandle, tPEmulDriverCommandReceived * pCommandCallback, void * pCommandCallbackParameter );

W_ERROR PEmulSendAnswer( tContext* pContext, W_HANDLE hDriverConnection, const uint8_t * pDataBuffer, uint32_t nDataLength );

W_ERROR PFeliCaGetCardList( tContext* pContext, W_HANDLE hConnection, tWFeliCaConnectionInfo * aFeliCaConnectionInfos, const uint32_t nArraySize );


#ifdef P_INCLUDE_JAVA_API
W_ERROR PFeliCaGetCardListBuffer( tContext* pContext, W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR PFeliCaGetCardNumber( tContext* pContext, W_HANDLE hConnection, uint32_t * pnCardNumber );


#ifdef P_INCLUDE_JAVA_API
W_ERROR PFeliCaGetCardNumberBuffer( tContext* pContext, W_HANDLE hConnection, uint32_t * pCardNumber );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
W_ERROR PFeliCaGetConnectionInfo( tContext* pContext, W_HANDLE hConnection, tWFeliCaConnectionInfo * pConnectionInfo );

#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

W_ERROR PFeliCaListenToCardDetection( tContext* pContext, tPReaderCardDetectionHandler * pHandler, void * pHandlerParameter, uint8_t nPriority, uint16_t nSystemCode, W_HANDLE * phEventRegistry );

void PFeliCaRead( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nLength, uint8_t nNumberOfService, const uint16_t * pServiceCodeList, uint8_t nNumberOfBlocks, const uint8_t * pBlockList, W_HANDLE * phOperation );


#ifdef P_INCLUDE_JAVA_API
void PFeliCaReadSimple( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nLength, uint32_t nNumberOfService, uint32_t * pServiceCodeList, uint32_t nNumberOfBlocks, const uint8_t * pBlockList );

#endif /* #ifdef P_INCLUDE_JAVA_API */

void PFeliCaSelectCard( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const tWFeliCaConnectionInfo * pFeliCaConnectionInfo );

void PFeliCaSelectSystem( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t nIndexSubSystem );

void PFeliCaWrite( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nLength, uint8_t nNumberOfService, const uint16_t * pServiceCodeList, uint8_t nNumberOfBlocks, const uint8_t * pBlockList, W_HANDLE * phOperation );


#ifdef P_INCLUDE_JAVA_API
void PFeliCaWriteSimple( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nLength, uint32_t nNumberOfService, uint32_t * pServiceCodeList, uint32_t nNumberOfBlocks, const uint8_t * pBlockList );

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR PHandoverAddBluetoothCarrier( tContext* pContext, W_HANDLE hConnectionHandover, tWBTPairingInfo * pBluetoothInfo, uint8_t nCarrierPowerState );

W_ERROR PHandoverAddWiFiCarrier( tContext* pContext, W_HANDLE hConnectionHandover, tWWiFiPairingInfo * pWiFiInfo, uint8_t nCarrierPowerState );

W_ERROR PHandoverCreate( tContext* pContext, W_HANDLE * phMessage );

void PHandoverFormatTag( tContext* pContext, W_HANDLE hConnectionHandover, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint32_t nActionMask, W_HANDLE * phOperation );

W_ERROR PHandoverGetBluetoothInfo( tContext* pContext, W_HANDLE hConnectionHandover, tWBTPairingInfo * pRemoteInfo );

W_ERROR PHandoverGetPairingInfo( tContext* pContext, W_HANDLE hConnectionHandover, tWHandoverPairingInfo * pPairingInfo );

W_ERROR PHandoverGetPairingInfoLength( tContext* pContext, W_HANDLE hConnectionHandover, uint32_t * pnLength );

W_ERROR PHandoverGetWiFiInfo( tContext* pContext, W_HANDLE hOperation, tWWiFiPairingInfo * pWiFiInfo );

void PHandoverPairingCompletion( tContext* pContext, W_HANDLE hHandoverConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, W_HANDLE * phOperation );

void PHandoverPairingStart( tContext* pContext, W_HANDLE hConnectionHandover, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint32_t nMode, W_HANDLE * phOperation );

W_ERROR PHandoverRemoveAllCarrier( tContext* pContext, W_HANDLE hHandoverConnection );


#ifdef P_INCLUDE_JAVA_API
W_HANDLE PJavaCreateByteBuffer( tContext* pContext, uint8_t * pJavaBuffer, uint32_t nOffset, uint32_t nLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
uint32_t PJavaGetByteBufferLength( tContext* pContext, W_HANDLE hBufferReference );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
uint32_t PJavaGetByteBufferOffset( tContext* pContext, W_HANDLE hBufferReference );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
uint8_t * PJavaGetByteBufferPointer( tContext* pContext, W_HANDLE hBufferReference );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
W_ERROR PJavaGetConnectionPropertyNameBuffer( tContext* pContext, uint8_t nPropertyIdentifier, uint8_t * pBuffer, uint32_t nBufferMaxLength, uint32_t * pnActualLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
W_ERROR PJavaHandoverAddBluetoothCarrierBuffer( tContext* pContext, W_HANDLE hConnectionHandover, uint8_t * pHandoverPairingInfoBuffer, uint32_t pHandoverPairingInfoBufferLength, uint8_t nCarrierPowerState );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
W_ERROR PJavaHandoverAddWiFiCarrierBuffer( tContext* pContext, W_HANDLE hConnectionHandover, uint8_t * pHandoverPairingInfoBuffer, uint32_t pHandoverPairingInfoBufferLength, uint8_t nCarrierPowerState );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
W_ERROR PJavaHandoverGetBluetoothInfoBuffer( tContext* pContext, W_HANDLE hConnectionHandover, uint8_t * pHandoverPairingInfoBuffer, uint32_t pHandoverPairingInfoBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
W_ERROR PJavaHandoverGetPairingInfoBuffer( tContext* pContext, W_HANDLE hConnectionHandover, uint8_t * pHandoverPairingInfoBuffer, uint32_t pHandoverPairingInfoBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
W_ERROR PJavaHandoverGetWiFiInfoBuffer( tContext* pContext, W_HANDLE hConnectionHandover, uint8_t * pHandoverPairingInfoBuffer, uint32_t pHandoverPairingInfoBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
uint32_t PJavaNDEFGetMessageContent( tContext* pContext, W_HANDLE hMessage, uint8_t * pMessageBuffer, uint32_t nMessageBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
void PJavaNdefSendMessage( tContext* pContext, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pMessageBuffer, uint32_t nMessageBufferLength, W_HANDLE * phOperation );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
void PJavaNDEFWriteMessage( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pMessageBuffer, uint32_t nMessageBufferLength, uint32_t nActionMask, W_HANDLE * phOperation );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
void PJavaNDEFWriteMessageOnAnyTag( tContext* pContext, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nPriority, uint8_t * pMessageBuffer, uint32_t nMessageBufferLength, uint32_t nActionMask, W_HANDLE * phOperation );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
bool_t PJavaNFCControllerGetBooleanProperty( tContext* pContext, uint8_t nPropertyIdentifier );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
W_ERROR PJavaP2PGetConfigurationBuffer( tContext* pContext, uint8_t * pConfigurationBuffer, uint32_t nConfigurationBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
W_ERROR PJavaP2PGetLinkPropertiesBuffer( tContext* pContext, W_HANDLE hLink, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
W_ERROR PJavaP2PSetConfigurationBuffer( tContext* pContext, uint8_t * pConfigurationBuffer, uint32_t nConfigurationBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */


#ifdef P_INCLUDE_JAVA_API
void PJavaReleaseByteBuffer( tContext* pContext, W_HANDLE hBufferReference, uint8_t * pJavaBuffer, uint32_t nJavaBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */

void PMifareClassicAuthenticate( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint16_t nSectorNumber, bool_t bWithKeyA, const uint8_t * pKey, uint8_t nKeyLength );

W_ERROR PMifareClassicGetConnectionInfo( tContext* pContext, W_HANDLE hConnection, tWMifareClassicConnectionInfo * pConnectionInfo );


#ifdef P_INCLUDE_JAVA_API
W_ERROR PMifareClassicGetConnectionInfoBuffer( tContext* pContext, W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR PMifareClassicGetSectorInfo( tContext* pContext, W_HANDLE hConnection, uint16_t nSectorNumber, tWMifareClassicSectorInfo * pSectorInfo );


#ifdef P_INCLUDE_JAVA_API
W_ERROR PMifareClassicGetSectorInfoBuffer( tContext* pContext, W_HANDLE hConnection, uint16_t nSectorNumber, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */

void PMifareClassicReadBlock( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint16_t nBlockNumber, uint8_t * pBuffer, uint8_t nBufferLength );

void PMifareClassicWriteBlock( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint16_t nBlockNumber, const uint8_t * pBuffer, uint8_t nBufferLength );

W_ERROR PMifareGetConnectionInfo( tContext* pContext, W_HANDLE hConnection, tWMifareConnectionInfo * pConnectionInfo );


#ifdef P_INCLUDE_JAVA_API
W_ERROR PMifareGetConnectionInfoBuffer( tContext* pContext, W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */

void PMifareRead( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, W_HANDLE * phOperation );

void PMifareULCAuthenticate( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pKey, uint32_t nKeyLength );

void PMifareULCSetAccessRights( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pKey, uint32_t nKeyLength, uint8_t nThreshold, uint32_t nRights, bool_t bLockConfiguration );

W_ERROR PMifareULForceULC( tContext* pContext, W_HANDLE hConnection );

void PMifareULFreezeDataLockConfiguration( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter );

W_ERROR PMifareULGetAccessRights( tContext* pContext, W_HANDLE hConnection, uint32_t nOffset, uint32_t nLength, uint32_t * pnRights );

void PMifareULRetrieveAccessRights( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter );

void PMifareWrite( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, bool_t bLockSectors, W_HANDLE * phOperation );

W_ERROR PMyDGetConnectionInfo( tContext* pContext, W_HANDLE hConnection, tWMyDConnectionInfo * pConnectionInfo );


#ifdef P_INCLUDE_JAVA_API
W_ERROR PMyDGetConnectionInfoBuffer( tContext* pContext, W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */

void PMyDMoveAuthenticate( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint32_t nPassword );

void PMyDMoveFreezeDataLockConfiguration( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter );

void PMyDMoveGetConfiguration( tContext* pContext, W_HANDLE hConnection, tPMyDMoveGetConfigurationCompleted * pCallback, void * pCallbackParameter );

void PMyDMoveSetConfiguration( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nStatusByte, uint8_t nPasswordRetryCounter, uint32_t nPassword, bool_t bLockConfiguration );

void PMyDRead( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, W_HANDLE * phOperation );

void PMyDWrite( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, bool_t bLockSectors, W_HANDLE * phOperation );

W_ERROR PNDEFAppendRecord( tContext* pContext, W_HANDLE hMessage, W_HANDLE hRecord );

W_ERROR PNDEFBuildMessage( tContext* pContext, const uint8_t * pBuffer, uint32_t nBufferLength, W_HANDLE * phMessage );

W_ERROR PNDEFBuildRecord( tContext* pContext, const uint8_t * pBuffer, uint32_t nBufferLength, W_HANDLE * phRecord );

bool_t PNDEFCheckIdentifier( tContext* pContext, W_HANDLE hRecord, const char16_t * pIdentifierString );

bool_t PNDEFCheckType( tContext* pContext, W_HANDLE hRecord, uint8_t nTNF, const char16_t * pTypeString );

W_ERROR PNDEFCreateNestedMessageRecord( tContext* pContext, uint8_t nTNF, const char16_t * pTypeString, W_HANDLE hNestedMessage, W_HANDLE * phRecord );

W_ERROR PNDEFCreateNewMessage( tContext* pContext, W_HANDLE * phMessage );

W_ERROR PNDEFCreateRecord( tContext* pContext, uint8_t nTNF, const char16_t * pTypeString, const uint8_t * pPayloadBuffer, uint32_t nPayloadLength, W_HANDLE * phRecord );

W_ERROR PNDEFGetEnclosedMessage( tContext* pContext, W_HANDLE hRecord, W_HANDLE * phMessage );

W_ERROR PNDEFGetIdentifierString( tContext* pContext, W_HANDLE hRecord, char16_t * pIdentifierStringBuffer, uint32_t nIdentifierStringBufferLength, uint32_t * pnActualLength );

W_ERROR PNDEFGetMessageContent( tContext* pContext, W_HANDLE hMessage, uint8_t * pMessageBuffer, uint32_t nMessageBufferLength, uint32_t * pnActualLength );

uint32_t PNDEFGetMessageLength( tContext* pContext, W_HANDLE hMessage );

W_HANDLE PNDEFGetNextMessage( tContext* pContext, W_HANDLE hMessage );

W_ERROR PNDEFGetPayloadPointer( tContext* pContext, W_HANDLE hRecord, uint8_t ** ppBuffer );

W_HANDLE PNDEFGetRecord( tContext* pContext, W_HANDLE hMessage, uint32_t nIndex );

W_ERROR PNDEFGetRecordInfo( tContext* pContext, W_HANDLE hRecord, uint32_t nInfoType, uint32_t * pnValue );

W_ERROR PNDEFGetRecordInfoBuffer( tContext* pContext, W_HANDLE hRecord, uint32_t nInfoType, uint8_t * pBuffer, uint32_t nBufferLength, uint32_t * pnActualLength );

uint32_t PNDEFGetRecordNumber( tContext* pContext, W_HANDLE hMessage );

W_ERROR PNDEFGetTagInfo( tContext* pContext, W_HANDLE hConnection, tNDEFTagInfo * pTagInfo );


#ifdef P_INCLUDE_JAVA_API
W_ERROR PNDEFGetTagInfoBuffer( tContext* pContext, W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR PNDEFGetTypeString( tContext* pContext, W_HANDLE hRecord, char16_t * pTypeStringBuffer, uint32_t nTypeStringBufferLength, uint32_t * pnActualLength );

void PNDEFHandlerWorkPerformed( tContext* pContext, bool_t bGiveToNextListener, bool_t bMessageMatch );

W_ERROR PNDEFInsertRecord( tContext* pContext, W_HANDLE hMessage, uint32_t nIndex, W_HANDLE hRecord );

void PNDEFReadMessage( tContext* pContext, W_HANDLE hConnection, tPBasicGenericHandleCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nTNF, const char16_t * pTypeString, W_HANDLE * phOperation );


#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
void PNDEFReadMessageOnAnyTag( tContext* pContext, tPBasicGenericHandleCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nPriority, uint8_t nTNF, const char16_t * pTypeString, W_HANDLE * phRegistry );

#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

W_ERROR PNDEFRegisterMessageHandler( tContext* pContext, tPBasicGenericHandleCallbackFunction * pHandler, void * pHandlerParameter, const uint8_t * pPropertyArray, uint32_t nPropertyNumber, uint8_t nPriority, uint8_t nTNF, const char16_t * pTypeString, W_HANDLE * phRegistry );

W_ERROR PNDEFRemoveRecord( tContext* pContext, W_HANDLE hMessage, uint32_t nIndex );

void PNDEFSendMessage( tContext* pContext, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pPropertyArray, uint32_t nPropertyNumber, W_HANDLE hMessage, W_HANDLE * phOperation );

W_ERROR PNDEFSetIdentifierString( tContext* pContext, W_HANDLE hRecord, const char16_t * pIdentifierString );

W_ERROR PNDEFSetRecord( tContext* pContext, W_HANDLE hMessage, uint32_t nIndex, W_HANDLE hRecord );

W_ERROR PNDEFSetRecordInfo( tContext* pContext, W_HANDLE hRecord, uint32_t nInfoType, const uint8_t * pBuffer, uint32_t nBufferLength );

void PNDEFWriteMessage( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, W_HANDLE hMessage, uint32_t nActionMask, W_HANDLE * phOperation );

void PNDEFWriteMessageOnAnyTag( tContext* pContext, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nPriority, W_HANDLE hMessage, uint32_t nActionMask, W_HANDLE * phOperation );

W_ERROR PNFCControllerActivateSwpLine( tContext* pContext, uint32_t nSlotIdentifier );

void PNFCControllerFirmwareUpdate( tContext* pContext, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pUpdateBuffer, uint32_t nUpdateBufferLength, uint32_t nMode );

uint32_t PNFCControllerFirmwareUpdateState( tContext* pContext );

W_ERROR PNFCControllerGetBooleanProperty( tContext* pContext, uint8_t nPropertyIdentifier, bool_t * pbValue );

W_ERROR PNFCControllerGetIntegerProperty( tContext* pContext, uint8_t nPropertyIdentifier, uint32_t * pnValue );

uint32_t PNFCControllerGetMode( tContext* pContext );

W_ERROR PNFCControllerGetProperty( tContext* pContext, uint8_t nPropertyIdentifier, char16_t * pValueBuffer, uint32_t nBufferLength, uint32_t * pnValueLength );

W_ERROR PNFCControllerGetRawMessageData( tContext* pContext, uint8_t * pBuffer, uint32_t nBufferLength, uint32_t * pnActualLength );

void PNFCControllerGetRFActivity( tContext* pContext, uint8_t * pnReaderState, uint8_t * pnCardState, uint8_t * pnP2PState );

void PNFCControllerGetRFLock( tContext* pContext, uint32_t nLockSet, bool_t * pbReaderLock, bool_t * pbCardLock );

W_ERROR PNFCControllerMonitorException( tContext* pContext, tPBasicGenericEventHandler * pHandler, void * pHandlerParameter, W_HANDLE * phEventRegistry );

W_ERROR PNFCControllerMonitorFieldEvents( tContext* pContext, tPBasicGenericEventHandler * pHandler, void * pHandlerParameter, W_HANDLE * phEventRegistry );

void PNFCControllerProductionTest( tContext* pContext, const uint8_t * pParameterBuffer, uint32_t nParameterBufferLength, uint8_t * pResultBuffer, uint32_t nResultBufferLength, tPBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter );

W_ERROR PNFCControllerRegisterRawListener( tContext* pContext, tPBasicGenericDataCallbackFunction * pReceiveMessageEventHandler, void * pHandlerParameter );

void PNFCControllerReset( tContext* pContext, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint32_t nMode );

void PNFCControllerSelfTest( tContext* pContext, tPNFCControllerSelfTestCompleted * pCallback, void * pCallbackParameter );

void PNFCControllerSetRFLock( tContext* pContext, uint32_t nLockSet, bool_t bReaderLock, bool_t bCardLock, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter );

W_ERROR PNFCControllerSwitchStandbyMode( tContext* pContext, bool_t bStandbyOn );

void PNFCControllerSwitchToRawMode( tContext* pContext, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter );

void PNFCControllerWriteRawMessage( tContext* pContext, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nLength );

void PP2PConnect( tContext* pContext, W_HANDLE hSocket, W_HANDLE hLink, tPBasicGenericCallbackFunction * pEstablishmentCallback, void * pEstablishmentCallbackParameter );

W_ERROR PP2PCreateSocket( tContext* pContext, uint8_t nType, const char16_t * pServiceURI, uint8_t nSAP, W_HANDLE * phSocket );

W_HANDLE PP2PEstablishLinkDriver1( tContext* pContext, tPBasicGenericHandleCallbackFunction * pEstablishmentCallback, void * pEstablishmentCallbackParameter );

void PP2PEstablishLinkDriver2( tContext* pContext, W_HANDLE hLink, tPBasicGenericCallbackFunction * pReleaseCallback, void * pReleaseCallbackParameter, W_HANDLE * phOperation );

W_ERROR PP2PGetConfiguration( tContext* pContext, tWP2PConfiguration * pConfiguration );

W_ERROR PP2PGetLinkProperties( tContext* pContext, W_HANDLE hLink, tWP2PLinkProperties * pProperties );

W_ERROR PP2PGetSocketParameter( tContext* pContext, W_HANDLE hSocket, uint32_t nParameter, uint32_t * pnValue );

void PP2PRead( tContext* pContext, W_HANDLE hSocket, tPBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pReceptionBuffer, uint32_t nReceptionBufferLength, W_HANDLE * phOperation );

void PP2PRecvFrom( tContext* pContext, W_HANDLE hSocket, tPP2PRecvFromCompleted * pCallback, void * pCallbackParameter, uint8_t * pReceptionBuffer, uint32_t nReceptionBufferLength, W_HANDLE * phOperation );

void PP2PSendTo( tContext* pContext, W_HANDLE hSocket, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t nSAP, const uint8_t * pSendBuffer, uint32_t nSendBufferLength, W_HANDLE * phOperation );

W_ERROR PP2PSetConfiguration( tContext* pContext, const tWP2PConfiguration * pConfiguration );

W_ERROR PP2PSetSocketParameter( tContext* pContext, W_HANDLE hSocket, uint32_t nParameter, uint32_t nValue );

void PP2PShutdown( tContext* pContext, W_HANDLE hSocket, tPBasicGenericCallbackFunction * pReleaseCallback, void * pReleaseCallbackParameter );

void PP2PURILookup( tContext* pContext, W_HANDLE hLink, tPP2PURILookupCompleted * pCallback, void * pCallbackParameter, const char16_t * pServiceURI );

void PP2PWrite( tContext* pContext, W_HANDLE hSocket, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pSendBuffer, uint32_t nSendBufferLength, W_HANDLE * phOperation );

W_ERROR PPicoGetConnectionInfo( tContext* pContext, W_HANDLE hConnection, tWPicoConnectionInfo * pConnectionInfo );


#ifdef P_INCLUDE_JAVA_API
W_ERROR PPicoGetConnectionInfoBuffer( tContext* pContext, W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR PPicoIsWritable( tContext* pContext, W_HANDLE hConnection );

void PPicoRead( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, W_HANDLE * phOperation );

void PPicoWrite( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, bool_t bLockCard, W_HANDLE * phOperation );

W_ERROR PReaderErrorEventRegister( tContext* pContext, tPBasicGenericEventHandler * pHandler, void * pHandlerParameter, uint8_t nEventType, bool_t bCardDetectionRequested, W_HANDLE * phRegistryHandle );

void PReaderExchangeDataEx( tContext* pContext, W_HANDLE hConnection, uint8_t nPropertyIdentifier, tPBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pReaderToCardBuffer, uint32_t nReaderToCardBufferLength, uint8_t * pCardToReaderBuffer, uint32_t nCardToReaderBufferMaxLength, W_HANDLE * phOperation );

W_ERROR PReaderGetIdentifier( tContext* pContext, W_HANDLE hConnection, uint8_t * pIdentifierBuffer, uint32_t nIdentifierBufferMaxLength, uint32_t * pnIdentifierActualLength );

W_ERROR PReaderGetPulsePeriod( tContext* pContext, uint32_t * pnTimeout );

void PReaderHandlerWorkPerformed( tContext* pContext, W_HANDLE hConnection, bool_t bGiveToNextListener, bool_t bCardApplicationMatch );

bool_t PReaderIsPropertySupported( tContext* pContext, uint8_t nPropertyIdentifier );

W_ERROR PReaderListenToCardDetection( tContext* pContext, tPReaderCardDetectionHandler * pHandler, void * pHandlerParameter, uint8_t nPriority, const uint8_t * pConnectionPropertyArray, uint32_t nPropertyNumber, W_HANDLE * phEventRegistry );

W_ERROR PReaderListenToCardRemovalDetection( tContext* pContext, W_HANDLE hConnection, tPBasicGenericEventHandler * pEventHandler, void * pCallbackParameter, W_HANDLE * phEventRegistry );

bool_t PReaderPreviousApplicationMatch( tContext* pContext, W_HANDLE hConnection );

void PReaderSetPulsePeriod( tContext* pContext, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint32_t nPulsePeriod );

void PRoutingTableApply( tContext* pContext, W_HANDLE hRoutingTable, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter );

W_ERROR PRoutingTableCreate( tContext* pContext, W_HANDLE * phRoutingTable );

void PRoutingTableEnable( tContext* pContext, bool_t bIsEnabled, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter );

W_ERROR PRoutingTableGetEntry( tContext* pContext, W_HANDLE hRoutingTable, uint16_t nEntryIndex, tWRoutingTableEntry * pRoutingTableEntry );

W_ERROR PRoutingTableGetEntryCount( tContext* pContext, W_HANDLE hRoutingTable, uint16_t * pnEntryCount );

W_ERROR PRoutingTableIsEnabled( tContext* pContext, bool_t * pbIsEnabled );

W_ERROR PRoutingTableModify( tContext* pContext, W_HANDLE hRoutingTable, uint32_t nOperation, uint16_t nEntryIndex, const tWRoutingTableEntry * pRoutingTableEntry );

void PRoutingTableRead( tContext* pContext, tPBasicGenericHandleCallbackFunction * pCallback, void * pCallbackParameter );

bool_t PRTDIsTextRecord( tContext* pContext, W_HANDLE hRecord );

bool_t PRTDIsURIRecord( tContext* pContext, W_HANDLE hRecord );

W_ERROR PRTDTextAddRecord( tContext* pContext, W_HANDLE hMessage, const char16_t * pLanguage, bool_t bUseUtf8, const char16_t * pText, uint32_t nTextLength );

W_ERROR PRTDTextCreateRecord( tContext* pContext, const char16_t * pLanguage, bool_t bUseUtf8, const char16_t * pText, uint32_t nTextLength, W_HANDLE * phRecord );

W_ERROR PRTDTextFind( tContext* pContext, W_HANDLE hMessage, const char16_t * pLanguage1, const char16_t * pLanguage2, W_HANDLE * phRecord, uint8_t * pnMatch );

W_ERROR PRTDTextGetLanguage( tContext* pContext, W_HANDLE hRecord, char16_t * pLanguageBuffer, uint32_t nBufferLength );

uint32_t PRTDTextGetLength( tContext* pContext, W_HANDLE hRecord );

W_ERROR PRTDTextGetValue( tContext* pContext, W_HANDLE hRecord, char16_t * pBuffer, uint32_t nBufferLength );

uint8_t PRTDTextLanguageMatch( tContext* pContext, W_HANDLE hRecord, const char16_t * pLanguage1, const char16_t * pLanguage2 );

W_ERROR PRTDURIAddRecord( tContext* pContext, W_HANDLE hMessage, const char16_t * pURI );

W_ERROR PRTDURICreateRecord( tContext* pContext, const char16_t * pURI, W_HANDLE * phRecord );

uint32_t PRTDURIGetLength( tContext* pContext, W_HANDLE hRecord );

W_ERROR PRTDURIGetValue( tContext* pContext, W_HANDLE hRecord, char16_t * pBuffer, uint32_t nBufferLength );

W_ERROR PSECheckAIDAccess( tContext* pContext, uint32_t nSlotIdentifier, const uint8_t * pAIDBuffer, uint32_t nAIDLength, const uint8_t * pImpersonationDataBuffer, uint32_t nImpersonationDataBufferLength );

W_ERROR PSecurityAuthenticate( tContext* pContext, const uint8_t * pApplicationDataBuffer, uint32_t nApplicationDataBufferLength );

W_ERROR PSEGetConnectivityEventParameter( tContext* pContext, uint32_t nSlotIdentifier, uint8_t * pDataBuffer, uint32_t nBufferLength, uint32_t * pnActualDataLength );

W_ERROR PSEGetInfoEx( tContext* pContext, uint32_t nSlotIdentifier, tWSEInfoEx * pSEInfo );


#ifdef P_INCLUDE_JAVA_API
W_ERROR PSEGetInfoExBuffer( tContext* pContext, uint32_t nSlotIdentifier, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */

void PSEGetStatus( tContext* pContext, uint32_t nSlotIdentifier, tPSEGetStatusCompleted * pCallback, void * pCallbackParameter );

uint32_t PSEGetTransactionAID( tContext* pContext, uint32_t nSlotIdentifier, uint8_t * pBuffer, uint32_t nBufferLength );

W_ERROR PSEMonitorConnectivityEvent( tContext* pContext, uint32_t nSlotIdentifier, tPBasicGenericEventHandler2 * pHandler, void * pHandlerParameter, W_HANDLE * phEventRegistry );

W_ERROR PSEMonitorEndOfTransaction( tContext* pContext, uint32_t nSlotIdentifier, tPBasicGenericEventHandler2 * pHandler, void * pHandlerParameter, W_HANDLE * phEventRegistry );

W_ERROR PSEMonitorHotPlugEvents( tContext* pContext, uint32_t nSlotIdentifier, tPBasicGenericEventHandler2 * pHandler, void * pHandlerParameter, W_HANDLE * phEventRegistry );

void PSEOpenConnection( tContext* pContext, uint32_t nSlotIdentifier, bool_t bForce, tPBasicGenericHandleCallbackFunction * pCallback, void * pCallbackParameter );

void PSESetPolicy( tContext* pContext, uint32_t nSlotIdentifier, uint32_t nStorageType, uint32_t nProtocols, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter );


#ifdef P_INCLUDE_TEST_ENGINE
void * PTestAlloc( tContext* pContext, uint32_t nSize );

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void PTestExecuteRemoteFunction( tContext* pContext, const char * pFunctionIdentifier, uint32_t nParameter, const uint8_t * pParameterBuffer, uint32_t nParameterBufferLength, uint8_t * pResultBuffer, uint32_t nResultBufferLength, tPBasicGenericDataCallbackFunction * pCallback, void * pCallbackParameter );

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void PTestFree( tContext* pContext, void * pBuffer );

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
const void * PTestGetConstAddress( tContext* pContext, const void * pConstData );

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
uint32_t PTestGetCurrentTime( tContext* pContext );

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
tTestExecuteContext * PTestGetExecuteContext( tContext* pContext );

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
bool_t PTestIsInAutomaticMode( tContext* pContext );

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void PTestMessageBox( tContext* pContext, uint32_t nFlags, const char * pMessage, uint32_t nAutomaticResult, tPTestMessageBoxCompleted * pCallback, void * pCallbackParameter );

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void PTestNotifyEnd( tContext* pContext );

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void PTestPresentObject( tContext* pContext, const char * pObjectName, const char * pOperatorMessage, uint32_t nDistance, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter );

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void PTestRemoveObject( tContext* pContext, const char * pOperatorMessage, bool_t bSaveState, bool_t bCheckUnmodifiedState, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter );

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void PTestSetErrorResult( tContext* pContext, uint32_t nResult, const char * pMessage );

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void PTestSetResult( tContext* pContext, uint32_t nResult, const void * pResultData, uint32_t nResultDataLength );

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void PTestSetTimer( tContext* pContext, uint32_t nTimeout, tPBasicGenericCompletionFunction * pCallback, void * pCallbackParameter );

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void PTestTraceBuffer( tContext* pContext, const uint8_t * pBuffer, uint32_t nLength );

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void PTestTraceError( tContext* pContext, const char * pMessage, va_list args );

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void PTestTraceInfo( tContext* pContext, const char * pMessage, va_list args );

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
void PTestTraceWarning( tContext* pContext, const char * pMessage, va_list args );

#endif /* #ifdef P_INCLUDE_TEST_ENGINE */

W_ERROR PType1ChipGetConnectionInfo( tContext* pContext, W_HANDLE hConnection, tWType1ChipConnectionInfo * pConnectionInfo );


#ifdef P_INCLUDE_JAVA_API
W_ERROR PType1ChipGetConnectionInfoBuffer( tContext* pContext, W_HANDLE hConnection, uint8_t * pInfoBuffer, uint32_t nInfoBufferLength );

#endif /* #ifdef P_INCLUDE_JAVA_API */

W_ERROR PType1ChipIsWritable( tContext* pContext, W_HANDLE hConnection );

void PType1ChipRead( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, W_HANDLE * phOperation );

void PType1ChipWrite( tContext* pContext, W_HANDLE hConnection, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter, const uint8_t * pBuffer, uint32_t nOffset, uint32_t nLength, bool_t bLockBlocks, W_HANDLE * phOperation );


#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
void PUICCActivateSWPLine( tContext* pContext, tPBasicGenericCallbackFunction * pCallback, void * pCallbackParameter );

#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */


#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
void PUICCGetSlotInfo( tContext* pContext, tPUICCGetSlotInfoCompleted * pCallback, void * pCallbackParameter );

#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

W_ERROR PVirtualTagCreate( tContext* pContext, uint8_t nTagType, const uint8_t * pIdentifier, uint32_t nIdentifierLength, uint32_t nMaximumMessageLength, W_HANDLE * phVirtualTag );

void PVirtualTagStart( tContext* pContext, W_HANDLE hVirtualTag, tPBasicGenericCallbackFunction * pCompletionCallback, void * pCompletionCallbackParameter, tPBasicGenericEventHandler * pEventCallback, void * pEventCallbackParameter, bool_t bReadOnly );

void PVirtualTagStop( tContext* pContext, W_HANDLE hVirtualTag, tPBasicGenericCallbackFunction * pCompletionCallback, void * pCallbackParameter );

#ifndef OPEN_NFC_BUILD_NUMBER
#define OPEN_NFC_BUILD_NUMBER 14883
#define OPEN_NFC_BUILD_NUMBER_S  { '1', '4', '8', '8', '3',  0 }
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
#define OPEN_NFC_PRODUCT_VERSION_BUILD v4.4.3 (Build 14883)
#define OPEN_NFC_PRODUCT_VERSION_BUILD_S  { 'v', '4', '.', '4', '.', '3', ' ', '(', 'B', 'u', 'i', 'l', 'd', ' ', '1', '4', '8', '8', '3', ')',  0 }
#endif /* #ifndef OPEN_NFC_PRODUCT_VERSION_BUILD */


#endif /* #ifdef __WME_CLIENT_API_AUTOGEN_H */
