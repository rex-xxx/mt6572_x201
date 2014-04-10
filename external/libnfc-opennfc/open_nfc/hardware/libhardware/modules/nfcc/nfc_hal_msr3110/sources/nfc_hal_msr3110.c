/*
* Copyright (c) 2007-2010 Inside Secure, All Rights Reserved.
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

NFC HAL Binding for the MSR3110 module

*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( MSR3110)

#include "nal_porting_os.h"
#include "nal_porting_hal.h"
#include "nfc_hal.h"
//#include "linux_nal_porting_hal.h"


/* for the definition of constant values */
#include "open_nfc.h"

typedef struct __tNALMsr3110Instance
{
    P_NAL_SYNC_CS  hCriticalSection;

    tNALInstance * pNALInstance;

    /* the reception parameters */
    uint8_t* pReceptionBuffer;
    uint32_t nReceptionBufferLength;
    tNALBindingReadCompleted* pReadCallbackFunction;
    void* pReadCallbackParameter;

    /* the connection parameters */
    tNALBindingConnectCompleted* pConnectCallbackFunction;
    void* pConnectCallbackParameter;

    /* The write parameters */
    tNALBindingWriteCompleted* pWriteCallbackFunction;
    void* pWriteCallbackParameter;

    /* The reception counter */
    uint32_t nReceptionCounter;

    /* The connection flag */
    bool_t bIsConnected;

    tNALBindingTimerHandler* pTimerHandlerFunction;

    tNALMsrComInstance* pComPort;
    tNALTimerInstance* pTimer;

    void* pPortingConfig;
    void* pCallbackContext;

    uint32_t nMode;

} tNALMsr3110Instance;

/* See NFC HAL Binding header file */
static void static_NALBindingReset(
    tNALVoidContext* pNALContext)
{
    tNALMsr3110Instance* pInstance = (tNALMsr3110Instance*)pNALContext;

    PNALDebugLog("[static_NALBindingReset]");

    CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);

    pInstance->pConnectCallbackFunction = null;
    pInstance->pConnectCallbackParameter = null;

    pInstance->nReceptionCounter = 0;

    pInstance->bIsConnected = W_FALSE;

    CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);
}

/* See NFC HAL Binding header file */
static void static_NALBindingConnect(
    tNALVoidContext* pNALContext,
    uint32_t nType,
    tNALBindingConnectCompleted* pCallbackFunction,
    void* pCallbackParameter)
{
    tNALMsr3110Instance* pInstance = (tNALMsr3110Instance*)pNALContext;

    PNALDebugLog("[static_NALBindingConnect]");
    CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);

    pInstance->pConnectCallbackFunction = pCallbackFunction;
    pInstance->pConnectCallbackParameter = pCallbackParameter;
    CNALResetNFCController(pInstance->pPortingConfig, nType);

    CNALSyncTriggerEventPump(pInstance->pPortingConfig);

    CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);
}

/* See NFC HAL Binding header file */
static void static_NALBindingWrite(
    tNALVoidContext* pNALContext,
    uint8_t* pBuffer,
    uint32_t nLength,
    tNALBindingWriteCompleted* pCallbackFunction,
    void* pCallbackParameter )
{
    tNALMsr3110Instance* pInstance = (tNALMsr3110Instance*)pNALContext;
    PNALDebugLog("static_NALBindingWrite start...");
    PNALDebugLog("Write Buf len=%d, buf:", nLength);
    PNALDebugLogBuffer(pBuffer, nLength);
    CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);

    CNALDebugAssert(pInstance->bIsConnected == W_TRUE);
    CNALDebugAssert(pInstance->pWriteCallbackFunction == null);

    CNALComWriteBytes(pInstance->pComPort,pBuffer, nLength);

    pInstance->pWriteCallbackFunction = pCallbackFunction;
    pInstance->pWriteCallbackParameter = pCallbackParameter;

    CNALSyncTriggerEventPump(pInstance->pPortingConfig);

    CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);
    PNALDebugLog("static_NALBindingWrite end...");
}

