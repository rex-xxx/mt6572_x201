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

#define P_MODULE  P_MODULE_DEC( NAL_BIND_BOOT )

#include "nfc_hal_binding.h"

/******************************************************************************************************************
*                                         Definition
*******************************************************************************************************************/

/* NFC boot state state Machine */
enum {
      P_NFC_HAL_BINDING_BOOT_GET_FIRMWARE_VERSION,
      P_NFC_HAL_BINDING_BOOT_GET_PATCH_STATUS,
      P_NFC_HAL_BINDING_BOOT_GET_SESSION_ID,
      P_NFC_HAL_BINDING_BOOT_GET_HDS_OWNER_READER,
      P_NFC_HAL_BINDING_BOOT_GET_HDS_OWNER_CARD,
      P_NFC_HAL_BINDING_BOOT_OPEN_AND_CLOSE_PIPE,
      P_NFC_HAL_BINDING_BOOT_CONFIGURE_NFCC,
      P_NFC_HAL_BINDING_BOOT_SERVICE_SUBSCRIBE,
      P_NFC_HAL_BINDING_BOOT_GET_PERSISTENT_POLICY,
      P_NFC_HAL_BINDING_BOOT_GET_CARD_DETECT_STATE,
      P_NFC_HAL_BINDING_BOOT_SET_POLLING_PERIOD,
      P_NFC_HAL_BINDING_BOOT_GET_ROUTING_TABLE_STATE,
      P_NFC_HAL_BINDING_BOOT_FINISHED,

      P_NFC_HAL_BINDING_BOOT_OPEN_PIPE_MANAGEMENT,
      P_NFC_HAL_BINDING_BOOT_LOADER_PENDING,
  };

/******************************************************************************************************************
*                                        Functions declaration
*******************************************************************************************************************/

static void static_PNALBindingGetFirmwareVersionCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         W_ERROR nError,
         uint8_t nStatusCode,
         uint32_t nReceptionCounter);

static void static_PNALBindingGetLoaderVersionCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         W_ERROR nError,
         uint8_t nStatusCode,
         uint32_t nReceptionCounter);

/******************************************************************************************************************
*                                         constants
*******************************************************************************************************************/

/* Loader Version parameters */
static const uint8_t g_aLoaderVersionParam[] = {
    HCI2_PAR_MGT_VERSION_HARDWARE,
    HCI2_PAR_MGT_VERSION_LOADER,
    HCI2_PAR_MGT_PATCH_STATUS
#if 0    /* the NFCC doest not support this command in loader mode*/
    ,HCI2_PAR_MGT_SERIAL_ID
#endif/* 0 */
};

/* Several services parameters set during initial boot after a firmware update */
static const uint8_t g_aHCIServiceParam[] =
{
   P_HCI_SERVICE_INSTANCE,
   20,
   HCI2_PAR_INSTANCES_DEFAULT_MCARD_GRANTED_TO_SIM,        0x02,    0x00, 0x00,
   HCI2_PAR_INSTANCES_CURRENT_MCARD_GRANTED_TO_SIM,        0x02,    0x00, 0x00,
   HCI2_PAR_INSTANCES_STANDBY_MCARD_GRANTED_TO_SIM,        0x02,    0x00, 0x00,
   HCI2_PAR_INSTANCES_DEFAULT_MREAD_GRANTED_TO_SIM,        0x02,    0x00, 0x00,
   HCI2_PAR_INSTANCES_CURRENT_MREAD_GRANTED_TO_SIM,        0x02,    0x00, 0x00,

   P_HCI_SERVICE_ADMINISTRATION,
   4,
   HCI2_PAR_ADM_WHITE_LIST, 2, ETSI_HOST_UICC, ETSI_HOST_RFU3,

   P_HCI_SERVICE_PIPE_MANAGEMENT,
   33,
   HCI2_PAR_MGT_SWP_GATE_ACCESS,        0x10, 0x71, 0x00, 0x0A, 0x00, 0x1E, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
   HCI2_PAR_MGT_DEFAULT_RF_LOCK_CARD,   0x01, 0x00,
   HCI2_PAR_MGT_CURRENT_RF_LOCK_CARD,   0x01, 0x00,
   HCI2_PAR_MGT_STANDBY_RF_LOCK_CARD,   0x01, 0x00,
   HCI2_PAR_MGT_DEFAULT_RF_LOCK_READER, 0x01, 0x00,
   HCI2_PAR_MGT_CURRENT_RF_LOCK_READER, 0x01, 0x00,

   P_HCI_SERVICE_SE,
   8,
   HCI2_PAR_SE_SETTINGS_DEFAULT,        0x02, 0x80, 0x00,
   HCI2_PAR_SE_SETTINGS_STANDBY,        0x02, 0x80, 0x00
};

/**
 * @See definition of tPHCIServiceOpenCompleted()
 **/
static void static_PNALBindingManagementServiceOpenCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         W_ERROR nError )
{
   if ( nError != W_SUCCESS )
   {
      PNALDebugError( "static_PNALBindingManagementServiceOpenCompleted: nError 0x%X", nError);

      pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS ;
      PNALBindingBootMachine(pBindingContext, P_NFC_HAL_BINDING_BOOT_FINISHED);
   }
   else
   {
      PNALBindingBootMachine(pBindingContext, P_NFC_HAL_BINDING_BOOT_GET_FIRMWARE_VERSION);
   }
}

/**
 * @See definition of tPNALGenericCompletion()
 **/
static void static_PNALBindingServiceCloseCompleteted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter)
{
   PNALDebugTrace("static_PNALBindingServiceCloseCompleteted: service 0x%x closed",
     PNALUtilConvertPointerToUint32(pCallbackParameter));
}

/**
 * @See definition of tPHCIServiceOpenCompleted()
 **/
static void static_PNALBindingServiceOpenCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         W_ERROR nError )
{
   pBindingContext->nProtocolCounter--;
   if ( nError != W_SUCCESS )
   {
         PNALDebugError( "static_PNALBindingServiceOpenCompleteted: nError 0x%X", nError);
         pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS ;
   }
   else
   {
      PNALDebugTrace("static_PNALBindingServiceOpenCompleteted: service 0x%x opened",
      PNALUtilConvertPointerToUint32(pCallbackParameter));
   }
   if(pBindingContext->nProtocolCounter == 0)
   {
       PNALBindingBootMachine(pBindingContext,
                           P_NFC_HAL_BINDING_BOOT_CONFIGURE_NFCC);
   }
}

/**
 * @See definition of tPHCIServiceExecuteCommandCompleted
 **/
static void static_PNALBindingGetOpenedProtocolsCompleted(
          tNALBindingContext* pBindingContext,
          void* pCallbackParameter,
          uint8_t* pBuffer,
          uint32_t nLength,
          W_ERROR nError,
          uint8_t nStatusCode,
          uint32_t nReceptionCounter)
{
   uint8_t nState =  (uint8_t)PNALUtilConvertPointerToUint32(pCallbackParameter);
   uint8_t nIndex;

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   /* Check the result */
   if ( nError != W_SUCCESS )
   {
      PNALDebugError( "static_PNALBindingGetOpenedProtocolsCompleted: nError %s",
         PNALUtilTraceError(nError));

      pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS ;
      nState = P_NFC_HAL_BINDING_BOOT_FINISHED;
      goto end_function;
   }
   if ( nStatusCode != ETSI_ERR_ANY_OK )
   {
      PNALDebugError(
         "static_PNALBindingGetOpenedProtocolsCompleted: nStatusCode 0x%08X",
         nStatusCode );

      pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS ;
      nState = P_NFC_HAL_BINDING_BOOT_FINISHED;
   }

   if(nState == P_NFC_HAL_BINDING_BOOT_GET_HDS_OWNER_READER)
   {
      if (( pBuffer == null )||
          ((nLength - 2) != HCI2_PAR_MGT_HDS_OWNER_READER_MSG_SIZE)||
          (pBuffer[1] != HCI2_PAR_MGT_HDS_OWNER_READER_MSG_SIZE))
      {
         PNALDebugError(
            "static_PNALBindingGetOpenedProtocolsCompleted:Bad parameters");

         pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS ;
         nState = P_NFC_HAL_BINDING_BOOT_FINISHED;
         goto end_function;
      }

      pBindingContext->nHCIReaderProtocolOpened = 0;

      /* Update the Reader Protocol capabilities value  */
      for(nIndex = 0; nIndex < HCI2_PAR_MGT_HDS_OWNER_READER_MSG_SIZE; nIndex++)
      {
         pBindingContext->nHCIReaderProtocolOpened |= (static_PNALBindingGetHCIReaderProtocolFromPipeID(pBuffer[2 + nIndex]));
      }

      pBindingContext->nHCIReaderProtocolToOpen &= ~pBindingContext->nHCIReaderProtocolOpened;

      nState = P_NFC_HAL_BINDING_BOOT_GET_HDS_OWNER_CARD;
   }
   else if(nState == P_NFC_HAL_BINDING_BOOT_GET_HDS_OWNER_CARD)
   {
      nState = P_NFC_HAL_BINDING_BOOT_OPEN_AND_CLOSE_PIPE;

      if (( pBuffer == null )||
           ((nLength - 2) != HCI2_PAR_MGT_HDS_OWNER_CARD_MSG_SIZE)||
           (pBuffer[1] != HCI2_PAR_MGT_HDS_OWNER_CARD_MSG_SIZE))
      {
         PNALDebugError(
            "static_PNALBindingGetOpenedProtocolsCompleted:Bad parameters");

         pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS ;
         nState = P_NFC_HAL_BINDING_BOOT_FINISHED;
         goto end_function;
      }

      pBindingContext->nHCICardProtocolOpened = 0;

      /* Update the Card Protocol capabilities value */
      for(nIndex = 0; nIndex < HCI2_PAR_MGT_HDS_OWNER_CARD_MSG_SIZE; nIndex++)
      {
         pBindingContext->nHCICardProtocolOpened |= (static_PNALBindingGetHCICardProtocolFromPipeID(pBuffer[2 + nIndex]));
      }
      pBindingContext->nHCICardProtocolToOpen &= ~pBindingContext->nHCICardProtocolOpened;
   }
   else
   {
      PNALDebugError(
            "static_PNALBindingGetOpenedProtocolsCompleted:Unknown State");
         pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS ;
         nState = P_NFC_HAL_BINDING_BOOT_FINISHED;
   }

end_function:
   PNALBindingBootMachine(pBindingContext, nState);
}

static void static_PNALBindingSetConfigurationCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         W_ERROR nError,
         uint8_t nStatusCode,
         uint32_t nReceptionCounter)
{
   uint8_t nState = P_NFC_HAL_BINDING_BOOT_SERVICE_SUBSCRIBE;

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   /* Check the result */
   if(( nError != W_SUCCESS )||
      ( nStatusCode != ETSI_ERR_ANY_OK ))
   {
      PNALDebugError( "static_PNALBindingSetConfigurationCompleted(: nError %s; nStatusCode 0x%08X",
         PNALUtilTraceError(nError), nStatusCode);
      pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS;
      nState = P_NFC_HAL_BINDING_BOOT_FINISHED;
   }
   PNALBindingBootMachine(pBindingContext, nState);
}

/**
 * @See definition of tPHCIServiceExecuteCommandCompleted
 **/
static void static_PNALBindingSubscribeCompleted(
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

   /* Check the result */
   if(( nError != W_SUCCESS )||
      ( nStatusCode != ETSI_ERR_ANY_OK ))
   {
      PNALDebugError( "static void static_PNALBindingSubscribeCompleted(: nError %s; nStatusCode 0x%08X",
         PNALUtilTraceError(nError), nStatusCode);
      pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS;
   }
   pBindingContext->nProtocolCounter--;

   if(pBindingContext->nProtocolCounter == 0)
   {
      PNALBindingBootMachine(pBindingContext, P_NFC_HAL_BINDING_BOOT_GET_PERSISTENT_POLICY);
   }
}

/**
 * @See definition of tPHCIServiceExecuteCommandCompleted
 **/
static void static_PNALBindingFirmwareStatusExecuteCommandCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         W_ERROR nError,
         uint8_t nStatusCode,
         uint32_t nReceptionCounter)
{
   uint8_t nState = P_NFC_HAL_BINDING_BOOT_GET_SESSION_ID;

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   if (( nStatusCode != ETSI_ERR_ANY_OK )||
      ( nError != W_SUCCESS ))
   {
      PNALDebugError(
      "static_PNALBindingFirmwareStatusExecuteCommandCompleted: nStatusCode 0x%08X, nError 0x%08X",
       nStatusCode, nError );
       pBindingContext->sNALParHardwareInfo.nFirmwareStatus = 0x00;
       pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS;
       nState = P_NFC_HAL_BINDING_BOOT_FINISHED;
       pBindingContext->nPatchStatus = 0xFF;
   }
   else
   {
      pBindingContext->nPatchStatus = pBuffer[2];
      if(( nLength != 0x03 )||
           ( pBuffer[2] != HCI2_VAL_MGT_PATCH_CONFIRMED ))
      {
          PNALDebugError(
             "static_PNALBindingFirmwareStatusExecuteCommandCompleted: Patch status (expected 0x40) (received 0x%02X)",
                pBuffer[2] );
          pBindingContext->sNALParHardwareInfo.nFirmwareStatus = 0x00;
          pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS;
          nState = P_NFC_HAL_BINDING_BOOT_FINISHED;
      }
   }

  PNALBindingBootMachine(pBindingContext, nState);
}
static void static_PNALBindingResetSessionIdExecuteCommandCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         W_ERROR nError,
         uint8_t nStatusCode,
         uint32_t nReceptionCounter)
{
   if ((nError != W_SUCCESS) || (nStatusCode != ETSI_ERR_ANY_OK))
   {
      PNALDebugError("static_PNALBindingResetSessionIdExecuteCommandCompleted : nError %d - nStatusCode %d", nError, nStatusCode);
   }
}

/**
 * @See definition of tPHCIServiceExecuteCommandCompleted
 **/
static void static_PNALBindingSessionIdExecuteCommandCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         W_ERROR nError,
         uint8_t nStatusCode,
         uint32_t nReceptionCounter)
{
   uint8_t nState = P_NFC_HAL_BINDING_BOOT_GET_HDS_OWNER_READER;
   uint8_t allZeros[8] = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
   uint8_t allOnes[8]  = { 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff };

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   if (( nStatusCode != ETSI_ERR_ANY_OK ) || ( nError != W_SUCCESS ) || (nLength != 10))
   {
      PNALDebugError("static_PNALBindingFirmwareStatusExecuteCommandCompleted: nStatusCode 0x%08X, nError 0x%08X", nStatusCode, nError );

      pBindingContext->sNALParHardwareInfo.nFirmwareStatus = 0x00;
      pBindingContext->nPatchStatus = 0xFF;

      pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS;
      nState = P_NFC_HAL_BINDING_BOOT_FINISHED;
   }

   if ((CNALMemoryCompare(pBuffer + 2 , allZeros, 8 ) == 0) || (CNALMemoryCompare(pBuffer + 2, allOnes, 8) == 0))
   {
      PNALDebugError("static_PNALBindingFirmwareStatusExecuteCommandCompleted: force complete reconfiguration");
      pBindingContext->bInitialBoot = W_TRUE;
   }

  PNALBindingBootMachine(pBindingContext, nState);
}

static void static_PNALBindingPersistentPolicyExecuteCommandCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         W_ERROR nError,
         uint8_t nStatusCode,
         uint32_t nHCIMessageReceptionCounter)
{
   uint8_t nState = P_NFC_HAL_BINDING_BOOT_GET_CARD_DETECT_STATE;

   bool_t bIsReaderLocked = W_FALSE;
   bool_t bIsCardLocked = W_FALSE;
   uint16_t nUICCCardPolicy = 0;
   uint16_t nUICCReaderPolicy = 0;
   uint16_t nSECardPolicy = 0;
   uint32_t nSESwitchPosition = NAL_POLICY_FLAG_SE_OFF;
   uint8_t nHCIMode;
   uint8_t nHCIFilter;

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nHCIMessageReceptionCounter);

   if (( nStatusCode != ETSI_ERR_ANY_OK ) || ( nError != W_SUCCESS ))
   {
      PNALDebugError( "static_PNALBindingFirmwareStatusExecuteCommandCompleted: nStatusCode 0x%08X, nError 0x%08X", nStatusCode, nError );
      pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS;
      nState = P_NFC_HAL_BINDING_BOOT_FINISHED;

      goto end;
   }

   /* Answer has the following format

         P_HCI_SERVICE_PIPE_MANAGEMENT
         6,
         HCI2_PAR_MGT_DEFAULT_RF_LOCK_CARD
         1,
         <value>
         HCI2_PAR_MGT_DEFAULT_RF_LOCK_READER
         1,
         <value>
         P_HCI_SERVICE_INSTANCE,
         8,
         HCI2_PAR_INSTANCES_DEFAULT_MCARD_GRANTED_TO_SIM
         2,
         <value><value>
         HCI2_PAR_INSTANCES_DEFAULT_MREAD_GRANTED_TO_SIM
         2,
         <value><value>

         P_HCI_SERVICE_SE                 Optional, depending of the presence of the SE
         4,
         HCI2_PAR_SE_SETTINGS_DEFAULT
         2,
         <value><value>
      */

   if ( (nLength != 24) && (nLength != 18))
   {
      PNALDebugError( "static_PNALBindingFirmwareStatusExecuteCommandCompleted: invalid answer length");
      pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS;
      nState = P_NFC_HAL_BINDING_BOOT_FINISHED;
      goto end;
   }

   if (  (pBuffer[0] != P_HCI_SERVICE_PIPE_MANAGEMENT) ||
         (pBuffer[1] != 6) ||
         (pBuffer[2] != HCI2_PAR_MGT_DEFAULT_RF_LOCK_CARD) ||
         (pBuffer[3] != 1) ||
         (pBuffer[5] != HCI2_PAR_MGT_DEFAULT_RF_LOCK_READER) ||
         (pBuffer[6] != 1) ||
         (pBuffer[8] != P_HCI_SERVICE_INSTANCE) ||
         (pBuffer[9] != 8) ||
         (pBuffer[10] != HCI2_PAR_INSTANCES_DEFAULT_MCARD_GRANTED_TO_SIM) ||
         (pBuffer[11] != 2) ||
         (pBuffer[14] != HCI2_PAR_INSTANCES_DEFAULT_MREAD_GRANTED_TO_SIM) ||
         (pBuffer[15] != 2))
   {
      PNALDebugError( "static_PNALBindingFirmwareStatusExecuteCommandCompleted: invalid answer format");
      pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS;
      nState = P_NFC_HAL_BINDING_BOOT_FINISHED;
      goto end;
   }

   if ( (nLength == 24) && ((pBuffer[18] != P_HCI_SERVICE_SE) || (pBuffer[19] != 4) || (pBuffer[20] != HCI2_PAR_SE_SETTINGS_DEFAULT) || (pBuffer[21] != 2)))
   {
      PNALDebugError( "static_PNALBindingFirmwareStatusExecuteCommandCompleted: invalid answer format");
      pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS;
      nState = P_NFC_HAL_BINDING_BOOT_FINISHED;
      goto end;
   }

    /* Reset policy flag */
   CNALMemoryFill(pBindingContext->aParam_NAL_PAR_PERSISTENT_POLICY, 0, NAL_POLICY_SIZE);

   /* set lock bits */
   bIsCardLocked = (pBuffer[4] == HCI2_VAL_UNLOCKED)?W_FALSE:W_TRUE;
   bIsReaderLocked = (pBuffer[7] == HCI2_VAL_UNLOCKED)?W_FALSE:W_TRUE;

   static_PNALBindingGetNALCardProtocolCapabilities(
                                 &nUICCCardPolicy,
                                 static_PNALBindingReadUint16FromHCIBuffer(&pBuffer[12]) );

   static_PNALBindingGetNALReaderProtocolCapabilities(
                                 &nUICCReaderPolicy,
                                 static_PNALBindingReadUint16FromHCIBuffer(&pBuffer[16]) );

   if (nLength == 24)
   {
      nHCIMode = pBuffer[22];
      nHCIFilter = pBuffer[23];

      if(nHCIMode == 1)
      {
         nSESwitchPosition = NAL_POLICY_FLAG_HOST_INTERFACE; /* Host interface */
      }
      else if(nHCIFilter != 0)
      {
         nSESwitchPosition = NAL_POLICY_FLAG_RF_INTERFACE; /* RF interface */
      }

      static_PNALBindingGetNALCardProtocolCapabilities(&nSECardPolicy, nHCIFilter);
   }
   else
   {
      nHCIMode = 0;
      nSESwitchPosition = 0;
      nSECardPolicy = 0;
   }

   /** Build the answer */
   PNALProtocolFormat_NAL_PAR_POLICY(
               pBindingContext->aParam_NAL_PAR_PERSISTENT_POLICY,
               bIsReaderLocked,
               bIsCardLocked,
               nUICCCardPolicy,
               nUICCReaderPolicy,
               nSESwitchPosition,
               0,
               nSECardPolicy);

   /* Store the value of the persistent policy in the volatile */
   CNALMemoryCopy(pBindingContext->aParam_NAL_PAR_POLICY, pBindingContext->aParam_NAL_PAR_PERSISTENT_POLICY, NAL_POLICY_SIZE);

