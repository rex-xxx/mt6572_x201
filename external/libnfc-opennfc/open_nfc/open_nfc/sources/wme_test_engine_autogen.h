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

#ifndef __WME_TEST_ENGINE_AUTOGEN_H
#define __WME_TEST_ENGINE_AUTOGEN_H

const tTestAPI g_aTestAPI =
{

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   W14Part3ExchangeData,
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

   W14Part3ExchangeRawBits,
   W14Part3GetConnectionInfo,
   W14Part3ListenToCardDetectionTypeB,
   W14Part3SetTimeout,

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   W14Part4ExchangeData,
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

   W14Part4GetConnectionInfo,
   W14Part4ListenToCardDetectionTypeA,
   W14Part4ListenToCardDetectionTypeB,
   W14Part4SetNAD,
   W15GetConnectionInfo,
   W15IsWritable,
   W15ListenToCardDetection,
   W15Read,
   W15SetAttribute,
   W15SetTagSize,
   W15Write,
   W7816ExchangeAPDU,
   W7816GetAid,
   W7816GetATR,
   W7816GetATRSize,
   W7816GetResponseAPDUData,
   W7816OpenChannel,

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   W7816OpenLogicalChannel,
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

   WBasicCancelOperation,
   WBasicCheckConnectionProperty,
   WBasicCloseHandle,
   WBasicCloseHandleSafe,
   WBasicGetConnectionProperties,
   WBasicGetConnectionPropertyName,
   WBasicGetConnectionPropertyNumber,
   WBasicGetErrorString,
   WBPrimeGetConnectionInfo,
   WBPrimeListenToCardDetection,
   WBPrimeSetTimeout,
   WEmulClose,
   WEmulGetMessageData,
   WEmulIsPropertySupported,
   WEmulOpenConnection,
   WEmulSendAnswer,
   WFeliCaGetCardList,
   WFeliCaGetCardNumber,

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   WFeliCaGetConnectionInfo,
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

   WFeliCaListenToCardDetection,
   WFeliCaRead,
   WFeliCaSelectCard,
   WFeliCaSelectSystem,
   WFeliCaWrite,
   WHandoverAddBluetoothCarrier,
   WHandoverAddWiFiCarrier,
   WHandoverCreate,
   WHandoverFormatTag,
   WHandoverGetBluetoothInfo,
   WHandoverGetPairingInfo,
   WHandoverGetPairingInfoLength,
   WHandoverGetWiFiInfo,
   WHandoverPairingCompletion,
   WHandoverPairingStart,
   WHandoverRemoveAllCarrier,
   WMifareClassicAuthenticate,
   WMifareClassicGetConnectionInfo,
   WMifareClassicGetSectorInfo,
   WMifareClassicReadBlock,
   WMifareClassicWriteBlock,
   WMifareGetConnectionInfo,
   WMifareRead,
   WMifareULCAuthenticate,
   WMifareULCSetAccessRights,
   WMifareULForceULC,
   WMifareULFreezeDataLockConfiguration,
   WMifareULGetAccessRights,
   WMifareULRetrieveAccessRights,
   WMifareWrite,
   WMyDGetConnectionInfo,
   WMyDMoveAuthenticate,
   WMyDMoveFreezeDataLockConfiguration,
   WMyDMoveGetConfiguration,
   WMyDMoveSetConfiguration,
   WMyDRead,
   WMyDWrite,
   WNDEFAppendRecord,
   WNDEFBuildMessage,
   WNDEFBuildRecord,
   WNDEFCheckIdentifier,
   WNDEFCheckRecordType,
   WNDEFCheckType,
   WNDEFCompareRecordType,
   WNDEFCreateNestedMessageRecord,
   WNDEFCreateNewMessage,
   WNDEFCreateRecord,
   WNDEFGetEnclosedMessage,
   WNDEFGetIdentifierString,
   WNDEFGetMessageContent,
   WNDEFGetMessageLength,
   WNDEFGetNextMessage,
   WNDEFGetPayloadPointer,
   WNDEFGetRecord,
   WNDEFGetRecordInfo,
   WNDEFGetRecordInfoBuffer,
   WNDEFGetRecordNumber,
   WNDEFGetTagInfo,
   WNDEFGetTypeString,
   WNDEFHandlerWorkPerformed,
   WNDEFInsertRecord,
   WNDEFReadMessage,

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   WNDEFReadMessageOnAnyTag,
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

   WNDEFRegisterMessageHandler,
   WNDEFRemoveRecord,
   WNDEFSendMessage,
   WNDEFSetIdentifierString,
   WNDEFSetRecord,
   WNDEFSetRecordInfo,
   WNDEFWriteMessage,
   WNDEFWriteMessageOnAnyTag,
   WNFCControllerActivateSwpLine,
   WNFCControllerFirmwareUpdate,
   WNFCControllerFirmwareUpdateState,
   WNFCControllerGetBooleanProperty,
   WNFCControllerGetFirmwareProperty,
   WNFCControllerGetIntegerProperty,
   WNFCControllerGetMode,
   WNFCControllerGetProperty,
   WNFCControllerGetRawMessageData,
   WNFCControllerGetRFActivity,
   WNFCControllerGetRFLock,
   WNFCControllerMonitorException,
   WNFCControllerMonitorFieldEvents,
   WNFCControllerProductionTest,
   WNFCControllerRegisterRawListener,
   WNFCControllerReset,
   WNFCControllerSelfTest,
   WNFCControllerSetRFLock,
   WNFCControllerSwitchStandbyMode,
   WNFCControllerSwitchToRawMode,
   WNFCControllerWriteRawMessage,
   WP2PConnect,
   WP2PCreateSocket,
   WP2PEstablishLink,
   WP2PGetConfiguration,
   WP2PGetLinkProperties,
   WP2PGetSocketParameter,
   WP2PRead,
   WP2PRecvFrom,
   WP2PSendTo,
   WP2PSetConfiguration,
   WP2PSetSocketParameter,
   WP2PShutdown,
   WP2PURILookup,
   WP2PWrite,
   WPicoGetConnectionInfo,
   WPicoIsWritable,
   WPicoRead,
   WPicoWrite,
   WReaderErrorEventRegister,
   WReaderExchangeData,
   WReaderExchangeDataEx,
   WReaderGetIdentifier,
   WReaderGetPulsePeriod,
   WReaderHandlerWorkPerformed,
   WReaderIsPropertySupported,
   WReaderListenToCardDetection,
   WReaderListenToCardRemovalDetection,
   WReaderPreviousApplicationMatch,
   WReaderSetPulsePeriod,
   WRoutingTableApply,
   WRoutingTableCreate,
   WRoutingTableEnable,
   WRoutingTableGetEntry,
   WRoutingTableGetEntryCount,
   WRoutingTableIsEnabled,
   WRoutingTableModify,
   WRoutingTableRead,
   WRTDIsTextRecord,
   WRTDIsURIRecord,
   WRTDTextAddRecord,
   WRTDTextCreateRecord,
   WRTDTextFind,
   WRTDTextGetLanguage,
   WRTDTextGetLength,
   WRTDTextGetValue,
   WRTDTextLanguageMatch,
   WRTDURIAddRecord,
   WRTDURICreateRecord,
   WRTDURIGetLength,
   WRTDURIGetValue,
   WSECheckAIDAccess,
   WSecurityAuthenticate,
   WSEGetConnectivityEventParameter,
   WSEGetInfoEx,
   WSEGetStatus,
   WSEGetTransactionAID,
   WSEMonitorConnectivityEvent,
   WSEMonitorEndOfTransaction,
   WSEMonitorHotPlugEvents,
   WSEOpenConnection,
   WSESetPolicy,

#ifdef P_INCLUDE_TEST_ENGINE
   WTestAlloc,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestCompare,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestConvertUTF16ToUTF8,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestCopy,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestExecuteRemoteFunction,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestFill,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestFree,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestGetConstAddress,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestGetCurrentTime,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestGetExecuteContext,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestIsInAutomaticMode,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestMessageBox,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestMove,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestNotifyEnd,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestPresentObject,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestRemoveObject,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestSetErrorResult,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestSetResult,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestSetTimer,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestStringCompare,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestStringCopy,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestStringLength,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestTraceBuffer,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestTraceError,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestTraceInfo,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestTraceWarning,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestWriteDecimalUint32,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestWriteHexaUint32,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */


#ifdef P_INCLUDE_TEST_ENGINE
   WTestWriteHexaUint8,
#endif /* #ifdef P_INCLUDE_TEST_ENGINE */

   WType1ChipGetConnectionInfo,
   WType1ChipIsWritable,
   WType1ChipRead,
   WType1ChipWrite,

#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   WUICCGetSlotInfo,
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */


#ifdef P_INCLUDE_DEPRECATED_FUNCTIONS
   WUICCSetAccessPolicy,
#endif /* #ifdef P_INCLUDE_DEPRECATED_FUNCTIONS */

   WVirtualTagCreate,
   WVirtualTagStart,
   WVirtualTagStop,
};

#endif /* #ifdef __WME_TEST_ENGINE_AUTOGEN_H */