/* See NFC HAL Binding header file */
static void static_NALBindingPoll(
    tNALVoidContext* pNALContext)
{
    tNALMsr3110Instance* pInstance = (tNALMsr3110Instance*)pNALContext;
    PNALDebugLog("static_NALBindingPoll start...");
    CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);

    if(CNALTimerIsTimerElapsed( pInstance->pTimer ))
    {
        CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);
        pInstance->pTimerHandlerFunction( pInstance->pCallbackContext );
        CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);
    }

    if(pInstance->bIsConnected != W_FALSE)
    {
        int32_t nResult;

        if(pInstance->pWriteCallbackFunction != null)
        {
            PNALDebugLog("static_NALBindingPoll: call pWriteCallback start");
            tNALBindingWriteCompleted * pWriteCallback;

            pWriteCallback = pInstance->pWriteCallbackFunction;
            pInstance->pWriteCallbackFunction = null;

            CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);

            pWriteCallback(
                pInstance->pCallbackContext,
                pInstance->pWriteCallbackParameter,
                pInstance->nReceptionCounter++);

            CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);
            PNALDebugLog("static_NALBindingPoll: call pWriteCallback done");
        }


        nResult = CNALComReadBytes(pInstance->pComPort, pInstance->pReceptionBuffer, pInstance->nReceptionBufferLength);
        PNALDebugLog("static_NALBindingPoll: call CNALComReadBytes, result= %d", nResult);
        if( nResult > 0 )
        {
            CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);

            uint8_t cmdCode;

            cmdCode = pInstance->pReceptionBuffer[1];
            PNALDebugTrace("[static_NALBindingPoll]cmdCode_0: %02x", cmdCode );

			if( (( cmdCode & 0xF0) == NAL_MESSAGE_TYPE_EVENT) && (pInstance->pComPort->ComReadWriteCtrl > 0))
			{
				PNALDebugTrace("[static_NALBindingPoll]pComPort->ComReadWriteCtrl: %d", pInstance->pComPort->ComReadWriteCtrl);
				return;
			}
			pInstance->pReadCallbackFunction(
				pInstance->pCallbackContext,
				pInstance->pReadCallbackParameter,
				(uint32_t)nResult,
				pInstance->nReceptionCounter++);
			///TODO: Check Write buffer

            PNALDebugTrace("[static_NALBindingPoll]cmdCode_1: %02x", cmdCode );
			PNALDebugTrace("[static_NALBindingPoll]pReceptionBuffer[1]: %02x", ( cmdCode & 0xF0) );
			
			if( ( cmdCode & 0xF0) != NAL_MESSAGE_TYPE_EVENT)
            {
                pInstance->pComPort->ComReadWriteCtrl--;				
			    PNALDebugTrace("[static_NALBindingPoll]pComPort->ComReadWriteCtrl_1: %d", pInstance->pComPort->ComReadWriteCtrl);
				if (pInstance->pComPort->sendDetectCmdDone == W_TRUE)
				{
				    pInstance->pComPort->sendDetectCmdDone = W_FALSE;
                    PNALDebugTrace("[static_NALBindingPoll]Start_IRQ_Detect");
                    Start_IRQ_Detect(pInstance->pComPort);   //wakeup irq thread    
				}
            }
            //Target used
			if( cmdCode == NAL_EVT_P2P_INITIATOR_DISCOVERED)
            {
                PNALDebugTrace("[static_NALBindingPoll]Target: discover evt already send" );
				pInstance->pComPort->sendTargetSddEvt = W_TRUE;
				if (pInstance->pComPort->temp_target_buf_len > 0)
				{
                    PNALDebugTrace("[static_NALBindingPoll]Target: send temp_target_buf" );
					usleep(10000); 
					SetEventAvailableBytes(pInstance->pComPort, pInstance->pComPort->temp_target_buf_len);
				}
            }			
			CNALWriteDataFinish(pInstance->pComPort);


            CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);
        }
    }
    else
    {
        if(pInstance->pConnectCallbackFunction != null)
        {
            pInstance->pConnectCallbackFunction(
                pInstance->pCallbackContext, pInstance->pConnectCallbackParameter, 0);

            pInstance->pConnectCallbackFunction = null;
            pInstance->pConnectCallbackParameter = null;

            pInstance->bIsConnected = W_TRUE;
        }
    }

    CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);
    PNALDebugLog("static_NALBindingPoll end...");
}

/* See NFC HAL Binding header file */
static void static_NALBindingDestroy(
    tNALVoidContext* pNALContext)
{
    tNALMsr3110Instance* pInstance = (tNALMsr3110Instance*)pNALContext;
    PNALDebugLog("[static_NALBindingDestroy]");

    if(pInstance != null)
    {

        CNALPreDestroy(pInstance->pNALInstance);

        CNALComDestroy(pInstance->pComPort);

        CNALTimerDestroy(pInstance->pTimer);

        CNALSyncDestroyCriticalSection(&pInstance->hCriticalSection);

        CNALPostDestroy(pInstance->pNALInstance);

        CNALMemoryFree(pInstance);

    }
}

