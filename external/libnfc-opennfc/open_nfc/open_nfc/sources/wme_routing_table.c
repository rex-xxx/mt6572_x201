/*
 * Copyright (c) 2012 Inside Secure, All Rights Reserved.
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
   Contains the implementation of the routing table functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( ROUTING_TABLE )

#include "wme_context.h"

/** Configuration flag indicating that the Routing Table is enabled */
#define P_ROUTING_TABLE_CONFIG_ENABLED 0x00000001

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/** The pending flag indicating that there is no operation currently processed */
#define P_ROUTING_TABLE_IDLE    0x0000
/** The pending flag indicating that an operation is pending */
#define P_ROUTING_TABLE_PENDING 0x0001

typedef struct __tRoutingTable
{
   /* Header for the object registry */
   tHandleObjectHeader sObjectHeader;

   /** The number of allocated entries */
   uint16_t nAllocEntryCount;
   /** The number of available entries */
   uint16_t nEntryCount;
   /* The array of routing table entries */
   tWRoutingTableEntry sEntry[1];
}  tRoutingTable;

#define P_ROUTING_TABLE_DEFAULT_ENTRIES 40

#define sizeof_tRoutingTable(nAllocEntryCount)\
   (sizeof(tRoutingTable) + (nAllocEntryCount - 1)*sizeof(tWRoutingTableEntry))

static uint32_t static_PRoutingTableDestroy(
         tContext* pContext,
         void* pObject )
{
   CMemoryFree(pObject);
   return P_HANDLE_DESTROY_DONE;
}

/** Handle type for an AID Routing Table */
static tHandleType g_sRoutingTable =
   { static_PRoutingTableDestroy, null, null, null, null, null, null, null, null };

#define P_HANDLE_TYPE_ROUTING_TABLE (&g_sRoutingTable)

/* See client API */
W_ERROR PRoutingTableCreate(
            tContext * pContext,
            W_HANDLE* phRoutingTable )
{
   W_HANDLE hRoutingTable = W_NULL_HANDLE;
   tRoutingTable* pRoutingTable;
   W_ERROR nResult;

   if (phRoutingTable == null)
   {
      PDebugError("PRoutingTableCreate: phRoutingTable == null");
      return W_ERROR_BAD_PARAMETER;
   }

   pRoutingTable = (tRoutingTable*)CMemoryAlloc(sizeof_tRoutingTable(P_ROUTING_TABLE_DEFAULT_ENTRIES));
   if(pRoutingTable == null)
   {
      PDebugError("PRoutingTableCreate: Failed to allocate AID Routing Table");
      return W_ERROR_OUT_OF_RESOURCE;
   }
   CMemoryFill(pRoutingTable, 0, sizeof_tRoutingTable(P_ROUTING_TABLE_DEFAULT_ENTRIES));

   nResult = PHandleRegister(pContext, pRoutingTable, P_HANDLE_TYPE_ROUTING_TABLE, &hRoutingTable);
   if(nResult == W_SUCCESS)
   {
      pRoutingTable->nAllocEntryCount = P_ROUTING_TABLE_DEFAULT_ENTRIES;
      pRoutingTable->nEntryCount = 0;
   }
   else
   {
      PDebugError("PRoutingTableCreate: PHandleRegister returned error %s", PUtilTraceError(nResult));
      CMemoryFree(pRoutingTable);
      hRoutingTable = W_NULL_HANDLE;
   }

   *phRoutingTable = hRoutingTable;

   return nResult;
}

#define P_ROUTING_TABLE_SEID_NOT_FOUND ((uint32_t)(-1))

#define P_ROUTING_TABLE_TARGETID_NOT_FOUND ((uint8_t)(-1))

#define P_ROUTING_TABLE_TARGETID_HANDSET 0
#define P_ROUTING_TABLE_TARGETID_UICC 1
#define P_ROUTING_TABLE_TARGETID_SE_0 2

static uint32_t static_PRoutingTableConvertTargetIdToSeId(
         tContext* pContext,
         uint8_t nTargetId,
         uint32_t nSeNumber)
{
   uint32_t nCapability;
   uint32_t nSeId;

   switch(nTargetId)
   {
   case P_ROUTING_TABLE_TARGETID_HANDSET:
      {
         return nSeNumber;
      }

   case P_ROUTING_TABLE_TARGETID_UICC:
      {
         nCapability = W_SE_FLAG_UICC;
         break;
      }

   case P_ROUTING_TABLE_TARGETID_SE_0:
      {
         nCapability = 0;
         break;
      }

   default:
      {
         return P_ROUTING_TABLE_SEID_NOT_FOUND;
      }
   }

   for(nSeId = 0; nSeId < nSeNumber; nSeId++)
   {
      tWSEInfoEx info;

      if (PSEGetInfoEx(pContext, nSeId, &info) == W_SUCCESS)
      {
         if ((info.nCapabilities & W_SE_FLAG_UICC) == nCapability)
         {
            return nSeId;
         }
      }
   }

   return P_ROUTING_TABLE_SEID_NOT_FOUND;
}

static uint8_t static_PRoutingTableConvertSeIdToTargetId(
         tContext* pContext,
         uint32_t nSeId,
         uint32_t nSeNumber)
{
   if (nSeId == nSeNumber)
   {
      return P_ROUTING_TABLE_TARGETID_HANDSET;
   }
   else if (nSeId < nSeNumber)
   {
      tWSEInfoEx info;

      if (PSEGetInfoEx(pContext, nSeId, &info) == W_SUCCESS)
      {
         if ((info.nCapabilities & W_SE_FLAG_UICC) == W_SE_FLAG_UICC)
         {
            return P_ROUTING_TABLE_TARGETID_UICC;
         }
         else
         {
            return P_ROUTING_TABLE_TARGETID_SE_0;
         }
      }
   }

   return P_ROUTING_TABLE_TARGETID_NOT_FOUND;
}

