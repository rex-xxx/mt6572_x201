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
   Contains the jupiter specific implementation.
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( JUPITER )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/* Buffer maximum size */
#define P_JUPITER_BUFFER_MAX_SIZE    (256 + 5)

/* Jupiter management operation */
#define P_JUPITER_OPERATION_GET_TRANSACTION_AID      0x00
#define P_JUPITER_OPERATION_APPLY_POLICY             0x01
#define P_JUPITER_OPERATION_GET_POLICY               0x02

/* Declare an Jupiter SE structure paramters for asynchronous mode */
typedef struct __tPJupiterInstance
{
   /* Operation requested */
   uint8_t nOperationRequested;

   uint8_t* pAIDListBuffer;
   uint32_t nAIDListBufferMaxLength;
   uint32_t* pnAIDListLength;

   /* SE persistent policy */
   uint32_t nPersistentPolicy;
   /* SE volatile policy */
   uint32_t nVolatilePolicy;
   /* Flag for the applet limitation: no support for 0xFFFF policy value */
   bool_t bNoSupportFor0xFFFF;

   tPBasicExchangeData* pExchangeApduFunction;
   W_HANDLE hConnection;

   uint8_t aResponseBuffer[P_JUPITER_BUFFER_MAX_SIZE];

   tDFCCallbackContext sCallbackContext;
   tDFCCallbackContext sOperationCallbackContext;

   /* Getting APDU Policy : store pointer on the Persistent and volatile policies*/
   uint32_t  *pnPersistentPolicyGet;
   uint32_t  *pnVolatilePolicyGet;

} tPJupiterInstance;

#ifndef P_INCLUDE_GET_TRANSACTION_AID
/* The Mifare Classic pseudo AID */
static const uint8_t g_aMifareAid[] =
   { 0xD2, 0x76, 0x00, 0x00, 0x05, 0xAC, 0x00, 0x04, 0x03, 0xE0, 0x10, 0x01, 0x01 };

/* The iClass pseudo AID */
static const uint8_t g_aIClassAid[] =
   { 0xD2, 0x76, 0x00, 0x00, 0x05, 0xAC, 0x00, 0x04, 0x03, 0xE0, 0x10, 0x01, 0x02 };
#endif /* not P_INCLUDE_GET_TRANSACTION_AID */

/* Jupiter AID */
static const uint8_t g_aJupiterAid[] =
   { 0xF0, 0x69, 0x6E, 0x63, 0x6C, 0x53, 0x45, 0x53, 0x65, 0x74, 0x74, 0x69, 0x6E, 0x67, 0x73 };

/* Disable all protocols Configuration Constant */
#define P_JUPITER_DISABLE_ALL_PROTOCOLS   0x00000000
#define P_JUPITER_DO_NOT_CHANGE_PROTOCOLS 0xFFFFFFFF

static const uint32_t g_nINDEX_HANDSET_OFF = 5;
static const uint32_t g_nINDEX_HANDSET_ON = 7;

/* Build the APDU value of the policy */
static uint8_t* static_PJupiterBuildPolicy(
            tPJupiterInstance* pJupiterInstance,
            uint32_t nProtocolPolicy,
            uint8_t* pValue)
{
   if (nProtocolPolicy == P_JUPITER_DO_NOT_CHANGE_PROTOCOLS)
   {
      pValue[0] = 0;
      pValue[1] = 0;
   }
   else if (nProtocolPolicy == P_JUPITER_DISABLE_ALL_PROTOCOLS)
   {
      pValue[0] = 0xFF; pValue[1] = 0xFF;

      if(pJupiterInstance->bNoSupportFor0xFFFF != W_FALSE)
      {
         /* Use ISO 14443 B instead of none */
         pValue[0] = 0x00; pValue[1] = 0x20;
      }
   }
   else
   {
      pValue[0] = 0;
      pValue[1] = 0;

      if ((nProtocolPolicy & W_NFCC_PROTOCOL_CARD_ISO_14443_4_A) != 0)
      {
         pValue[0] |= 0x00; pValue[1] |= 0x10;
      }
      if ((nProtocolPolicy & W_NFCC_PROTOCOL_CARD_ISO_14443_4_B) != 0)
      {
         pValue[0] |= 0x00; pValue[1] |= 0x20;
      }
      if ((nProtocolPolicy & W_NFCC_PROTOCOL_CARD_MIFARE_CLASSIC) != 0)
      {
         pValue[0] |= 0x00; pValue[1] |= 0x80;
      }
      if ((nProtocolPolicy & W_NFCC_PROTOCOL_CARD_ISO_15693_2) != 0)
      {
         pValue[0] |= 0x01; pValue[1] |= 0x00;
      }
   }

   return pValue;
}