/* See NFC HAL Binding header file */
static tNALVoidContext* static_NALBindingCreate(
    void* pPortingConfig,
    void* pCallbackContext,
    uint8_t* pReceptionBuffer,
    uint32_t nReceptionBufferLength,
    tNALBindingReadCompleted* pReadCallbackFunction,
    void* pCallbackParameter,
    uint32_t nAutoStandbyTimeout,
    uint32_t nStandbyTimeout,
    tNALBindingTimerHandler* pTimerHandlerFunction,
    tNALBindingAntropySourceHandler* pAntropySourceHandlerFunction)
{
    uint32_t nComType;
    tNALMsr3110Instance* pInstance = (tNALMsr3110Instance*)CNALMemoryAlloc(
                                         sizeof(tNALMsr3110Instance));
    PNALDebugLog("static_NALBindingCreate: start...");
    CNALMemoryFill(pInstance, 0x00, sizeof(tNALMsr3110Instance));

    CNALSyncCreateCriticalSection(&pInstance->hCriticalSection);

    pInstance->pReceptionBuffer = pReceptionBuffer;
    pInstance->nReceptionBufferLength = nReceptionBufferLength;
    pInstance->pReadCallbackFunction = pReadCallbackFunction;
    pInstance->pReadCallbackParameter = pCallbackParameter;

    pInstance->pTimerHandlerFunction = pTimerHandlerFunction;

    pInstance->pPortingConfig = pPortingConfig;
    pInstance->pCallbackContext = pCallbackContext;

    pInstance->pConnectCallbackFunction = null;
    pInstance->pConnectCallbackParameter = null;

    pInstance->nReceptionCounter = 0;

    pInstance->bIsConnected = W_FALSE;

    PNALDebugLog("static_NALBindingCreate: CNALPreCreate");
    if ((pInstance->pNALInstance = CNALPreCreate(pPortingConfig)) == null)
    {
        PNALDebugError("static_NALBindingCreate: CNALPreCreate failed");
        goto return_error;
    }
    PNALDebugLog("static_NALBindingCreate: CNALComCreate");
    if((pInstance->pComPort = CNALComCreate(pPortingConfig, &nComType)) == null)
    {
        PNALDebugError("static_NALBindingCreateFunction: Cannot create the com port");
        goto return_error;
    }
    PNALDebugLog("static_NALBindingCreate: CNALTimerCreate");
    if((pInstance->pTimer = CNALTimerCreate(pPortingConfig)) == null)
    {
        PNALDebugError("static_NALBindingCreate: Cannot create the timer");
        goto return_error;
    }
    PNALDebugLog("static_NALBindingCreate: CNALPostCreate");
    if (CNALPostCreate(pInstance->pNALInstance, pInstance) == W_FALSE)
    {
        PNALDebugError("CNALPostCreate failed");
        goto return_error;
    }
    PNALDebugLog("static_NALBindingCreate: end...");
    return (tNALVoidContext*)pInstance;

return_error:
    PNALDebugLog("static_NALBindingCreate: error end...");
    static_NALBindingDestroy((tNALVoidContext*)pInstance);

    return null;
}

static uint32_t static_NALBindingGetVariable(
    tNALVoidContext* pNALContext,
    uint32_t nType)
{
    tNALMsr3110Instance* pInstance = (tNALMsr3110Instance*)pNALContext;
    uint32_t nValue = 0;

    CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);

    switch(nType)
    {
    case NAL_PARAM_SUB_MODE:
        nValue = W_NFCC_MODE_ACTIVE;  /* The sub mode is not simulated */
        break;
    case NAL_PARAM_MODE:
        nValue = pInstance->nMode;
        break;
    case NAL_PARAM_FIRMWARE_UPDATE:
        nValue = 0; /* The update progression is not simulated */
        break;
    case NAL_PARAM_CURRENT_TIME:
        nValue = CNALTimerGetCurrentTime(pInstance->pTimer);
        break;
    }

    CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);

    return nValue;
}

static void static_NALBindingSetVariable(
    tNALVoidContext* pNALContext,
    uint32_t nType,
    uint32_t nValue)
{
    tNALMsr3110Instance* pInstance = (tNALMsr3110Instance*)pNALContext;

    CNALSyncEnterCriticalSection(&pInstance->hCriticalSection);

    switch(nType)
    {
    case NAL_PARAM_MODE:
        pInstance->nMode = nValue;
        break;
    case NAL_PARAM_STATISTICS:
        /* Reset the protocol statistics */
        break;
    case NAL_PARAM_CURRENT_TIMER:
        if(nValue == 0)
        {
            CNALTimerCancel(pInstance->pTimer);
        }
        else
        {
            CNALTimerSet( pInstance->pTimer, nValue);
        }
        break;
    }

    CNALSyncLeaveCriticalSection(&pInstance->hCriticalSection);
}

static void static_NALBindingGetStatistics(
    tNALVoidContext* pNALContext,
    tNALProtocolStatistics* pStatistics)
{
    CNALMemoryFill(pStatistics, 0x00, sizeof(tNALProtocolStatistics));
}

static const tNALBinding g_sSimulatorNALBinding =
{
    NAL_BINDING_MAGIC_WORD,

    static_NALBindingCreate,
    static_NALBindingDestroy,
    static_NALBindingReset,
    static_NALBindingConnect,
    static_NALBindingWrite,
    static_NALBindingPoll,
    static_NALBindingGetVariable,
    static_NALBindingSetVariable,
    static_NALBindingGetStatistics
};

const tNALBinding* const g_pNALBinding = &g_sSimulatorNALBinding;