static bool_t static_PRoutingTableIsSupported(
         tContext* pContext )
{
   bool_t bIsSupported = W_FALSE;

   PNFCControllerGetBooleanProperty(pContext, W_NFCC_PROP_ROUTING_TABLE_SUPPORTED, &bIsSupported);

   return bIsSupported;
}

static void static_PRoutingTableReadCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         uint32_t nLength,
         W_ERROR nResult)
{
   W_HANDLE hRoutingTable = W_NULL_HANDLE;
   uint32_t nSeNumber = 0;
   const uint8_t* pBuffer;

   tRoutingTableInstance* pRoutingTableInstance = (tRoutingTableInstance*)pCallbackParameter;
   CDebugAssert(pRoutingTableInstance != null);

   pRoutingTableInstance->nPendingFlag = P_ROUTING_TABLE_IDLE;

   if (nResult != W_SUCCESS)
   {
      PDebugError("static_PRoutingTableReadCompleted: Received error is %s", PUtilTraceError(nResult));
      goto return_result;
   }

   if (PNFCControllerGetIntegerProperty(pContext, W_NFCC_PROP_SE_NUMBER, &nSeNumber) != W_SUCCESS)
   {
      PDebugError("static_PRoutingTableReadCompleted: Failed to get W_NFCC_PROP_SE_NUMBER");
      goto return_result;
   }

   nResult = PRoutingTableCreate(pContext, &hRoutingTable);
   if (nResult != W_SUCCESS)
   {
      goto return_result;
   }

   /* Parse the answer and fill in the AID routing table */
   pBuffer = pRoutingTableInstance->aBuffer;
   while(nLength >= (sizeof(uint8_t) + sizeof(uint8_t)))
   {
      uint8_t nFlags = pBuffer[0];
      uint8_t nAidLength = pBuffer[1];

      tWRoutingTableEntry entry;
      CMemoryFill(&entry, 0, sizeof(entry));

      /* Check entry type */
      if (nAidLength == 1)
      {
         if (nLength < 3)
         {
            PDebugError("static_PRoutingTableReadCompleted: The Routing Table is malformed (Last entry is truncated)");
            nResult = W_ERROR_BAD_PARAMETER;
            goto return_result;
         }

         switch(pBuffer[2])
         {
         default:
            {
               PDebugError("static_PRoutingTableReadCompleted: The Routing Table is malformed (Bad specific AID)");
               nResult = W_ERROR_BAD_PARAMETER;
               goto return_result;
            }

         case 0x00:
            {
               entry.nEntryType = W_ROUTING_TABLE_DEFAULT_AID_TARGET;
               break;
            }

         case 0x01:
            {
               entry.nEntryType = W_ROUTING_TABLE_DEFAULT_APDU_TARGET;
               break;
            }
         }
      }
      else if ((nAidLength >= 5) && (nAidLength <= 16))
      {
         if (nLength < (uint32_t)(2 + nAidLength))
         {
            PDebugError("static_PRoutingTableReadCompleted: The Routing Table is malformed (Last entry is truncated)");
            nResult = W_ERROR_BAD_PARAMETER;
            goto return_result;
         }

         entry.nEntryType = W_ROUTING_TABLE_AID_ENTRY;
         entry.nAidLength = nAidLength;
         CMemoryCopy(entry.aAid, pBuffer + 2, entry.nAidLength);
      }
      else
      {
         PDebugError("static_PRoutingTableReadCompleted: The Routing Table is malformed (AID value is not in range 5 to 16)");
         nResult = W_ERROR_BAD_PARAMETER;
         goto return_result;
      }

      entry.nTargetId = static_PRoutingTableConvertTargetIdToSeId(pContext, (uint8_t)(nFlags & 0x07), nSeNumber);
      entry.nValidityFlags = (uint8_t)((nFlags >> 3) & 0x07);

      if (entry.nTargetId == P_ROUTING_TABLE_SEID_NOT_FOUND)
      {
         PDebugError("static_PRoutingTableReadCompleted: The Routing Table contains an entry that does not match the current hardware configuration");
         nResult = W_ERROR_BAD_PARAMETER;
         goto return_result;
      }

      nResult = PRoutingTableModify(pContext, hRoutingTable, W_ROUTING_TABLE_OPERATION_APPEND, 0, &entry);
      if (nResult != W_SUCCESS)
      {
         goto return_result;
      }

      pBuffer += (2 + nAidLength);
      nLength -= (2 + nAidLength);
   }

return_result:

   if ((nResult != W_SUCCESS) && (hRoutingTable != W_NULL_HANDLE))
   {
      PBasicCloseHandle(pContext, hRoutingTable);
      hRoutingTable = W_NULL_HANDLE;
   }

   PDFCPostContext3(&pRoutingTableInstance->sCallbackContext, hRoutingTable, nResult);
}