/* Build the APDU value of the policy */
static uint32_t static_PJupiterExtractPolicy(uint8_t* pValue)
{
   uint32_t nProtocolPolicy = 0x00;

   if(pValue[0] == 0xFF && pValue[1] == 0xFF)
   {
      nProtocolPolicy = P_JUPITER_DISABLE_ALL_PROTOCOLS;
   }
   else
   {
      if(pValue[1] & 0x10)
      {
         nProtocolPolicy |= W_NFCC_PROTOCOL_CARD_ISO_14443_4_A;
      }
      if(pValue[1] & 0x20)
      {
         nProtocolPolicy |= W_NFCC_PROTOCOL_CARD_ISO_14443_4_B;
      }
      if(pValue[1] & 0x80)
      {
         nProtocolPolicy |= W_NFCC_PROTOCOL_CARD_MIFARE_CLASSIC;
      }
      if(pValue[0] & 0x01)
      {
         nProtocolPolicy |= W_NFCC_PROTOCOL_CARD_ISO_15693_2;
      }
   }

   return nProtocolPolicy;
}

static void static_PJupiterExchangeAPDUCompleted(
               tContext* pContext,
               void * pCallbackParameter,
               uint32_t nDataLength,
               W_ERROR nError);

static void static_PJupiterSendSetPolicy(
            tContext* pContext,
            tPJupiterInstance* pJupiterInstance )
{
   uint8_t aValue[2];

   /* Set enabled SE protocols APDU */
   static uint8_t aSetPolicyAPDU[] = {0x90, 0xDA, 0x01, 0x02, 0x04, 0x00, 0x00, 0x00, 0x00};

   /* The persistent policy of WSESetPolicy() shall be mapped on the "handset OFF" policy of the SE. */
   CMemoryCopy(&aSetPolicyAPDU[g_nINDEX_HANDSET_OFF], static_PJupiterBuildPolicy(pJupiterInstance, pJupiterInstance->nPersistentPolicy , aValue), 2);

   /* The volatile policy of WSESetPolicy() shall be mapped on the "handset ON" policy of the SE. */
   CMemoryCopy(&aSetPolicyAPDU[g_nINDEX_HANDSET_ON], static_PJupiterBuildPolicy(pJupiterInstance, pJupiterInstance->nVolatilePolicy , aValue), 2);

   PDebugTrace("Send SET POLICY APDU");
   pJupiterInstance->pExchangeApduFunction(
      pContext,
      pJupiterInstance->hConnection,
      static_PJupiterExchangeAPDUCompleted, pJupiterInstance,
      aSetPolicyAPDU, sizeof(aSetPolicyAPDU),
      pJupiterInstance->aResponseBuffer, 2,
      (W_HANDLE*)null );
}

/**
 * Looks for an AID value in a buffer.
 *
 * @param[in]  pBuffer  The buffer.
 *
 * @param[in]  nBufferLength  The buffer length in bytes.
 *
 * @param[in]  pAidValue  The AID value.
 *
 * @param[in]  nAidLength  The lenght in bytes of the AID value.
 *
 * @return W_TRUE if the AID value is found, W_FALSE otherwise.
 **/