end:

   PNALBindingBootMachine(pBindingContext, nState);
}

static void static_PNALBindingCardDetectStateExecuteCommandCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         W_ERROR nError,
         uint8_t nStatusCode,
         uint32_t nHCIMessageReceptionCounter)
{
   uint8_t nState = P_NFC_HAL_BINDING_BOOT_SET_POLLING_PERIOD;

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nHCIMessageReceptionCounter);

   if (( nStatusCode != ETSI_ERR_ANY_OK ) || ( nError != W_SUCCESS ))
   {
      PNALDebugError( "static_PNALBindingCardDetectStateExecuteCommandCompleted: nStatusCode 0x%08X, nError 0x%08X", nStatusCode, nError );
      pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS;
      nState = P_NFC_HAL_BINDING_BOOT_FINISHED;

      goto end;
   }

   /* Answer has the following format

         P_HCI_SERVICE_PIPE_MANAGEMENT
         3,
         HCI2_PAR_MGT_CARD_DETECT_STATE
         1,
         <value>
      */

   if (nLength != 5)
   {
      PNALDebugError( "static_PNALBindingCardDetectStateExecuteCommandCompleted: invalid answer length");
      pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS;
      nState = P_NFC_HAL_BINDING_BOOT_FINISHED;
      goto end;
   }

   if (  (pBuffer[0] != P_HCI_SERVICE_PIPE_MANAGEMENT) ||
         (pBuffer[1] != 3) ||
         (pBuffer[2] != HCI2_PAR_MGT_CARD_DETECT_STATE) ||
         (pBuffer[3] != 1) )
   {
      PNALDebugError( "static_PNALBindingCardDetectStateExecuteCommandCompleted: invalid answer format");
      pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS;
      nState = P_NFC_HAL_BINDING_BOOT_FINISHED;
      goto end;
   }

   pBindingContext->bCardDetectEnabled = ((pBuffer[4] & 0x01) == 0) ? W_FALSE : W_TRUE;

end:

   PNALBindingBootMachine(pBindingContext, nState);
}


static void static_PNALBindingSetPollingPeriodCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         W_ERROR nError,
         uint8_t nStatusCode,
         uint32_t nHCIMessageReceptionCounter)
{
   uint8_t nState = P_NFC_HAL_BINDING_BOOT_GET_ROUTING_TABLE_STATE;

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nHCIMessageReceptionCounter);

   if (( nStatusCode != ETSI_ERR_ANY_OK ) || ( nError != W_SUCCESS ))
   {
      PNALDebugError( "static_PNALBindingCardDetectStateExecuteCommandCompleted: nStatusCode 0x%08X, nError 0x%08X", nStatusCode, nError );
      pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS;
      nState = P_NFC_HAL_BINDING_BOOT_FINISHED;
   }

   PNALBindingBootMachine(pBindingContext, nState);
}


static void static_PNALBindingGetRoutingTableStateCompleted(
          tNALBindingContext* pBindingContext,
          void* pCallbackParameter,
          uint8_t* pBuffer,
          uint32_t nLength,
          W_ERROR nError,
          uint8_t nStatusCode,
          uint32_t nReceptionCounter)
{
   uint8_t nState = P_NFC_HAL_BINDING_BOOT_FINISHED;
   tNALParFirmwareInfo *pNALParFirmwareInfo = &pBindingContext->sNALParFirmwareInfo;
   tNALParHardwareInfo *pNALParHardwareInfo = &pBindingContext->sNALParHardwareInfo;

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   if (nError == W_SUCCESS)
   {
      if (pNALParHardwareInfo->nSEType != P_NFC_HAL_SE_NONE)
      {
         /* Add the routing capabilities */
         uint16_t nCurrentFirmwareCapabilities = static_PNALBindingReadUint16FromNALBuffer(pNALParFirmwareInfo->aFirmwareCapabilities);

         nCurrentFirmwareCapabilities |= NAL_CAPA_ROUTING_TABLE;

         static_PNALBindingWriteUint16ToNALBuffer(nCurrentFirmwareCapabilities, pNALParFirmwareInfo->aFirmwareCapabilities);
      }

   }
   else if (nError != W_ERROR_ITEM_NOT_FOUND)
   {
      PNALDebugError( "static_PNALBindingGetRoutingTableStateCompleted: nError %s",
         PNALUtilTraceError(nError));

      pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS ;
      nState = P_NFC_HAL_BINDING_BOOT_FINISHED;
   }

   PNALBindingBootMachine(pBindingContext, nState);
}



/**
 * Call the connection callback function.
 *
 * @param[in]  pBindingContext  The NFC HAL Binding Instance.
 **/
static void static_PNALBindingBootCallConnectCallback(
            tNALBindingContext *pBindingContext)
{
   if(pBindingContext->pCallbackConnectionFunction != null)
   {
      tNALBindingConnectCompleted * pFunction;

      PNALDebugTrace("static_PNALBindingBootCallConnectCallback : call pCallbackFunction");

      pFunction = pBindingContext->pCallbackConnectionFunction;
      pBindingContext->pCallbackConnectionFunction = null;

      pFunction(
         pBindingContext->pCallbackContext,
         pBindingContext->pCallbackConnectionParameter,
         pBindingContext->nNALBindingStatus);
   }
   else if(pBindingContext->pInternalCallbackConnectionFunction != null)
   {
      tNALBindingInternalConnectCompleted * pFunction;

      PNALDebugTrace("static_PNALBindingBootCallConnectCallback : call pInternalCallbackConnectionFunction");

      pFunction = pBindingContext->pInternalCallbackConnectionFunction;
      pBindingContext->pInternalCallbackConnectionFunction = null;

      PNALDFCPost2(
         pBindingContext,
         P_DFC_TYPE_NFC_HAL_BINDING,
         pFunction,
         pBindingContext->pCallbackConnectionParameter,
         PNALUtilConvertUint32ToPointer(pBindingContext->nNALBindingStatus));
   }
}

/**
 * @brief   Initiates The NFC HAL Binding Initialisation.
 *
 * @param[in]  pBindingContext  The NFC HAL Binding Instance.
 *
 * @param[in]  nState  The current state of the Boot Machine.
 **/