/* See client API */
void PRoutingTableRead(
            tContext * pContext,
            tPBasicGenericHandleCallbackFunction* pCallback,
            void* pCallbackParameter )
{
   W_ERROR nResult;
   tRoutingTableInstance* pRoutingTableInstance;

   PDebugTrace("PRoutingTableRead()");

   if (pContext == null)
   {
      PDebugError("PRoutingTableRead: pContext == null");
      return;
   }

   if (static_PRoutingTableIsSupported(pContext) == W_FALSE)
   {
      PDebugError("PRoutingTableRead: The Routing Table is not supported");

      nResult = W_ERROR_FEATURE_NOT_SUPPORTED;
      goto return_error;
   }

   pRoutingTableInstance = PContextGetRoutingTableInstance(pContext);
   CDebugAssert(pRoutingTableInstance != null);

   if (pRoutingTableInstance->nPendingFlag != P_ROUTING_TABLE_IDLE)
   {
      PDebugError("PRoutingTableRead: Another request is already pending");

      nResult = W_ERROR_BAD_STATE;
      goto return_error;
   }

   pRoutingTableInstance->nPendingFlag = P_ROUTING_TABLE_PENDING;

   PDFCFillCallbackContext(pContext, (tDFCCallback *)pCallback, pCallbackParameter,
      &pRoutingTableInstance->sCallbackContext);

   PRoutingTableReadDriver(pContext,
      pRoutingTableInstance->aBuffer, sizeof(pRoutingTableInstance->aBuffer),
      static_PRoutingTableReadCompleted,
      pRoutingTableInstance);
   return;

return_error:
   {
      tDFCCallbackContext sCallbackContext;
      PDFCFillCallbackContext(pContext, (tDFCCallback *)pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext3(&sCallbackContext, W_NULL_HANDLE, nResult);
   }
}

/* See client API */
W_ERROR WRoutingTableReadSync(
            W_HANDLE* phRoutingTable )
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WRoutingTableReadSync");

   if (phRoutingTable == null)
   {
      PDebugTrace("WRoutingTableReadSync: phRoutingTable == null");
      return W_ERROR_BAD_PARAMETER;
   }
   else
   {
      *phRoutingTable = W_NULL_HANDLE;
   }

   if (WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WRoutingTableRead(PBasicGenericSyncCompletionHandle, &param);
   }

   return PBasicGenericSyncWaitForResultHandle(&param, phRoutingTable);
}

static uint32_t static_PRoutingTableGetEntrySize(
         const tWRoutingTableEntry* pEntry)
{
   switch(pEntry->nEntryType)
   {
   case W_ROUTING_TABLE_AID_ENTRY:
      {
         return 2 + pEntry->nAidLength;
      }

   case W_ROUTING_TABLE_DEFAULT_AID_TARGET:
   case W_ROUTING_TABLE_DEFAULT_APDU_TARGET:
      {
         return 2 + 1;
      }

   default:
      {
         CDebugAssert(W_FALSE);
         return 0;
      }
   }
}

static uint32_t static_PRoutingTableGetSize(
         const tRoutingTable* pRoutingTable,
         const tWRoutingTableEntry* pEntry)
{
   uint32_t nIndex;
   uint32_t nLength = 0;

   for(nIndex = 0; nIndex < pRoutingTable->nEntryCount; nIndex++)
   {
      nLength += static_PRoutingTableGetEntrySize(pRoutingTable->sEntry + nIndex);
   }

   return nLength + static_PRoutingTableGetEntrySize(pEntry);
}

static W_ERROR static_PRoutingTablePrepareEntries(
         tContext* pContext,
         tRoutingTable* pRoutingTable,
         uint8_t* pBuffer, uint32_t nBufferLength, uint32_t* pnResultLength )
{
   W_ERROR nResult = W_SUCCESS;

   uint32_t nSeNumber = 0;
   uint32_t nResultLength = 0;
   uint32_t nIndex;

   CDebugAssert(pRoutingTable != null);
   CDebugAssert(pBuffer != null);
   CDebugAssert(pnResultLength != null);

   if ((nResult = PNFCControllerGetIntegerProperty(pContext, W_NFCC_PROP_SE_NUMBER, &nSeNumber)) != W_SUCCESS)
   {
      PDebugError("static_PRoutingTableReadCompleted: Failed to get W_NFCC_PROP_SE_NUMBER");
      return nResult;
   }

   for(nIndex = 0; nIndex < pRoutingTable->nEntryCount; nIndex++)
   {
      uint32_t nAidLength = pRoutingTable->sEntry[nIndex].nAidLength;
      uint8_t* pAid = pRoutingTable->sEntry[nIndex].aAid;
      uint8_t nTargetId;

      switch(pRoutingTable->sEntry[nIndex].nEntryType)
      {
      case W_ROUTING_TABLE_AID_ENTRY:
         {
            break;
         }

      case W_ROUTING_TABLE_DEFAULT_AID_TARGET:
         {
            static uint8_t aDefaultAidTargetAid[] = { 0x00 };
            pAid = aDefaultAidTargetAid;
            nAidLength = 1;
            break;
         }

      case W_ROUTING_TABLE_DEFAULT_APDU_TARGET:
         {
            static uint8_t aDefaultApduTargetAid[] = { 0x01 };
            pAid = aDefaultApduTargetAid;
            nAidLength = 1;
            break;
         }

      default:
         {
            PDebugError("static_PRoutingTablePrepareEntries: Bad entry type. Ignored");
            continue;
         }
      }

      nResultLength += sizeof(uint8_t) + sizeof(uint8_t) + nAidLength;

      if (nResultLength > nBufferLength)
      {
         nResult = W_ERROR_BUFFER_TOO_SHORT;
         continue;
      }

      nTargetId = static_PRoutingTableConvertSeIdToTargetId(pContext, pRoutingTable->sEntry[nIndex].nTargetId, nSeNumber);

      if (nTargetId == P_ROUTING_TABLE_TARGETID_NOT_FOUND)
      {
         PDebugError("static_PRoutingTablePrepareEntries: Bad target id");
      }

      *pBuffer = (uint8_t)(((pRoutingTable->sEntry[nIndex].nValidityFlags & 0x07) << 3) | (nTargetId));
      pBuffer++;

      *pBuffer = (uint8_t)nAidLength;
      pBuffer++;

      CMemoryCopy(pBuffer, pAid, nAidLength);
      pBuffer += nAidLength;
   }

   *pnResultLength = nResultLength;

   return nResult;
}