static bool_t static_PJupiterFindAid(
               const uint8_t* pBuffer,
               uint32_t nBufferLength,
               const uint8_t* pAidValue,
               uint32_t nAidLength)
{
   uint8_t nCurrentLength = 0;
   uint32_t nSourcePos;

   for(nSourcePos = 0; nSourcePos < nBufferLength; nSourcePos += nCurrentLength)
   {
      nCurrentLength = pBuffer[nSourcePos++];

      if ((nCurrentLength == nAidLength)
      && (CMemoryCompare(&pBuffer[nSourcePos], pAidValue, nCurrentLength) == 0))
      {
         return W_TRUE;
      }
   }

   return W_FALSE;
}

/**
 * Parses the AID buffer sent by the Jupiter applet.
 *
 * The AID of the Jupiter applet is removed from the list. The format of the list is:
 *    |Length 1 | AID 1| ... |Length N | AID N |
 *
 * @param[in]  pSourceBuffer  A pointer on the AID list returned by the applet.
 *
 * @param[in]  nSourceBufferLength  The length in bytes of the AID buffer list.
 *
 * @param[in]  pDestBuffer  A pointer on the buffer receiving the filtered list.
 *
 * @param[in]  nCurrentListLength  The current list length.
 *
 * @param[in]  nDestBufferMaxLength  the length in bytes of the destination buffer.
 *
 * @return  The actual size in bytes of the filtered list.
 **/
static uint32_t static_PJupiterFilterAid(
               const uint8_t* pSourceBuffer,
               uint32_t nSourceBufferLength,
               uint8_t* pDestBuffer,
               uint32_t nCurrentListLength,
               uint32_t nDestBufferMaxLength)
{
   uint8_t nAidLength = 0;
   uint32_t nSourcePos;

   for(nSourcePos = 0; nSourcePos < nSourceBufferLength; nSourcePos += nAidLength)
   {
      nAidLength = pSourceBuffer[nSourcePos++];
      if (((nAidLength > (nSourceBufferLength - nSourcePos)) || (nAidLength == 0)))
      {
         PDebugError("static_PJupiterFilterAid: SE malformed response");
         return nCurrentListLength;
      }

      /* Exclude Jupiter Aid (internal use only) */
      if ((nAidLength == sizeof(g_aJupiterAid))
      && (CMemoryCompare(&pSourceBuffer[nSourcePos], g_aJupiterAid, nAidLength) == 0))
      {
         break;
      }

#ifdef P_INCLUDE_SE_SECURITY
      /* Exclude PKCS15 Aid (for the security stack only) */
      if ((nAidLength == g_nSecurityStackPkcs15AidSize)
      && (CMemoryCompare(&pSourceBuffer[nSourcePos], g_aSecurityStackPkcs15Aid, nAidLength) == 0))
      {
         break;
      }
#endif /* #ifdef P_INCLUDE_SE_SECURITY */

      /* Exclude double AID values */
      if(static_PJupiterFindAid(
         pDestBuffer, nCurrentListLength,
         &pSourceBuffer[nSourcePos], nAidLength) != W_FALSE)
      {
         break;
      }

      if((nCurrentListLength + 1 + nAidLength) > nDestBufferMaxLength)
      {
         PDebugError("static_PJupiterFilterAid: Destination buffer too short");
         break;
      }

      /* Append the AID */
      pDestBuffer[nCurrentListLength++] = nAidLength;
      CMemoryCopy(&pDestBuffer[nCurrentListLength], &pSourceBuffer[nSourcePos] , nAidLength);
      nCurrentListLength += nAidLength;
   }

   return nCurrentListLength;
}

/**
 * Callback of P7816ExchangeAPDU
 *
 * @param[in]  pContext context.
 *
 * @param[in]  pCallbackParameter  The blind parameter given to the function initiating the operation.
 *
 * @param[in]  nDataLength  The length in bytes of the data received.
 *
 * @param[in]  nError  The result code of the operation:
 **/