NFC_HAL_INTERNAL void PNALBindingBootMachine(
                            tNALBindingContext *pBindingContext,
                            uint8_t nState)
{
   uint8_t nProtocolIndex;
   uint32_t nIndex = 0;

   switch ( nState )
   {
      case P_NFC_HAL_BINDING_BOOT_GET_FIRMWARE_VERSION:

         PNALDebugTrace("PNALBindingBootMachine : P_NFC_HAL_BINDING_BOOT_GET_FIRMWARE_VERSION");

         /* Set up the initial state of tht NFC HAL Binding boot machine */
         pBindingContext->nHCIMode = W_NFCC_MODE_BOOT_PENDING;
         pBindingContext->bInitialBoot = W_FALSE;

         /* we are not in loader */
         pBindingContext->bInLoader = W_FALSE;

         PHCIServiceExecuteCommand(
                     pBindingContext,
                     P_HCI_SERVICE_PIPE_MANAGEMENT,
                     PNALBindingGetOperation(pBindingContext),
                     HCI2_CMD_MGT_IDENTITY,
                     null, 0x00,
                     pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
                     static_PNALBindingGetFirmwareVersionCompleted, null,
                     W_TRUE );

         /* On success : next step is P_NFC_HAL_BINDING_GET_PATCH_STATUS,
            otherwise, next step will be
             - P_NFC_HAL_BINDING_BOOT_LOADER_PENDING  if the command failed with ETSI_ERR_ANY_E_CMD_NOT_SUPPORTED
             - P_NFC_HAL_BINDING_BOOT_OPEN_PIPE_MANAGEMENT if the command failed with ETSI_ERR_ANY_E_PIPE_NOT_OPENED
             - P_NFC_HAL_BINDING_BOOT_FINISHED_NO_FIRMWARE if the command failed for another reason (bad payload, etc...) */

         break;

      case P_NFC_HAL_BINDING_BOOT_OPEN_PIPE_MANAGEMENT:

         PNALDebugTrace("PNALBindingBootMachine : P_NFC_HAL_BINDING_BOOT_OPEN_PIPE_MANAGEMENT");

         PHCIServiceOpen(
                  pBindingContext,
                  P_HCI_SERVICE_PIPE_MANAGEMENT,
                  static_PNALBindingManagementServiceOpenCompleted,
                  null,
                  W_TRUE);

         /* On success : next step is P_NFC_HAL_BINDING_BOOT_GET_FIRMWARE_VERSION,
            otherwise, next step will be
             - P_NFC_HAL_BINDING_BOOT_FINISHED if the command failed for another reason (bad payload, etc...) */

         break;

      case P_NFC_HAL_BINDING_BOOT_GET_PATCH_STATUS:

          PNALDebugTrace("PNALBindingBootMachine : P_NFC_HAL_BINDING_BOOT_GET_PATCH_STATUS");

          pBindingContext->aHCISendBuffer[0]= HCI2_PAR_MGT_PATCH_STATUS;
          PHCIServiceExecuteCommand(
                  pBindingContext,
                  P_HCI_SERVICE_PIPE_MANAGEMENT,
                  PNALBindingGetOperation(pBindingContext),
                  P_HCI_SERVICE_CMD_GET_PROPERTIES,
                  pBindingContext->aHCISendBuffer,
                  0x01,
                  pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
                  static_PNALBindingFirmwareStatusExecuteCommandCompleted,
                  pBindingContext,
                  W_TRUE );

          /* On success, next step is P_NFC_HAL_BINDING_BOOT_GET_SESSION_ID,
             otherwise, next step will be P_NFC_HAL_BINDING_BOOT_FINISHED */

      break;

      case P_NFC_HAL_BINDING_BOOT_GET_SESSION_ID:

         PNALDebugTrace("PNALBindingBootMachine : P_NFC_HAL_BINDING_BOOT_GET_SESSION_ID");

         pBindingContext->aHCISendBuffer[0]= HCI2_PAR_ADM_SESSION_ID;

         PHCIServiceExecuteCommand(
                  pBindingContext,
                  P_HCI_SERVICE_ADMINISTRATION,
                  PNALBindingGetOperation(pBindingContext),
                  P_HCI_SERVICE_CMD_GET_PROPERTIES,
                  pBindingContext->aHCISendBuffer,
                  0x01,
                  pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
                  static_PNALBindingSessionIdExecuteCommandCompleted,
                  pBindingContext,
                  W_TRUE );

         /* On success : next step is P_NFC_HAL_BINDING_GET_HDS_OWNER_READER
            otherwise, next step will be
             - P_NFC_HAL_BINDING_BOOT_FINISHED if the command failed for another reason (bad payload, etc...) */
         break;

      case P_NFC_HAL_BINDING_BOOT_GET_HDS_OWNER_READER:

         PNALDebugTrace("PNALBindingBootMachine : P_NFC_HAL_BINDING_BOOT_GET_HDS_OWNER_READER");

         /* Get the opened Reader Protocols by NFC Device */

         pBindingContext->aHCISendBuffer[0] = HCI2_PAR_MGT_HDS_OWNER_READER;
         PHCIServiceExecuteCommand(
                     pBindingContext,
                     P_HCI_SERVICE_PIPE_MANAGEMENT,
                     PNALBindingGetOperation(pBindingContext),
                     P_HCI_SERVICE_CMD_GET_PROPERTIES,
                     pBindingContext->aHCISendBuffer,
                     0x01,
                     pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
                     static_PNALBindingGetOpenedProtocolsCompleted, PNALUtilConvertUint32ToPointer(P_NFC_HAL_BINDING_BOOT_GET_HDS_OWNER_READER),
                     W_TRUE );

         /* Next step is P_NFC_HAL_BINDING_BOOT_GET_HDS_OWNER_CARD */

         break;

      case P_NFC_HAL_BINDING_BOOT_GET_HDS_OWNER_CARD:

         PNALDebugTrace("PNALBindingBootMachine : P_NFC_HAL_BINDING_BOOT_GET_HDS_OWNER_CARD");

         /* Get the opened Card Protocols  by NFC Device */
         pBindingContext->aHCISendBuffer[0] = HCI2_PAR_MGT_HDS_OWNER_CARD;
         PHCIServiceExecuteCommand(
                     pBindingContext,
                     P_HCI_SERVICE_PIPE_MANAGEMENT,
                     PNALBindingGetOperation(pBindingContext),
                     P_HCI_SERVICE_CMD_GET_PROPERTIES,
                     pBindingContext->aHCISendBuffer,
                     0x01,
                     pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
                     static_PNALBindingGetOpenedProtocolsCompleted, PNALUtilConvertUint32ToPointer(P_NFC_HAL_BINDING_BOOT_GET_HDS_OWNER_CARD),
                     W_TRUE );

         /* Next step is P_NFC_HAL_BINDING_BOOT_HDS_OWNER_CARD_EXECUTED */

         break;

      case P_NFC_HAL_BINDING_BOOT_OPEN_AND_CLOSE_PIPE:

         PNALDebugTrace("PNALBindingBootMachine : P_NFC_HAL_BINDING_BOOT_OPEN_AND_CLOSE_PIPE");

         CNALDebugAssert(pBindingContext->nProtocolCounter == 0);

         /* event register for generic reader service */

         PNALDebugTrace("PHCIServiceRegister(P_HCI_SERVICE_MREAD,P_EVT_REGISTER_TYPE)");

         if (pBindingContext->bRegisterDone == W_FALSE)
         {
            PHCIServiceRegister(
                 pBindingContext,
                 P_HCI_SERVICE_MREAD,
                 P_EVT_REGISTER_TYPE,
                 W_FALSE,
                 PNALBindingGetOperation(pBindingContext),
                 PNALBindingReaderEventDataReceived,
                 PNALUtilConvertUint32ToPointer(P_HCI_SERVICE_MREAD),
                W_TRUE );

            /* event register for generic card service */
            PHCIServiceRegister(
               pBindingContext,
               P_HCI_SERVICE_MCARD,
               P_EVT_REGISTER_TYPE,
               W_FALSE,
               PNALBindingGetOperation(pBindingContext),
               PNALBindingCardEventDataReceived,
               PNALUtilConvertUint32ToPointer(P_HCI_SERVICE_MCARD),
               W_TRUE );
         }

         /* Close Reader Protocol if not requested */
         for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingReaderProtocolArraySize; nProtocolIndex++)
         {
            if(((g_aNALBindingReaderProtocolArray[nProtocolIndex].nProtocolAttributes & P_NFC_HAL_BINDING_PROTOCOL_ATTR_CLOSE) != 0)&&
               ((g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIProtocolCapability & pBindingContext->nHCIReaderProtocolOpened) != 0))
            {
               PNALDebugTrace("PHCIServiceClose(reader %d)", g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier);

               PHCIServiceClose( pBindingContext,
                  g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier,
                  static_PNALBindingServiceCloseCompleteted,
                  PNALUtilConvertUint32ToPointer(g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier),
                  W_TRUE
                  );
            }
         }

         /* Close card Protocol if not requested */
         for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingCardProtocolArraySize; nProtocolIndex++)
         {
            if(((g_aNALBindingCardProtocolArray[nProtocolIndex].nProtocolAttributes & P_NFC_HAL_BINDING_PROTOCOL_ATTR_CLOSE) != 0)&&
               ((g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIProtocolCapability & pBindingContext->nHCICardProtocolOpened) != 0))
            {
               PNALDebugTrace("PHCIServiceClose(card %d)", g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIServiceIdentifier);

               PHCIServiceClose( pBindingContext,
                  g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIServiceIdentifier,
                  static_PNALBindingServiceCloseCompleteted,
                  PNALUtilConvertUint32ToPointer(g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIServiceIdentifier),
                  W_TRUE
                  );
            }
         }
         /* Open Reader Protocol if not opened */
         for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingReaderProtocolArraySize; nProtocolIndex++)
         {
            if(((g_aNALBindingReaderProtocolArray[nProtocolIndex].nProtocolAttributes & P_NFC_HAL_BINDING_PROTOCOL_ATTR_OPEN) != 0)&&
               ((g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIProtocolCapability & pBindingContext->nHCIReaderProtocolToOpen) != 0))
            {
               PNALDebugTrace("PHCIServiceOpen(reader %d)", g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier);

               PHCIServiceOpen( pBindingContext,
                  g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier,
                  static_PNALBindingServiceOpenCompleted,
                  PNALUtilConvertUint32ToPointer(g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier),
                  W_TRUE
                  );
               pBindingContext->nProtocolCounter++;
            }
         }
         /* Open card Protocol if not opened */
         for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingCardProtocolArraySize; nProtocolIndex++)
         {
            if(((g_aNALBindingCardProtocolArray[nProtocolIndex].nProtocolAttributes & P_NFC_HAL_BINDING_PROTOCOL_ATTR_OPEN) != 0)&&
               ((g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIProtocolCapability & pBindingContext->nHCICardProtocolToOpen) != 0))
            {
               PNALDebugTrace("PHCIServiceOpen(card %d)", g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIServiceIdentifier);

               PHCIServiceOpen( pBindingContext,
                  g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIServiceIdentifier,
                  static_PNALBindingServiceOpenCompleted,
                  PNALUtilConvertUint32ToPointer(g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIServiceIdentifier),
                  W_TRUE
                  );
               pBindingContext->nProtocolCounter++;
            }
         }

         if(pBindingContext->nProtocolCounter != 0)
         {
            /* There are some pending open / close waiting for processing
               On success, wext step will be P_NFC_HAL_BINDING_BOOT_CONFIGURE_NFCC when all pending operation will be done,
               on failure, next step will be P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS */

           break;
         }
         else
         {
            PNALDebugTrace("No need to open/close any pipe");
         }

         /* otherwise, walk through to the configure procedure.... */

      case P_NFC_HAL_BINDING_BOOT_CONFIGURE_NFCC:

         PNALDebugTrace("PNALBindingBootMachine : P_NFC_HAL_BINDING_BOOT_CONFIGURE_NFCC");

         if (pBindingContext->bInitialBoot == W_TRUE)
         {
            PHCIServiceExecuteMultiServiceCommand(
                                 pBindingContext,
                                 PNALBindingGetOperation(pBindingContext),
                                 P_HCI_SERVICE_CMD_SET_PROPERTIES,
                                 g_aHCIServiceParam,
                                 sizeof(g_aHCIServiceParam),
                                 pBindingContext->aHCIResponseBuffer,
                                 sizeof(pBindingContext->aHCIResponseBuffer),
                                 static_PNALBindingSetConfigurationCompleted,
                                 pBindingContext,
                                 W_TRUE);

            /* On success, next step will be P_NFC_HAL_BINDING_BOOT_SERVICE_SUBSCRIBE
               On failure, next step will be P_NFC_HAL_BINDING_BOOT_FINISHED */

            break;
         }
         else
         {
            PNALDebugTrace("PNALBindingBootMachine : no need to reconfigure the NFCC");
         }

         /* otherwise, walk through to the subscribe procedure.... */

       case P_NFC_HAL_BINDING_BOOT_SERVICE_SUBSCRIBE:

          PNALDebugTrace("PNALBindingBootMachine : P_NFC_HAL_BINDING_BOOT_SERVICE_SUBSCRIBE");

         /* The subscribe procedure allows to reset all parameters to known values :
            the parameters are persistent, so when we boot we can not be sure the current value is the same as the default one,
            since they might have been modified in the past */

         CNALDebugAssert(pBindingContext->nProtocolCounter == 0);

         for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingReaderProtocolArraySize; nProtocolIndex++)
         {
            if ((g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIProtocolCapability & pBindingContext->nHCIAvailableReaderProtocol) != 0)
            {
               if ((g_aNALBindingReaderProtocolArray[nProtocolIndex].nProtocolAttributes & P_NFC_HAL_BINDING_PROTOCOL_ATTR_SUBSCRIBE) != 0)
               {
                  /* susbscribe command is supported */

                  PNALDebugTrace("PHCIServiceExecuteCommand(HCI2_CMD_MREAD_SUBSCRIBE : nIndex %d - HCI id %d)", nProtocolIndex, g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier);

                  PHCIServiceExecuteCommand(
                     pBindingContext,
                     g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier,
                     PNALBindingGetOperation(pBindingContext),
                     HCI2_CMD_MREAD_SUBSCRIBE,
                     g_aNALBindingReaderProtocolArray[nProtocolIndex].pHCIProtocolConfig,
                     g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIProtocolConfigLength,
                     pBindingContext->aHCIResponseBuffer,
                     sizeof(pBindingContext->aHCIResponseBuffer),
                     static_PNALBindingSubscribeCompleted, null,
                     W_TRUE );

                  pBindingContext->nProtocolCounter++;
               }
               else
               {
                  if (g_aNALBindingReaderProtocolArray[nProtocolIndex].pHCIProtocolConfig != null)
                  {
                     /* subscribe command is not supported, we use SET_PROPERTIES instead if some parameters are provided*/

                     PHCIServiceExecuteCommand(
                        pBindingContext,
                        g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier,
                        PNALBindingGetOperation(pBindingContext),
                        P_HCI_SERVICE_CMD_SET_PROPERTIES,
                        g_aNALBindingReaderProtocolArray[nProtocolIndex].pHCIProtocolConfig,
                        g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIProtocolConfigLength,
                        pBindingContext->aHCIResponseBuffer,
                        sizeof(pBindingContext->aHCIResponseBuffer),
                        static_PNALBindingSubscribeCompleted, null,
                        W_TRUE );

                     pBindingContext->nProtocolCounter++;
                  }
               }
            }
         }

        for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingCardProtocolArraySize; nProtocolIndex++)
        {
              if(((g_aNALBindingCardProtocolArray[nProtocolIndex].nProtocolAttributes & P_NFC_HAL_BINDING_PROTOCOL_ATTR_SUBSCRIBE) != 0)
                &&((g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIProtocolCapability & pBindingContext->nHCIAvailableCardProtocol) != 0)
                &&(g_aNALBindingCardProtocolArray[nProtocolIndex].pHCIProtocolConfig != null))
              {
                 PNALDebugTrace("PHCIServiceExecuteCommand(P_HCI_SERVICE_CMD_SET_PROPERTIES, nIndex %d - HCI id %d)", nProtocolIndex, g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier);

                 PHCIServiceExecuteCommand(
                                    pBindingContext,
                                    g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIServiceIdentifier,
                                    PNALBindingGetOperation(pBindingContext),
                                    P_HCI_SERVICE_CMD_SET_PROPERTIES, /*HCI2_CMD_MCARD_SUBSCRIBE,*/
                                    g_aNALBindingCardProtocolArray[nProtocolIndex].pHCIProtocolConfig,
                                    g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIProtocolConfigLength,
                                    pBindingContext->aHCIResponseBuffer,
                                    sizeof(pBindingContext->aHCIResponseBuffer),
                                    static_PNALBindingSubscribeCompleted, null,
                                    W_TRUE );

                  pBindingContext->nProtocolCounter++;
             }
        }

        if(pBindingContext->nProtocolCounter != 0)
        {
           /* There are some pending subscribe waiting for processing
               On success, wext step will be P_NFC_HAL_BINDING_BOOT_GET_PERSISTENT_POLICY when all pending operation will be done,
               on failure, next step will be P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS */

           break;
        }

        /* otherwise, walk through to the boot finished procedure.... */

      case  P_NFC_HAL_BINDING_BOOT_GET_PERSISTENT_POLICY:

         PNALDebugTrace("PNALBindingBootMachine : P_NFC_HAL_BINDING_BOOT_GET_PERSISTENT_POLICY");

                  /* Get the persistent policy value */

         pBindingContext->aHCISendBuffer[nIndex++] = P_HCI_SERVICE_PIPE_MANAGEMENT;
         pBindingContext->aHCISendBuffer[nIndex++] = 0x02;
         pBindingContext->aHCISendBuffer[nIndex++] = HCI2_PAR_MGT_DEFAULT_RF_LOCK_CARD;
         pBindingContext->aHCISendBuffer[nIndex++] = HCI2_PAR_MGT_DEFAULT_RF_LOCK_READER;

         pBindingContext->aHCISendBuffer[nIndex++] = P_HCI_SERVICE_INSTANCE;
         pBindingContext->aHCISendBuffer[nIndex++] = 0x02;
         pBindingContext->aHCISendBuffer[nIndex++] = HCI2_PAR_INSTANCES_DEFAULT_MCARD_GRANTED_TO_SIM;
         pBindingContext->aHCISendBuffer[nIndex++] = HCI2_PAR_INSTANCES_DEFAULT_MREAD_GRANTED_TO_SIM;

         if (pBindingContext->sNALParHardwareInfo.nSEType != P_NFC_HAL_SE_NONE)
         {
            pBindingContext->aHCISendBuffer[nIndex++] = P_HCI_SERVICE_SE;
            pBindingContext->aHCISendBuffer[nIndex++] = 0x01;
            pBindingContext->aHCISendBuffer[nIndex++] = HCI2_PAR_SE_SETTINGS_DEFAULT;
         }

         PHCIServiceExecuteMultiServiceCommand(
            pBindingContext,
            PNALBindingGetOperation(pBindingContext),
            P_HCI_SERVICE_CMD_GET_PROPERTIES,
            pBindingContext->aHCISendBuffer,
            nIndex,
            pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
            static_PNALBindingPersistentPolicyExecuteCommandCompleted,
            pBindingContext,
            W_TRUE );

         /* Next step will be P_NFC_HAL_BINDING_BOOT_GET_CARD_DETECT_STATUS */
         break;

      case P_NFC_HAL_BINDING_BOOT_GET_CARD_DETECT_STATE:

         PNALDebugTrace("PNALBindingBootMachine : P_NFC_HAL_BINDING_BOOT_GET_CARD_DETECT_STATUS");

                  /* Get the persistent policy value */

         pBindingContext->aHCISendBuffer[nIndex++] = P_HCI_SERVICE_PIPE_MANAGEMENT;
         pBindingContext->aHCISendBuffer[nIndex++] = 0x01;
         pBindingContext->aHCISendBuffer[nIndex++] = HCI2_PAR_MGT_CARD_DETECT_STATE;

         PHCIServiceExecuteMultiServiceCommand(
            pBindingContext,
            PNALBindingGetOperation(pBindingContext),
            P_HCI_SERVICE_CMD_GET_PROPERTIES,
            pBindingContext->aHCISendBuffer,
            nIndex,
            pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
            static_PNALBindingCardDetectStateExecuteCommandCompleted,
            pBindingContext,
            W_TRUE );


         /* Next step will be P_NFC_HAL_BINDING_BOOT_SET_POLLING_PERIOD */
         break;

      case P_NFC_HAL_BINDING_BOOT_SET_POLLING_PERIOD:

         PNALDebugTrace("PNALBindingBootMachine : P_NFC_HAL_BINDING_BOOT_SET_POLLING_PERIOD");

         if (pBindingContext->bCardDetectEnabled != W_FALSE)
         {
            pBindingContext->aParam_NAL_PAR_DETECT_PULSE[0] = NAL_SERVICE_ADMIN;
            pBindingContext->aParam_NAL_PAR_DETECT_PULSE[1] = NAL_CMD_SET_PARAMETER;
            pBindingContext->aParam_NAL_PAR_DETECT_PULSE[2] = NAL_PAR_DETECT_PULSE;
            static_PNALBindingWriteUint16ToNALBuffer(NAL_PAR_DETECT_PULSE_DEFAULT_VALUE_CD_ON, &pBindingContext->aParam_NAL_PAR_DETECT_PULSE[3]);

            PNALBindingBuildHCICommandPulse(pBindingContext, NAL_SERVICE_ADMIN, pBindingContext->aParam_NAL_PAR_DETECT_PULSE, sizeof(pBindingContext->aParam_NAL_PAR_DETECT_PULSE),
                                                         pBindingContext->aHCISendBuffer, &nIndex);

            PHCIServiceExecuteMultiServiceCommand(
                  pBindingContext,
                  & pBindingContext->sNALBindingWakeUpOperation,
                  P_HCI_SERVICE_CMD_SET_PROPERTIES,
                  pBindingContext->aHCISendBuffer,
                  nIndex,
                  pBindingContext->aHCIResponseBuffer,
                  sizeof(pBindingContext->aHCIResponseBuffer),
                  static_PNALBindingSetPollingPeriodCompleted,
                  null,
                  W_TRUE );

            break;

            /* Next step will be P_NFC_HAL_BINDING_BOOT_GET_ROUTING_TABLE_STATE */

         }

         /* otherwise, walk through to the get routing table state .... */

      case P_NFC_HAL_BINDING_BOOT_GET_ROUTING_TABLE_STATE:

         PNALDebugTrace("PNALBindingBootMachine : P_NFC_HAL_BINDING_BOOT_GET_ROUTING_TABLE_STATE");

         pBindingContext->aHCISendBuffer[0] = HCI2_PAR_MCARD_GEN_ROUTING_TABLE_ENABLED;

         PHCIServiceExecuteCommand(
                     pBindingContext,
                     P_HCI_SERVICE_MCARD,
                     PNALBindingGetOperation(pBindingContext),
                     P_HCI_SERVICE_CMD_GET_PROPERTIES,
                     pBindingContext->aHCISendBuffer,
                     0x01,
                     pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
                     static_PNALBindingGetRoutingTableStateCompleted,
                     null,
                     W_TRUE );

         break;

      case P_NFC_HAL_BINDING_BOOT_FINISHED:

         PNALDebugTrace("PNALBindingBootMachine : P_NFC_HAL_BINDING_BOOT_FINISHED");

         if(pBindingContext->nNALBindingStatus == P_NFC_HAL_BINDING_INIT_STATUS)
         {
            /* event register for card mode */

            if (pBindingContext->bRegisterDone == W_FALSE)
            {
               for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingCardProtocolArraySize; nProtocolIndex++)
                 {
                    if(((g_aNALBindingCardProtocolArray[nProtocolIndex].nProtocolAttributes & P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_EVT) != 0)&&
                       ((g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIProtocolCapability & pBindingContext->nHCIAvailableCardProtocol) != 0))
                    {
                        PNALDebugTrace("PHCIServiceRegister(P_EVT_REGISTER_TYPE, HCI Service ID %d", g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIServiceIdentifier);

                        PHCIServiceRegister(
                               pBindingContext,
                               g_aNALBindingCardProtocolArray[nProtocolIndex].nHCIServiceIdentifier,
                               P_EVT_REGISTER_TYPE,
                               W_FALSE,
                               PNALBindingGetOperation(pBindingContext),
                               PNALBindingCardEventDataReceived,
                               PNALUtilConvertUint32ToPointer(g_aNALBindingCardProtocolArray[nProtocolIndex].nNALServiceIdentifier),
                               W_TRUE );
                   }
                }

                for(nProtocolIndex = 0; nProtocolIndex < g_nNALBindingReaderProtocolArraySize; nProtocolIndex++)
                {
                   if((g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIProtocolCapability & pBindingContext->nHCIAvailableReaderProtocol) != 0)
                   {
                      /* event register for Reader Mode */
                      if((g_aNALBindingReaderProtocolArray[nProtocolIndex].nProtocolAttributes & P_NFC_HAL_BINDING_PROTOCOL_ATTR_REG_EVT) != 0)
                      {
                         PNALDebugTrace("PHCIServiceRegister(P_EVT_REGISTER_TYPE, HCI Service ID %d)", g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier);

                         PHCIServiceRegister(
                                       pBindingContext,
                                       g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier,
                                       P_EVT_REGISTER_TYPE,
                                       W_FALSE,
                                       PNALBindingGetOperation(pBindingContext),
                                       PNALBindingReaderEventDataReceived,
                                       PNALUtilConvertUint32ToPointer(g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier),
                                       W_TRUE );

                        /* the reader start_discovery cmd is sent to one of NFCC opened gate reader */
                        pBindingContext->nOneOfOpenedReaderServiceId = g_aNALBindingReaderProtocolArray[nProtocolIndex].nHCIServiceIdentifier;
                      }
                  }
                }

               /* register for Connectivity Message events */

               PNALDebugTrace("PHCIServiceRegister(P_EVT_REGISTER_TYPE, P_HCI_SERVICE_UICC_CONNECTIVITY)");

               PHCIServiceRegister(
                     pBindingContext,
                     P_HCI_SERVICE_UICC_CONNECTIVITY,
                     P_EVT_REGISTER_TYPE,
                     W_FALSE,
                     PNALBindingGetOperation(pBindingContext),
                     PNALBindingUICCEventDataReceived,
                     PNALUtilConvertUint32ToPointer(NAL_SERVICE_UICC),
                     W_TRUE );

               /* register for SE events */

               PHCIServiceRegister(
                     pBindingContext,
                     P_HCI_SERVICE_SE,
                     P_EVT_REGISTER_TYPE,
                     W_FALSE,
                     PNALBindingGetOperation(pBindingContext),
                     PNALBindingSEEventDataReceived,
                     PNALUtilConvertUint32ToPointer(NAL_SERVICE_SECURE_ELEMENT),
                     W_TRUE );
            }

            /* register for chip management events */

            PNALDebugTrace("PHCIServiceRegister(P_EVT_REGISTER_TYPE, P_HCI_SERVICE_PIPE_MANAGEMENT)");

            PHCIServiceRegister(
                  pBindingContext,
                  P_HCI_SERVICE_PIPE_MANAGEMENT,
                  P_EVT_REGISTER_TYPE,
                  W_FALSE,
                  PNALBindingGetOperation(pBindingContext),
                  PNALBindingChipManagementEventDataReceived,
                  PNALUtilConvertUint32ToPointer(NAL_SERVICE_ADMIN),
                  W_TRUE );

            pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_VALID_STATUS;
            pBindingContext->bRegisterDone = W_TRUE;
            pBindingContext->nHCIMode = W_NFCC_MODE_ACTIVE;


            pBindingContext->bLLCPTimeoutSupported = W_TRUE;
         }
         else
         {
            PNALDebugError("NFCC Boot procedure failed!!!");

            if (pBindingContext->bInLoader == W_FALSE)
            {
               /* NFCC boot procedure fails, reset session id to force a complete boot at next re-start*/

               pBindingContext->aHCISendBuffer[0]= HCI2_PAR_ADM_SESSION_ID;
               pBindingContext->aHCISendBuffer[1]= 8;
               CNALMemoryFill(& pBindingContext->aHCISendBuffer[2], 0, 8);

               PHCIServiceExecuteCommand(
                     pBindingContext,
                     P_HCI_SERVICE_ADMINISTRATION,
                     PNALBindingGetOperation(pBindingContext),
                     P_HCI_SERVICE_CMD_SET_PROPERTIES,
                     pBindingContext->aHCISendBuffer,
                     10,
                     pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
                     static_PNALBindingResetSessionIdExecuteCommandCompleted,
                     pBindingContext,
                     W_TRUE );
            }
         }

         PNALDebugTrace("call the boot callback");

         pBindingContext->nHCIMode = W_NFCC_MODE_ACTIVE;

         /* Post NFC HAL connection callback once NFC HAL binding initialisation is finished */
         static_PNALBindingBootCallConnectCallback(pBindingContext);
         break;

      case P_NFC_HAL_BINDING_BOOT_LOADER_PENDING:

         /* We enter this state when we detect we are in loader */

         PNALDebugTrace("PNALBindingBootMachine : P_NFC_HAL_BINDING_BOOT_LOADER_PENDING");

         /* we are in loader */
         pBindingContext->bInLoader = W_TRUE;

         pBindingContext->sNALParHardwareInfo.nFirmwareStatus = 0x00;
         pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS;
         CNALMemoryCopy(pBindingContext->aHCISendBuffer, g_aLoaderVersionParam, sizeof(g_aLoaderVersionParam));

         PHCIServiceExecuteCommand(
            pBindingContext,
            P_HCI_SERVICE_PIPE_MANAGEMENT,
            PNALBindingGetOperation(pBindingContext),
            P_HCI_SERVICE_CMD_GET_PROPERTIES,
            pBindingContext->aHCISendBuffer,
            sizeof(g_aLoaderVersionParam),
            pBindingContext->aHCIResponseBuffer, sizeof(pBindingContext->aHCIResponseBuffer),
            static_PNALBindingGetLoaderVersionCompleted, null,
            W_TRUE );

        break;
    }
}

