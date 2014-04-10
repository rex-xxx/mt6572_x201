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

#define P_MODULE  P_MODULE_DEC( NAL_BIND_FW )

#include "nfc_hal_binding.h"

/******************************************************************************************************************
*                                         Definition
*******************************************************************************************************************/

/* firmware update timeout */
#ifndef TIMER_T5_TIMEOUT_UPDATE
#define TIMER_T5_TIMEOUT_UPDATE              500
#endif
#define TIMER_T5_TIMEOUT_REBOOT1             100
#define TIMER_T5_TIMEOUT_REBOOT2             100
#define TIMER_T5_TIMEOUT_REBOOT3             100
#define TIMER_T5_TIMEOUT_REBOOT4             100

/* Init Config */
#define P_NFC_HAL_BINDING_INIT_CONFIG_SWP_BIT                  0x0040

/* Firmware Update State Machine */
#define P_NFC_HAL_BINDING_UPDATE_EVENT_INITIAL                0x00

#define P_NFC_HAL_BINDING_UPDATE_EVENT_COMMAND_EXECUTED       0x01
#define P_NFC_HAL_BINDING_UPDATE_EVENT_TIMEOUT_ELAPSED        0x02

#define P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED      0x0200
#define P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_CANCEL        0x0400
#define P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_TIMEOUT       0x0800

#define P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_ENTER1             0x0010
#define P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT1            0x0011
#define P_NFC_HAL_BINDING_UPDATE_STATE_SWP_RESET1              0x0012
#define P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT2            0x0013
#define P_NFC_HAL_BINDING_UPDATE_STATE_PATCH_STATUS            0x0014
#define P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_CHUNK              0x0015
#define P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_EXIT1               0x0016
#define P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT3            0x0017
#define P_NFC_HAL_BINDING_UPDATE_STATE_SWP_RESET2              0x0018
#define P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT4            0x001A
#define P_NFC_HAL_BINDING_UPDATE_STATE_FIRMWARE_CONFIRM        0x001B
#define P_NFC_HAL_BINDING_UPDATE_STATE_SEND_RESULT             0x001C
#define P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_CHECKCRC           0x001D
#define P_NFC_HAL_BINDING_UPDATE_STATE_GET_INIT_CONFIG         0x001E
#define P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_ENTER2             0x001F
#define P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT5            0x0020
#define P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT6            0x0021
#define P_NFC_HAL_BINDING_UPDATE_STATE_SWP_RESET3              0x0022
#define P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_EXIT2              0x0023
#define P_NFC_HAL_BINDING_UPDATE_STATE_INIT_CONFIRM            0x0024
#define P_NFC_HAL_BINDING_UPDATE_STATE_INIT_STATUS             0x0025
#define P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_EXIT3              0x0026

#define P_NFC_HAL_BINDING_UPDATE_CONFIRM_INIT                0x10
#define P_NFC_HAL_BINDING_UPDATE_CONFIRM_FIRMWARE            0x11
#define P_NFC_HAL_BINDING_UPDATE_CHANGE_INIT                 0x12

#define P_NFC_HAL_BINDING_LOADER_EVENT_ENTER_LOADER            0
#define P_NFC_HAL_BINDING_LOADER_EVENT_LEAVE_LOADER            1
#define P_NFC_HAL_BINDING_LOADER_EVENT_COMMAND_EXECUTED        2
#define P_NFC_HAL_BINDING_LOADER_EVENT_TIMEOUT_ELAPSED         3

#define P_NFC_HAL_BINDING_LOADER_STATE_LOAD_ENTER              0
#define P_NFC_HAL_BINDING_LOADER_STATE_LOAD_EXIT               1
#define P_NFC_HAL_BINDING_LOADER_STATE_WAIT_REBOOT             2
#define P_NFC_HAL_BINDING_LOADER_STATE_RECONNECT               3
#define P_NFC_HAL_BINDING_LOADER_STATE_RECONNECTED             4

#define P_NFC_HAL_BINDING_CONFIG_SECTION_FIRMWARE      0x01
#define P_NFC_HAL_BINDING_CONFIG_SECTION_LOADER        0x02
#define P_NFC_HAL_BINDING_CONFIG_SECTION_PARAM         0x03
#define P_NFC_HAL_BINDING_CONFIG_SECTION_LOADER_PARAM  0x04
#define P_NFC_HAL_BINDING_CONFIG_SECTION_PARAM_SET     0x05
#define P_NFC_HAL_BINDING_CONFIG_SECTION_PARAM_MODIFY  0x06

#define P_NFC_HAL_BINDING_CONFIG_ACTION_CHECK          0
#define P_NFC_HAL_BINDING_CONFIG_ACTION_PERFORM        1

/******************************************************************************************************************
*                                         constants
*******************************************************************************************************************/

/* Firmware Update */
/*
 *  Reset
 *   7 6-5     4-0
 *  +-+-------------+
 *  |R| RFU |Nb of Boot | R: 0=No harware reboot
 *  +-+-------------+    1:Harware reboot
 *
 *  INIT_CONFIG
 *  15 14          8   7            0
 *  +-+-------------+ +--------------+
 *  |S|    L.S.B.   | |    M.S.B.    | S: 0=Ignore config
 *  +-+-------------+ +--------------+    1=Set config
 *

 */
static const uint8_t g_aInitConfig[] = { 0x00};

static const uint8_t g_aInitConfigParam[] = { HCI2_PAR_MGT_INIT_CONFIG_CURRENT, HCI2_PAR_MGT_INIT_CONFIG_BACKUP};
/* Firmware confirm config */
static const uint8_t g_aFirmwareConfirm[] = { 0x80 };
static const uint8_t g_aInitConfirm[]     = { 0x40 };

/**
  * Allocates a configuration operation
  */

tNALBindingConfigOperation * static_PNALBindingAllocConfigOperation(
   tNALBindingContext* pBindingContext)
{
   uint32_t i;

   for (i=0; i<P_NFC_HAL_BINDING_MAX_OPERATIONS; i++)
   {
      if (pBindingContext->aConfigOperations[i].nOperation == P_NFC_HAL_BINDING_CONFIG_NULL)
      {
         return &pBindingContext->aConfigOperations[i];
      }
   }

   return null;
}

/**
  * Frees a configuration operation
  */

void static_PNALBindingFreeConfigOperation(
   tNALBindingContext* pBindingContext,
   tNALBindingConfigOperation * pOperation)
{
   CNALDebugAssert(pOperation->nOperation != P_NFC_HAL_BINDING_CONFIG_NULL);
   pOperation->nOperation = P_NFC_HAL_BINDING_CONFIG_NULL;
}

/*-------CALLBACK-------------------------------------------------------*/

/**
 * Command   E X E C U T E D  OS Callback
 */
static void static_PNALBindingUpdateExecuteCommandCompleted(
         tNALBindingContext* pBindingContext,
             void* pCallbackParameter,
          uint8_t* pBuffer,
          uint32_t nLength,
           W_ERROR nError,
           uint8_t nStatusCode,
           uint32_t nReceptionCounter)
{
   PNALBindingUpdateMachine(
      pBindingContext, P_NFC_HAL_BINDING_UPDATE_EVENT_COMMAND_EXECUTED,
      pBuffer, nLength,
      nError, nStatusCode);
}

/**
 *@See definition of tPHCIServiceExecuteCommandCompleted
 **/
static void static_PNALBindingUpdateGetPatchStatusCompleted(
         tNALBindingContext* pBindingContext,
             void* pCallbackParameter,
          uint8_t* pBuffer,
          uint32_t nLength,
           W_ERROR nError,
           uint8_t nStatusCode,
           uint32_t nReceptionCounter)
{
   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   if (( nStatusCode != ETSI_ERR_ANY_OK )||
      ( nError != W_SUCCESS ))
   {
      PNALDebugError(
      "static_PNALBindingUpdateGetPatchStatusCompleted: nStatusCode 0x%08X, nError 0x%08X",
       nStatusCode, nError );
   }
   else if(( nLength != 0x03 )||
           ( pBuffer[2] != pBindingContext->nPatchStatus ))
   {
       /* Force error */
       nError = W_ERROR_BAD_NFCC_MODE;
       PNALDebugError(
          "static_PNALBindingUpdateGetPatchStatusCompleted: Patch status (expected 0x%08X) (received 0x%08X)",
       pBindingContext->nPatchStatus, pBuffer[2] );
   }
   PNALBindingUpdateMachine(
      pBindingContext, P_NFC_HAL_BINDING_UPDATE_EVENT_COMMAND_EXECUTED,
      pBuffer, nLength,
      nError, nStatusCode);
}

/**
 *@See definition of tPHCIServiceExecuteCommandCompleted
 **/
static void static_PNALBindingUpdateGetInitStatusCompleted(
         tNALBindingContext* pBindingContext,
             void* pCallbackParameter,
          uint8_t* pBuffer,
          uint32_t nLength,
           W_ERROR nError,
           uint8_t nStatusCode,
           uint32_t nReceptionCounter)
{
   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   if (( nStatusCode != ETSI_ERR_ANY_OK )||
       ( nError != W_SUCCESS )||
       ( nLength != 0x03 ))
   {
      PNALDebugError(
      "static_PNALBindingUpdateGetInitStatusCompleted: nStatusCode 0x%08X, nError 0x%08X",
       nStatusCode, nError );
      /* Force error: nLength may be wrong only */
       nError = W_ERROR_BAD_NFCC_MODE;
   }
   else
   {
      if(pBuffer[2] != pBindingContext->nInitStatus )
      {
          PNALDebugWarning(
             "static_PNALBindingUpdateGetInitStatusCompleted: Init status (expected 0x%08X) (received 0x%08X)",
            pBindingContext->nInitStatus, pBuffer[2] );
          pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_EXIT2 | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
          pBindingContext->aInitConfig[0] = 0x83;
          pBindingContext->nInitConfigLen  = 0x01;
      }
   }
   PNALBindingUpdateMachine(
      pBindingContext, P_NFC_HAL_BINDING_UPDATE_EVENT_COMMAND_EXECUTED,
      pBuffer, nLength,
      nError, nStatusCode);
}

/**
 *@See definition of tPHCIServiceExecuteCommandCompleted
 **/