static void static_PJupiterExchangeAPDUCompleted(
               tContext* pContext,
               void * pCallbackParameter,
               uint32_t nDataLength,
               W_ERROR nError)
{
   tPJupiterInstance* pJupiterInstance = (tPJupiterInstance*)pCallbackParameter;
   uint8_t* pCardToReaderBuffer;

   PDebugTrace("static_PJupiterExchangeAPDUCompleted()");

   /* Check the error code return */
   if ( nError != W_SUCCESS )
   {
      goto return_function;
   }

   /* Check the parameters */
   if ( nDataLength < 2 )
   {
      nError = W_ERROR_RF_COMMUNICATION;
      goto return_function;
   }

   pCardToReaderBuffer = pJupiterInstance->aResponseBuffer;

   /* Check the SW1 & SW2 values */
   if ( pCardToReaderBuffer[nDataLength - 2] != 0x90)
   {
      /* Filter the error status '6A80' on the disable all protocols cmd. */
      if((pJupiterInstance->nOperationRequested == P_JUPITER_OPERATION_APPLY_POLICY)
      && (pJupiterInstance->bNoSupportFor0xFFFF == W_FALSE)
      && ((pJupiterInstance->nVolatilePolicy == P_JUPITER_DISABLE_ALL_PROTOCOLS)
         || (pJupiterInstance->nPersistentPolicy == P_JUPITER_DISABLE_ALL_PROTOCOLS))
      && (pCardToReaderBuffer[nDataLength - 2] == 0x6A)
      && (pCardToReaderBuffer[nDataLength - 1] == 0x80) )
      {
         PDebugWarning("static_PJupiterExchangeAPDUCompleted: 6A80 is not an error, 0xFFFF is not supported");
         /* Set the limitation of the applet */
         pJupiterInstance->bNoSupportFor0xFFFF = W_TRUE;
         /* Resend the command without 0xFFFF */
         static_PJupiterSendSetPolicy( pContext, pJupiterInstance );
         return;
      }
      else
      {
         PDebugError("static_PJupiterExchangeAPDUCompleted: received %d bytes SW1=0x%02X SW2=0x%02X",
                  nDataLength,
                  pCardToReaderBuffer[nDataLength - 2],
                  pCardToReaderBuffer[nDataLength - 1]);

         if((pJupiterInstance->nOperationRequested == P_JUPITER_OPERATION_APPLY_POLICY)
         && (pCardToReaderBuffer[nDataLength - 2] == 0x6A)
         && (pCardToReaderBuffer[nDataLength - 1] == 0x80) )
         {
            nError = W_ERROR_FEATURE_NOT_SUPPORTED;
         }
         else
         {
            nError = W_ERROR_RF_COMMUNICATION;
         }
         goto return_function;
      }
   }

   /* Copy the response */
   if (pJupiterInstance->nOperationRequested == P_JUPITER_OPERATION_GET_TRANSACTION_AID)
   {
      /* First retrieve the AIDs Lenght */
      *pJupiterInstance->pnAIDListLength = static_PJupiterFilterAid(
         pCardToReaderBuffer, (nDataLength-2),
         pJupiterInstance->pAIDListBuffer,
         *pJupiterInstance->pnAIDListLength,
         pJupiterInstance->nAIDListBufferMaxLength);
   }
   else if(pJupiterInstance->nOperationRequested == P_JUPITER_OPERATION_GET_POLICY)
   {
      if(nDataLength != 6)
      {
         PDebugError("static_PJupiterExchangeAPDUCompleted nDataLength = %d / expected = %d", nDataLength, 6);
         nError = W_ERROR_RF_COMMUNICATION;
         goto return_function;
      }

      /* Persistent policy aResponseBuffer[0]|aResponseBuffer[1] */
      *pJupiterInstance->pnPersistentPolicyGet = static_PJupiterExtractPolicy(pCardToReaderBuffer);

      /* Volatile policy aResponseBuffer[2]|aResponseBuffer[3] */
      *pJupiterInstance->pnVolatilePolicyGet = static_PJupiterExtractPolicy(&pCardToReaderBuffer[2]);
   }


return_function:

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PJupiterExchangeAPDUCompleted: Sending error %s", PUtilTraceError(nError));
   }

   PDFCPostContext2( &pJupiterInstance->sOperationCallbackContext, nError );
}