/**
 * @See definition of tPHCIServiceExecuteCommandCompleted
 **/
static void static_PNALBindingGetLoaderVersionCompleted(
          tNALBindingContext* pBindingContext,
          void* pCallbackParameter,
          uint8_t* pBuffer,
          uint32_t nLength,
          W_ERROR nError,
          uint8_t nStatusCode,
          uint32_t nReceptionCounter)
{
   tNALParHardwareInfo* pNALParHardwareInfo;
   uint32_t nIndex = 0x00;

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   /* Check the result */
   if(( nError != W_SUCCESS )||
      ( nStatusCode != ETSI_ERR_ANY_OK ))
   {
      PNALDebugError( "static void static_PNALBindingGetLoaderVersionCompleted(: nError %s; nStatusCode 0x%08X",
         PNALUtilTraceError(nError), nStatusCode);
      pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_HARDWARE_STATUS;
   }
   pNALParHardwareInfo = &pBindingContext->sNALParHardwareInfo;
   /* loader command success, get loader version */

   if((pBuffer != null)&&
      (nLength > 0))
   {
      while ( nIndex < nLength )
      {
         switch (pBuffer[nIndex])
         {
            case HCI2_PAR_MGT_VERSION_HARDWARE:
               CNALMemoryCopy(pNALParHardwareInfo->aHardwareVersion, &pBuffer[nIndex + 2], P_NFC_HAL_VERSION_SIZE);
               /* Add a temporary patch to correct NFCC bug: set lats byte to 0x00 */
               pNALParHardwareInfo->aHardwareVersion[P_NFC_HAL_VERSION_SIZE - 1] = 0x00;
               break;
            case HCI2_PAR_MGT_VERSION_LOADER:
               CNALMemoryCopy(pNALParHardwareInfo->aLoaderVersion, &pBuffer[nIndex + 2], P_NFC_HAL_VERSION_SIZE);
               break;
            case HCI2_PAR_MGT_SERIAL_ID:
              CNALMemoryCopy(pNALParHardwareInfo->aHardwareSerialNumber, &pBuffer[nIndex + 2], P_NFC_HAL_HARDWARE_SERIAL_NUMBER_SIZE);
              break;
            case HCI2_PAR_MGT_PATCH_STATUS:
               pBindingContext->nPatchStatus = pBuffer[nIndex + 2];
               break;
            default:
              PNALDebugWarning(
                       "static_PNALBindingGetLoaderVersionCompleted: the parameter 0x%02X is not managed",
                       pBuffer[nIndex] );
              break;
         }

         nIndex += pBuffer[nIndex + 1] + 2;
      }

      pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS ;
      PNALBindingBootMachine(pBindingContext, P_NFC_HAL_BINDING_BOOT_FINISHED);
   }
}