static void static_PNALBindingUpdateGetInitConfigCompleted(
         tNALBindingContext* pBindingContext,
             void* pCallbackParameter,
          uint8_t* pBuffer,
          uint32_t nLength,
           W_ERROR nError,
           uint8_t nStatusCode,
           uint32_t nReceptionCounter)
{
   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   if (( nStatusCode != ETSI_ERR_ANY_OK )||
      ( nError != W_SUCCESS ))
   {
      PNALDebugError(
      "static_PNALBindingUpdateGetInitConfigCompleted: nStatusCode 0x%08X, nError 0x%08X",
       nStatusCode, nError );
   }
   else
   {
       if(    (pBuffer[0] != HCI2_PAR_MGT_INIT_CONFIG_CURRENT)
           || (pBuffer[1] != HCI2_PAR_MGT_INIT_CONFIG_CURRENT_MSG_SIZE)
           || (pBuffer[6] != HCI2_PAR_MGT_INIT_CONFIG_BACKUP)
           || (pBuffer[7] != HCI2_PAR_MGT_INIT_CONFIG_BACKUP_MSG_SIZE)
           || (nLength != 12 ))
        {
            /* Force error */
          nError = W_ERROR_BAD_NFCC_MODE;
          PNALDebugError( "static_PNALBindingUpdateGetInitConfigCompleted error: Get Init Config fails");
          PNALDebugTraceBuffer(pBuffer, 12);
        }
       else
       {
          uint16_t nCurrentConfig1 = static_PNALBindingReadUint16FromHCIBuffer(&pBuffer[2]);
          uint16_t nCurrentConfig2 = static_PNALBindingReadUint16FromHCIBuffer(&pBuffer[4]);
          uint16_t nBackupConfig1 = static_PNALBindingReadUint16FromHCIBuffer(&pBuffer[8]);
          uint16_t nBackupConfig2 = static_PNALBindingReadUint16FromHCIBuffer(&pBuffer[10]);

          if ((nCurrentConfig1 == nBackupConfig1) && (nCurrentConfig2 == nBackupConfig2))
          {
            if((nCurrentConfig1 & P_NFC_HAL_BINDING_INIT_CONFIG_SWP_BIT) != 0)
            {
               /* SWP line in battery on mode is set by Firmware: do nothing */
               pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_SEND_RESULT | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
            }
            else
            {
               /* Rebuild the Init config parameters: set SWP line in battery on bit */
               pBindingContext->aInitConfig[0] = 0x83;

               nCurrentConfig1 |= P_NFC_HAL_BINDING_INIT_CONFIG_SWP_BIT;
               static_PNALBindingWriteUint16ToHCIBuffer( nCurrentConfig1, &pBindingContext->aInitConfig[1]);
               static_PNALBindingWriteUint16ToHCIBuffer( nCurrentConfig2, &pBindingContext->aInitConfig[3]);
               pBindingContext->nInitConfigLen = 0x05;
            }
          }
          else
          {
             /* Force error */
             nError = W_ERROR_BAD_NFCC_MODE;

             PNALDebugError(
                "static_PNALBindingUpdateGetInitConfigCompleted: current Init Config (0x%04X 0x%04X)  different to backup Init config (0x%04X 0x%04X)",
                  nCurrentConfig1, nCurrentConfig2, nBackupConfig1, nBackupConfig2);
          }
       }
   }
   PNALBindingUpdateMachine(
      pBindingContext, P_NFC_HAL_BINDING_UPDATE_EVENT_COMMAND_EXECUTED,
      pBuffer, nLength,
      nError, nStatusCode);
}

static void static_PNALBindingExecuteFirmwareUpdateCompleted(
   tNALBindingContext * pBindingContext,
   void     * pCallbackParameter,
   uint8_t    nNALError);

static void static_PNALBindingUpdateSendResult(
                                    tNALBindingContext* pBindingContext,
                                    void    * pCallbackParameter,
                                    uint8_t   nNALError
                                    )
{
   if(nNALError != NAL_RES_OK)
   {
      /* Firmware update fails: Reset flags */
      pBindingContext->bIsFirmwareUpdateStarted  = W_FALSE;
   }

   PNALDFCPost2(pBindingContext,
      P_DFC_TYPE_NFC_HAL_BINDING,
      static_PNALBindingExecuteFirmwareUpdateCompleted, pCallbackParameter,
      PNALUtilConvertUint32ToPointer((uint32_t)nNALError));
}

/**
 * Time Out ELAPSED Callback
 */
static void static_PNALBindingUpdateTimeoutElapsed(
         tNALBindingContext* pBindingContext,
             void* pCallbackParameter)
{
   PNALDebugWarning("static_PNALBindingUpdateTimeoutElapsed: Timer elapsed");
   static_PNALBindingUpdateMachineDefault(pBindingContext, P_NFC_HAL_BINDING_UPDATE_EVENT_TIMEOUT_ELAPSED);
}

/**
 * Reboot Time Out ELAPSED Callback
 */
static void static_PNALBindingUpdateRebootTimeoutElapsed(
         tNALBindingContext* pBindingContext,
             void* pCallbackParameter)
{
   static_PNALBindingUpdateMachineDefault(pBindingContext, P_NFC_HAL_BINDING_UPDATE_EVENT_COMMAND_EXECUTED);
}

/**
 * Execute MGT Patch Status proprietaty command
 */
static void static_PNALBindingUpdateGetPatchStatus(
                      tNALBindingContext* pBindingContext )
{
   pBindingContext->aHCISendBuffer[0]= HCI2_PAR_MGT_PATCH_STATUS;

   PHCIServiceExecuteCommand(
      pBindingContext,
      P_HCI_SERVICE_PIPE_MANAGEMENT,
      &pBindingContext->sFirmwareUpdateOperation,
      P_HCI_SERVICE_CMD_GET_PROPERTIES,
      pBindingContext->aHCISendBuffer,
      0x01,
      pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
      static_PNALBindingUpdateGetPatchStatusCompleted,
      pBindingContext,
      W_FALSE );

   PNALMultiTimerSet( pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE, TIMER_T5_TIMEOUT_UPDATE,
      &static_PNALBindingUpdateTimeoutElapsed, null );
}

/**
 * Execute MGT Patch Status proprietaty command
 */
static void static_PNALBindingUpdateGetInitStatus(
                      tNALBindingContext* pBindingContext )
{
   pBindingContext->aHCISendBuffer[0]= HCI2_PAR_MGT_INIT_STATUS;

   PHCIServiceExecuteCommand(
      pBindingContext,
      P_HCI_SERVICE_PIPE_MANAGEMENT,
      &pBindingContext->sFirmwareUpdateOperation,
      P_HCI_SERVICE_CMD_GET_PROPERTIES,
      pBindingContext->aHCISendBuffer,
      0x01,
      pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
      static_PNALBindingUpdateGetInitStatusCompleted,
      pBindingContext,
      W_TRUE );

   PNALMultiTimerSet( pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE, TIMER_T5_TIMEOUT_UPDATE,
      &static_PNALBindingUpdateTimeoutElapsed, null );
}

/**
 * Execute MGT Patch Status proprietaty command
 */
static void static_PNALBindingUpdateGetInitConfig(
                      tNALBindingContext* pBindingContext )
{
   PHCIServiceExecuteCommand(
      pBindingContext,
      P_HCI_SERVICE_PIPE_MANAGEMENT,
      &pBindingContext->sFirmwareUpdateOperation,
      P_HCI_SERVICE_CMD_GET_PROPERTIES,
      g_aInitConfigParam, sizeof(g_aInitConfigParam),
      pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
      static_PNALBindingUpdateGetInitConfigCompleted,
      pBindingContext,
      W_FALSE );

   PNALMultiTimerSet( pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE, TIMER_T5_TIMEOUT_UPDATE,
      &static_PNALBindingUpdateTimeoutElapsed, null );
}

/**
 * Execute OS Load enter proprietaty command
 */
static void static_PNALBindingUpdateExecuteLoadEnter(
                 tNALBindingContext* pBindingContext )
{
   PHCIServiceExecuteCommand(
      pBindingContext,
      P_HCI_SERVICE_FIRMWARE,
      &pBindingContext->sFirmwareUpdateOperation,
      HCI2_CMD_OS_LOADER_ENTER,
      null, 0,
      pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
      static_PNALBindingUpdateExecuteCommandCompleted,
      pBindingContext,
      W_FALSE );

   PNALMultiTimerSet( pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE, TIMER_T5_TIMEOUT_UPDATE,
      &static_PNALBindingUpdateTimeoutElapsed, null );
}

/**
 * Execute OS Load Check Crc proprietaty command
 */
static void static_PNALBindingUpdateExecuteLoadCheckCrc(
                 tNALBindingContext* pBindingContext,
                 uint8_t *pBuffer,
                 uint32_t nLength )
{
   PHCIServiceExecuteCommand(
      pBindingContext,
      P_HCI_SERVICE_FIRMWARE,
      &pBindingContext->sFirmwareUpdateOperation,
      HCI2_CMD_OS_LOADER_CHECKCRC,
      pBuffer, nLength,
      pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
      static_PNALBindingUpdateExecuteCommandCompleted,
      pBindingContext,
      W_FALSE );

   PNALMultiTimerSet( pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE, TIMER_T5_TIMEOUT_UPDATE,
      &static_PNALBindingUpdateTimeoutElapsed, null );
}

/**
 * Execute OS Load exit proprietaty command
 */
static void static_PNALBindingUpdateExecuteLoadExit(
                      tNALBindingContext* pBindingContext,
                      const uint8_t *pBuffer,
                      uint32_t nLength)
{
   PHCIServiceExecuteCommand(
      pBindingContext,
      P_HCI_SERVICE_FIRMWARE,
      &pBindingContext->sFirmwareUpdateOperation,
      HCI2_CMD_OS_LOADER_QUIT,
      pBuffer, nLength,
      pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
      static_PNALBindingUpdateExecuteCommandCompleted,
      pBindingContext,
      W_FALSE );

   PNALMultiTimerSet( pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE, TIMER_T5_TIMEOUT_UPDATE,
      &static_PNALBindingUpdateTimeoutElapsed, null );
}

/**
 * Execute OS Load Block proprietaty command
 */

static void static_PNALBindingUpdateExecuteLoadBlock(
                       tNALBindingContext* pBindingContext,
                       uint8_t* aUpdateBuffer,
                       uint32_t nUpdateBufferLength )
{
   PHCIServiceExecuteCommand(
      pBindingContext,
      P_HCI_SERVICE_FIRMWARE,
      &pBindingContext->sFirmwareUpdateOperation,
      HCI2_CMD_OS_LOADER_LOADPAGE,
      aUpdateBuffer, nUpdateBufferLength,
      pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
      static_PNALBindingUpdateExecuteCommandCompleted,
      pBindingContext,
      W_FALSE );

   PNALMultiTimerSet( pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE, TIMER_T5_TIMEOUT_UPDATE,
      &static_PNALBindingUpdateTimeoutElapsed, null );
}

/**
 * Execute OS Load confirm proprietaty command
 */
static void static_PNALBindingUpdateExecuteLoadConfirm(
                 tNALBindingContext* pBindingContext,
                 const uint8_t * pBuffer,
                 uint32_t nLength)
{
   PHCIServiceExecuteCommand(
      pBindingContext,
      P_HCI_SERVICE_FIRMWARE,
      &pBindingContext->sFirmwareUpdateOperation,
      HCI2_CMD_OS_LOADER_CONFIRM,
      pBuffer, nLength,
      pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
      static_PNALBindingUpdateExecuteCommandCompleted,
      pBindingContext,
      W_FALSE );

   PNALMultiTimerSet( pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE, TIMER_T5_TIMEOUT_UPDATE,
      &static_PNALBindingUpdateTimeoutElapsed, null );
}

/**
 * Service   C O N N E C T E D   Callback
 */
static void static_PNALBindingUpdateServiceConnectCompleted(
         tNALBindingContext* pBindingContext,
             void* pCallbackParameter )
{
   static_PNALBindingUpdateMachineDefault(pBindingContext, P_NFC_HAL_BINDING_UPDATE_EVENT_COMMAND_EXECUTED);
}

/**
 * Firmware Update Machine State
 */