static void static_PJupiterOperation(
            tContext* pContext,
            void* pInstance,
            tPBasicExchangeData* pExchangeApduFunction,
            W_HANDLE hConnection,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderDataLength,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter )
{
   tPJupiterInstance* pJupiterInstance = (tPJupiterInstance*)pInstance;
   W_ERROR nError;

   PDebugTrace("static_PJupiterOperation()");

   PDFCFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter, &pJupiterInstance->sOperationCallbackContext );

   /* Check the parameters */
   if ( nCardToReaderDataLength < 2 )
   {
      PDebugError("static_PJupiterOperation: R-APDU is too short");
      nError = W_ERROR_RF_COMMUNICATION;
      goto send_error;
   }

   /* Check the SW1 & SW2 values */
   if ( pCardToReaderBuffer[nCardToReaderDataLength - 2] != 0x90 )
   {
      PDebugError("static_PJupiterOperation: received %d bytes SW1=0x%02X SW2=0x%02X",
                     nCardToReaderDataLength,
                     pCardToReaderBuffer[nCardToReaderDataLength - 2],
                     pCardToReaderBuffer[nCardToReaderDataLength - 1]);

      /* Is the Jupiter application present in the Secure element? */
      if ( (nCardToReaderDataLength == 2) &&
           (pCardToReaderBuffer[0] == 0x6A) &&
           (pCardToReaderBuffer[1] == 0x82) ) /* File not found */
      {
         nError = W_ERROR_ITEM_NOT_FOUND;
      }
      else
      {
         nError = W_ERROR_RF_COMMUNICATION;
      }
      goto send_error;
   }

   switch(pJupiterInstance->nOperationRequested)
   {
      case P_JUPITER_OPERATION_GET_TRANSACTION_AID:
      {
         static const uint8_t g_aGetAidAPDU[] = {0x90, 0xCA, 0x01, 0x01};

         PDebugTrace("Send GET AID APDU");

         pExchangeApduFunction(
            pContext,
            hConnection,
            static_PJupiterExchangeAPDUCompleted, pJupiterInstance,
            g_aGetAidAPDU, sizeof(g_aGetAidAPDU),
            pJupiterInstance->aResponseBuffer, P_JUPITER_BUFFER_MAX_SIZE,
            (W_HANDLE*)null );
         break;
      }
      case P_JUPITER_OPERATION_APPLY_POLICY:
      {
         pJupiterInstance->pExchangeApduFunction = pExchangeApduFunction;
         pJupiterInstance->hConnection = hConnection;
         static_PJupiterSendSetPolicy(pContext, pJupiterInstance );
         break;
      }
      case P_JUPITER_OPERATION_GET_POLICY:
      {
         static const uint8_t g_aGetPolicyAPDU[] = {0x90, 0xCA, 0x01, 0x02, 0x04};

         PDebugTrace("Send GET Policy");

         pExchangeApduFunction(
            pContext,
            hConnection,
            static_PJupiterExchangeAPDUCompleted, pJupiterInstance,
            g_aGetPolicyAPDU, sizeof(g_aGetPolicyAPDU),
            pJupiterInstance->aResponseBuffer, P_JUPITER_BUFFER_MAX_SIZE,
            (W_HANDLE*)null );
         break;
      }
      default:
         nError = W_ERROR_BAD_PARAMETER;
         goto send_error;

   }

   return;

send_error:

   PDebugError("static_PJupiterOperation: Sending error %s", PUtilTraceError(nError));

   PDFCPostContext2( &pJupiterInstance->sOperationCallbackContext, nError );
}