/* Special Serial Number range */

/*
 * securead 628 without active SE
 * Information from Production
 * 14 October 2010 15:47
 */
static const uint8_t g_aNoSE_SerialNumberRangeInf1[] = {0xFA, 0x85, 0x0E, 0x00, 0x0C, 0x00, 0x12, 0xE0};
static const uint8_t g_aNoSE_SerialNumberRangeSup1[] = {0x82, 0xB6, 0x0E, 0x00, 0x0C, 0x00, 0x12, 0xE0};

static const uint8_t g_aNoSE_SerialNumberRangeInf2[] = {0x94, 0xB6, 0x0E, 0x00, 0x0C, 0x00, 0x12, 0xE0};
static const uint8_t g_aNoSE_SerialNumberRangeSup2[] = {0x25, 0xE4, 0x0E, 0x00, 0x0C, 0x00, 0x12, 0xE0};

/*
 * securead 628 with SE
 * Information from Production
 * 18 October 2010 17:57
 */
static const uint8_t g_aJupiterSE_SerialNumberRangeInf1[] = {0x66, 0x84, 0x03, 0x00, 0x0D, 0x00, 0x12, 0xE0};
static const uint8_t g_aJupiterSE_SerialNumberRangeSup1[] = {0x3B, 0x97, 0x03, 0x00, 0x0D, 0x00, 0x12, 0xE0};

static const uint8_t g_aJupiterSE_SerialNumberRangeInf2[] = {0x74, 0xD7, 0x03, 0x00, 0x0D, 0x00, 0x12, 0xE0};
static const uint8_t g_aJupiterSE_SerialNumberRangeSup2[] = {0x69, 0xE0, 0x03, 0x00, 0x0D, 0x00, 0x12, 0xE0};

/**
 * Checks if the serial number is in a specified range.
 *
 * @param[in]  pSerialNumberToCheck  The serial number to check.
 *
 * @param[in]  pSerialNumberInf  The first serial number of the range.
 *
 * @param[in]  pSerialNumberSup  The last serial number of the range.
 *
 * @return W_TRUE if the serial number is in the range, inclusive. W_FALSE otherwise.
 **/
static bool_t static_PNALBintingCheckSerialNumber(
                  const uint8_t* pSerialNumberToCheck,
                  const uint8_t* pSerialNumberInf,
                  const uint8_t* pSerialNumberSup)
{
   uint32_t nPos = P_NFC_HAL_HARDWARE_SERIAL_NUMBER_SIZE;

   while(nPos != 0)
   {
      nPos--;
      if(pSerialNumberToCheck[nPos] < pSerialNumberInf[nPos])
      {
         return W_FALSE;
      }
      else if(pSerialNumberToCheck[nPos] > pSerialNumberInf[nPos])
      {
         break;
      }
   }

   nPos = P_NFC_HAL_HARDWARE_SERIAL_NUMBER_SIZE;
   while(nPos != 0)
   {
      nPos--;
      if(pSerialNumberToCheck[nPos] > pSerialNumberSup[nPos])
      {
         return W_FALSE;
      }
      else if(pSerialNumberToCheck[nPos] < pSerialNumberSup[nPos])
      {
         break;
      }
   }

   return W_TRUE;
}

/**
 * @See definition of tPHCIServiceExecuteCommandCompleted
 **/