NFC_HAL_INTERNAL void PNALBindingUpdateMachine(
         tNALBindingContext*       pBindingContext,
         uint32_t        nEvent,
         uint8_t*        pBuffer,
         uint32_t        nLength,
         W_ERROR         nError,
         uint8_t         nStatusCode)
{
   PNALMultiTimerCancel( pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE );

   if ( nStatusCode != ETSI_ERR_ANY_OK )
   {
      PNALDebugError( "PNALBindingUpdateMachine: nStatusCode 0x%08X", nStatusCode );
      static_PNALBindingUpdateSendResult(pBindingContext, pBindingContext->pOperation, NAL_RES_BAD_STATE);
      return;
   }
   /**
    * Status Machine
    */

   switch ( nEvent )
   {
   case P_NFC_HAL_BINDING_UPDATE_EVENT_INITIAL:

      pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_ENTER1;
      break;

   case P_NFC_HAL_BINDING_UPDATE_EVENT_COMMAND_EXECUTED:

      if ( (pBindingContext->nUpdateState & P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_CANCEL) != 0x00 )
      {
         if ( (pBindingContext->nUpdateState & P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_TIMEOUT) != 0x00 )
         {
            static_PNALBindingUpdateSendResult(pBindingContext, pBindingContext->pOperation, NAL_RES_TIMEOUT);
         }
         else
         {
            static_PNALBindingUpdateSendResult(pBindingContext, pBindingContext->pOperation, NAL_RES_BAD_STATE);
         }

         return;
      }
      pBindingContext->nUpdateState &= ~P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      break;

   case P_NFC_HAL_BINDING_UPDATE_EVENT_TIMEOUT_ELAPSED:

      if ( (pBindingContext->nUpdateState & P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED) != 0x00 )
      {
         pBindingContext->nUpdateState &= ~P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
         pBindingContext->nUpdateState |= (P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_CANCEL | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_TIMEOUT);

         PHCIServiceCancelOperation(
            pBindingContext,
            P_HCI_SERVICE_FIRMWARE,
            &pBindingContext->sFirmwareUpdateOperation );

         return;
      }

      /* Boot stopped */
      PNALDebugError("PNALBindingUpdateMachine: NFCC not responding.");
      static_PNALBindingUpdateSendResult(pBindingContext, pBindingContext->pOperation, NAL_RES_TIMEOUT);
      return;
   }

   /**
    * STATE Machine
    * -------------
    *
    * Check nMode = MODE_MAINTENANCE / MODE_NO_FIRMWARE / MODE_FIRMWARE_NOT_SUPPORTED
    * ! ->Error
    *
    * nMode = MODE_BOOT
    *
    * Load Start
    *
    * Load Stop no reset (retry=0)
    *
    * Load Chunk until Load completed
    *
    * Load Stop no reset (retry=0)
    *
    * Check Load error
    * ! ->MODE_NO_FIRMWARE
    *
    * Load Confirm
    *
    * Call Boot State Machine
    *
    */

   switch ( pBindingContext->nUpdateState )
   {
   case  P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_ENTER1:/*
         ----------------------------*/

      /**
       * Load Start
       */

      PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_ENTER1");

      static_PNALBindingUpdateExecuteLoadEnter(pBindingContext);

      pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT1 | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      return;

   case  P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT1:/*
         ------------------------------*/

      PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT1");

      /* Check last Load Block result */
      if ( nError != W_SUCCESS )
      {
         break;
      }

      PNALMultiTimerSet( pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE, TIMER_T5_TIMEOUT_REBOOT1,
         &static_PNALBindingUpdateRebootTimeoutElapsed, null );

      pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_SWP_RESET1 | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      return;

   case  P_NFC_HAL_BINDING_UPDATE_STATE_SWP_RESET1:

      PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_SWP_RESET1");

      /* Check Load Start result */
      if ( nError != W_SUCCESS )
      {
         break;
      }

      PHCIServicePreReset( pBindingContext );

      PHCIServiceConnect(
         pBindingContext,
         static_PNALBindingUpdateServiceConnectCompleted, null );

      pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT2 | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      return;

   case  P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT2:/*
         ------------------------------*/

      PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT2");

      /* Check last Load Block result */
      if ( nError != W_SUCCESS )
      {
         break;
      }

      PNALMultiTimerSet( pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE, TIMER_T5_TIMEOUT_REBOOT2,
         &static_PNALBindingUpdateRebootTimeoutElapsed, null );

      pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_INIT_STATUS | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      return;

  case  P_NFC_HAL_BINDING_UPDATE_STATE_INIT_STATUS:/*
         -----------------------------*/

     PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_INIT_STATUS");

      /* Check Load Stop result */
      if ( nError != W_SUCCESS )
      {
         break;
      }

      /*
       * Init Status
       */
      pBindingContext->nInitStatus = HCI2_VAL_MGT_INIT_CONFIRMED;

      static_PNALBindingUpdateGetInitStatus(pBindingContext);

      if(pBindingContext->nUpdateStep == P_NFC_HAL_BINDING_UPDATE_CONFIRM_INIT)
      {
         pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_CHUNK | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      }
      else
      {
         pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_SEND_RESULT | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      }
      return;

   case  P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_CHUNK:/*
         ----------------------------*/

      PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_CHUNK");

      /* Check Load start/block result */
      if ( nError != W_SUCCESS )
      {
         break;
      }

      if(pBindingContext->nUpdateBufferLength >= P_NFC_HAL_BINDING_NFCC_LOAD_BLOCK_SIZE)
      {
         /* Copy data to send */
         CNALMemoryCopy(
                  &pBindingContext->aUpdateBuffer,
                  pBindingContext->pUpdateBuffer,
                  P_NFC_HAL_BINDING_NFCC_LOAD_BLOCK_SIZE);

         /* Update parameters */
         pBindingContext->nUpdateBufferLength -= P_NFC_HAL_BINDING_NFCC_LOAD_BLOCK_SIZE;
         pBindingContext->pUpdateBuffer += P_NFC_HAL_BINDING_NFCC_LOAD_BLOCK_SIZE;

         /*
          * Load Block
          */

          pBindingContext->nConfigProgression += P_NFC_HAL_BINDING_NFCC_LOAD_BLOCK_SIZE;

          static_PNALBindingUpdateExecuteLoadBlock(
             pBindingContext,
             pBindingContext->aUpdateBuffer,
             P_NFC_HAL_BINDING_NFCC_LOAD_BLOCK_SIZE );

         if(pBindingContext->nUpdateBufferLength == 0)
         {
             /* All binary has been sent */
              pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_CHECKCRC | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
         }
         else
         {
           pBindingContext->nUpdateState |= P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
         }
      }
      else
      {
         /* should not occur, the buffer contains n blocks */

         static_PNALBindingUpdateSendResult(pBindingContext, pBindingContext->pOperation, NAL_RES_BAD_DATA);
      }
      return;

   case  P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_CHECKCRC:/*
         ----------------------------*/

      PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_CHECKCRC");

      /* Check last Load Block result */
      if ( nError != W_SUCCESS )
      {
         break;
      }

      /**
       * Check crc
       **/
      static_PNALBindingUpdateExecuteLoadCheckCrc(
         pBindingContext,
         pBindingContext->aCheckCrcBuffer,
         sizeof(pBindingContext->aCheckCrcBuffer));

      pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_EXIT1 | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      return;

   case  P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_EXIT1:/*
         ----------------------------*/

      PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_EXIT1");

      /* Check last Load Block result */
      if ( nError != W_SUCCESS )
      {
         break;
      }

      /*
       * Exit loader mode
       */
      static_PNALBindingUpdateExecuteLoadExit(
         pBindingContext,
         g_aInitConfig,
         sizeof(g_aInitConfig));

      pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT3 | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      /* Entering firmware confirm step */
      pBindingContext->nUpdateStep = P_NFC_HAL_BINDING_UPDATE_CONFIRM_FIRMWARE;
      return;

   case  P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT3:/*
         ------------------------------*/

      PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT3");

      /* Check last Load Block result */
      if ( nError != W_SUCCESS )
      {
         break;
      }

      PNALMultiTimerSet( pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE, TIMER_T5_TIMEOUT_REBOOT3,
         &static_PNALBindingUpdateRebootTimeoutElapsed, null );

      pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_SWP_RESET2 | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      return;

   case  P_NFC_HAL_BINDING_UPDATE_STATE_SWP_RESET2:

      PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_SWP_RESET2");

      /* Check Load Start result */
      if ( nError != W_SUCCESS )
      {
         break;
      }

      PHCIServicePreReset( pBindingContext );

      PHCIServiceConnect(
         pBindingContext,
         static_PNALBindingUpdateServiceConnectCompleted, null );

      pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT4 | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      return;

   case  P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT4:/*
         ------------------------------*/

      PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT4");

      /* Check last Load Block result */
      if ( nError != W_SUCCESS )
      {
         break;
      }

      PNALMultiTimerSet( pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE, TIMER_T5_TIMEOUT_REBOOT4,
         &static_PNALBindingUpdateRebootTimeoutElapsed, null );

      if (pBindingContext->pOperation->nOperation == P_NFC_HAL_BINDING_CONFIG_FIRMWARE_UPDATE)
      {
         pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_FIRMWARE_CONFIRM | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      }
      else
      {
         pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_SEND_RESULT;
      }
      return;

   case  P_NFC_HAL_BINDING_UPDATE_STATE_FIRMWARE_CONFIRM:/*
         ------------------------------*/

      PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_FIRMWARE_CONFIRM");

      /* Check last Load Block result */
      if ( nError != W_SUCCESS )
      {
         break;
      }

      /*
       * Load Confirm
       */
      static_PNALBindingUpdateExecuteLoadConfirm(
                                          pBindingContext,
                                          g_aFirmwareConfirm,
                                          sizeof(g_aFirmwareConfirm));

      pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_PATCH_STATUS | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;

      pBindingContext->nPatchStatus = HCI2_VAL_MGT_PATCH_CONFIRMED;
      /* Set the next state machine after the get patch status */
      pBindingContext->nNextUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_GET_INIT_CONFIG | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      return;

      case  P_NFC_HAL_BINDING_UPDATE_STATE_PATCH_STATUS:/*
         -----------------------------*/

      PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_PATCH_STATUS");

      /* Check Load Stop result */
      if ( nError != W_SUCCESS )
      {
         break;
      }

      /*
       * Patch Status
       */
      static_PNALBindingUpdateGetPatchStatus(pBindingContext);
      pBindingContext->nUpdateState = pBindingContext->nNextUpdateState;
      /* Entering change inits step */
      pBindingContext->nUpdateStep = P_NFC_HAL_BINDING_UPDATE_CHANGE_INIT;
      return;

    case  P_NFC_HAL_BINDING_UPDATE_STATE_GET_INIT_CONFIG:/*
         ----------------------------*/

      PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_GET_INIT_CONFIG");

      /* Check last Load Block result */
      if ( nError != W_SUCCESS )
      {
         break;
      }

      /**
       * Get the backup and current inits
       **/
      static_PNALBindingUpdateGetInitConfig(pBindingContext);

      pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_ENTER2 | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      return;

    case  P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_ENTER2:/*
         ----------------------------*/

       PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_ENTER2");

      /**
       * Loader Enter
       */

      static_PNALBindingUpdateExecuteLoadEnter(pBindingContext);

      pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT5 | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      /* Set the state after NFCC reboot */
      pBindingContext->nNextUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_EXIT2 | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      return;

   case  P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT5:/*
         ------------------------------*/

      PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT5");

      /* Check result */
      if ( nError != W_SUCCESS )
      {
         break;
      }

      PNALMultiTimerSet( pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE, TIMER_T5_TIMEOUT_REBOOT3,
         &static_PNALBindingUpdateRebootTimeoutElapsed, null );

      pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_SWP_RESET3 | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      return;

   case  P_NFC_HAL_BINDING_UPDATE_STATE_SWP_RESET3:

      PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_SWP_RESET3");

      /* Check  result */
      if ( nError != W_SUCCESS )
      {
         break;
      }

      PHCIServicePreReset( pBindingContext );

      PHCIServiceConnect(
         pBindingContext,
         static_PNALBindingUpdateServiceConnectCompleted, null );

      pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT6 | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      return;

   case  P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT6:/*
         ------------------------------*/

      PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT6");

      /* Check last Load Block result */
      if ( nError != W_SUCCESS )
      {
         break;
      }

      PNALMultiTimerSet( pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE, TIMER_T5_TIMEOUT_REBOOT4,
         &static_PNALBindingUpdateRebootTimeoutElapsed, null );

      pBindingContext->nUpdateState = pBindingContext->nNextUpdateState;
      return;

   case  P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_EXIT2:/*
         ----------------------------*/

      PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_EXIT2");

      /* Check last Load Block result */
      if ( nError != W_SUCCESS )
      {
         break;
      }

      /*
       * Exit loader mode with Init config set or not
       */
       /* Set the Init config value */
       CNALMemoryCopy(
               pBindingContext->aHCISendBuffer,
               pBindingContext->aInitConfig,
               pBindingContext->nInitConfigLen);

      static_PNALBindingUpdateExecuteLoadExit(
         pBindingContext,
         pBindingContext->aHCISendBuffer,
         pBindingContext->nInitConfigLen);

      pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_WAIT_REBOOT5 | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      /* set the state after NFCC Reboot */
      pBindingContext->nNextUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_INIT_CONFIRM | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      return;

   case  P_NFC_HAL_BINDING_UPDATE_STATE_INIT_CONFIRM:/*
         ------------------------------*/

      PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_INIT_CONFIRM");

      /* Check result */
      if ( nError != W_SUCCESS )
      {
         break;
      }

      /*
       * Init Confirm
       */
      static_PNALBindingUpdateExecuteLoadConfirm(
                                          pBindingContext,
                                          g_aInitConfirm,
                                          sizeof(g_aInitConfirm));

      if(pBindingContext->nUpdateStep == P_NFC_HAL_BINDING_UPDATE_CONFIRM_INIT)
      {
         pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_LOAD_ENTER1 | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      }
      else
      {
         pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_INIT_STATUS | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
      }

      return;

    case  P_NFC_HAL_BINDING_UPDATE_STATE_SEND_RESULT:/*
         -----------------------*/

       PNALDebugTrace("PNALBindingUpdateMachine : P_NFC_HAL_BINDING_UPDATE_STATE_SEND_RESULT");

      /* Check Load Confirm result */
      if ( nError != W_SUCCESS )
      {
         break;
      }
      else
      {
         static_PNALBindingUpdateSendResult(pBindingContext, pBindingContext->pOperation, NAL_RES_OK);

         return;
      }
   } /* End of switch */

   /**
    *  Completed MODE_LOADER_NOT_SUPPORTED
    */
   PNALDebugError("PNALBindingUpdateMachine: Firmware Update error.");

   static_PNALBindingUpdateSendResult(pBindingContext, pBindingContext->pOperation, NAL_RES_BAD_STATE);
}