/* See header file */
W_ERROR PJupiterProcessAIDList(
         tContext* pContext,
         uint32_t nSlotIdentifier,
         uint32_t nCardProtocols,
         uint8_t* pAIDListBuffer,
         uint32_t* pnAIDListLength)
{
   PDebugTrace("PJupiterProcessAIDList()");

   if(*pnAIDListLength != 0)
   {
      if(*pnAIDListLength == 3)
      {
         if(pAIDListBuffer[0] != 2)
         {
            PDebugError("PJupiterProcessAIDList: wrong AID list format, erased");
            *pnAIDListLength = 0;
            return W_SUCCESS;
         }
         else if((pAIDListBuffer[1] == 0xFF) && (pAIDListBuffer[2] == 0xFF)
            && ((nCardProtocols & W_NFCC_PROTOCOL_CARD_ISO_14443_4_A) != 0))
         {
            /* Buffer is "02 FF FF", for Mifare or
               ISO 14443 A with no SELECT AID APDU but the transaction history of the SE is empty */
            PDebugTrace("PJupiterProcessAIDList: Mifare (or TypeA) transaction");
            *pnAIDListLength = 0;
#ifdef P_INCLUDE_GET_TRANSACTION_AID
            return W_ERROR_OPERATION_PENDING;
#else
            if ((nCardProtocols & W_NFCC_PROTOCOL_CARD_MIFARE_CLASSIC) != 0)
            {
               pAIDListBuffer[0] = sizeof(g_aMifareAid);
               CMemoryCopy(&pAIDListBuffer[1], g_aMifareAid, sizeof(g_aMifareAid));
               *pnAIDListLength = 1 + sizeof(g_aMifareAid);
            }
            return W_SUCCESS;
#endif /* P_INCLUDE_GET_TRANSACTION_AID */
         }
         else if((pAIDListBuffer[1] == 0xFF) && (pAIDListBuffer[2] == 0xFF)
            && (nCardProtocols == W_NFCC_PROTOCOL_CARD_ISO_14443_4_B))
         {
            /* ISO 14443 B with no SELECT AID APDU but the transaction history of the SE is empty */
            PDebugTrace("PJupiterProcessAIDList: TypeB transaction but MR has not spied any AID");
            *pnAIDListLength = 0;
#ifdef P_INCLUDE_GET_TRANSACTION_AID
            return W_ERROR_OPERATION_PENDING;
#else
            return W_SUCCESS;
#endif /* P_INCLUDE_GET_TRANSACTION_AID */
         }
         else if((pAIDListBuffer[1] == 0xFF) && (pAIDListBuffer[2] == 0xFF)
            && (nCardProtocols == W_NFCC_PROTOCOL_CARD_ISO_15693_2))
         {
            /* Buffer is "02 FF FF", for iClass */
            PDebugTrace("PJupiterProcessAIDList: iClass transaction");
#ifdef P_INCLUDE_GET_TRANSACTION_AID
            *pnAIDListLength = 0;
            return W_ERROR_OPERATION_PENDING;
#else
            pAIDListBuffer[0] = sizeof(g_aIClassAid);
            CMemoryCopy(&pAIDListBuffer[1], g_aIClassAid, sizeof(g_aIClassAid));
            *pnAIDListLength = 1 + sizeof(g_aIClassAid);
            return W_SUCCESS;
#endif /* P_INCLUDE_GET_TRANSACTION_AID */
         }
         else
         {
            /* AID spied by MR but not recorded by SE. Keep the NFCC buffer */
            PDebugTrace("PJupiterProcessAIDList: AID spied by MR but not recorded by SE");
            return W_SUCCESS;
         }
      }
      else if((*pnAIDListLength == 1) && (pAIDListBuffer[0] == 0))
      {
         /* The default applet has been selected. Erased */
         PDebugTrace("PJupiterProcessAIDList: default applet has been selected");
         *pnAIDListLength = 0;
#ifdef P_INCLUDE_GET_TRANSACTION_AID
         return W_ERROR_OPERATION_PENDING;
#else
         return W_SUCCESS;
#endif /* P_INCLUDE_GET_TRANSACTION_AID */
      }
      else if(*pnAIDListLength > 5)
      {
         /* The NFCC sent an AID list and SE indicates a transaction history, keep it */
         PDebugTrace("PJupiterProcessAIDList: correct AID List");
#ifdef P_INCLUDE_GET_TRANSACTION_AID
         return W_ERROR_OPERATION_PENDING;
#else
         return W_SUCCESS;
#endif /* P_INCLUDE_GET_TRANSACTION_AID */
      }
      else
      {
         /* AID list is not 3, nor greater than 5 (AID is at least 5 bytes) */
         PDebugError("PJupiterProcessAIDList: wrong AID list length, erased");
         *pnAIDListLength = 0;
         return W_SUCCESS;
      }
   }

   PDebugTrace("PJupiterProcessAIDList: empty list");
   return W_SUCCESS;
}