static void static_PNALBindingGetFirmwareVersionCompleted(
          tNALBindingContext* pBindingContext,
          void* pCallbackParameter,
          uint8_t* pBuffer,
          uint32_t nLength,
          W_ERROR nError,
          uint8_t nStatusCode,
          uint32_t nReceptionCounter)
{
   tNALParHardwareInfo *pNALParHardwareInfo;
   tNALParFirmwareInfo *pNALParFirmwareInfo;
   uint8_t nIndex = 0;
   uint16_t nNALCardProtocolCapabilities;
   uint16_t nNALReaderProtocolCapabilities;
   uint16_t nHCIP2PProtocolCapabilities;
   uint16_t nFirmwareCapabilities = 0;
   uint8_t nState = P_NFC_HAL_BINDING_BOOT_GET_PATCH_STATUS;

   pNALParHardwareInfo = &pBindingContext->sNALParHardwareInfo;
   pNALParFirmwareInfo = &pBindingContext->sNALParFirmwareInfo;

   /* update the reception counter */
   PNALBindingUpdateReceptionCounter(pBindingContext, nReceptionCounter);

   /* Check the result */
   if ( nError != W_SUCCESS )
   {
      PNALDebugError( "static_PNALBindingGetFirmwareVersionCompleted: nError %s",
         PNALUtilTraceError(nError));
      nState = P_NFC_HAL_BINDING_BOOT_LOADER_PENDING;
      goto end_function;
   }

   if ( nStatusCode == ETSI_ERR_ANY_E_CMD_NOT_SUPPORTED )
   {
      PNALDebugError("static_PNALBindingGetFirmwareVersionCompleted: ETSI_ERR_ANY_E_CMD_NOT_SUPPORTED");
      nState = P_NFC_HAL_BINDING_BOOT_LOADER_PENDING;
      goto end_function;
   }

   if (nStatusCode == ETSI_ERR_ANY_E_PIPE_NOT_OPENED )
   {
      PNALDebugError("static_PNALBindingGetFirmwareVersionCompleted: ETSI_ERR_ANY_E_PIPE_NOT_OPENED");
      nState = P_NFC_HAL_BINDING_BOOT_OPEN_PIPE_MANAGEMENT;
      goto end_function;
   }

   if (( nLength == 0 )||
       (pBuffer == null))
   {
      PNALDebugError( "static_PNALBindingGetFirmwareVersionCompleted: Bad parameters");
      pBindingContext->nNALBindingStatus = P_NFC_HAL_BINDING_NO_FIRMWARE_STATUS ;
      nState = P_NFC_HAL_BINDING_BOOT_FINISHED;
      goto end_function;
   }

   /* NFC HAL Binding interface */

   if(pBuffer[0] != P_NFC_HAL_BINDING_INTERFACE)
   {
      PNALDebugError(
         "static_PNALBindingGetFirmwareVersionCompleted: unsupported Interface Value 0x%08X",
         pBuffer[0] );
      nState = P_NFC_HAL_BINDING_BOOT_LOADER_PENDING;
      goto end_function;
   }
   /* check length coherence */
   if((pBuffer[0] == P_NFC_HAL_BINDING_INTERFACE) && (nLength != HCI2_ANS_MGT_IDENTITY_MSG_SIZE))
   {
      PNALDebugError(
         "static_PNALBindingGetFirmwareVersionCompleted: inconsistency between Interface Value 0x%08X and payload length: 0x%08X",
         pBuffer[0], nLength);
      nState = P_NFC_HAL_BINDING_BOOT_LOADER_PENDING;
      goto end_function;
   }

   /*
     Fill the NFC HAL LOADER VERSION structure
     with loader infos provided in the HCI2_PAR_MGT_IDENTITY response
    */

   /* skip interface byte */
   nIndex += 1;

   /* HARDWARE VERSION */
   CNALMemoryCopy(pNALParHardwareInfo->aHardwareVersion, &pBuffer[nIndex], P_NFC_HAL_VERSION_SIZE);
   nIndex += P_NFC_HAL_VERSION_SIZE;
   /* Add a temporary patch to correct NFCC bug */
   static_PNALBindingUint16BufferSwap(pNALParHardwareInfo->aHardwareVersion);
   pNALParHardwareInfo->aHardwareVersion[P_NFC_HAL_VERSION_SIZE - 1] = 0x00;
   /* End correction */

   /* HARDWARE serial ID */
   CNALMemoryCopy(pNALParHardwareInfo->aHardwareSerialNumber, &pBuffer[nIndex], P_NFC_HAL_HARDWARE_SERIAL_NUMBER_SIZE);
   nIndex += P_NFC_HAL_HARDWARE_SERIAL_NUMBER_SIZE;

   /* LOADER version */
   CNALMemoryCopy(pNALParHardwareInfo->aLoaderVersion, &pBuffer[nIndex], P_NFC_HAL_VERSION_SIZE);
   nIndex += P_NFC_HAL_VERSION_SIZE;

   /* Stacked Element Info */
   pNALParHardwareInfo->nSEType = static_PNALBindingReadUint16FromHCIBuffer(&pBuffer[nIndex]);
   nIndex += P_NFC_HAL_BINDING_SE_IDENTIFICATION_SIZE;


   if(pNALParHardwareInfo->nSEType == 0x0000)
   {
      /* The value 0x0000 is used to identify microread (no SE) chips */
      pNALParHardwareInfo->nSEType = P_NFC_HAL_SE_NONE;
   }

   /* Special case: force the SE absence for specific serial number */
   if(pNALParHardwareInfo->nSEType != P_NFC_HAL_SE_NONE)
   {
      if((static_PNALBintingCheckSerialNumber(pNALParHardwareInfo->aHardwareSerialNumber,
            g_aNoSE_SerialNumberRangeInf1, g_aNoSE_SerialNumberRangeSup1) != W_FALSE)
      || (static_PNALBintingCheckSerialNumber(pNALParHardwareInfo->aHardwareSerialNumber,
            g_aNoSE_SerialNumberRangeInf2, g_aNoSE_SerialNumberRangeSup2) != W_FALSE))
      {
         PNALDebugWarning( "static_PNALBindingGetFirmwareVersionCompleted: Forcing absence of SE because of SN");
         pNALParHardwareInfo->nSEType = P_NFC_HAL_SE_NONE;
      }
   }

   /* Special case: force the SE presence for specific serial number */
   if(pNALParHardwareInfo->nSEType == P_NFC_HAL_SE_NONE)
   {
      if((static_PNALBintingCheckSerialNumber(pNALParHardwareInfo->aHardwareSerialNumber,
            g_aJupiterSE_SerialNumberRangeInf1, g_aJupiterSE_SerialNumberRangeSup1) != W_FALSE)
      || (static_PNALBintingCheckSerialNumber(pNALParHardwareInfo->aHardwareSerialNumber,
            g_aJupiterSE_SerialNumberRangeInf2, g_aJupiterSE_SerialNumberRangeSup2) != W_FALSE))
      {
         PNALDebugWarning( "static_PNALBindingGetFirmwareVersionCompleted: Forcing presence of Jupiter SE because of SN");
         pNALParHardwareInfo->nSEType = P_NFC_HAL_SE_JUPITER;
      }
   }

   pNALParHardwareInfo->nFirmwareStatus = 0x01;

   /*
    Fill the Firmware Version structure
   */
   CNALMemoryCopy(pNALParFirmwareInfo->aFirmwareVersion, &pBuffer[nIndex], P_NFC_HAL_VERSION_SIZE);
   nIndex += P_NFC_HAL_VERSION_SIZE;

   /* skip HCI Protocol Version */
   nIndex += 3;

   /* Get P2P protocol capabilities */
   nHCIP2PProtocolCapabilities = static_PNALBindingReadUint16FromHCIBuffer(&pBuffer[nIndex + 4]);

   /* Get Card  Protocol capabilities */
   pBindingContext->nHCIAvailableCardProtocol = static_PNALBindingReadUint16FromHCIBuffer(&pBuffer[nIndex]) ;
   pBindingContext->nHCIAvailableCardProtocol &= HCI2_PAR_MGT_AVAILABLE_MODE_CARD_MASK;
   if((nHCIP2PProtocolCapabilities & HCI2_PROTOCOL_P2P_TARGET) != 0)
   {
      pBindingContext->nHCIAvailableCardProtocol |= HCI2_PROTOCOL_CARD_P2P_TARGET;
   }
   static_PNALBindingGetNALCardProtocolCapabilities(
                                    &nNALCardProtocolCapabilities,
                                    pBindingContext->nHCIAvailableCardProtocol);

   pBindingContext->nHCICardProtocolToOpen = pBindingContext->nHCIAvailableCardProtocol;

   static_PNALBindingWriteUint16ToNALBuffer(
                                    nNALCardProtocolCapabilities,
                                    pNALParFirmwareInfo->aCardProtocolCapabilities);
    nIndex += P_NFC_HAL_BINDING_PROTOCOL_CAPABILITIES_SIZE;

   /* Get Reader Protocol capabilities */
   pBindingContext->nHCIAvailableReaderProtocol = static_PNALBindingReadUint16FromHCIBuffer(&pBuffer[nIndex]);
   pBindingContext->nHCIAvailableReaderProtocol &= HCI2_PAR_MGT_AVAILABLE_MODE_READER_MASK;
   if((nHCIP2PProtocolCapabilities & HCI2_PROTOCOL_P2P_INITIATOR) != 0)
   {
      pBindingContext->nHCIAvailableReaderProtocol |= HCI2_PROTOCOL_READER_P2P_INITIATOR;
   }

   static_PNALBindingGetNALReaderProtocolCapabilities(
                                       &nNALReaderProtocolCapabilities,
                                       pBindingContext->nHCIAvailableReaderProtocol);


   /* Mifare activited by a Compilation FLAG*/
#  ifdef P_INCLUDE_MIFARE_CLASSIC
   nNALReaderProtocolCapabilities |= NAL_PROTOCOL_READER_MIFARE_CLASSIC;
#  endif


   pBindingContext->nHCIReaderProtocolToOpen = pBindingContext->nHCIAvailableReaderProtocol;

   static_PNALBindingWriteUint16ToNALBuffer(
                                    nNALReaderProtocolCapabilities,
                                    pNALParFirmwareInfo->aReaderProtocolCapabilities);

   nIndex += P_NFC_HAL_BINDING_PROTOCOL_CAPABILITIES_SIZE;

   /* skip P2P protocol capabilities (already parsed before) */
   nIndex += P_NFC_HAL_BINDING_PROTOCOL_CAPABILITIES_SIZE;

   /* copy the firmware tracking ID */
   CNALMemoryCopy(pNALParFirmwareInfo->aFirmwareTrackingId, &pBuffer[nIndex], P_NFC_HAL_BINDING_FIRMARE_TRACKING_ID_SIZE);
   nIndex += P_NFC_HAL_BINDING_FIRMARE_TRACKING_ID_SIZE;

   /* get the battery low supported byte */
   if(pBuffer[nIndex] == P_NFC_HAL_BINDING_HCI_BATTERY_LOW_SUPPORTED)
   {
      nFirmwareCapabilities |= NAL_CAPA_BATTERY_LOW ;
      nFirmwareCapabilities |= NAL_CAPA_BATTERY_OFF;
   }

   /* Always supported by the NFC Controller */
   nFirmwareCapabilities |= NAL_CAPA_READER_ISO_14443_B_PICO;
   nFirmwareCapabilities |= NAL_CAPA_READER_ISO_14443_A_BIT;

   /* Standby support is supported from FW OPEN_NFC_INTERFACE 0x23 */
   nFirmwareCapabilities |= NAL_CAPA_STANDBY_MODE;

   static_PNALBindingWriteUint16ToNALBuffer(nFirmwareCapabilities, pNALParFirmwareInfo->aFirmwareCapabilities);

   /* Set Data Rate Max Value */
   /* @todo should be set from HCI2_PAR_MCARD_ISO_A_CLF_DATARATEMAX and HCI2_PAR_MCARD_ISO_B_CLF_DATARATEMAX */

   pNALParFirmwareInfo->nReaderISOADataRateMax    = P_NFC_HAL_BINDING_DATA_RATE_MAX_DEFAULT;
   pNALParFirmwareInfo->nReaderISOBDataRateMax    = P_NFC_HAL_BINDING_DATA_RATE_MAX_DEFAULT;
   pNALParFirmwareInfo->nCardISOADataRateMax      = P_NFC_HAL_BINDING_DATA_RATE_MAX_DEFAULT;
   pNALParFirmwareInfo->nCardISOBDataRateMax      = P_NFC_HAL_BINDING_DATA_RATE_MAX_DEFAULT;

   pNALParFirmwareInfo->nReaderISOAInputBufferSize = P_NFC_HAL_BINDING_INPUT_BUFFER_SIZE_DEFAULT;
   pNALParFirmwareInfo->nReaderISOBInputBufferSize = P_NFC_HAL_BINDING_INPUT_BUFFER_SIZE_DEFAULT;

end_function:
   PNALBindingBootMachine(pBindingContext, nState);
}