static void static_PRoutingTableApplyCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         W_ERROR nResult)
{
   tRoutingTableInstance* pRoutingTableInstance = (tRoutingTableInstance*)pCallbackParameter;
   CDebugAssert(pRoutingTableInstance != null);

   pRoutingTableInstance->nPendingFlag = P_ROUTING_TABLE_IDLE;

   if (nResult != W_SUCCESS)
   {
      PDebugError("static_PRoutingTableApplyCompleted: Received error is %s", PUtilTraceError(nResult));
   }

   PDFCPostContext2(&pRoutingTableInstance->sCallbackContext, nResult);
}

/* See client API */
void PRoutingTableApply(
            tContext * pContext,
            W_HANDLE hRoutingTable,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter )
{
   tRoutingTableInstance* pRoutingTableInstance;
   tRoutingTable* pRoutingTable = (tRoutingTable*)null;
   W_ERROR nResult;
   uint32_t nNALDataBufferLength = 0;

   PDebugTrace("PRoutingTableApply()");

   if (pContext == null)
   {
      PDebugError("PRoutingTableApply: pContext == null");
      return;
   }

   if (hRoutingTable == W_NULL_HANDLE)
   {
      PDebugError("PRoutingTableApply: hRoutingTable == null");

      nResult = W_ERROR_BAD_HANDLE;
      goto return_error;
   }

   nResult = PHandleGetObject(pContext, hRoutingTable, P_HANDLE_TYPE_ROUTING_TABLE, (void**)&pRoutingTable);
   if ((nResult != W_SUCCESS) || (pRoutingTable == null))
   {
      PDebugError("PRoutingTableApply: PHandleGetObject returned error %s", PUtilTraceError(nResult));

      nResult = W_ERROR_BAD_HANDLE;
      goto return_error;
   }

   if (static_PRoutingTableIsSupported(pContext) == W_FALSE)
   {
      PDebugError("PRoutingTableApply: The Routing Table is not supported");

      nResult = W_ERROR_FEATURE_NOT_SUPPORTED;
      goto return_error;
   }

   pRoutingTableInstance = PContextGetRoutingTableInstance(pContext);
   CDebugAssert(pRoutingTableInstance != null);

   /* Prepare the data to be sent to the NAL */
   nResult = static_PRoutingTablePrepareEntries(pContext, pRoutingTable,
      pRoutingTableInstance->aBuffer, sizeof(pRoutingTableInstance->aBuffer),
      &nNALDataBufferLength);
   if (nResult != W_SUCCESS)
   {
      PDebugError("static_PRoutingTablePrepareEntries: Buffer overflow. Buffer size is %d bytes, expected size is %d bytes",
         sizeof(pRoutingTableInstance->aBuffer), nNALDataBufferLength);

      goto return_error;
   }

   if (pRoutingTableInstance->nPendingFlag != P_ROUTING_TABLE_IDLE)
   {
      PDebugError("PRoutingTableApply: Another request is already pending");

      nResult = W_ERROR_BAD_STATE;
      goto return_error;
   }

   pRoutingTableInstance->nPendingFlag = P_ROUTING_TABLE_PENDING;

   PDFCFillCallbackContext(pContext, (tDFCCallback *)pCallback, pCallbackParameter,
      &pRoutingTableInstance->sCallbackContext);

   PRoutingTableApplyDriver(pContext,
      (nNALDataBufferLength == 0) ? (uint8_t*)null : pRoutingTableInstance->aBuffer,
      nNALDataBufferLength,
      static_PRoutingTableApplyCompleted,
      pRoutingTableInstance);
   return;

return_error:
   {
      tDFCCallbackContext sCallbackContext;
      PDFCFillCallbackContext(pContext, (tDFCCallback *)pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext2(&sCallbackContext, nResult);
   }
}

/* See client API */
W_ERROR WRoutingTableApplySync(
            W_HANDLE hRoutingTable )
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WRoutingTableApplySync");

   if (WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WRoutingTableApply(hRoutingTable, PBasicGenericSyncCompletion, &param);
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See client API */
W_ERROR PRoutingTableIsEnabled(
         tContext* pContext,
         bool_t * pbIsEnabled )
{
   uint32_t nConfig = 0;

   PDebugTrace("PRoutingTableIsEnabled");

   if (pContext == null)
   {
      PDebugError("PRoutingTableIsEnabled: pContext == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pbIsEnabled == null)
   {
      PDebugError("PRoutingTableIsEnabled: pbIsEnabled == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (static_PRoutingTableIsSupported(pContext) == W_FALSE)
   {
      PDebugError("PRoutingTableIsEnabled: The Routing Table is not supported");
      return W_ERROR_FEATURE_NOT_SUPPORTED;
   }

   nConfig = PRoutingTableGetConfigDriver(pContext);

   *pbIsEnabled = ((nConfig & P_ROUTING_TABLE_CONFIG_ENABLED) == P_ROUTING_TABLE_CONFIG_ENABLED) ? W_TRUE : W_FALSE;

   return W_SUCCESS;
}

static void static_PRoutingTableEnableCompleted(
            tContext* pContext,
            void* pCallbackParameter,
            W_ERROR nResult )
{
   tRoutingTableInstance* pRoutingTableInstance = (tRoutingTableInstance*)pCallbackParameter;
   CDebugAssert(pRoutingTableInstance != null);

   pRoutingTableInstance->nPendingFlag = P_ROUTING_TABLE_IDLE;

   if (nResult != W_SUCCESS)
   {
      PDebugError("static_PRoutingTableEnableCompleted: Received error is %s", PUtilTraceError(nResult));
   }

   PDFCPostContext2(&pRoutingTableInstance->sCallbackContext, nResult);
}

/* See client API */
void PRoutingTableEnable(
            tContext * pContext,
            bool_t bIsEnabled,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter )
{
   W_ERROR nResult;
   tRoutingTableInstance* pRoutingTableInstance;
   uint32_t nConfig;

   PDebugTrace("PRoutingTableEnable()");

   if (pContext == null)
   {
      PDebugError("PRoutingTableEnable: pContext == null");
      return;
   }

   if (static_PRoutingTableIsSupported(pContext) == W_FALSE)
   {
      PDebugError("PRoutingTableEnable: The Routing Table is not supported");

      nResult = W_ERROR_FEATURE_NOT_SUPPORTED;
      goto return_error;
   }

   nConfig = (bIsEnabled != W_FALSE) ? P_ROUTING_TABLE_CONFIG_ENABLED : 0;

   pRoutingTableInstance = PContextGetRoutingTableInstance(pContext);
   CDebugAssert(pRoutingTableInstance != null);

   if (pRoutingTableInstance->nPendingFlag != P_ROUTING_TABLE_IDLE)
   {
      PDebugError("PRoutingTableEnable: Another request is already pending");

      nResult = W_ERROR_BAD_STATE;
      goto return_error;
   }

   pRoutingTableInstance->nPendingFlag = P_ROUTING_TABLE_PENDING;

   PDFCFillCallbackContext(pContext, (tDFCCallback *)pCallback, pCallbackParameter,
      &pRoutingTableInstance->sCallbackContext);

   PRoutingTableSetConfigDriver(pContext, nConfig,
      static_PRoutingTableEnableCompleted,
      pRoutingTableInstance);
   return;

return_error:
   {
      tDFCCallbackContext sCallbackContext;
      PDFCFillCallbackContext(pContext, (tDFCCallback *)pCallback, pCallbackParameter, &sCallbackContext);
      PDFCPostContext2(&sCallbackContext, nResult);
   }
}

/* See client API */
W_ERROR WRoutingTableEnableSync(
            bool_t  bIsEnabled )
{
   tPBasicGenericSyncParameters param;

   PDebugTrace("WRoutingTableEnableSync");

   if (WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WRoutingTableEnable(bIsEnabled, PBasicGenericSyncCompletion, &param);
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* See client API */
W_ERROR PRoutingTableGetEntryCount(
            tContext * pContext,
            W_HANDLE hRoutingTable,
            uint16_t* pnEntryCount )
{
   W_ERROR nResult;
   tRoutingTable* pRoutingTable = (tRoutingTable*)null;

   PDebugTrace("PRoutingTableGetEntryCount()");

   if (hRoutingTable == W_NULL_HANDLE)
   {
      PDebugError("PRoutingTableGetentryCount: hRoutingTable == null");
      return W_ERROR_BAD_HANDLE;
   }

   nResult = PHandleGetObject(pContext, hRoutingTable, P_HANDLE_TYPE_ROUTING_TABLE, (void**)&pRoutingTable);
   if ((nResult != W_SUCCESS) || (pRoutingTable == null))
   {
      PDebugError("PRoutingTableGetEntryCount: PHandleGetObject returned error %s", PUtilTraceError(nResult));
      return W_ERROR_BAD_HANDLE;
   }

   if (pnEntryCount == null)
   {
      PDebugError("PRoutingTableGetEntryCount: pnEntryCount == null");
      return W_ERROR_BAD_PARAMETER;
   }

   *pnEntryCount = pRoutingTable->nEntryCount;

   return nResult;
}

/* See client API */
W_ERROR PRoutingTableGetEntry(
            tContext * pContext,
            W_HANDLE hRoutingTable,
            uint16_t nEntryIndex,
            tWRoutingTableEntry* pRoutingTableEntry )
{
   W_ERROR nResult;
   tRoutingTable* pRoutingTable = (tRoutingTable*)null;

   PDebugTrace("PRoutingTableGetEntry()");

   if (hRoutingTable == W_NULL_HANDLE)
   {
      PDebugError("PRoutingTableGetEntry: hRoutingTable == null");
      return W_ERROR_BAD_HANDLE;
   }

   nResult = PHandleGetObject(pContext, hRoutingTable, P_HANDLE_TYPE_ROUTING_TABLE, (void**)&pRoutingTable);
   if ((nResult != W_SUCCESS) || (pRoutingTable == null))
   {
      PDebugError("PRoutingTableGetEntry: PHandleGetObject returned error %s", PUtilTraceError(nResult));
      return W_ERROR_BAD_HANDLE;
   }

   if (nEntryIndex >= pRoutingTable->nEntryCount)
   {
      PDebugError("PRoutingTableGetEntry: nEntryIndex (%u) is not less than the entry count (%u)",
         nEntryIndex, pRoutingTable->nEntryCount);
      return W_ERROR_BAD_PARAMETER;
   }

   if (pRoutingTableEntry == null)
   {
      PDebugError("PRoutingTableGetEntry: pRoutingTableEntry == null");
      return W_ERROR_BAD_PARAMETER;
   }

   *pRoutingTableEntry = pRoutingTable->sEntry[nEntryIndex];

   return W_SUCCESS;
}

#define W_ROUTING_TABLE_POWERSTATE_MASK (\
   W_ROUTING_TABLE_POWERSTATE_BATT_OFF|\
   W_ROUTING_TABLE_POWERSTATE_PHONE_OFF|\
   W_ROUTING_TABLE_POWERSTATE_PHONE_ON)

static W_ERROR static_PRoutingTableCheckEntry(
            tContext * pContext,
            const tWRoutingTableEntry* pRoutingTableEntry )
{
   W_ERROR nResult;
   uint32_t nSeNumber = 0;

   CDebugAssert(pRoutingTableEntry != null);

   switch(pRoutingTableEntry->nEntryType)
   {
   case W_ROUTING_TABLE_AID_ENTRY:
      {
         if ((pRoutingTableEntry->nAidLength < 5) || (pRoutingTableEntry->nAidLength > 16))
         {
            PDebugError("static_PRoutingTableCheckEntry: nAidLength not in range 5 to 16");
            return W_ERROR_BAD_PARAMETER;
         }

         break;
      }

   case W_ROUTING_TABLE_DEFAULT_AID_TARGET:
   case W_ROUTING_TABLE_DEFAULT_APDU_TARGET:
      {
         /*
            * Do not chek the AID length, beause this field is unused.
            *
         if (pRoutingTableEntry->nAidLength != 0)
         {
            PDebugError("static_PRoutingTableCheckEntry: nAidLength not 0");
            return W_ERROR_BAD_PARAMETER;
         }
            */

         break;
      }

   default:
      {
         PDebugError("static_PRoutingTableCheckEntry: Bad nEntryType");
         return W_ERROR_BAD_PARAMETER;
      }
   }

   if (((pRoutingTableEntry->nValidityFlags & ~W_ROUTING_TABLE_POWERSTATE_MASK) != 0) || ((pRoutingTableEntry->nValidityFlags & W_ROUTING_TABLE_POWERSTATE_MASK) == 0))
   {
      PDebugError("static_PRoutingTableCheckEntry: Bad nValidityFlags");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((nResult = PNFCControllerGetIntegerProperty(pContext, W_NFCC_PROP_SE_NUMBER, &nSeNumber)) != W_SUCCESS)
   {
      PDebugError("static_PRoutingTableReadCompleted: Failed to get W_NFCC_PROP_SE_NUMBER");
      return nResult;
   }

   if (pRoutingTableEntry->nTargetId > nSeNumber)
   {
      PDebugError("static_PRoutingTableCheckEntry: Bad nTargetId");
      return W_ERROR_BAD_PARAMETER;
   }

   return W_SUCCESS;
}

/* See client API */
W_ERROR PRoutingTableModify(
            tContext * pContext,
            W_HANDLE hRoutingTable,
            uint32_t nOperation,
            uint16_t nEntryIndex,
            const tWRoutingTableEntry* pRoutingTableEntry )
{
   W_ERROR nResult;
   tRoutingTable* pRoutingTable = (tRoutingTable*)null;

   PDebugTrace("PRoutingTableModify()");

   if (hRoutingTable == W_NULL_HANDLE)
   {
      PDebugError("PRoutingTableModify: hRoutingTable == null");
      return W_ERROR_BAD_HANDLE;
   }

   nResult = PHandleGetObject(pContext, hRoutingTable, P_HANDLE_TYPE_ROUTING_TABLE, (void**)&pRoutingTable);
   if ((nResult != W_SUCCESS) || (pRoutingTable == null))
   {
      PDebugError("PRoutingTableModify: PHandleGetObject returned error %s", PUtilTraceError(nResult));
      return W_ERROR_BAD_HANDLE;
   }

   switch(nOperation)
   {
   case W_ROUTING_TABLE_OPERATION_RESET:
      {
         if ((nEntryIndex != 0) || (pRoutingTableEntry != null))
         {
            PDebugError("PRoutingTableModify(RESET): nEntryIndex != 0 || pRoutingTableEntry != null");
            return W_ERROR_BAD_PARAMETER;
         }

         pRoutingTable->nEntryCount = 0;
         CMemoryFill(pRoutingTable->sEntry, 0, sizeof(pRoutingTable->sEntry));
         break;
      }

   case W_ROUTING_TABLE_OPERATION_DELETE:
      {
         if (nEntryIndex >= pRoutingTable->nEntryCount)
         {
            PDebugError("PRoutingTableModify(DELETE): nEntryIndex (%u) is not less than the entry count (%u)",
               nEntryIndex, pRoutingTable->nEntryCount);
            return W_ERROR_BAD_PARAMETER;
         }

         if (pRoutingTableEntry != null)
         {
            PDebugError("PRoutingTableModify(DELETE): pRoutingTableEntry != null");
            return W_ERROR_BAD_PARAMETER;
         }

         /* Shift entries that are after the deleted entry, if needed */
         while(nEntryIndex < (pRoutingTable->nEntryCount - 1))
         {
            pRoutingTable->sEntry[nEntryIndex] = pRoutingTable->sEntry[nEntryIndex + 1];
            nEntryIndex++;
         }

         /* Clear last entry */
         CMemoryFill(&pRoutingTable->sEntry[nEntryIndex], 0, sizeof(tWRoutingTableEntry));

         /* Decrement entry count */
         pRoutingTable->nEntryCount--;

         break;
      }

   case W_ROUTING_TABLE_OPERATION_APPEND:
      {
         if ((nEntryIndex != 0) || (pRoutingTableEntry == null))
         {
            PDebugError("PRoutingTableModify(APPEND): nEntryIndex != 0 || pRoutingTableEntry == null");
            return W_ERROR_BAD_PARAMETER;
         }

         /* Check whether there is still a free entry */
         if (pRoutingTable->nEntryCount >= pRoutingTable->nAllocEntryCount)
         {
            PDebugWarning("PRoutingTableModify(APPEND): All Routing Table entries are used");
            return W_ERROR_OUT_OF_RESOURCE;
         }

         if ((nResult = static_PRoutingTableCheckEntry(pContext, pRoutingTableEntry)) != W_SUCCESS)
         {
            return nResult;
         }

         if (static_PRoutingTableGetSize(pRoutingTable, pRoutingTableEntry) > P_ROUTING_TABLE_MAX_LENGTH)
         {
            PDebugWarning("PRoutingTableModify(APPEND): New entry generates a buffer overflow");
            return W_ERROR_OUT_OF_RESOURCE;
         }

         /* Append entry and increment entry count */
         pRoutingTable->sEntry[pRoutingTable->nEntryCount] = *pRoutingTableEntry;
         pRoutingTable->nEntryCount++;

         break;
      }

   case W_ROUTING_TABLE_OPERATION_INSERT:
      {
         uint16_t nIndex;

         if (nEntryIndex >= pRoutingTable->nEntryCount)
         {
            PDebugError("PRoutingTableModify(INSERT): nEntryIndex (%u) is not less than the entry count (%u)",
               nEntryIndex, pRoutingTable->nEntryCount);
            return W_ERROR_BAD_PARAMETER;
         }

         if (pRoutingTableEntry == null)
         {
            PDebugError("PRoutingTableModify(INSERT): pRoutingTableEntry == null");
            return W_ERROR_BAD_PARAMETER;
         }

         /* Check whether there is still a free entry */
         if (pRoutingTable->nEntryCount >= pRoutingTable->nAllocEntryCount)
         {
            PDebugWarning("PRoutingTableModify(INSERT): All Routing Table entries are used");
            return W_ERROR_OUT_OF_RESOURCE;
         }

         if ((nResult = static_PRoutingTableCheckEntry(pContext, pRoutingTableEntry)) != W_SUCCESS)
         {
            return nResult;
         }

         if (static_PRoutingTableGetSize(pRoutingTable, pRoutingTableEntry) > P_ROUTING_TABLE_MAX_LENGTH)
         {
            PDebugWarning("PRoutingTableModify(INSERT): New entry generates a buffer overflow");
            return W_ERROR_OUT_OF_RESOURCE;
         }

         /* Move entries */
         for(nIndex = pRoutingTable->nEntryCount; nIndex > nEntryIndex; nIndex--)
         {
            pRoutingTable->sEntry[nIndex] = pRoutingTable->sEntry[nIndex - 1];
         }

         /* Insert entry and increment entry count */
         pRoutingTable->sEntry[nEntryIndex] = *pRoutingTableEntry;
         pRoutingTable->nEntryCount++;

         break;
      }

   default:
      {
         uint16_t nNewIndex, nIndex;

         if ((nOperation & 0xFFFF0000) != W_ROUTING_TABLE_OPERATION_MOVE(0))
         {
            PDebugError("PRoutingTableModify: nOperation is invalid");
            return W_ERROR_BAD_PARAMETER;
         }

         /* Processing W_ROUTING_TABLE_OPERATION_MOVE(nNewIndex) */
         nNewIndex = (uint16_t)nOperation;

         if (nNewIndex >= pRoutingTable->nEntryCount)
         {
            PDebugError("PRoutingTableModify(MOVE): nNewIndex (%u) is not less than the entry count (%u)",
               nEntryIndex, pRoutingTable->nEntryCount);
            return W_ERROR_BAD_PARAMETER;
         }

         if (nEntryIndex >= pRoutingTable->nEntryCount)
         {
            PDebugError("PRoutingTableModify(MOVE): nEntryIndex (%u) is not less than the entry count (%u)",
               nEntryIndex, pRoutingTable->nEntryCount);
            return W_ERROR_BAD_PARAMETER;
         }

         if (pRoutingTableEntry != null)
         {
            PDebugError("PRoutingTableModify(MOVE): pRoutingTableEntry != null");
            return W_ERROR_BAD_PARAMETER;
         }

         if (nNewIndex > nEntryIndex)
         {
            /* The new position is after the old position */
            tWRoutingTableEntry sEntry = pRoutingTable->sEntry[nEntryIndex];

            for(nIndex = nEntryIndex; nIndex < nNewIndex; nIndex++)
            {
               pRoutingTable->sEntry[nIndex] = pRoutingTable->sEntry[nIndex + 1];
            }

            pRoutingTable->sEntry[nNewIndex] = sEntry;
         }
         else if (nNewIndex < nEntryIndex)
         {
            /* The new position is before the old position */
            tWRoutingTableEntry sEntry = pRoutingTable->sEntry[nEntryIndex];

            for(nIndex = nEntryIndex; nIndex > nNewIndex; nIndex--)
            {
               pRoutingTable->sEntry[nIndex] = pRoutingTable->sEntry[nIndex - 1];
            }

            pRoutingTable->sEntry[nNewIndex] = sEntry;
         }
         else /* if (nNewIndex == nEntryIndex) */
         {
            /* Do nothing */
         }
      }
   }

   return W_SUCCESS;
}

#endif


#if (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

static void static_PRoutingTableReadDriverCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         uint32_t nLength,
         W_ERROR nResult)
{
   tRoutingTableDriverInstance* pDriverInstance = (tRoutingTableDriverInstance*)pCallbackParameter;
   CDebugAssert(pDriverInstance != null);

   if (nResult != W_SUCCESS)
   {
      PDebugError("static_PRoutingTableReadDriverCompleted: Received error is %s", PUtilTraceError(nResult));
   }

   PDFCDriverPostCC3(pDriverInstance->sDriverCC, nLength, nResult);
}

void PRoutingTableReadDriver(
         tContext* pContext,
         OPEN_NFC_BUF1_O uint8_t * pBuffer,
         OPEN_NFC_BUF1_LENGTH uint32_t nBufferLength,
         OPEN_NFC_USER_CALLBACK tPBasicGenericDataCallbackFunction * pCallback,
         void * pCallbackParameter )
{
   W_ERROR nResult;
   tRoutingTableDriverInstance* pDriverInstance = (tRoutingTableDriverInstance*)null;

   PDebugTrace("PRoutingTableReadDriver()");

   if (pContext == null)
   {
      PDebugError("PRoutingTableReadDriver: pContext == null");
      return;
   }

   if (pBuffer == null)
   {
      PDebugError("PRoutingTableReadDriver: pBuffer == null");

      nResult = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   pDriverInstance = PContextGetRoutingTableDriverInstance(pContext);
   CDebugAssert(pDriverInstance != null);

   PDFCDriverFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter,
      &pDriverInstance->sDriverCC);

   PNALServiceGetParameter(pContext, NAL_SERVICE_ADMIN,
      &pDriverInstance->sNALServiceOperation,
      NAL_PAR_ROUTING_TABLE_ENTRIES,
      pBuffer, nBufferLength,
      static_PRoutingTableReadDriverCompleted,
      pDriverInstance);
   return;

return_error:
   {
      tDFCDriverCCReference sDriverCC;
      PDFCDriverFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter, &sDriverCC);
      PDFCDriverPostCC3(sDriverCC, 0, nResult);
   }
}

static void static_PRoutingTableApplyDriverCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         W_ERROR nResult)
{
   tRoutingTableDriverInstance* pDriverInstance = (tRoutingTableDriverInstance*)pCallbackParameter;
   CDebugAssert(pDriverInstance != null);

   if (nResult != W_SUCCESS)
   {
      PDebugError("static_PRoutingTableApplyDriverCompleted: Received error is %s", PUtilTraceError(nResult));
   }

   PDFCDriverPostCC2(pDriverInstance->sDriverCC, nResult);
}

void PRoutingTableApplyDriver(
         tContext* pContext,
         OPEN_NFC_BUF1_I uint8_t * pBuffer,
         OPEN_NFC_BUF1_LENGTH uint32_t nBufferLength,
         OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction * pCallback,
         void * pCallbackParameter )
{
   W_ERROR nResult;
   tRoutingTableDriverInstance* pDriverInstance = (tRoutingTableDriverInstance*)null;

   PDebugTrace("PRoutingTableApplyDriver()");

   if (pContext == null)
   {
      PDebugError("PRoutingTableApplyDriver: pContext == null");
      return;
   }

   if ((pBuffer == null) && (nBufferLength != 0))
   {
      PDebugError("PRoutingTableApplyDriver: pBuffer == null && nBufferLength != 0");

      nResult = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   pDriverInstance = PContextGetRoutingTableDriverInstance(pContext);
   CDebugAssert(pDriverInstance != null);

   PDFCDriverFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter,
      &pDriverInstance->sDriverCC);

   PNALServiceSetParameter(pContext, NAL_SERVICE_ADMIN,
      &pDriverInstance->sNALServiceOperation,
      NAL_PAR_ROUTING_TABLE_ENTRIES,
      pBuffer, nBufferLength,
      static_PRoutingTableApplyDriverCompleted,
      pDriverInstance);
   return;

return_error:
   {
      tDFCDriverCCReference sDriverCC;
      PDFCDriverFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter, &sDriverCC);
      PDFCDriverPostCC2(sDriverCC, nResult);
   }
}

uint32_t PRoutingTableGetConfigDriver(
         tContext* pContext )
{
   tRoutingTableDriverInstance* pDriverInstance = (tRoutingTableDriverInstance*)null;

   PDebugTrace("PRoutingTableGetConfigDriver()");

   if (pContext == null)
   {
      PDebugError("PRoutingTableGetConfigDriver: pContext == null");
      return 0;
   }

   pDriverInstance = PContextGetRoutingTableDriverInstance(pContext);
   CDebugAssert(pDriverInstance != null);

   return pDriverInstance->nConfig;
}

static void static_PRoutingTableSetConfigDriverCompleted(
         tContext* pContext,
         void* pCallbackParameter,
         W_ERROR nResult)
{
   tRoutingTableDriverInstance* pDriverInstance = (tRoutingTableDriverInstance*)pCallbackParameter;
   CDebugAssert(pDriverInstance != null);

   if (nResult != W_SUCCESS)
   {
      PDebugError("static_PRoutingTableSetConfigDriverCompleted: Received error is %s", PUtilTraceError(nResult));
   }
   else
   {
      /* Update the cached configuration parameter */
      pDriverInstance->nConfig = PUtilReadUint16FromBigEndianBuffer(pDriverInstance->aNALDataBuffer);
   }

   PDFCDriverPostCC2(pDriverInstance->sDriverCC, nResult);
}

void PRoutingTableSetConfigDriver(
         tContext* pContext,
         uint32_t nConfig,
         OPEN_NFC_USER_CALLBACK tPBasicGenericCallbackFunction * pCallback,
         void * pCallbackParameter )
{
   tRoutingTableDriverInstance* pDriverInstance = (tRoutingTableDriverInstance*)null;
   uint32_t nNALDataBufferLength = 0;

   PDebugTrace("PRoutingTableSetConfigDriver()");

   if (pContext == null)
   {
      PDebugError("PRoutingTableSetConfigDriver: pContext == null");
      return;
   }

   pDriverInstance = PContextGetRoutingTableDriverInstance(pContext);
   CDebugAssert(pDriverInstance != null);

   nNALDataBufferLength = PUtilWriteUint16ToBigEndianBuffer((uint16_t)nConfig, pDriverInstance->aNALDataBuffer);

   PDFCDriverFillCallbackContext(pContext, (tDFCCallback*)pCallback, pCallbackParameter,
      &pDriverInstance->sDriverCC);

   PNALServiceSetParameter(pContext, NAL_SERVICE_ADMIN,
      &pDriverInstance->sNALServiceOperation,
      NAL_PAR_ROUTING_TABLE_CONFIG,
      pDriverInstance->aNALDataBuffer, nNALDataBufferLength,
      static_PRoutingTableSetConfigDriverCompleted,
      pDriverInstance);
}

#endif

/* EOF */