static void static_PJupiterOperationCompleted(tContext* pContext, void * pCallbackParameter, W_ERROR nResult)
{
   tPJupiterInstance* pJupiterInstance = (tPJupiterInstance*)pCallbackParameter;

   PDebugTrace("static_PJupiterOperationCompleted");

   if (nResult != W_SUCCESS)
   {
      PDebugError("static_PJupiterOperationCompleted: Sending error %s", PUtilTraceError(nResult));
   }

   PDFCPostContext2(&pJupiterInstance->sCallbackContext, nResult);

   CMemoryFree(pJupiterInstance);
}

/* See header file */
void PJupiterGetTransactionAID(
         tContext* pContext,
         uint32_t nSlotIdentifier,
         uint8_t* pAIDListBuffer,
         uint32_t nAIDListBufferMaxLength,
         uint32_t* pnAIDListLength,
         tPBasicGenericCallbackFunction* pCallback,
         void* pCallbackParameter)
{
   tPJupiterInstance* pJupiterInstance;

   PDebugTrace("PJupiterGetTransactionAID()");

   pJupiterInstance = (tPJupiterInstance*)CMemoryAlloc(sizeof(tPJupiterInstance));
   if(pJupiterInstance == null)
   {
      PDebugError("PJupiterGetTransactionAID: cannot allocate the instance");
      return;
   }
   CMemoryFill(pJupiterInstance, 0, sizeof(tPJupiterInstance));

   pJupiterInstance->nOperationRequested = P_JUPITER_OPERATION_GET_TRANSACTION_AID;

   pJupiterInstance->pAIDListBuffer = pAIDListBuffer;
   pJupiterInstance->nAIDListBufferMaxLength = nAIDListBufferMaxLength;
   pJupiterInstance->pnAIDListLength = pnAIDListLength;

   PDFCFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter, &pJupiterInstance->sCallbackContext );

   PSEDriverOperation(
            pContext,
            nSlotIdentifier, W_FALSE,
            g_aJupiterAid, sizeof(g_aJupiterAid),
            static_PJupiterOperation, pJupiterInstance,
            static_PJupiterOperationCompleted, pJupiterInstance );
}