/* ------------------------------------------------------------------ */

static void static_PNALBindingConfigurationCompleted(
   tNALBindingContext * pBindingContext,
   W_ERROR    nError,
   uint8_t    nNALError)
{
   PNALDebugTrace("static_PNALBindingConfigurationCompleted : nError %d - nStatusCode %d", nError, nNALError);

   if (pBindingContext->bInPoll == W_FALSE)
   {
      PNALDFCPost2(pBindingContext, P_DFC_TYPE_NFC_HAL_BINDING, static_PNALBindingConfigurationCompleted, PNALUtilConvertUint32ToPointer(nError), PNALUtilConvertUint32ToPointer((uint32_t) nNALError));
   }
   else
   {
      pBindingContext->bInPoll = W_FALSE;
      CNALSyncLeaveCriticalSection(&pBindingContext->hCriticalSection);

      pBindingContext->pNALReceptionBuffer[0] = NAL_SERVICE_ADMIN;
      pBindingContext->pNALReceptionBuffer[1] = nNALError;

      pBindingContext->pNALReadCallbackFunction(
         pBindingContext->pCallbackContext,
         pBindingContext->pNALReadCallbackParameter,
         0x02, pBindingContext->nReceptionCounter);

      CNALSyncEnterCriticalSection(&pBindingContext->hCriticalSection);
      pBindingContext->bInPoll = W_TRUE;
   }
}

static void static_PNALBindingExecuteNextConfigOperation(tNALBindingContext * pBindingContext);

/* ------------------------------------------------------------------ */

static void static_PNALBindingExecuteFirmwareUpdateCompleted(
   tNALBindingContext * pBindingContext,
   void     * pCallbackParameter,
   uint8_t    nNALError)
{
   tNALBindingConfigOperation * pOperation  = (tNALBindingConfigOperation *) pCallbackParameter;

   PNALDebugTrace("static_PNALBindingExecuteFirmwareUpdateCompleted : nNALError %d", nNALError);

   /* no longer need the operation, free it */
   static_PNALBindingFreeConfigOperation(pBindingContext, pOperation);

   if (nNALError != NAL_RES_OK)
   {
      PNALDebugError("static_PNALBindingExecuteFirmwareUpdateCompleted : nNALError %d", nNALError);

      static_PNALBindingConfigurationCompleted(pBindingContext, W_ERROR_BAD_FIRMWARE_FORMAT, nNALError);
   }
   else
   {
      /* SET parameter succeeded, perform next operation */
      static_PNALBindingExecuteNextConfigOperation(pBindingContext);
   }
}

/**
  * Firmware Update
  *
  */
static void static_PNALBindingExecuteFirmwareUpdate(
   tNALBindingContext * pBindingContext,
   tNALBindingConfigOperation * pOperation)
{
   uint32_t nEvent;

   /* store the current operation */
   pBindingContext->pOperation = pOperation;

   /* entering in firmware update mode*/
   pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS;
   pBindingContext->nUpdateStep = P_NFC_HAL_BINDING_UPDATE_CONFIRM_INIT;

   /* pOperation->pBuffer points to a data area that contains
        - the firmware update description (including version, build....) : to be skipped
        - the firmware itself
        - the CRC */

   /* pBindingContext->pUpdateBuffer points to the firmware */
   pBindingContext->pUpdateBuffer = pOperation->Operation.sFirmwareUpdateOperation.pBuffer + 8;
   pBindingContext->nUpdateBufferLength = pOperation->Operation.sFirmwareUpdateOperation.nLength - 12;

   /* aCheckCrcBuffer contains the CRC */
   CNALMemoryCopy(pBindingContext->aCheckCrcBuffer, pBindingContext->pUpdateBuffer + pBindingContext->nUpdateBufferLength, 4);

   if((pBindingContext->nPatchStatus & HCI2_VAL_MGT_PATCH_LOADER) != 0x00)
   {
      nEvent = P_NFC_HAL_BINDING_UPDATE_EVENT_COMMAND_EXECUTED;
      pBindingContext->nUpdateState = P_NFC_HAL_BINDING_UPDATE_STATE_INIT_STATUS | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
   }
   else
   {
      nEvent = P_NFC_HAL_BINDING_UPDATE_EVENT_INITIAL;
   }

   pBindingContext->bIsFirmwareUpdateStarted = W_TRUE;

   /* call the state machine */
   static_PNALBindingUpdateMachineDefault(pBindingContext, nEvent);
}

/**
  * Execute Loader Update
  *
  */
static void static_PNALBindingExecuteLoaderUpdate(
   tNALBindingContext * pBindingContext,
   tNALBindingConfigOperation * pOperation)
{
   /* a loader update is basically the same as a firmware update,
      except that no confirmation must be done at the end of the update process.
      this is managed during update automaton */

   static_PNALBindingExecuteFirmwareUpdate(
      pBindingContext,
      pOperation);
}

/* ------------------------------------------------------------------ */

static void static_PNALBindingLoaderAutomaton(
      tNALBindingContext*       pBindingContext,
      uint32_t        nEvent,
      uint8_t*        pBuffer,
      uint32_t        nLength,
      W_ERROR         nError,
      uint8_t         nStatusCode);

/**
  * HCI2_CMD_OS_LOADER_ENTER command execution completion callback
  *
  */
static void static_PNALBindingExecuteLoaderCommandCompleted(
   tNALBindingContext* pBindingContext,
   void* pCallbackParameter,
   uint8_t* pBuffer,
   uint32_t nLength,
   W_ERROR nError,
   uint8_t nStatusCode,
   uint32_t nReceptionCounter)
{
   PNALDebugTrace("static_PNALBindingExecuteLoaderCommandCompleted nError %d - nStatus %d", nError, nStatusCode);

   static_PNALBindingLoaderAutomaton(pBindingContext, P_NFC_HAL_BINDING_LOADER_EVENT_COMMAND_EXECUTED, null, 0, nError, nStatusCode);
}

/**
 * Waited a while to allow MR to enter into loader
 */
static void static_PNALBindingExecuteLoaderRebootTimerElapsed(
      tNALBindingContext* pBindingContext,
      void* pCallbackParameter)
{
   PNALDebugTrace("static_PNALBindingExecuteLoaderEnterRebootTimerElapsed");

   static_PNALBindingLoaderAutomaton(pBindingContext, P_NFC_HAL_BINDING_LOADER_EVENT_COMMAND_EXECUTED, null, 0, W_SUCCESS, NAL_RES_OK);
}

/**
 * Waited a while to allow MR to enter into loader
 */
static void static_PNALBindingExecuteLoaderWatchdogTimerElapsed(
      tNALBindingContext* pBindingContext,
      void* pCallbackParameter)
{
   PNALDebugTrace("static_PNALBindingExecuteLoaderEnterRebootTimerElapsed");

   static_PNALBindingLoaderAutomaton(pBindingContext, P_NFC_HAL_BINDING_LOADER_EVENT_TIMEOUT_ELAPSED, null, 0, W_SUCCESS, NAL_RES_OK);
}

/**
 * The HCI has reconnected
 */
static void static_PNALBindingExecuteLoaderServiceConnectCompleted(
   tNALBindingContext* pBindingContext,
   void* pCallbackParameter )
{
   PNALDebugTrace("static_PNALBindingExecuteLoaderEnterServiceConnectCompleted");

   static_PNALBindingLoaderAutomaton(pBindingContext, P_NFC_HAL_BINDING_LOADER_EVENT_COMMAND_EXECUTED, null, 0, W_SUCCESS, NAL_RES_OK);
}

/**
  * Automaton that sequences the loader enter procedure
  *
  */
static void static_PNALBindingLoaderAutomaton(
      tNALBindingContext*       pBindingContext,
      uint32_t        nEvent,
      uint8_t*        pBuffer,
      uint32_t        nLength,
      W_ERROR         nError,
      uint8_t         nStatusCode)
{
   switch (nEvent)
   {
      case P_NFC_HAL_BINDING_LOADER_EVENT_ENTER_LOADER :

         pBindingContext->nUpdateState = P_NFC_HAL_BINDING_LOADER_STATE_LOAD_ENTER;
         break;

      case P_NFC_HAL_BINDING_LOADER_EVENT_LEAVE_LOADER :

         pBindingContext->nUpdateState = P_NFC_HAL_BINDING_LOADER_STATE_LOAD_EXIT;
         break;

      case P_NFC_HAL_BINDING_LOADER_EVENT_COMMAND_EXECUTED:

         if ( (pBindingContext->nUpdateState & P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_CANCEL) != 0x00 )
         {
            if ( (pBindingContext->nUpdateState & P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_TIMEOUT) != 0x00 )
            {
               static_PNALBindingUpdateSendResult(pBindingContext, pBindingContext->pOperation, NAL_RES_TIMEOUT);
            }
            else
            {
               static_PNALBindingUpdateSendResult(pBindingContext, pBindingContext->pOperation, NAL_RES_BAD_STATE);
            }
            return;
         }
         pBindingContext->nUpdateState &= ~P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;

      break;

      case P_NFC_HAL_BINDING_LOADER_EVENT_TIMEOUT_ELAPSED:

         if ( (pBindingContext->nUpdateState & P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED) != 0x00 )
         {
            pBindingContext->nUpdateState &= ~P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
            pBindingContext->nUpdateState |= (P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_CANCEL | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_TIMEOUT);

            PHCIServiceCancelOperation(
               pBindingContext,
               P_HCI_SERVICE_FIRMWARE,
               &pBindingContext->sFirmwareUpdateOperation );

            return;
         }

         /* Boot stopped */

         PNALDebugError("PNALBindingUpdateMachine: NFCC not responding.");
         static_PNALBindingUpdateSendResult(pBindingContext, pBindingContext->pOperation, NAL_RES_TIMEOUT);
         return;
   }

   switch (pBindingContext->nUpdateState)
   {
      case P_NFC_HAL_BINDING_LOADER_STATE_LOAD_ENTER :

         PNALDebugTrace("static_PNALBindingLoaderAutomaton : P_NFC_HAL_BINDING_LOADER_STATE_LOAD_ENTER");

         /* Send the HCI2_CMD_OS_LOADER_ENTER enter command */

         PHCIServiceExecuteCommand(
            pBindingContext,
            P_HCI_SERVICE_FIRMWARE,
            &pBindingContext->sFirmwareUpdateOperation,
            HCI2_CMD_OS_LOADER_ENTER,
            null, 0,
            pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
            static_PNALBindingExecuteLoaderCommandCompleted,
            null,
            W_TRUE );

         /* Allow a delay of TIMER_T5_TIMEOUT_UPDATE ms to allow this operation to complete */

         PNALMultiTimerSet( pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE, TIMER_T5_TIMEOUT_UPDATE,
            &static_PNALBindingExecuteLoaderWatchdogTimerElapsed, null );

         pBindingContext->nUpdateState = P_NFC_HAL_BINDING_LOADER_STATE_WAIT_REBOOT | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
         break;

      case P_NFC_HAL_BINDING_LOADER_STATE_LOAD_EXIT:

         PNALDebugTrace("static_PNALBindingLoaderAutomaton : P_NFC_HAL_BINDING_LOADER_STATE_LOAD_EXIT");

         /* Send the HCI2_CMD_OS_LOADER_ENTER enter command */

         PHCIServiceExecuteCommand(
            pBindingContext,
            P_HCI_SERVICE_FIRMWARE,
            &pBindingContext->sFirmwareUpdateOperation,
            HCI2_CMD_OS_LOADER_QUIT,
            g_aInitConfig, sizeof(g_aInitConfig),
            pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
            static_PNALBindingExecuteLoaderCommandCompleted,
            pBindingContext,
            W_TRUE );

         /* Allow a delay of TIMER_T5_TIMEOUT_UPDATE ms to allow this operation to complete */

         PNALMultiTimerSet( pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE, TIMER_T5_TIMEOUT_UPDATE,
            &static_PNALBindingExecuteLoaderWatchdogTimerElapsed, null );

         pBindingContext->nUpdateState = P_NFC_HAL_BINDING_LOADER_STATE_WAIT_REBOOT | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
         break;

      case P_NFC_HAL_BINDING_LOADER_STATE_WAIT_REBOOT :

         PNALDebugTrace("static_PNALBindingLoaderEnterAutomaton : P_NFC_HAL_BINDING_LOADER_STATE_WAIT_REBOOT");

         /* cancel the previously started timer */
         PNALMultiTimerCancel(pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE);

         if ((nError != W_SUCCESS) || (nStatusCode != ETSI_ERR_ANY_OK))
         {
            /* the HCI2_CMD_OS_LOADER_ENTER execution failed */
            PNALDebugError("static_PNALBindingExecuteLoaderEnterCompleted nError %d - nStatus %d", nError, nStatusCode);

            /* free the current operation */
            static_PNALBindingFreeConfigOperation(pBindingContext, pBindingContext->pOperation);

            /* return the error */
            static_PNALBindingConfigurationCompleted(pBindingContext, nError, nStatusCode);
         }
         else
         {
            /* Ok, the HCI2_CMD_OS_LOADER_ENTER succeed, arm a TIMER_T5_TIMEOUT_REBOOT1 to wait allow MR to reboot */

            PNALMultiTimerSet( pBindingContext, TIMER_T5_NFC_HAL_BOOT_UPDATE, TIMER_T5_TIMEOUT_REBOOT1,
               &static_PNALBindingExecuteLoaderRebootTimerElapsed, null );

            pBindingContext->nUpdateState = P_NFC_HAL_BINDING_LOADER_STATE_RECONNECT | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
         }
         break;

      case P_NFC_HAL_BINDING_LOADER_STATE_RECONNECT :

         PNALDebugTrace("static_PNALBindingLoaderEnterAutomaton : P_NFC_HAL_BINDING_LOADER_STATE_RECONNECT");

         /* We've waited a while to allow MR to reboot, reconnect the HCI layer */

         PHCIServicePreReset( pBindingContext );

         PHCIServiceConnect(
            pBindingContext,
            static_PNALBindingExecuteLoaderServiceConnectCompleted, null );

         pBindingContext->nUpdateState = P_NFC_HAL_BINDING_LOADER_STATE_RECONNECTED | P_NFC_HAL_BINDING_UPDATE_STATUS_PENDING_EXECUTED;
         break;

      case P_NFC_HAL_BINDING_LOADER_STATE_RECONNECTED :

         PNALDebugTrace("static_PNALBindingLoaderEnterAutomaton : P_NFC_HAL_BINDING_LOADER_STATE_RECONNECTED");

         pBindingContext->bInLoader = pBindingContext->bInLoader == W_TRUE ? W_FALSE : W_TRUE;

         /* The HCI has reconnected, we can go on... */

         static_PNALBindingFreeConfigOperation(pBindingContext, pBindingContext->pOperation);

         /* perform next operation */
         static_PNALBindingExecuteNextConfigOperation(pBindingContext);
   }
}

/**
  * Execute a loader enter operation
  *
  */
static void static_PNALBindingExecuteLoaderEnter(
   tNALBindingContext * pBindingContext,
   tNALBindingConfigOperation * pOperation)
{
   PNALDebugTrace("static_PNALBindingExecuteLoaderEnter");

   if (pBindingContext->bInLoader == W_FALSE)
   {
      pBindingContext->pOperation = pOperation;

      /* Start the loader enter automaton */
      static_PNALBindingLoaderAutomaton(pBindingContext, P_NFC_HAL_BINDING_LOADER_EVENT_ENTER_LOADER, null, 0, W_SUCCESS, NAL_RES_OK);
   }
   else
   {
      /* we are already in loader, do not enter loader mode */
      static_PNALBindingFreeConfigOperation(pBindingContext, pOperation);

      /* perform next operation */
      static_PNALBindingExecuteNextConfigOperation(pBindingContext);
   }
}

/* ------------------------------------------------------------------ */

static void static_PNALBindingExecuteLoaderExit(
   tNALBindingContext * pBindingContext,
   tNALBindingConfigOperation * pOperation)
{
   if (pBindingContext->nNALBindingStatus != P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS)
   {
      PNALDebugTrace("static_PNALBindingExecuteLoaderExit");

      pBindingContext->pOperation = pOperation;

      /* Start the loader quit automaton */
      static_PNALBindingLoaderAutomaton(pBindingContext, P_NFC_HAL_BINDING_LOADER_EVENT_LEAVE_LOADER, null, 0, W_SUCCESS, NAL_RES_OK);
   }
   else
   {
      /* there's no firware, we cannot quit loader ! */
      static_PNALBindingFreeConfigOperation(pBindingContext, pOperation);

      /* perform next operation */
      static_PNALBindingExecuteNextConfigOperation(pBindingContext);
   }
}

/* ------------------------------------------------------------------ */

/**
  * SET parameter operation completed
  *
  */
static void static_PNALBindingExecuteSetParameterCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         W_ERROR nError,
         uint8_t nStatusCode,
         uint32_t nHCIMessageReceptionCounter)
{
   tNALBindingConfigOperation * pOperation  = (tNALBindingConfigOperation *) pCallbackParameter;

   PNALDebugTrace("static_PNALBindingExecuteSetParameterCompleted : nError %d nStatus %d", nError, nStatusCode);

   pBindingContext->nConfigProgression += pOperation->Operation.sSetOperation.nLength;

   /* no longer need the operation, free it */
   static_PNALBindingFreeConfigOperation(pBindingContext, pOperation);

   if ((nError != W_SUCCESS) || (nStatusCode != ETSI_ERR_ANY_OK))
   {
      PNALDebugError("static_PNALBindingExecuteSetParameterCompleted : nError %d nStatus %d", nError, nStatusCode);

      static_PNALBindingConfigurationCompleted(pBindingContext, nError, nStatusCode);
   }
   else
   {
      /* SET parameter succeeded, perform next operation */
      static_PNALBindingExecuteNextConfigOperation(pBindingContext);
   }
}

/**
  * Execute a SET parameter operation
  *
  */
static void static_PNALBindingExecuteSetParameter(
   tNALBindingContext * pBindingContext,
   tNALBindingConfigOperation * pOperation)
{
   PNALDebugTrace("static_PNALBindingExecuteSetParameter");

   if (pBindingContext->bInLoader == W_FALSE)
   {
      /* Prepare the command */
      pBindingContext->aHCISendBuffer[0] = pOperation->Operation.sSetOperation.nParameter;
      pBindingContext->aHCISendBuffer[1] = (uint8_t) pOperation->Operation.sSetOperation.nLength;
      CNALMemoryCopy(& pBindingContext->aHCISendBuffer[2], pOperation->Operation.sSetOperation.pValue, pOperation->Operation.sSetOperation.nLength);

      PNALDebugTrace("PHCIServiceExecuteCommand(P_HCI_SERVICE_CMD_SET_PROPERTIES, HCI Service ID %d)", pOperation->Operation.sSetOperation.nService);

      PHCIServiceExecuteCommand(
           pBindingContext,
           pOperation->Operation.sSetOperation.nService,
           &pBindingContext->sFirmwareUpdateOperation,
           P_HCI_SERVICE_CMD_SET_PROPERTIES,
           pBindingContext->aHCISendBuffer,
           pOperation->Operation.sSetOperation.nLength + 2,
           null, 0,
           static_PNALBindingExecuteSetParameterCompleted,
           pOperation,
           W_TRUE);
   }
   else
   {
      /* Specific case for VTHSET */
      if ((pOperation->Operation.sSetOperation.nService == P_HCI_SERVICE_FIRMWARE) && (pOperation->Operation.sSetOperation.nParameter == 0x00))
      {
         PNALDebugTrace("PHCIServiceExecuteCommand(HCI2_CMD_OS_LOADER_SET_VTHSET)");

         CNALMemoryCopy(pBindingContext->aHCISendBuffer, pOperation->Operation.sSetOperation.pValue, pOperation->Operation.sSetOperation.nLength);

         PHCIServiceExecuteCommand(
            pBindingContext,
            P_HCI_SERVICE_FIRMWARE,
            &pBindingContext->sFirmwareUpdateOperation,
            HCI2_CMD_OS_LOADER_SET_VTHSET,
            pBindingContext->aHCISendBuffer, pOperation->Operation.sSetOperation.nLength,
            pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
            static_PNALBindingExecuteSetParameterCompleted,
            pOperation,
            W_TRUE );
      }
   }
}

/* ------------------------------------------------------------------ */

/**
  * MODIFY Parameter : SET parameter completed
  *
  */
void static_PNALBindingExecuteModifyParameterSetParameterCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         W_ERROR nError,
         uint8_t nStatusCode,
         uint32_t nHCIMessageReceptionCounter)
{
   tNALBindingConfigOperation * pOperation  = (tNALBindingConfigOperation *) pCallbackParameter;

   PNALDebugTrace("static_PNALBindingExecuteModifyParameterSetParameterCompleted");

   pBindingContext->nConfigProgression += pOperation->Operation.sSetOperation.nLength;

   /* no longer need the operation */
   static_PNALBindingFreeConfigOperation(pBindingContext, pOperation);

   if ((nError != W_SUCCESS) || (nStatusCode != ETSI_ERR_ANY_OK))
   {
      PNALDebugError("static_PNALBindingExecuteModifyParameterSetParameterCompleted : nError %d, nStatusCode %d", nError, nStatusCode);

      static_PNALBindingConfigurationCompleted(pBindingContext, nError, nStatusCode);
   }
   else
   {
      /* MODIFY parameter succeeded, perform next operation */
      static_PNALBindingExecuteNextConfigOperation(pBindingContext);
   }
}

/**
  * MODIFY Parameter : GET parameter completed
  *
  */
void static_PNALBindingExecuteModifyParameterGetParameterCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         W_ERROR nError,
         uint8_t nStatusCode,
         uint32_t nHCIMessageReceptionCounter)
{
   tNALBindingConfigOperation * pOperation  = (tNALBindingConfigOperation *) pCallbackParameter;

   PNALDebugTrace("static_PNALBindingExecuteModifyParameterGetParameterCompleted");

   if ((nError != W_SUCCESS) || (nStatusCode != ETSI_ERR_ANY_OK) || (nLength != pOperation->Operation.sModifyOperation.nLength))
   {
      PNALDebugError("static_PNALBindingExecuteModifyParameterGetParameterCompleted nError %d nStatus %d", nError, nStatusCode);

      /* no longer need the operation */
      static_PNALBindingFreeConfigOperation(pBindingContext, pOperation);
      static_PNALBindingConfigurationCompleted(pBindingContext, nError, nStatusCode);
   }
   else
   {
      uint32_t i;

      /* Prepare the SET parameter command */

      pBindingContext->aHCISendBuffer[0] = pOperation->Operation.sModifyOperation.nParameter;
      pBindingContext->aHCISendBuffer[1] = (uint8_t) pOperation->Operation.sModifyOperation.nLength;

      /* Apply AND and OR mask */
      for (i=0; i<nLength; i++)
      {
         pBindingContext->pHCISendBuffer[2+i] = (pBuffer[i] & pOperation->Operation.sModifyOperation.pAndMask[i]) | pOperation->Operation.sModifyOperation.pOrMask[i];
      }

      PHCIServiceExecuteCommand(
        pBindingContext,
        pOperation->Operation.sSetOperation.nService,
        &pBindingContext->sFirmwareUpdateOperation,
        P_HCI_SERVICE_CMD_SET_PROPERTIES,
        pBindingContext->aHCISendBuffer,
        pOperation->Operation.sSetOperation.nLength + 2,
        null, 0,
        static_PNALBindingExecuteModifyParameterSetParameterCompleted,
        pOperation,
        W_TRUE);
   }
}

/**
  * Execute a MODIFY parameter operation
  *
  */
static void static_PNALBindingExecuteModifyParameter(
   tNALBindingContext * pBindingContext,
   tNALBindingConfigOperation * pOperation)
{
   PNALDebugTrace("static_PNALBindingExecuteModifyParameter");

   /* the MODIFY operation is performed in two steps,
      - first we retrieve the current parameter value
      - then (in the get parameter callback), we compute the  new value and set it
      */

   pBindingContext->aHCISendBuffer[0]= pOperation->Operation.sModifyOperation.nParameter;

   PHCIServiceExecuteCommand(
      pBindingContext,
      pOperation->Operation.sModifyOperation.nService,
      &pBindingContext->sFirmwareUpdateOperation,
      P_HCI_SERVICE_CMD_GET_PROPERTIES,
      pBindingContext->aHCISendBuffer,
      0x01,
      pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
      static_PNALBindingExecuteModifyParameterGetParameterCompleted,
      pOperation,
      W_FALSE );
}

/* ------------------------------------------------------------------ */

/**
 * NFC HAL boot operation completed
 */
static void static_PNALBindingExecuteNALBootCompleted(
         tNALBindingContext * pBindingContext,
         void* pCallbackParameter,
         uint32_t nResultCode )
{
   tNALBindingConfigOperation * pOperation  = (tNALBindingConfigOperation *) pCallbackParameter;
   uint32_t nExpectedResult = pOperation->Operation.sNALBootOperation.nExpectedResult;

   PNALDebugTrace("static_PNALBindingExecuteNALBoot : nResultCode %d", nResultCode);

   static_PNALBindingFreeConfigOperation(pBindingContext, pOperation);

   if (nResultCode != nExpectedResult)
   {
      PNALDebugError("static_PNALBindingExecuteNALBoot : nResultCode %d - expected %d", nResultCode, nExpectedResult);

      static_PNALBindingConfigurationCompleted(pBindingContext, W_ERROR_BAD_STATE, NAL_RES_BAD_STATE);
   }
   else
   {
      static_PNALBindingExecuteNextConfigOperation(pBindingContext);
   }
}

/**
  * Execute a NFC HAL boot operatop,
  *
  */

static void static_PNALBindingExecuteNALBoot(
   tNALBindingContext * pBindingContext,
   tNALBindingConfigOperation * pOperation)
{
   pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_INIT_STATUS;
   pBindingContext->pInternalCallbackConnectionFunction = static_PNALBindingExecuteNALBootCompleted;
   pBindingContext->pCallbackConnectionParameter = pOperation;

   PNALBindingBootMachine(pBindingContext, P_NFC_HAL_BINDING_BOOT_START);
}

/* ------------------------------------------------------------------ */

/**
  * Initialize configuration operation list
  *
  */
static void static_PNALBindingInitializeConfigOperationList(
   tNALBindingContext * pBindingContext)
{
   pBindingContext->pFirstConfigOperation = null;
   pBindingContext->pLastConfigOperation = null;

   pBindingContext->nConfigProgression = 0;
   pBindingContext->nConfigLength = 0;
}

/**
  * Append a configuration operation at the tail of the config operation list
  *
  */
static void static_PNALBindingAppendConfigOperation(
   tNALBindingContext * pBindingContext,
   tNALBindingConfigOperation * pOperation)
{
   pOperation->pNext = null;

   /* compute the total configuration weight */

   switch (pOperation->nOperation)
   {
      case P_NFC_HAL_BINDING_CONFIG_FIRMWARE_UPDATE :
      case P_NFC_HAL_BINDING_CONFIG_LOADER_UPDATE :

         pBindingContext->nConfigLength += pOperation->Operation.sFirmwareUpdateOperation.nLength;
         break;

      case P_NFC_HAL_BINDING_CONFIG_SET_PARAMETER :
         pBindingContext->nConfigLength += pOperation->Operation.sSetOperation.nLength;
         break;

      case P_NFC_HAL_BINDING_CONFIG_MODIFY_PARAMETER :
         pBindingContext->nConfigLength += pOperation->Operation.sModifyOperation.nLength;
         break;
   }

   if (pBindingContext->pLastConfigOperation != null)
   {
      pBindingContext->pLastConfigOperation->pNext = pOperation;
      pBindingContext->pLastConfigOperation = pOperation;
   }
   else
   {
      pBindingContext->pFirstConfigOperation = pBindingContext->pLastConfigOperation = pOperation;
   }
}

/**
  * Retrieve configuration operation from the head of the config operation list
  *
  */
static tNALBindingConfigOperation * static_PNALBindingGetNextConfigOperation(
   tNALBindingContext * pBindingContext)
{
   tNALBindingConfigOperation * pOperation;

   pOperation = pBindingContext->pFirstConfigOperation;

   if (pOperation != null)
   {
      pBindingContext->pFirstConfigOperation = pOperation->pNext;

      if (pBindingContext->pFirstConfigOperation == null)
      {
         pBindingContext->pLastConfigOperation = null;
      }
   }

   return (pOperation);
}

/**
  * Flush the configuration operation list.
  * All operation will be freed
  *
  */
static void static_PNALBindingFlushConfigOperationList(
   tNALBindingContext * pBindingContext)
{
   tNALBindingConfigOperation * pOperation;

   while ((pOperation = static_PNALBindingGetNextConfigOperation(pBindingContext)) != null)
   {
      static_PNALBindingFreeConfigOperation(pBindingContext, pOperation);
   }
}

/**
  * Remove and execute the next configuration operation.
  *
  */
static void static_PNALBindingExecuteNextConfigOperation(
   tNALBindingContext * pBindingContext)
{
   tNALBindingConfigOperation * pOperation = static_PNALBindingGetNextConfigOperation(pBindingContext);

   if (pOperation == null)
   {
      /* no more operation pending, the configuration operation has been completed */

      static_PNALBindingConfigurationCompleted(pBindingContext, W_SUCCESS, NAL_RES_OK);
      return;
   }

   switch (pOperation->nOperation)
   {
      case P_NFC_HAL_BINDING_CONFIG_LOADER_ENTER :
         static_PNALBindingExecuteLoaderEnter(pBindingContext, pOperation);
         break;

      case P_NFC_HAL_BINDING_CONFIG_LOADER_EXIT :
         static_PNALBindingExecuteLoaderExit(pBindingContext, pOperation);
         break;

      case P_NFC_HAL_BINDING_CONFIG_SET_PARAMETER :
         static_PNALBindingExecuteSetParameter(pBindingContext, pOperation);
         break;

      case P_NFC_HAL_BINDING_CONFIG_MODIFY_PARAMETER :
         static_PNALBindingExecuteModifyParameter(pBindingContext, pOperation);
         break;

      case P_NFC_HAL_BINDING_CONFIG_FIRMWARE_UPDATE :
         static_PNALBindingExecuteFirmwareUpdate(pBindingContext, pOperation);
         break;

      case P_NFC_HAL_BINDING_CONFIG_LOADER_UPDATE :
         static_PNALBindingExecuteLoaderUpdate(pBindingContext, pOperation);
         break;

      case P_NFC_HAL_BINDING_CONFIG_BOOT :
         static_PNALBindingExecuteNALBoot(pBindingContext, pOperation);
         break;

      default :
         CNALDebugAssert(0);
         return;
   }
}

/**
 * Enqueues a Firmware update operation in the configuration operation list
 *
 */
static W_ERROR static_PNALBindingEnqueueFirmwareUpdate(
   tNALBindingContext * pBindingContext,
   uint8_t  * pBuffer,
   uint32_t   nLength)
{
   tNALBindingConfigOperation * pOperation = null;

   if ((pOperation = static_PNALBindingAllocConfigOperation(pBindingContext)) == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }

   pOperation->nOperation = P_NFC_HAL_BINDING_CONFIG_FIRMWARE_UPDATE;

   pOperation->Operation.sFirmwareUpdateOperation.pBuffer = pBuffer;
   pOperation->Operation.sFirmwareUpdateOperation.nLength = nLength;

   static_PNALBindingAppendConfigOperation(pBindingContext, pOperation);

   return W_SUCCESS;
}

/**
  * Enqueuers a Loader update operation in the configuration operation list
  *
  */
static W_ERROR static_PNALBindingEnqueueLoaderUpdate(
   tNALBindingContext * pBindingContext,
   uint8_t  * pBuffer,
   uint32_t   nLength)
{
   tNALBindingConfigOperation * pOperation;

   if ((pOperation = static_PNALBindingAllocConfigOperation(pBindingContext)) == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }

   pOperation->nOperation = P_NFC_HAL_BINDING_CONFIG_LOADER_UPDATE;

   pOperation->Operation.sFirmwareUpdateOperation.pBuffer = pBuffer;
   pOperation->Operation.sFirmwareUpdateOperation.nLength = nLength;

   static_PNALBindingAppendConfigOperation(pBindingContext, pOperation);

   return W_SUCCESS;
}

/**
 * Enqueues a Loader Enter operation in the configuration operation list
 *
 */
static W_ERROR static_PNALBindingEnqueueLoaderEnter(
   tNALBindingContext * pBindingContext)
{
   tNALBindingConfigOperation * pOperation;

   if ((pOperation = static_PNALBindingAllocConfigOperation(pBindingContext)) == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }

   pOperation->nOperation = P_NFC_HAL_BINDING_CONFIG_LOADER_ENTER;

   static_PNALBindingAppendConfigOperation(pBindingContext, pOperation);

   return W_SUCCESS;
}

/**
 * Enqueues a Loader Exit operation in the configuration operation list
 *
 */
static W_ERROR static_PNALBindingEnqueueLoaderExit(
   tNALBindingContext * pBindingContext)
{
   tNALBindingConfigOperation * pOperation;

   if ((pOperation = static_PNALBindingAllocConfigOperation(pBindingContext)) == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }

   pOperation->nOperation = P_NFC_HAL_BINDING_CONFIG_LOADER_EXIT;

   static_PNALBindingAppendConfigOperation(pBindingContext, pOperation);

   return W_SUCCESS;
}

/**
 * Enqueues a NFC HAL boot operation in the configuration operation list
 *
 */
static W_ERROR static_PNALBindingEnqueueNALBoot(
   tNALBindingContext * pBindingContext,
   uint32_t   nExpectedResult)
{
   tNALBindingConfigOperation * pOperation;

   if ((pOperation = static_PNALBindingAllocConfigOperation(pBindingContext)) == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }

   pOperation->nOperation = P_NFC_HAL_BINDING_CONFIG_BOOT;
   pOperation->Operation.sNALBootOperation.nExpectedResult = nExpectedResult;

   static_PNALBindingAppendConfigOperation(pBindingContext, pOperation);

   return W_SUCCESS;
}

/**
 * Enqueues a SET parameter operation in the configuration operation list
 *
 */
static W_ERROR static_PNALBindingEnqueueSetParameter(
   tNALBindingContext * pBindingContext,
   uint8_t    nService,
   uint8_t    nParameter,
   uint8_t  * pValue,
   uint32_t   nLength)
{
   tNALBindingConfigOperation * pOperation;

   if ((pOperation = static_PNALBindingAllocConfigOperation(pBindingContext)) == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }

   pOperation->nOperation = P_NFC_HAL_BINDING_CONFIG_SET_PARAMETER;
   pOperation->Operation.sSetOperation.nService = nService;
   pOperation->Operation.sSetOperation.nParameter = nParameter;
   pOperation->Operation.sSetOperation.nLength    = nLength;
   pOperation->Operation.sSetOperation.pValue     = pValue;

   static_PNALBindingAppendConfigOperation(pBindingContext, pOperation);

   return W_SUCCESS;
}

/**
 * Enqueues a MODIFY parameter operation in the configuration operation list
 */

static W_ERROR static_PNALBindingEnqueueModifyParameter(
   tNALBindingContext * pBindingContext,
   uint8_t    nService,
   uint8_t    nParameter,
   uint8_t  * pValue,
   uint32_t   nLength)
{
   tNALBindingConfigOperation * pOperation;

   if ((pOperation = static_PNALBindingAllocConfigOperation(pBindingContext)) == null)
   {
      return W_ERROR_OUT_OF_RESOURCE;
   }

   pOperation->nOperation = P_NFC_HAL_BINDING_CONFIG_MODIFY_PARAMETER;
   pOperation->Operation.sModifyOperation.nService = nService;
   pOperation->Operation.sModifyOperation.nParameter = nParameter;
   pOperation->Operation.sModifyOperation.pOrMask  = pValue;
   pOperation->Operation.sModifyOperation.pAndMask = pValue + nLength / 2;
   pOperation->Operation.sModifyOperation.nLength = nLength / 2;

   return W_SUCCESS;
}

/**
 * Parses configuration file parameter section contents
 *
 */
static W_ERROR static_PNALBindingConfigParseParameter(
   tNALBindingContext * pBindingContext,
   uint8_t  * pBuffer,
   uint32_t   nBufferSize,
   uint32_t * pParamSize,
   uint8_t    nAction)
{
   uint32_t nIndex = 0;
   uint32_t nSize;
   uint8_t  nOperation;
   W_ERROR  nError;

   /* check the type */

   if (nIndex + 1 > nBufferSize)
   {
      PNALDebugError("static_PNALBindingCheckParameter :  item too short");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   switch (nOperation = pBuffer[nIndex])
   {
      case P_NFC_HAL_BINDING_CONFIG_SECTION_PARAM_SET :
         PNALDebugTrace("static_PNALBindingCheckParameter : SET PARAM\n");
         break;

      case P_NFC_HAL_BINDING_CONFIG_SECTION_PARAM_MODIFY :
         PNALDebugTrace("static_PNALBindingCheckParameter : MODIFY PARAM\n");
         break;

      default :
         PNALDebugError("static_PNALBindingCheckParameter : bad parameter format");
         return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   nIndex++;

   /* check the length */

   if (nIndex + 4 > nBufferSize)
   {
      PNALDebugError("static_PNALBindingCheckParameter : item too short");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   nSize = PNALUtilReadUint32FromLittleEndianBuffer(pBuffer + nIndex);
   nIndex += 4;

   switch (nOperation)
   {
      case P_NFC_HAL_BINDING_CONFIG_SECTION_PARAM_SET :

         /* minimal size is 3 bytes */
         if (nSize < 3)
         {
            PNALDebugError("static_PNALBindingCheckParameter : SET_PARAM too short");
            return W_ERROR_BAD_FIRMWARE_FORMAT;
         }

         if (nAction == P_NFC_HAL_BINDING_CONFIG_ACTION_PERFORM)
         {
            if ((nError = static_PNALBindingEnqueueSetParameter(pBindingContext, pBuffer[nIndex],  pBuffer[nIndex+1],  pBuffer + nIndex + 2,  nSize - 2)) != W_SUCCESS)
            {
               PNALDebugError("static_PNALBindingCheckParameter : static_PNALBindingEnqueueSetParameter failed %d", nError);
               return nError;
            }
         }

         break;

      case P_NFC_HAL_BINDING_CONFIG_SECTION_PARAM_MODIFY :

         /* minimal size is 4 bytes */

         if (nSize < 4)
         {
            PNALDebugError("static_PNALBindingCheckParameter : MODIFY_PARAM too short");
            return W_ERROR_BAD_FIRMWARE_FORMAT;
         }

         if (nSize & 1)
         {
            PNALDebugError("static_PNALBindingCheckParameter : MODIFY_PARAM length is odd");
            return W_ERROR_BAD_FIRMWARE_FORMAT;
         }

         if (nAction == P_NFC_HAL_BINDING_CONFIG_ACTION_PERFORM)
         {
            if ((nError = static_PNALBindingEnqueueModifyParameter(pBindingContext,
                                 pBuffer[nIndex], pBuffer[nIndex+1], pBuffer + nIndex + 2, nSize - 2)) != W_SUCCESS)
            {
               PNALDebugError("static_PNALBinfingEnqueueModifyParameter : static_PNALBindingEnqueueModifyParameter failed %d", nError);
               return nError;
            }
         }
         break;
   }

   * pParamSize = nSize + 5;

   /* here all is fine */

   return (W_SUCCESS);
}

/**
  * Parses configuration file section contents
  *
  */
static W_ERROR static_PNALBindingConfigParseSectionItem(
   tNALBindingContext * pBindingContext,
   uint8_t   * pBuffer,
   uint32_t    nBufferSize,
   uint32_t  * pSectionItemSize,
   uint8_t     nAction
   )
{
   W_ERROR  nError;
   uint32_t nIndex = 0;
   uint32_t nSize;
   uint8_t  nType;
   uint32_t nParamSize = 0;

   /* check the type  */

   if (nIndex + 1 > nBufferSize)
   {
      PNALDebugError("static_PNALBindingConfigParseSectionItem : section too short");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   nType = pBuffer[nIndex];

   switch (nType)
   {
      case P_NFC_HAL_BINDING_CONFIG_SECTION_FIRMWARE :
         PNALDebugTrace("FIRMWARE\n");
         break;

      case P_NFC_HAL_BINDING_CONFIG_SECTION_LOADER :
         PNALDebugTrace("LOADER\n");
         break;

      case P_NFC_HAL_BINDING_CONFIG_SECTION_PARAM :
         PNALDebugTrace("PARAM\n");
         break;

      case P_NFC_HAL_BINDING_CONFIG_SECTION_LOADER_PARAM :
         PNALDebugTrace("LOADER PARAM\n");
         break;

      default:
         PNALDebugError("static_PNALBindingConfigParseSectionItem : unknown type");
         return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   nIndex++;

   /* check the length */

   if (nIndex + 4 > nBufferSize)
   {
      PNALDebugError("static_PNALBindingConfigParseSectionItem : section too short");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   nSize = PNALUtilReadUint32FromLittleEndianBuffer(pBuffer + nIndex);
   nIndex += 4;

   if (nIndex + nSize > nBufferSize)
   {
      PNALDebugError("static_PNALBindingConfigParseSectionItem : section too short");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   switch (nType)
   {
      case P_NFC_HAL_BINDING_CONFIG_SECTION_FIRMWARE :

         if (nAction == P_NFC_HAL_BINDING_CONFIG_ACTION_PERFORM)
         {
            nError = static_PNALBindingEnqueueFirmwareUpdate(pBindingContext, pBuffer + nIndex, nSize);

            if (nError != W_SUCCESS)
            {
               PNALDebugError("static_PNALBindingConfigParseSectionItem : static_PNALBindingEnqueueFirmwareUpdate failed %d", nError);
               return nError;
            }

            /* after a firmware update, the NFC HAL boot must return P_NFC_HAL_BINDING_VALID_STATUS; */

            nError = static_PNALBindingEnqueueNALBoot(pBindingContext, P_NFC_HAL_BINDING_VALID_STATUS);

            if (nError != W_SUCCESS)
            {
               PNALDebugError("static_PNALBindingConfigParseSectionItem : static_PNALBindingEnqueueNALBoot failed %d", nError);
               return nError;
            }
         }
         break;

      case P_NFC_HAL_BINDING_CONFIG_SECTION_LOADER :

         if (nAction == P_NFC_HAL_BINDING_CONFIG_ACTION_PERFORM)
         {
            nError = static_PNALBindingEnqueueLoaderUpdate(pBindingContext, pBuffer + nIndex, nSize);

            if (nError != W_SUCCESS)
            {
               PNALDebugError("static_PNALBindingConfigParseSectionItem : static_PNALBindingEnqueueLoaderUpdate failed %d", nError);
               return nError;
            }

            /* after a firmware update, the NFC HAL boot must return P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS; */

            nError = static_PNALBindingEnqueueNALBoot(pBindingContext, P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS);

            if (nError != W_SUCCESS)
            {
               PNALDebugError("static_PNALBindingConfigParseSectionItem : static_PNALBindingEnqueueNALBoot failed %d", nError);
               return nError;
            }
         }
         break;

      case P_NFC_HAL_BINDING_CONFIG_SECTION_LOADER_PARAM :
      case P_NFC_HAL_BINDING_CONFIG_SECTION_PARAM :
      {
         uint32_t nIndex2 = 0;

         if (nAction == P_NFC_HAL_BINDING_CONFIG_ACTION_PERFORM)
         {
            /* For Loader parameters, enqueue a request to enter loader */

            if (nType == P_NFC_HAL_BINDING_CONFIG_SECTION_LOADER_PARAM)
            {
               nError = static_PNALBindingEnqueueLoaderEnter(pBindingContext);

               if (nError != W_SUCCESS)
               {
                  PNALDebugError("static_PNALBindingConfigParseSectionItem : static_PNALBindingEnqueueLoaderEnter failed %d", nError);
                  return (nError);
               }
            }
         }

         while (nIndex2 < nSize)
         {
            nError = static_PNALBindingConfigParseParameter(pBindingContext, pBuffer + nIndex + nIndex2, nSize, &nParamSize, nAction);

            if (nError != W_SUCCESS)
            {
               return nError;
            }

            nIndex2 += nParamSize;
         }

         if (nIndex2 != nSize)
         {
            PNALDebugError("static_PNALBindingConfigParseSectionItem : error during parsing parameter list");
            return W_ERROR_BAD_FIRMWARE_FORMAT;
         }

         if (nAction == P_NFC_HAL_BINDING_CONFIG_ACTION_PERFORM)
         {
            /* For loader parameters, enter a request to exit loader */

            if (nType == P_NFC_HAL_BINDING_CONFIG_SECTION_LOADER_PARAM)
            {
               nError = static_PNALBindingEnqueueLoaderExit(pBindingContext);

               if (nError != W_SUCCESS)
               {
                  PNALDebugError("static_PNALBindingConfigParseSectionItem : static_PNALBindingEnqueueLoaderExit failed %d", nError);
                  return (nError);
               }
            }
         }
      }
      break;
   }

   * pSectionItemSize = nSize + 5;
   return W_SUCCESS;
}

/**
 * Parses a configuration file section
 *
 */
static W_ERROR static_PNALBindingConfigParseSection(
   tNALBindingContext * pBindingContext,
   uint8_t  * pBuffer,
   uint32_t   nBufferSize,
   uint32_t * pSectionSize,
   uint8_t    nAction)
{
   W_ERROR  nError;
   uint32_t nIndex = 0;
   uint32_t nItemSize = 0;
   uint32_t nSectionSize;
   uint32_t nTargetNum;
   uint32_t i;

   /*  get the section length : minimum of 4 bytes are required */

   if (nIndex + 4 > nBufferSize)
   {
      PNALDebugError("static_PNALBindingCheckSection : buffer too short");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   nSectionSize = PNALUtilReadUint32FromLittleEndianBuffer(pBuffer + nIndex);
   PNALDebugTrace("nSectionSize %x\n", nSectionSize);

   nIndex+=4;

   /*  get the target number : minimum of 1 bytes is required */

   if (nIndex + 1 > nBufferSize)
   {
      PNALDebugError("static_PNALBindingCheckSection : buffer too short");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   nTargetNum = pBuffer[nIndex];
   PNALDebugTrace("nTargetNum %x\n", nTargetNum);

   nIndex+=1;

   if (nTargetNum == 0)
   {
      PNALDebugError("static_PNALBindingCheckSection : Target number is 0");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   if (nSectionSize <= 5 + 10 * nTargetNum)
   {
      PNALDebugError("static_PNALBindingCheckSection : section too short");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   /* processing the target */

   if (nIndex + 10 * nTargetNum >  nBufferSize )
   {
      PNALDebugError("static_PNALBindingCheckSection : buffer too short");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   if (nAction == P_NFC_HAL_BINDING_CONFIG_ACTION_PERFORM)
   {
      for (i=0; i<nTargetNum; i++) {
         uint8_t aZero[3] = { 0, 0, 0 };

         /* check hardware version */
         if (CNALMemoryCompare(pBuffer + nIndex, aZero, 3) != 0)
         {
            if (CNALMemoryCompare(pBuffer + nIndex, pBindingContext->sNALParHardwareInfo.aHardwareVersion, 3) !=0 )
            {
               PNALDebugWarning("static_PNALBindingCheckSection : hardware version do not match, skip the current section");

               * pSectionSize = nSectionSize;
               return W_SUCCESS;
            }
         }

         nIndex+= 3;

         /* check loader version */
         if (CNALMemoryCompare(pBuffer + nIndex, aZero, 3) != 0)
         {
            if (CNALMemoryCompare(pBuffer + nIndex, pBindingContext->sNALParHardwareInfo.aLoaderVersion, 3) !=0 )
            {
               PNALDebugWarning("static_PNALBindingCheckSection : loader version do not match, skip the current section");

               * pSectionSize = nSectionSize;
               return W_SUCCESS;
            }
         }

         nIndex += 3;

         /* check firmware version */
         if (CNALMemoryCompare(pBuffer + nIndex, aZero, 3) != 0)
         {
            if (CNALMemoryCompare(pBuffer + nIndex, pBindingContext->sNALParFirmwareInfo.aFirmwareVersion, 3) !=0 )
            {
               PNALDebugWarning("static_PNALBindingCheckSection : firmware version do not match, skip the current section");

               * pSectionSize = nSectionSize;
               return W_SUCCESS;
            }
         }

         nIndex += 3;

         /* check interface version */
         if (pBuffer[nIndex] != 0)
         {
            if (pBuffer[nIndex] != P_NFC_HAL_BINDING_INTERFACE)
            {
               PNALDebugWarning("static_PNALBindingCheckSection : NFC HAL protocol version does not match, skip the current section");

               * pSectionSize = nSectionSize;
               return W_SUCCESS;
            }
         }

         nIndex++;
      }
   }
   else
   {
      /* skip the target section */
      nIndex += 10 * nTargetNum;
   }

   /* check all sections one after each other */

   while (nIndex < nSectionSize)
   {
      if (nBufferSize > nIndex)
      {
         nError = static_PNALBindingConfigParseSectionItem(pBindingContext, pBuffer + nIndex, nBufferSize - nIndex, &nItemSize, nAction);

         if (nError != W_SUCCESS)
         {
            return nError;
         }

         nIndex += nItemSize;
      }
      else
      {
         PNALDebugError("static_PNALBindingCheckSection : buffer too short");
         return W_ERROR_BAD_FIRMWARE_FORMAT;
      }
   }

   * pSectionSize = nSectionSize;

   return (W_SUCCESS);
}

/**
 * Checks the correctness of a configuration file
 *
 * @return W_SUCCESS if the file is correct.
 */

static W_ERROR static_PNALBindingConfigParseWholeBuffer(
   tNALBindingContext * pBindingContext,
   uint8_t * pBuffer,
   uint32_t  nBufferSize,
   uint8_t   nAction)
{
   W_ERROR  nError;
   uint32_t nIndex  = 0;
   uint32_t nSectionNb;
   uint32_t nTitleLength;
   uint32_t i;
   uint32_t nSectionSize = 0;

   /* Check magic number */
   if (nIndex + 4 > nBufferSize)
   {
      PNALDebugError("PNALBindingCheckUpdateBuffer : buffer too short");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   if (PNALUtilReadUint32FromLittleEndianBuffer(pBuffer + nIndex) != NAL_FIRMWARE_FORMAT_MAGIC_NUMBER)
   {
      PNALDebugError("PNALBindingCheckUpdateBuffer : bad signature");
      return W_ERROR_BAD_FIRMWARE_SIGNATURE;
   }
   nIndex += 4;

   /* Check config file version */
   if (nIndex + 1 > nBufferSize)
   {
      PNALDebugError("PNALBindingCheckUpdateBuffer : buffer too short");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   if (pBuffer[nIndex] != NAL_FIRMWARE_FORMAT_VERSION)
   {
      PNALDebugError("PNALBindingCheckUpdateBuffer : bad firmware version");
      return W_ERROR_BAD_FIRMWARE_VERSION;
   }
   nIndex++;

   if (nIndex + 4 > nBufferSize)
   {
      PNALDebugError("PNALBindingCheckUpdateBuffer : buffer too short");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   /* Check config file length */
   if (PNALUtilReadUint32FromLittleEndianBuffer(pBuffer + 5) != nBufferSize)
   {
      PNALDebugError("PNALBindingCheckUpdateBuffer : bad firmware length");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }
   nIndex += 4;

   /* skip the license */
   if (nIndex + 0x10 > nBufferSize)
   {
      PNALDebugError("PNALBindingCheckUpdateBuffer : buffer too short");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }
   nIndex += 0x10;

   if (nIndex + 1 > nBufferSize)
   {
      PNALDebugError("PNALBindingCheckUpdateBuffer : buffer too short");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   /* get the section number */
   nSectionNb = pBuffer[nIndex];
   nIndex++;

   if (nIndex + 1 > nBufferSize)
   {
      PNALDebugError("PNALBindingCheckUpdateBuffer : buffer too short");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   /* retrieve the title length */
   nTitleLength = pBuffer[nIndex];
   nIndex += 1;

   /* skip the title */
   if (nIndex + nTitleLength >= nBufferSize)
   {
      PNALDebugError("PNALBindingCheckUpdateBuffer : buffer too short");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   nIndex += nTitleLength;

   /* now, walk through the sections */

   for (i=0; i<nSectionNb; i++)
   {
      if (nBufferSize > nIndex)
      {
         nError = static_PNALBindingConfigParseSection(pBindingContext, pBuffer + nIndex, nBufferSize - nIndex, &nSectionSize, nAction);

         if (nError != W_SUCCESS)
         {
            return nError;
         }

         nIndex += nSectionSize;
      }
      else
      {
         PNALDebugError("PNALBindingCheckUpdateBuffer : buffer too short");
         return W_ERROR_BAD_FIRMWARE_FORMAT;
      }
   }

   /* check we've consumed all the provided buffer */

   if (nIndex != nBufferSize )
   {
      PNALDebugError("PNALBindingCheckUpdateBuffer : buffer too long");
      return W_ERROR_BAD_FIRMWARE_FORMAT;
   }

   return (W_SUCCESS);
}

W_ERROR PNALBindingConfigCheck(
   tNALBindingContext * pBindingContext,
   uint8_t  * pConfigBuffer,
   uint32_t   nConfigLength)
{
   /* check the configuration buffer syntax */
   return static_PNALBindingConfigParseWholeBuffer(pBindingContext, pConfigBuffer, nConfigLength, P_NFC_HAL_BINDING_CONFIG_ACTION_CHECK);
}

/**
  * Starts the configuration process
  *
  */
NFC_HAL_INTERNAL void PNALBindingConfigStart(
   tNALBindingContext* pBindingContext,
   uint8_t  * pConfigBuffer,
   uint32_t   nConfigLength)
{
   W_ERROR nError;

   /* build the configuration operation list */
   nError = static_PNALBindingConfigParseWholeBuffer(pBindingContext, pConfigBuffer, nConfigLength, P_NFC_HAL_BINDING_CONFIG_ACTION_CHECK);

   if (nError != W_SUCCESS)
   {
      /* the configuration buffer contents is not valid */

      static_PNALBindingConfigurationCompleted(pBindingContext, W_ERROR_BAD_FIRMWARE_FORMAT, NAL_RES_BAD_DATA);
      return;
   }

   /* initialize the operation list */
   static_PNALBindingInitializeConfigOperationList(pBindingContext);

   /* build the configuration operation list */
   nError = static_PNALBindingConfigParseWholeBuffer(pBindingContext, pConfigBuffer, nConfigLength, P_NFC_HAL_BINDING_CONFIG_ACTION_PERFORM);

   if (nError != W_SUCCESS)
   {
      /* something goes wrong, flush the current operation list */
      static_PNALBindingFlushConfigOperationList(pBindingContext);

      static_PNALBindingConfigurationCompleted(pBindingContext, nError, NAL_RES_BAD_DATA);
      return;
   }

   if (pBindingContext->pFirstConfigOperation == null)
   {
      /* no operation to be done, the configuration file did not contain any section matching the current config */
      static_PNALBindingConfigurationCompleted(pBindingContext, W_ERROR_BAD_FIRMWARE_VERSION, NAL_RES_BAD_VERSION);
      return;
   }

   /* all is fine, start processing the operation list */
   static_PNALBindingExecuteNextConfigOperation(pBindingContext);
}