/* See header file */
void PJupiterSetPolicy(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            uint32_t nStorageType,
            uint32_t nProtocols,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter)
{
   W_ERROR nError;
   tDFCCallbackContext sErrorCallback;
   tPJupiterInstance* pJupiterInstance;

   switch (nProtocols)
   {
      /* only the following combination are allowed */
      case 0:
      case W_NFCC_PROTOCOL_CARD_ISO_14443_4_A:
      case W_NFCC_PROTOCOL_CARD_ISO_14443_4_A | W_NFCC_PROTOCOL_CARD_MIFARE_CLASSIC:
      case W_NFCC_PROTOCOL_CARD_ISO_14443_4_A | W_NFCC_PROTOCOL_CARD_ISO_15693_2:
      case W_NFCC_PROTOCOL_CARD_ISO_14443_4_A | W_NFCC_PROTOCOL_CARD_MIFARE_CLASSIC | W_NFCC_PROTOCOL_CARD_ISO_15693_2:
      case W_NFCC_PROTOCOL_CARD_ISO_14443_4_B:
      case W_NFCC_PROTOCOL_CARD_ISO_14443_4_B | W_NFCC_PROTOCOL_CARD_ISO_15693_2:
      case W_NFCC_PROTOCOL_CARD_ISO_15693_2:
         break;

      default:
            /* invalid policy */
         PDebugError("PJupiterSetPolicy: unsupported protocols combination");
         nError = W_ERROR_FEATURE_NOT_SUPPORTED;
         goto return_error;
   }

   pJupiterInstance = (tPJupiterInstance*)CMemoryAlloc(sizeof(tPJupiterInstance));
   if(pJupiterInstance == null)
   {
      PDebugError("PJupiterSetPolicy: cannot allocate the instance");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }
   CMemoryFill(pJupiterInstance, 0, sizeof(tPJupiterInstance));

   pJupiterInstance->nOperationRequested = P_JUPITER_OPERATION_APPLY_POLICY;

   if ((nStorageType == W_NFCC_STORAGE_PERSISTENT) || (nStorageType == W_NFCC_STORAGE_BOTH))
   {
      pJupiterInstance->nPersistentPolicy = nProtocols;
   }
   else
   {
      pJupiterInstance->nPersistentPolicy = P_JUPITER_DO_NOT_CHANGE_PROTOCOLS;
   }

   if ((nStorageType == W_NFCC_STORAGE_VOLATILE) || (nStorageType == W_NFCC_STORAGE_BOTH))
   {
      pJupiterInstance->nVolatilePolicy = nProtocols;
   }
   else
   {
      pJupiterInstance->nVolatilePolicy = P_JUPITER_DO_NOT_CHANGE_PROTOCOLS;
   }

   PDFCFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter, &pJupiterInstance->sCallbackContext );

   PSEDriverOperation(
            pContext,
            nSlotIdentifier, W_FALSE,
            g_aJupiterAid, sizeof(g_aJupiterAid),
            static_PJupiterOperation, pJupiterInstance,
            static_PJupiterOperationCompleted, pJupiterInstance );
   return;

return_error:

   PDebugError("PJupiterSetPolicy: returning %s", PUtilTraceError(nError));

   PDFCFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter, &sErrorCallback );

   PDFCPostContext2( &sErrorCallback, nError );
}


/* See header file */
void PJupiterGetPolicy(
            tContext* pContext,
            uint32_t nSlotIdentifier,
            uint32_t * pnPersistentPolicy,
            uint32_t * pnVolatilePolicy,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter)
{
   W_ERROR nError;
   tDFCCallbackContext sErrorCallback;
   tPJupiterInstance* pJupiterInstance;

   pJupiterInstance = (tPJupiterInstance*)CMemoryAlloc(sizeof(tPJupiterInstance));
   if(pJupiterInstance == null)
   {
      PDebugError("PJupiterGetPolicy: cannot allocate the instance");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }
   CMemoryFill(pJupiterInstance, 0, sizeof(tPJupiterInstance));

   pJupiterInstance->nOperationRequested     = P_JUPITER_OPERATION_GET_POLICY;
   pJupiterInstance->pnPersistentPolicyGet   = pnPersistentPolicy;
   pJupiterInstance->pnVolatilePolicyGet     = pnVolatilePolicy;

   PDFCFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter, &pJupiterInstance->sCallbackContext );

   PSEDriverOperation(
            pContext,
            nSlotIdentifier, W_FALSE,
            g_aJupiterAid, sizeof(g_aJupiterAid),
            static_PJupiterOperation, pJupiterInstance,
            static_PJupiterOperationCompleted, pJupiterInstance );
   return;

return_error:

   PDebugError("PJupiterGetPolicy: returning %s", PUtilTraceError(nError));

   PDFCFillCallbackContext( pContext,
      (tDFCCallback*)pCallback, pCallbackParameter, &sErrorCallback );

   PDFCPostContext2( &sErrorCallback, nError );
}

#endif /* P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC */

