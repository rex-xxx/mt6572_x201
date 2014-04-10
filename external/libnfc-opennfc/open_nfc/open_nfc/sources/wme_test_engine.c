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
   Contains the test engine implementation.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( TEST )

#include "wme_context.h"

#if ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) ) && (defined P_INCLUDE_TEST_ENGINE)

#include "wme_test_engine_autogen.h"

/* The maximum length in bytes of the product name */
#define P_TEST_PRODUCT_NAME_LENGTH     15

/* Current version number of the test engine */
#define P_TEST_VERSION_NUMBER_LENGTH   4
#define P_TEST_PROTOCOL_VERSION        { 0x01, 0x00, 0x00, 0x08 }

/* List of message types */
#define P_TEST_CMD_PING             0x00
#define P_TEST_CMD_INIT             0x01
#define P_TEST_CMD_DOWNLOAD         0x02
#define P_TEST_CMD_EXECUTE          0x03
#define P_TEST_CMD_RESET_STATISTICS 0x04
#define P_TEST_CMD_GET_STATISTICS   0x05
#define P_TEST_RCMD_MESSAGE_BOX     0x06
#define P_TEST_RCMD_PRESENT_OBJECT  0x07
#define P_TEST_RCMD_REMOVE_OBJECT   0x08
#define P_TEST_RCMD_REMOTE_EXECUTE  0x09
#define P_TEST_CMD_GET_TEST_LIST    0x0A

/* Automatic mode type flags */
#define P_TEST_MESSAGE_AUTOMATIC    0x01

/* The statistics flags */
#define P_TEST_STATISTIC_MEMORY     0x02
#define P_TEST_STATISTIC_TIME       0x04
#define P_TEST_STATISTIC_HANDLE     0x08
#define P_TEST_STATISTIC_PROTOCOL   0x10

/* Test engine states */
#define P_TEST_STATE_STOPPED                    0
#define P_TEST_STATE_INIT                       1
#define P_TEST_STATE_LOADED                     2
#define P_TEST_STATE_EXECUTION                  3
#define P_TEST_STATE_RESULT_SET                 4
#define P_TEST_STATE_ERROR_TEST_IMPLEMENTATION  5

/**
 * @brief  Switches the state
 *
 * Checks the status of the variable set before the switch.
 *
 * Do the cleanup of the variable if necessary.
 *
 * @param[in]  pTestEngineInstance  The test engine instance.
 *
 * @param[in]  nState  The new state.
 **/
static void static_PTestSwitchState(
            tTestEngineInstance* pTestEngineInstance,
            uint32_t nState)
{
   uint32_t nOldState = pTestEngineInstance->nState;

   switch(nState)
   {
   case P_TEST_STATE_STOPPED:

      PDebugTrace("static_PTestSwitchState: P_TEST_STATE_STOPPED");
      /* Cleanup */
      pTestEngineInstance->aTestName[0] = 0;

      /* Checking */
      CDebugAssert(pTestEngineInstance->sBundle.pBundleData == null);
      CDebugAssert(pTestEngineInstance->sBundle.nBundleDataLength == 0);
      CDebugAssert(pTestEngineInstance->sBundle.pTestLinkTable == null);
      CDebugAssert(pTestEngineInstance->sBundle.npStartAddress == 0);
      CDebugAssert(pTestEngineInstance->sBundle.bOffsetOrder == W_FALSE);
      CDebugAssert(pTestEngineInstance->sBundle.nOffset == 0);

      CDebugAssert(pTestEngineInstance->bAutomaticMode == W_FALSE);
      CDebugAssert(pTestEngineInstance->nRunningTestId == 0);
      CDebugAssert(pTestEngineInstance->nResultErrorCode == 0);
      CDebugAssert(pTestEngineInstance->nResultDataLength == 0);
      CDebugAssert(pTestEngineInstance->nRunningInteraction == 0);
      CDebugAssert(pTestEngineInstance->pExecuteRemoteResultBuffer == null);
      CDebugAssert(pTestEngineInstance->nExecuteRemoteResultResultBufferLength == 0);

      CDebugAssert(pTestEngineInstance->sExecuteContext.pInputData == null);
      CDebugAssert(pTestEngineInstance->sExecuteContext.nInputDataLength == 0);
      CDebugAssert(pTestEngineInstance->sExecuteContext.nInputParameter == 0);
      CDebugAssert(pTestEngineInstance->sExecuteContext.pApplicationData == null);
      break;
   case P_TEST_STATE_INIT:
      PDebugTrace("static_PTestSwitchState: P_TEST_STATE_INIT");
      /* Cleanup */
      pTestEngineInstance->aTestName[0] = 0;
      if(nOldState == P_TEST_STATE_LOADED)
      {
         CDebugAssert(pTestEngineInstance->sBundle.pBundleData != null);
         CDebugAssert(pTestEngineInstance->sBundle.pTestLinkTable != null);
         if(pTestEngineInstance->sBundle.pBundleData != null)
         {
            CDebugAssert(pTestEngineInstance->sBundle.nBundleDataLength != 0);
            CDebugAssert(pTestEngineInstance->sBundle.pTestLinkTable != null);

            CMemoryFree(pTestEngineInstance->sBundle.pBundleData);
            pTestEngineInstance->sBundle.pBundleData = null;
            pTestEngineInstance->sBundle.nBundleDataLength = 0;
            pTestEngineInstance->sBundle.pTestLinkTable = null;
            pTestEngineInstance->sBundle.npStartAddress = 0;
            pTestEngineInstance->sBundle.bOffsetOrder = W_FALSE;
            pTestEngineInstance->sBundle.nOffset = 0;
         }
      }

      /* Checking */
      CDebugAssert((nOldState == P_TEST_STATE_STOPPED) || (nOldState == P_TEST_STATE_LOADED)|| (nOldState == P_TEST_STATE_INIT));
      CDebugAssert(pTestEngineInstance->sBundle.pBundleData == null);
      CDebugAssert(pTestEngineInstance->sBundle.nBundleDataLength == 0);
      CDebugAssert(pTestEngineInstance->sBundle.pTestLinkTable == null);
      CDebugAssert(pTestEngineInstance->sBundle.npStartAddress == 0);
      CDebugAssert(pTestEngineInstance->sBundle.bOffsetOrder == W_FALSE);
      CDebugAssert(pTestEngineInstance->sBundle.nOffset == 0);

      CDebugAssert(pTestEngineInstance->bAutomaticMode == W_FALSE);
      CDebugAssert(pTestEngineInstance->nRunningTestId == 0);
      CDebugAssert(pTestEngineInstance->nResultErrorCode == 0);
      CDebugAssert(pTestEngineInstance->nResultDataLength == 0);
      CDebugAssert(pTestEngineInstance->nRunningInteraction == 0);
      CDebugAssert(pTestEngineInstance->pExecuteRemoteResultBuffer == null);
      CDebugAssert(pTestEngineInstance->nExecuteRemoteResultResultBufferLength == 0);

      CDebugAssert(pTestEngineInstance->sExecuteContext.pInputData == null);
      CDebugAssert(pTestEngineInstance->sExecuteContext.nInputDataLength == 0);
      CDebugAssert(pTestEngineInstance->sExecuteContext.nInputParameter == 0);
      CDebugAssert(pTestEngineInstance->sExecuteContext.pApplicationData == null);
      break;
   case P_TEST_STATE_LOADED:
      PDebugTrace("static_PTestSwitchState: P_TEST_STATE_LOADED");
      /* Cleanup */
      pTestEngineInstance->aTestName[0] = 0;
      if((nOldState == P_TEST_STATE_ERROR_TEST_IMPLEMENTATION) || (nOldState == P_TEST_STATE_RESULT_SET))
      {
         pTestEngineInstance->bAutomaticMode = W_FALSE;
         pTestEngineInstance->nRunningTestId = 0;
         pTestEngineInstance->nResultErrorCode = 0;
         pTestEngineInstance->nResultDataLength = 0;
         pTestEngineInstance->nRunningInteraction = 0;
         pTestEngineInstance->pExecuteRemoteResultBuffer = null;
         pTestEngineInstance->nExecuteRemoteResultResultBufferLength = 0;

         if(pTestEngineInstance->sExecuteContext.pInputData != null)
         {
            CMemoryFree(pTestEngineInstance->sExecuteContext.pInputData);
            pTestEngineInstance->sExecuteContext.pInputData = null;
         }
         pTestEngineInstance->sExecuteContext.nInputDataLength = 0;
         pTestEngineInstance->sExecuteContext.nInputParameter = 0;
         pTestEngineInstance->sExecuteContext.pApplicationData = null;
      }
      /* Checking */
      CDebugAssert((nOldState == P_TEST_STATE_INIT) || (nOldState == P_TEST_STATE_ERROR_TEST_IMPLEMENTATION) || (nOldState == P_TEST_STATE_RESULT_SET));
      CDebugAssert(pTestEngineInstance->sBundle.pBundleData != null);
      CDebugAssert(pTestEngineInstance->sBundle.nBundleDataLength != 0);
      CDebugAssert(pTestEngineInstance->sBundle.pTestLinkTable != null);

      CDebugAssert(pTestEngineInstance->bAutomaticMode == W_FALSE);
      CDebugAssert(pTestEngineInstance->nRunningTestId == 0);
      CDebugAssert(pTestEngineInstance->nResultErrorCode == 0);
      CDebugAssert(pTestEngineInstance->nResultDataLength == 0);
      CDebugAssert(pTestEngineInstance->nRunningInteraction == 0);
      CDebugAssert(pTestEngineInstance->pExecuteRemoteResultBuffer == null);
      CDebugAssert(pTestEngineInstance->nExecuteRemoteResultResultBufferLength == 0);

      CDebugAssert(pTestEngineInstance->sExecuteContext.pInputData == null);
      CDebugAssert(pTestEngineInstance->sExecuteContext.nInputDataLength == 0);
      CDebugAssert(pTestEngineInstance->sExecuteContext.nInputParameter == 0);
      CDebugAssert(pTestEngineInstance->sExecuteContext.pApplicationData == null);
      break;
   case P_TEST_STATE_EXECUTION:
      PDebugTrace("static_PTestSwitchState: P_TEST_STATE_EXECUTION");
      /* Checking */
      CDebugAssert(nOldState == P_TEST_STATE_LOADED);
      CDebugAssert(pTestEngineInstance->sBundle.pBundleData != null);
      CDebugAssert(pTestEngineInstance->sBundle.nBundleDataLength != 0);
      CDebugAssert(pTestEngineInstance->sBundle.pTestLinkTable != null);

      CDebugAssert(pTestEngineInstance->nRunningTestId != 0);
      CDebugAssert(pTestEngineInstance->nResultErrorCode == 0);
      CDebugAssert(pTestEngineInstance->nResultDataLength == 0);
      CDebugAssert(pTestEngineInstance->nRunningInteraction == 0);
      CDebugAssert(pTestEngineInstance->pExecuteRemoteResultBuffer == null);
      CDebugAssert(pTestEngineInstance->nExecuteRemoteResultResultBufferLength == 0);
      break;
   case P_TEST_STATE_RESULT_SET:
      PDebugTrace("static_PTestSwitchState: P_TEST_STATE_RESULT_SET");
      /* Checking */
      CDebugAssert(nOldState == P_TEST_STATE_EXECUTION);
      CDebugAssert(pTestEngineInstance->sBundle.pBundleData != null);
      CDebugAssert(pTestEngineInstance->sBundle.nBundleDataLength != 0);
      CDebugAssert(pTestEngineInstance->sBundle.pTestLinkTable != null);

      CDebugAssert(pTestEngineInstance->nRunningTestId != 0);
      CDebugAssert(pTestEngineInstance->nRunningInteraction == 0);
      CDebugAssert(pTestEngineInstance->pExecuteRemoteResultBuffer == null);
      CDebugAssert(pTestEngineInstance->nExecuteRemoteResultResultBufferLength == 0);
      break;
   case P_TEST_STATE_ERROR_TEST_IMPLEMENTATION:
      PDebugTrace("static_PTestSwitchState: P_TEST_STATE_ERROR_TEST_IMPLEMENTATION");
      /* Checking */
      CDebugAssert((nOldState == P_TEST_STATE_EXECUTION) || (nOldState == P_TEST_STATE_RESULT_SET));
      CDebugAssert(pTestEngineInstance->sBundle.pBundleData != null);
      CDebugAssert(pTestEngineInstance->sBundle.nBundleDataLength != 0);
      CDebugAssert(pTestEngineInstance->sBundle.pTestLinkTable != null);

      CDebugAssert(pTestEngineInstance->nRunningTestId != 0);
      break;
   default:
      PDebugError("static_PTestSwitchState: wrong status value");
      CDebugAssert(W_FALSE);
   }

   pTestEngineInstance->nState = nState;
}

/**
 * Sets the test in error.
 *
 * @param[in]  pContext  Context of the test instance.
 *
 * @param[in]  pMessage  The error message.
 **/
static void static_PTestSetProgrammingError(
            tContext* pContext,
            const char* pMessage)
{
   tTestEngineInstance* pTestEngineInstance = PContextGetTestEngineInstance( pContext );
   uint32_t nLength = PUtilAsciiStringLength(pMessage);

   PDebugError("static_PTestSetProgrammingError: %s", pMessage);

   if((pTestEngineInstance->nState == P_TEST_STATE_EXECUTION)
   || (pTestEngineInstance->nState == P_TEST_STATE_RESULT_SET))
   {
      pTestEngineInstance->nResultErrorCode = P_TEST_RESULT_ERROR_TEST_IMPLEMENTATION;

      if(nLength > sizeof(pTestEngineInstance->aResultData))
      {
         /* Truncate the message to fit the buffer length */
         nLength = sizeof(pTestEngineInstance->aResultData);
      }
      CMemoryCopy(pTestEngineInstance->aResultData, pMessage, nLength);
      pTestEngineInstance->nResultDataLength = nLength;

      static_PTestSwitchState(pTestEngineInstance, P_TEST_STATE_ERROR_TEST_IMPLEMENTATION);
   }
   else if(pTestEngineInstance->nState == P_TEST_STATE_ERROR_TEST_IMPLEMENTATION)
   {
      /* test already in programming error state, append the message */
      if(nLength != 0)
      {
         if(pTestEngineInstance->nResultDataLength + nLength + 1 > sizeof(pTestEngineInstance->aResultData))
         {
            /* Truncate the message to fit the buffer length */
            nLength = sizeof(pTestEngineInstance->aResultData) - pTestEngineInstance->nResultDataLength - 1;
         }

         if(nLength != 0)
         {
            pTestEngineInstance->aResultData[pTestEngineInstance->nResultDataLength++] = 0x0A;
            CMemoryCopy(&pTestEngineInstance->aResultData[pTestEngineInstance->nResultDataLength], pMessage, nLength);
            pTestEngineInstance->nResultDataLength += nLength;
         }
      }
   }
   else
   {
      PDebugError("static_PTestSetProgrammingError: wrong state");
   }
}

/**
 * @brief   Sends a response.
 *
 * @param[in]  pContext  Context of the test instance.
 *
 * @param[in]  nCommandCode  The command code.
 *
 * @param[in]  nDataLength  Length in byte of the value to store in the buffer.
 **/
static void static_PTestSendResponse(
            tContext* pContext,
            uint8_t nCommandCode,
            uint32_t nDataLength )
{
   tTestEngineInstance* pTestEngineInstance = PContextGetTestEngineInstance( pContext );
   uint32_t nCrc;
   uint8_t* pBuffer = pTestEngineInstance->pResponseBuffer;

   PDebugTrace("static_PTestSendResponse( %d )", nCommandCode);

   CDebugAssert(pTestEngineInstance->pResponseBuffer != null);
   CDebugAssert(pTestEngineInstance->pResponseDataBuffer != null);

   pTestEngineInstance->pResponseBuffer = null;
   pTestEngineInstance->pResponseDataBuffer = null;

   /* Type of the command */
   pBuffer[0] = nCommandCode;

   /* Result data length */
   PUtilWriteUint32ToBigEndianBuffer( nDataLength, &pBuffer[1] );

   /* Compute CRC */
   nCrc = PUtilComputeCrc32(0, pBuffer, 5+nDataLength );
   PUtilWriteUint32ToBigEndianBuffer( nCrc, &pBuffer[5+nDataLength] );

   /* Call the test server callback */
   PDFCPostContext3(
      &pTestEngineInstance->sResponseCallbackContext,
      9+nDataLength, W_SUCCESS );
}

/**
 * @brief   Sends a standard response.
 *
 * @param[in]  pContext  Context of the test instance.
 *
 * @param[in]  nCommandCode  The command code.
 *
 * @param[in]  nErrorCode  The error code.
 **/
static void static_PTestSendStandardResponse(
            tContext* pContext,
            uint8_t nCommandCode,
            uint8_t nErrorCode )
{
   tTestEngineInstance* pTestEngineInstance = PContextGetTestEngineInstance( pContext );

   PDebugTrace("static_PTestSendStandardResponse( %d )", nCommandCode);

   CDebugAssert(pTestEngineInstance->pResponseDataBuffer != null);

   pTestEngineInstance->pResponseDataBuffer[0] = nErrorCode;

   static_PTestSendResponse(pContext, nCommandCode, 1 );
}

/**
 * @brief   Reads the bundle data to get the test structure.
 *
 * @param[in]  pContext  Current context.
 *
 * @param[in]  pBundleData  Bundle data.
 *
 * @param[in]  nBundleDataLength  The bundle data length in bytes
 *
 * @param[in]  nUUIDPosition  The bundle position where starts the UUID
 **/
static bool_t static_PTestReadBundle(
            tTestBundle* pBundle,
            const uint8_t* pBundleData,
            uint32_t nBundleDataLength,
            uint32_t nUUIDPosition )
{
   static const uint8_t pUUID[] = P_TEST_INTERFACE_UUID;
   tWTestEntryPoint* pTestEntryPoint;
   tTestLinkTable* pLinkTable;
   uintptr_t nValue;
   uint8_t i = 0;

   PDebugTrace("static_PTestReadBundle()");

   CDebugAssert( pBundle != null );

   /* Create the complete buffer */
   pBundle->pBundleData = (uint8_t *)CMemoryAlloc( nBundleDataLength );
   if ( pBundle->pBundleData == null )
   {
      PDebugError("static_PTestReadBundle: could not allocate the buffer");
      return W_FALSE;
   }

   /* copy the bundle data */
   CMemoryCopy(pBundle->pBundleData, pBundleData, nBundleDataLength);
   pBundle->nBundleDataLength = nBundleDataLength;

   pBundleData = pBundle->pBundleData;

   if(nUUIDPosition + sizeof(tTestLinkTable) >= nBundleDataLength)
   {
      PDebugError("static_PTestReadBundle: wrong position for the UUID");
      goto return_error;
   }

   pLinkTable = (tTestLinkTable*)&pBundleData[nUUIDPosition];

   /* Compare the UUID values */
   if ( CMemoryCompare( &pLinkTable->aInterfaceUUID, pUUID, P_TEST_INTERFACE_UUID_LENGTH ) != 0x00 )
   {
      PDebugError("static_PTestReadBundle: did not find the test engine UUID");
      goto return_error;
   }

   /* Compare the processor code */
   if ( pLinkTable->nProcessorCode != P_TEST_PROCESSOR )
   {
      PDebugError("static_PTestReadBundle: wrong processor code");
      goto return_error;
   }

   /* Checks the global variable size (not for x64) */
   if ( (pLinkTable->nGlobalVariableSize != 0) && (pLinkTable->nProcessorCode != 0x05))
   {
      PDebugError("static_PTestReadBundle: test code uses global variables");
      goto return_error;
   }

   /* Store the buffer start address */
   pBundle->npStartAddress = (uintptr_t)(pBundle->pBundleData);

   /* Get the test structure address */
   pBundle->pTestLinkTable = pLinkTable;

   /* Get the UUID address */
   nValue = pBundle->pTestLinkTable->nDeltaReference;

   /* Get the difference between UUID compilation address and real address */
   if ( nValue > nUUIDPosition )
   {
      /* Store the offset information */
      pBundle->bOffsetOrder  = W_FALSE;
      CDebugAssert(nValue <= (uintptr_t) ((uint32_t) -1));
      pBundle->nOffset       = (uint32_t) nValue - nUUIDPosition;
   }
   else
   {
      /* Store the offset information */
      pBundle->bOffsetOrder  = W_TRUE;
      pBundle->nOffset       = nUUIDPosition - (uint32_t) nValue;
   }

   /* Check for the overlapping between the heap address and the link address */
   if ((pBundle->nOffset <= (pBundle->npStartAddress + pBundle->nBundleDataLength))
   && ((pBundle->nOffset + pBundle->nBundleDataLength) >= pBundle->npStartAddress))
   {
      PDebugError("static_PTestReadBundle: overlapping between the heap address and the link address");
      goto return_error;
   }

   /* Get the test function addresses */
   for ( i=0; i<pBundle->pTestLinkTable->nTestNumber; i++ )
   {
      /* Retrieve the address in the buffer */
      if ( pBundle->bOffsetOrder == W_FALSE )
      {
         pTestEntryPoint = (tWTestEntryPoint *)((uintptr_t)
             (pBundle->pTestLinkTable->aTestList[i].pEntryPoint)
            - pBundle->nOffset
            + pBundle->npStartAddress);
      }
      else
      {
         pTestEntryPoint = (tWTestEntryPoint *)((uintptr_t)
             (pBundle->pTestLinkTable->aTestList[i].pEntryPoint)
            + pBundle->nOffset
            + pBundle->npStartAddress);
      }

      /* Store the test function address */
      pBundle->pTestLinkTable->aTestList[i].pEntryPoint = pTestEntryPoint;
   }

   return W_TRUE;

return_error:

   CMemoryFree(pBundle->pBundleData);
   pBundle->pBundleData = null;
   pBundle->nBundleDataLength = 0;
   pBundle->pTestLinkTable = null;
   pBundle->npStartAddress = 0;
   pBundle->bOffsetOrder = W_FALSE;
   pBundle->nOffset = 0;
   return W_FALSE;
}

void static_PTestGetStatistics(
            tContext* pContext,
            uint8_t nFlags,
            tTestStatistics* pStatistics)
{
   tTestEngineInstance* pTestEngineInstance = PContextGetTestEngineInstance( pContext );
#if (P_BUILD_CONFIG == P_CONFIG_USER)
   tContextDriverMemoryStatistics sMemoryStatistics;
#endif /* P_CONFIG_USER */

   PDebugTrace("static_PTestGetStatistics()");

   CMemoryFill(pStatistics, 0, sizeof(tTestStatistics));

   pStatistics->nFlags = nFlags;

   if((nFlags & P_TEST_STATISTIC_MEMORY) != 0)
   {
      pStatistics->nPeakTestMemory = pTestEngineInstance->nPeakTestMemory;
      pStatistics->nCurrentTestMemory = pTestEngineInstance->nCurrentTestMemory;
      pStatistics->nBundleDataLength = pTestEngineInstance->sBundle.nBundleDataLength;

      /* Get the Memory statistics (user side) */
      CMemoryGetStatistics(
         &pStatistics->nCurrentUserMemory,
         &pStatistics->nPeakUserMemory );

#if (P_BUILD_CONFIG == P_CONFIG_USER)

      CMemoryFill(&sMemoryStatistics, 0, sizeof(sMemoryStatistics));

      PContextDriverGetMemoryStatistics(pContext,
         &sMemoryStatistics, sizeof(tContextDriverMemoryStatistics));

      /* @todo here, if the IOCTL failed, the sMemoryStatistics value will be set to zero */

      pStatistics->nCurrentDriverMemory = sMemoryStatistics.nCurrentMemory;
      pStatistics->nPeakDriverMemory = sMemoryStatistics.nPeakMemory;
#endif /* P_CONFIG_USER */
   }

   /* The handle count is always retreived */
   /*if((nFlags & P_TEST_STATISTIC_HANDLE) != 0)
   {*/
      PHandleGetCount(
         pContext,
         &pStatistics->nUserHandleNumber,
         &pStatistics->nDriverHandleNumber);
   /*}*/

   if((nFlags & P_TEST_STATISTIC_PROTOCOL) != 0)
   {
      PNALServiceDriverGetProtocolStatistics(pContext,
         &pStatistics->sProtocolStatistics, sizeof(tNALProtocolStatistics));
      /* @todo If the IOCTL failed, the protocols statistics are erroneous */
   }
}

static void static_PTestResetStatistics(
            tContext* pContext,
            uint8_t nFlags)
{
   PDebugTrace("static_PTestResetStatistics()");

   /* Get the statistic type */
   if((nFlags & P_TEST_STATISTIC_MEMORY) != 0)
   {
      /* Reset the memory statistics */
      CMemoryResetStatistics( );
#if (P_BUILD_CONFIG == P_CONFIG_USER)
      /* Reset the driver memory statistics */
      PContextDriverResetMemoryStatistics(pContext);
      /*@todo here, if the IOCTL failed, the driver stats are not reset */

#endif /* P_CONFIG_USER */
   }

   if((nFlags & P_TEST_STATISTIC_PROTOCOL) != 0)
   {
      /* Reset the protocol statistics */
      PNALServiceDriverResetProtocolStatistics(pContext);
      /*@todo here, if the IOCTL failed, the driver stats are not reset */
   }
}

/**
 * @brief   Gets the statistic information in a buffer.
 *
 * @param[in]  pContext  Context of the test instance.
 *
 * @param[in]  pStatistics  The source of the statisitic data.
 *
 * @param[in]  pBuffer  Buffer to fill in.
 *
 * @return  The length of the generated command.
 **/
static uint32_t static_PTestGetStatisticsData(
            tContext* pContext,
            tTestStatistics* pStatistics,
            uint8_t* pBuffer )
{
   uint8_t* pStartBuffer = pBuffer;
   uint32_t nCommandLength;

   PDebugTrace("static_PTestGetStatisticsData()");

   if((pStatistics->nFlags & P_TEST_STATISTIC_MEMORY) != 0)
   {
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->nCurrentUserMemory, pBuffer);
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->nPeakUserMemory, pBuffer );
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->nCurrentDriverMemory, pBuffer);
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->nPeakDriverMemory, pBuffer );
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->nCurrentTestMemory, pBuffer);
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->nPeakTestMemory, pBuffer );
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->nBundleDataLength, pBuffer );
   }

   if((pStatistics->nFlags & P_TEST_STATISTIC_PROTOCOL) != 0)
   {
      /* Fill in the return buffer */
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->sProtocolStatistics.nOSI5ReadMessageLost, pBuffer);
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->sProtocolStatistics.nOSI5ReadByteErrorCount, pBuffer);

      /* Fill in the return buffer */
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->sProtocolStatistics.nOSI6ReadMessageLost, pBuffer);
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->sProtocolStatistics.nOSI6ReadByteErrorCount, pBuffer);

      /* Fill in the return buffer */
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->sProtocolStatistics.nOSI4WindowSize, pBuffer);
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->sProtocolStatistics.nOSI4ReadPayload, pBuffer);
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->sProtocolStatistics.nOSI4ReadFrameLost, pBuffer);
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->sProtocolStatistics.nOSI4ReadByteErrorCount, pBuffer);
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->sProtocolStatistics.nOSI4WritePayload, pBuffer);
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->sProtocolStatistics.nOSI4WriteFrameLost, pBuffer);
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->sProtocolStatistics.nOSI4WriteByteErrorCount, pBuffer);

      /* Fill in the return buffer */
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->sProtocolStatistics.nOSI2FrameReadByteErrorCount, pBuffer);
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->sProtocolStatistics.nOSI2FrameReadByteTotalCount, pBuffer);
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->sProtocolStatistics.nOSI2FrameWriteByteTotalCount, pBuffer);
   }

   if((pStatistics->nFlags & P_TEST_STATISTIC_TIME) != 0)
   {
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->nCumulatedTime, pBuffer);
   }

   if((pStatistics->nFlags & P_TEST_STATISTIC_HANDLE) != 0)
   {
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->nUserHandleNumber, pBuffer);
      pBuffer += PUtilWriteUint32ToBigEndianBuffer( pStatistics->nDriverHandleNumber, pBuffer);
   }

   nCommandLength = (uint32_t)(pBuffer - pStartBuffer);
   CDebugAssert(nCommandLength <= (uint32_t)-1);

   return nCommandLength;
}

static tTestEngineInstance* static_PTestCheckTestActive(
            tContext* pContext,
            const char* pFunctionName)
{
   tTestEngineInstance* pTestEngineInstance = PContextGetTestEngineInstance( pContext );

   PDebugTrace("%s()", pFunctionName);

   CDebugAssert( pTestEngineInstance != null );

   if((pTestEngineInstance->nState != P_TEST_STATE_EXECUTION)
   && (pTestEngineInstance->nState != P_TEST_STATE_RESULT_SET)
   && (pTestEngineInstance->nState != P_TEST_STATE_ERROR_TEST_IMPLEMENTATION))
   {
      static_PTestSetProgrammingError(pContext, "Cannot call a test API function when no test is being executed");
      return (tTestEngineInstance*)null;
   }

   return pTestEngineInstance;
}

/* See header file */
const void* PTestGetConstAddress(
            tContext* pContext,
            const void* pConstData )
{
   tTestEngineInstance* pTestEngineInstance = static_PTestCheckTestActive(pContext, "PTestGetConstAddress");
   uintptr_t npNewConstData;

   if(pTestEngineInstance == null)
   {
      return null;
   }

   /* Check if the test bundle is not already compiled in PIC */
   if(((uintptr_t)(pConstData) >= pTestEngineInstance->sBundle.npStartAddress)
   && ((uintptr_t)(pConstData) < pTestEngineInstance->sBundle.npStartAddress + pTestEngineInstance->sBundle.nBundleDataLength))
   {
      return pConstData;
   }

   /* Check the offset order */
   if ( pTestEngineInstance->sBundle.bOffsetOrder == W_FALSE )
   {
      /* Return the correct address */
      npNewConstData = (uintptr_t)(pConstData)
               + pTestEngineInstance->sBundle.npStartAddress
               - pTestEngineInstance->sBundle.nOffset;
   }
   else
   {
      /* Return the correct address */
      npNewConstData = (uintptr_t)(pConstData)
               + pTestEngineInstance->sBundle.npStartAddress
               + pTestEngineInstance->sBundle.nOffset;
   }

   if((npNewConstData >= pTestEngineInstance->sBundle.npStartAddress)
   && (npNewConstData < pTestEngineInstance->sBundle.npStartAddress + pTestEngineInstance->sBundle.nBundleDataLength))
   {
      return (const void*)npNewConstData;
   }
   else
   {
      static_PTestSetProgrammingError(pContext, "WTestGetConstAddress() called with a wrong pointer");
      return null;
   }
}

/* See header file */
bool_t PTestIsInAutomaticMode(
            tContext* pContext )
{
   tTestEngineInstance* pTestEngineInstance = static_PTestCheckTestActive(pContext, "PTestIsInAutomaticMode");

   if(pTestEngineInstance == null)
   {
      return W_FALSE;
   }

   return pTestEngineInstance->bAutomaticMode;
}

/* See header file */
void PTestTraceInfo(
            tContext* pContext,
            const char * pMessage,
            va_list args )
{
#ifdef P_TRACE_ACTIVE
   tTestEngineInstance* pTestEngineInstance = static_PTestCheckTestActive(pContext, "PTestTraceInfo");

   if(pTestEngineInstance == null)
   {
      return;
   }

   /* Display the trace */
   CDebugPrintTrace(
      pTestEngineInstance->aTestName,
      P_TRACE_TRACE,
      pMessage,
      args );
#endif /* P_TRACE_ACTIVE */
}

/* See header file */
void PTestTraceWarning(
            tContext* pContext,
            const char * pMessage,
            va_list args )
{
#ifdef P_TRACE_ACTIVE
   tTestEngineInstance* pTestEngineInstance = static_PTestCheckTestActive(pContext, "PTestTraceWarning");

   if(pTestEngineInstance == null)
   {
      return;
   }

   /* Display the trace */
   CDebugPrintTrace(
      pTestEngineInstance->aTestName,
      P_TRACE_WARNING,
      pMessage,
      args );
#endif /* P_TRACE_ACTIVE */
}

/* See header file */
void PTestTraceError(
            tContext* pContext,
            const char * pMessage,
            va_list args )
{
#ifdef P_TRACE_ACTIVE
   tTestEngineInstance* pTestEngineInstance = static_PTestCheckTestActive(pContext, "PTestTraceError");

   if(pTestEngineInstance == null)
   {
      return;
   }

   /* Display the trace */
   CDebugPrintTrace(
      pTestEngineInstance->aTestName,
      P_TRACE_WARNING,
      pMessage,
      args );
#endif /* P_TRACE_ACTIVE */
}

/* See header file */
void PTestTraceBuffer(
            tContext* pContext,
            const uint8_t* pBuffer,
            uint32_t nLength )
{
#ifdef P_TRACE_ACTIVE
   tTestEngineInstance* pTestEngineInstance = static_PTestCheckTestActive(pContext, "PTestTraceBuffer");

   if(pTestEngineInstance == null)
   {
      return;
   }

   /* Display the trace */
   CDebugPrintTraceBuffer(
      pTestEngineInstance->aTestName,
      P_TRACE_TRACE,
      pBuffer,
      nLength );
#endif /* P_TRACE_ACTIVE */
}

/* See header file */
void * PTestAlloc(
            tContext* pContext,
            uint32_t nSize )
{
   void* pBuffer = null;
   tTestEngineInstance* pTestEngineInstance = static_PTestCheckTestActive(pContext, "PTestAlloc");

   if(pTestEngineInstance == null)
   {
      return null;
   }

   if(nSize != 0)
   {
      /* Call the system API */
      if((pBuffer = CMemoryAlloc( nSize + sizeof(uint32_t))) != null)
      {
         *((uint32_t*)pBuffer) = nSize;
         pBuffer = (void*)( ((uint8_t*)pBuffer) + sizeof(uint32_t));

         CMemoryFill(pBuffer, 0, nSize);

         pTestEngineInstance->nCurrentTestMemory += nSize;  /* Do no include the memory header */
         pTestEngineInstance->nTrueCurrentTestMemory += nSize + sizeof(uint32_t);  /* With the memory header */
         if(pTestEngineInstance->nCurrentTestMemory > pTestEngineInstance->nPeakTestMemory)
         {
            pTestEngineInstance->nPeakTestMemory = pTestEngineInstance->nCurrentTestMemory;
         }
      }
   }

   return pBuffer;
}

/* See header file */
void PTestFree(
            tContext* pContext,
            void * pBuffer )
{
   tTestEngineInstance* pTestEngineInstance = static_PTestCheckTestActive(pContext, "PTestFree");

   if(pTestEngineInstance == null)
   {
      return;
   }

   if( pBuffer != null )
   {
      uint32_t nSize;

      pBuffer = (void*)( ((uint8_t*)pBuffer) - sizeof(uint32_t));
      nSize = *((uint32_t*)pBuffer);

      if(nSize == 0xCCCCCCCC)
      {
         static_PTestSetProgrammingError(pContext, "PTestFree() called on a buffer already freed");
         return;
      }
      if((pTestEngineInstance->nCurrentTestMemory < nSize)
      || (pTestEngineInstance->nTrueCurrentTestMemory < nSize + sizeof(uint32_t)))
      {
         static_PTestSetProgrammingError(pContext, "PTestFree() called on a wrong buffer");
         return;
      }

      /* To be sure the buffer is not reused */
      CMemoryFill(pBuffer, 0xCC, nSize + sizeof(uint32_t));

      /* Call the system API */
      CMemoryFree ( pBuffer );

      pTestEngineInstance->nCurrentTestMemory -= nSize; /* Do not include the memory header */
      pTestEngineInstance->nTrueCurrentTestMemory -= nSize + sizeof(uint32_t); /* With the memory header */
   }
}

static void static_PTimerExpired(
                  tContext* pContext,
                  void * pCallbackParameter)
{
   tTestEngineInstance* pTestEngineInstance = static_PTestCheckTestActive(pContext, "static_PTimerExpired");

   if (pTestEngineInstance == null)
   {
      return;
   }

   pTestEngineInstance->nCurrentExpiryTime = 0;
   PDFCPostContext1(& pTestEngineInstance->sTimerCallbackContext);
}

/* See header file */
void PTestSetTimer(
                  tContext* pContext,
                  uint32_t nTimeout,
                  tPBasicGenericCompletionFunction* pCallback,
                  void* pCallbackParameter )
{
   tTestEngineInstance* pTestEngineInstance = static_PTestCheckTestActive(pContext, "PTestSetTimer");

   if(pTestEngineInstance == null)
   {
      return;
   }

   if(nTimeout == 0)
   {
      pTestEngineInstance->nCurrentExpiryTime = 0;
      PMultiTimerCancelDriver(pContext, TIMER_T13_USER_TEST);
      /* @todo what can we do here if the driver call failed */

      PDFCFlushCall(& pTestEngineInstance->sTimerCallbackContext);
   }
   else
   {
      PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &pTestEngineInstance->sTimerCallbackContext);

      PMultiTimerSetDriver(pContext, TIMER_T13_USER_TEST,
                  PNALServiceDriverGetCurrentTime(pContext) + nTimeout,
                  static_PTimerExpired, null );
      /* @todo what can we do here if the driver call failed */
   }
}

/* See header file */
uint32_t PTestGetCurrentTime(
                  tContext* pContext )
{
   tTestEngineInstance* pTestEngineInstance = static_PTestCheckTestActive(pContext, "PTestGetCurrentTime");

   if(pTestEngineInstance == null)
   {
      return 0;
   }

   return PNALServiceDriverGetCurrentTime(pContext);
   /* @todo what can we do here if the driver call failed */
}

/* See header file */
void * WTestCopy(
            void * pDestination,
            void * pSource,
            uint32_t nLength )
{
   /* Call the system API */
   return CMemoryCopy( pDestination, (const void*)(pSource), nLength );
}

/* See header file */
void * WTestMove(
            void * pDestination,
            void * pSource,
            uint32_t nLength )
{
   /* Call the system API */
   return CMemoryMove( pDestination, (const void*)(pSource), nLength );
}

/* See header file */
void WTestFill(
            void * pBuffer,
            uint8_t nValue,
            uint32_t nLength )
{
   /* Call the system API */
   CMemoryFill( pBuffer, nValue, nLength );
}

/* See header file */
int32_t WTestCompare(
            const void * pBuffer1,
            const void * pBuffer2,
            uint32_t nLength )
{
   /* Call the system API */
   return CMemoryCompare( pBuffer1, pBuffer2, nLength );
}

/* See header file */
uint32_t WTestConvertUTF16ToUTF8(
                  uint8_t* pDestUtf8,
                  const char16_t* pSourceUtf16,
                  uint32_t nSourceCharLength )
{
   return PUtilConvertUTF16ToUTF8( pDestUtf8, pSourceUtf16, nSourceCharLength );
}

/* See header file */
uint32_t WTestWriteHexaUint8(
               char16_t* pStringBuffer,
               uint8_t nValue)
{
   return PUtilWriteHexaUint8(pStringBuffer, nValue);
}

/* See header file */
uint32_t WTestWriteHexaUint32(
               char16_t* pStringBuffer,
               uint32_t nValue)
{
   return PUtilWriteHexaUint32(pStringBuffer, nValue);
}

/* See header file */
uint32_t WTestWriteDecimalUint32(
               char16_t* pStringBuffer,
               uint32_t nValue)
{
   return PUtilWriteDecimalUint32(pStringBuffer, nValue);
}

/* See header file */
uint32_t WTestStringLength(
               const char16_t* pString )
{
   return PUtilStringLength( pString );
}

/* See header file */
int32_t WTestStringCompare(
               const char16_t* pString1,
               const char16_t* pString2 )
{
   return PUtilStringCompare( pString1, pString2 );
}

/* See header file */
char16_t* WTestStringCopy(
               char16_t* pBuffer,
               uint32_t* pPos,
               const char16_t* pString)
{
   return PUtilStringCopy( pBuffer, pPos, pString);
}

/* See header file */
void PTestNotifyEnd(
            tContext* pContext )
{
   tTestEngineInstance* pTestEngineInstance = PContextGetTestEngineInstance( pContext );
   tTestStatistics sEndStatistics;
   uint32_t nDataLength = 0;

   PDebugTrace("PTestNotifyEnd()");

   CDebugAssert( pTestEngineInstance != null );

   /* Check if a result has been received */
   switch ( pTestEngineInstance->nState )
   {
      case P_TEST_STATE_RESULT_SET:
         break;
      case P_TEST_STATE_EXECUTION:
         static_PTestSetProgrammingError(pContext, "WTestNotifyEnd() is called but the test result is not set with WTestSetResult()");
         break;
      case P_TEST_STATE_ERROR_TEST_IMPLEMENTATION:
         static_PTestSetProgrammingError(pContext, "WTestNotifyEnd() is called but the test is already in programming error");
         break;
      case P_TEST_STATE_STOPPED:
      case P_TEST_STATE_INIT:
      case P_TEST_STATE_LOADED:
      default:
         PDebugError("PTestNotifyEnd: wrong state");
         return;
   }

   if((pTestEngineInstance->sStartStatistics.nFlags & P_TEST_STATISTIC_TIME) != 0)
   {
      pTestEngineInstance->sStartStatistics.nCumulatedTime +=
         PNALServiceDriverGetCurrentTime(pContext) - pTestEngineInstance->sStartStatistics.nStartTime;
      /* @todo what can we do here if the driver call failed */
   }

   /* Checks if the test has released the memeory */
   if(pTestEngineInstance->nCurrentTestMemory != 0)
   {
      static_PTestSetProgrammingError(pContext, "WTestNotifyEnd() is called but some memory is still allocated by the test.");
   }

   static_PTestGetStatistics(pContext,
      pTestEngineInstance->sStartStatistics.nFlags, &sEndStatistics);

   sEndStatistics.nCumulatedTime = pTestEngineInstance->sStartStatistics.nCumulatedTime;
   pTestEngineInstance->sStartStatistics.nCumulatedTime = 0;

   pTestEngineInstance->pResponseDataBuffer[nDataLength++] = P_TEST_ENGINE_RESULT_OK;

   nDataLength += static_PTestGetStatisticsData( pContext,
            &pTestEngineInstance->sStartStatistics, &pTestEngineInstance->pResponseDataBuffer[nDataLength] );
   nDataLength += static_PTestGetStatisticsData( pContext,
            &sEndStatistics, &pTestEngineInstance->pResponseDataBuffer[nDataLength] );

   /* Change on the fly the result code if the numbers of handle before and after the test are different */
   if(sEndStatistics.nUserHandleNumber != pTestEngineInstance->sStartStatistics.nUserHandleNumber)
   {
      PDebugError("PTestNotifyEnd: The handles numbers before (%d) and after (%d) the test are different",
                  pTestEngineInstance->sStartStatistics.nUserHandleNumber,
                  sEndStatistics.nUserHandleNumber);

      static_PTestSetProgrammingError(pContext, "WTestNotifyEnd() is called when handles are still open");
   }

   if((9 + 4 + nDataLength + pTestEngineInstance->nResultDataLength) > W_TEST_SERVER_RESPONSE_MAX_LENGTH)
   {
      static_PTestSetProgrammingError(pContext, "Result data too large");
   }

   /* Adding the result */
   nDataLength += PUtilWriteUint32ToBigEndianBuffer( pTestEngineInstance->nResultErrorCode, &pTestEngineInstance->pResponseDataBuffer[nDataLength]);

   if(pTestEngineInstance->nResultDataLength != 0)
   {
      /* Copy the result data */
      CMemoryCopy(&pTestEngineInstance->pResponseDataBuffer[nDataLength],
         pTestEngineInstance->aResultData,
         pTestEngineInstance->nResultDataLength);

      nDataLength += pTestEngineInstance->nResultDataLength;
   }

   /* Send the response */
   static_PTestSendResponse(
      pContext,
      P_TEST_CMD_EXECUTE,
      nDataLength);

   static_PTestSwitchState(pTestEngineInstance, P_TEST_STATE_LOADED);

   PContextTriggerEventPump(pContext);
}

/* See header file */
void PTestSetErrorResult(
         tContext* pContext,
         uint32_t nResult,
         const char* pMessage)
{
   uint32_t nResultDataLength = PUtilAsciiStringLength(pMessage);

   if(nResult == P_TEST_RESULT_PASSED)
   {
      static_PTestSetProgrammingError(pContext, "PTestSetErrorResult() wrong result code");
   }

   PTestSetResult(pContext, nResult,
            pMessage, nResultDataLength );
}

/* See header file */
void PTestSetResult(
            tContext* pContext,
            uint32_t nResult,
            const void * pResultData,
            uint32_t nResultDataLength )
{
   tTestEngineInstance* pTestEngineInstance = PContextGetTestEngineInstance( pContext );

   PDebugTrace("PTestSetResult()");

   CDebugAssert( pTestEngineInstance != null );

   /* Check if a result has been received */
   switch ( pTestEngineInstance->nState )
   {
      case P_TEST_STATE_EXECUTION:
         break;
      case P_TEST_STATE_RESULT_SET:
         static_PTestSetProgrammingError(pContext, "PTestSetResult() the result is set twice");
         break;
      case P_TEST_STATE_ERROR_TEST_IMPLEMENTATION:
         static_PTestSetProgrammingError(pContext, "PTestSetResult() is called but the test is already in programming error");
         break;
      case P_TEST_STATE_STOPPED:
      case P_TEST_STATE_INIT:
      case P_TEST_STATE_LOADED:
      default:
         PDebugError("PTestSetResult: wrong state");
         return;
   }

   if((nResult != P_TEST_RESULT_PASSED)
   && (nResult != P_TEST_RESULT_FAILED)
   && (nResult != P_TEST_RESULT_CANCELLED)
   && (nResult != P_TEST_RESULT_NOT_RELEVANT)
   && (nResult != P_TEST_RESULT_ERROR_IN_TEST)
   && (nResult != P_TEST_RESULT_ERROR_INTERACTION)
   && (nResult != P_TEST_RESULT_ERROR_INITIALIZATION))
   {
      /* P_TEST_RESULT_ERROR_TEST_IMPLEMENTATION cannot be set by the test implementation */
      static_PTestSetProgrammingError(pContext, "PTestSetResult() wrong result code");
   }

   if(((pResultData == null) && (nResultDataLength != 0))
   || ((pResultData != null) && (nResultDataLength == 0)))
   {
      static_PTestSetProgrammingError(pContext, "PTestSetResult() bad result buffer parameters");
   }

   if(nResultDataLength > sizeof(pTestEngineInstance->aResultData))
   {
      static_PTestSetProgrammingError(pContext, "PTestSetResult() result buffer is too large");
   }

   if(pTestEngineInstance->nState == P_TEST_STATE_EXECUTION)
   {
      pTestEngineInstance->nResultErrorCode = nResult;
      pTestEngineInstance->nResultDataLength = nResultDataLength;

      /* Store the result data */
      if (  ( nResultDataLength != 0 )
         && ( pResultData != null ) )
      {
         CMemoryCopy(
            pTestEngineInstance->aResultData,
            pResultData,
            nResultDataLength );
      }

      static_PTestSwitchState(pTestEngineInstance, P_TEST_STATE_RESULT_SET);
   }
}

/* See header file */
void PTestMessageBox(
            tContext* pContext,
            uint32_t nFlags,
            const char * pMessage,
            uint32_t nAutomaticResult,
            tPTestMessageBoxCompleted * pCallback,
            void * pCallbackParameter )
{
   uint32_t nMesssageLength;
   uint32_t nDataLength = 0;
   tDFCCallbackContext aErrorCallback;
   tTestEngineInstance* pTestEngineInstance = static_PTestCheckTestActive(pContext, "PTestMessageBox");

   if(pTestEngineInstance == null)
   {
      goto return_error;
   }

   /* Check if an interaction is pending */
   if ( pTestEngineInstance->nRunningInteraction != 0 )
   {
      PDebugError("Message box - a message is already displayed");
      goto return_error;
   }

   PDFCFillCallbackContext(
                  pContext,
                  (tDFCCallback*)pCallback,
                  pCallbackParameter,
                  &pTestEngineInstance->aInteractionCallback );

   /* Check if the test is in automatic mode */
   if ( pTestEngineInstance->bAutomaticMode != W_FALSE )
   {
      PDebugTrace("Message box - a test is running in automatic mode");
      /* Call the callback with the expected result */
      PDFCPostContext2(
         &pTestEngineInstance->aInteractionCallback,
         nAutomaticResult );

      return;
   }

   /* Set the displayed message box/card flag */
   pTestEngineInstance->nRunningInteraction = P_TEST_RCMD_MESSAGE_BOX;

   if((pTestEngineInstance->sStartStatistics.nFlags & P_TEST_STATISTIC_TIME) != 0)
   {
      pTestEngineInstance->sStartStatistics.nCumulatedTime +=
         PNALServiceDriverGetCurrentTime(pContext) - pTestEngineInstance->sStartStatistics.nStartTime;
      /* @todo what can we do here if the driver call failed */
   }

   /* Store the callback and callback parameter */

   /* - flags */
   nDataLength += PUtilWriteUint32ToBigEndianBuffer(
      nFlags,
      &pTestEngineInstance->pResponseDataBuffer[nDataLength]);

   /* - message */
   nMesssageLength = PUtilAsciiStringLength(pMessage);
   if ( nMesssageLength != 0)
   {
      CMemoryCopy(
         &pTestEngineInstance->pResponseDataBuffer[nDataLength],
         (void*)pMessage,
         nMesssageLength );
      nDataLength += nMesssageLength;
   }

   static_PTestSendResponse(
      pContext,
      P_TEST_RCMD_MESSAGE_BOX,
      nDataLength);

   return;

return_error:

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback, pCallbackParameter,
      &aErrorCallback );

   PDFCPostContext2(
      &pTestEngineInstance->aInteractionCallback,
      P_TEST_MSG_BOX_RESULT_CANCEL);
}

/* See header file */
tTestExecuteContext * PTestGetExecuteContext( tContext* pContext )
{
   tTestEngineInstance* pTestEngineInstance = static_PTestCheckTestActive(pContext, "PTestGetExecuteContext");

   if(pTestEngineInstance == null)
   {
      return (tTestExecuteContext *)null;
   }

   return &(pTestEngineInstance->sExecuteContext);
}

/* See header file */
void PTestExecuteRemoteFunction(
                                tContext* pContext,
                                const char* pFunctionIdentifier,
                                uint32_t nParameter,
                                const uint8_t* pParameterBuffer,
                                uint32_t nParameterBufferLength,
                                uint8_t* pResultBuffer,
                                uint32_t nResultBufferLength,
                                tPBasicGenericDataCallbackFunction* pCallback,
                                void* pCallbackParameter )
{
   uint32_t nDataLength;
   uint32_t nFunctionNameLength;
   tTestEngineInstance* pTestEngineInstance = static_PTestCheckTestActive(pContext, "PTestExecuteRemoteFunction");
   tDFCCallbackContext aErrorCallback;

   if(pTestEngineInstance == null)
   {
      goto return_error;
   }

   nFunctionNameLength = PUtilAsciiStringLength(pFunctionIdentifier);
   if(nFunctionNameLength == 0)
   {
      static_PTestSetProgrammingError(pContext, "PTestExecuteRemoteFunction() called without object name");
      goto return_error;
   }

   /* Check if an interaction is pending */
   if ( pTestEngineInstance->nRunningInteraction != 0 )
   {
      static_PTestSetProgrammingError(pContext, "PTestExecuteRemoteFunction() called while an interaction is pending");
      goto return_error;
   }

   /* Set the displayed message box/card flag */
   pTestEngineInstance->nRunningInteraction = P_TEST_RCMD_REMOTE_EXECUTE;

   if((pTestEngineInstance->sStartStatistics.nFlags & P_TEST_STATISTIC_TIME) != 0)
   {
      pTestEngineInstance->sStartStatistics.nCumulatedTime +=
         PNALServiceDriverGetCurrentTime(pContext) - pTestEngineInstance->sStartStatistics.nStartTime;
      /* @todo what can we do here if the driver call failed */
   }

   /*Configure the result buffer*/
   pTestEngineInstance->pExecuteRemoteResultBuffer= pResultBuffer;
   pTestEngineInstance->nExecuteRemoteResultResultBufferLength= nResultBufferLength;

   /* Send the request to the test server */

   nDataLength=0;

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &pTestEngineInstance->aInteractionCallback );

   /* - parameter */
   nDataLength += PUtilWriteUint32ToBigEndianBuffer(
      nParameter,
      &pTestEngineInstance->pResponseDataBuffer[nDataLength]);

   /* function name string length */
   nDataLength += PUtilWriteUint32ToBigEndianBuffer(
             nFunctionNameLength,
             &pTestEngineInstance->pResponseDataBuffer[nDataLength]);

   /* function name string */
   if(nFunctionNameLength != 0)
   {
      CMemoryCopy(
         &pTestEngineInstance->pResponseDataBuffer[nDataLength],
         (void*)pFunctionIdentifier,
         nFunctionNameLength);
      nDataLength+=  nFunctionNameLength;
   }

   /* function name string nParameterBufferLength */
   nDataLength+=PUtilWriteUint32ToBigEndianBuffer(
      nParameterBufferLength,
      &pTestEngineInstance->pResponseDataBuffer[nDataLength]);

   if(nParameterBufferLength > 0)
   {
      CMemoryCopy(
         &pTestEngineInstance->pResponseDataBuffer[nDataLength],
         (void*)pParameterBuffer,
         nParameterBufferLength);
      nDataLength += nParameterBufferLength;
   }

   static_PTestSendResponse(
      pContext,
      P_TEST_RCMD_REMOTE_EXECUTE,
      nDataLength);

   return;

return_error:

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback, pCallbackParameter,
      &aErrorCallback );

   PDFCPostContext3(
      &pTestEngineInstance->aInteractionCallback,
      0,
      0);
}

/* See header file */
void PTestPresentObject(
            tContext* pContext,
            const char * pObjectName,
            const char * pOperatorMessage,
            uint32_t nDistance,
            tPBasicGenericCallbackFunction * pCallback,
            void * pCallbackParameter )
{
   uint32_t nOperatorMessageLength = 0;
   uint32_t nObjectNameLength = 0;
   uint32_t nDataLength = 0;
   tDFCCallbackContext aErrorCallback;
   tTestEngineInstance* pTestEngineInstance = static_PTestCheckTestActive(pContext, "PTestPresentObject");

   if(pTestEngineInstance == null)
   {
      goto return_error;
   }

   nObjectNameLength = PUtilAsciiStringLength(pObjectName);
   if(nObjectNameLength == 0)
   {
      static_PTestSetProgrammingError(pContext, "PTestPresentObject() called without object name");
      goto return_error;
   }

   /* Check if an interaction is pending */
   if ( pTestEngineInstance->nRunningInteraction != 0 )
   {
      static_PTestSetProgrammingError(pContext, "PTestPresentObject() called while an interaction is pending");
      goto return_error;
   }

   PDFCFillCallbackContext(
                  pContext,
                  (tDFCCallback*)pCallback, pCallbackParameter,
                  &pTestEngineInstance->aInteractionCallback );

   /* Set the displayed message box/card flag */
   pTestEngineInstance->nRunningInteraction = P_TEST_RCMD_PRESENT_OBJECT;

   if((pTestEngineInstance->sStartStatistics.nFlags & P_TEST_STATISTIC_TIME) != 0)
   {
      pTestEngineInstance->sStartStatistics.nCumulatedTime +=
         PNALServiceDriverGetCurrentTime(pContext) - pTestEngineInstance->sStartStatistics.nStartTime;
      /* @todo what can we do here if the driver call failed */
   }

   /* Send the request to the test server */
   /* - request data length */
   nOperatorMessageLength = PUtilAsciiStringLength(pOperatorMessage);

   nDataLength += PUtilWriteUint32ToBigEndianBuffer(
      nObjectNameLength, &pTestEngineInstance->pResponseDataBuffer[nDataLength]);

   nDataLength += PUtilWriteUint32ToBigEndianBuffer(
      nOperatorMessageLength, &pTestEngineInstance->pResponseDataBuffer[nDataLength]);

   nDataLength += PUtilWriteUint32ToBigEndianBuffer(
      nDistance, &pTestEngineInstance->pResponseDataBuffer[nDataLength]);

   /* - request card name */
   if ( nObjectNameLength != 0 )
   {
      CMemoryCopy(
         &pTestEngineInstance->pResponseDataBuffer[nDataLength],
         (void*)pObjectName, nObjectNameLength );
      nDataLength += nObjectNameLength;
   }

   if ( nOperatorMessageLength != 0 )
   {
      CMemoryCopy(
         &pTestEngineInstance->pResponseDataBuffer[nDataLength],
         (void*)pOperatorMessage, nOperatorMessageLength );
      nDataLength += nOperatorMessageLength;
   }

   static_PTestSendResponse(
      pContext,
      P_TEST_RCMD_PRESENT_OBJECT,
      nDataLength);

   return;

return_error:

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback, pCallbackParameter,
      &aErrorCallback );

   PDFCPostContext2(
      &aErrorCallback,
      P_TEST_MSG_BOX_RESULT_CANCEL );
}

/* See header file */
void PTestRemoveObject(
            tContext* pContext,
            const char* pOperatorMessage,
            bool_t bSaveState,
            bool_t bCheckUnmodifiedState,
            tPBasicGenericCallbackFunction * pCallback,
            void * pCallbackParameter )
{
   uint32_t nOperatorMessageLength = 0;
   uint32_t nDataLength = 0;
   uint32_t nFlags = 0;
   tDFCCallbackContext aErrorCallback;
   tTestEngineInstance* pTestEngineInstance = static_PTestCheckTestActive(pContext, "PTestPresentObject");

   if(pTestEngineInstance == null)
   {
      goto return_error;
   }

   /* Check if an interaction is pending */
   if ( pTestEngineInstance->nRunningInteraction != 0 )
   {
      static_PTestSetProgrammingError(pContext, "PTestRemoveObject() called while an interaction is pending");
      goto return_error;
   }

   PDFCFillCallbackContext(
                  pContext,
                  (tDFCCallback*)pCallback, pCallbackParameter,
                  &pTestEngineInstance->aInteractionCallback );

   /* Set the displayed message box/card flag */
   pTestEngineInstance->nRunningInteraction = P_TEST_RCMD_REMOVE_OBJECT;

   if((pTestEngineInstance->sStartStatistics.nFlags & P_TEST_STATISTIC_TIME) != 0)
   {
      pTestEngineInstance->sStartStatistics.nCumulatedTime +=
         PNALServiceDriverGetCurrentTime(pContext) - pTestEngineInstance->sStartStatistics.nStartTime;
      /* @todo what can we do here if the driver call failed */
   }

   /* Send the request to the test server */
   /* - request data length */
   nOperatorMessageLength = PUtilAsciiStringLength(pOperatorMessage);

   nDataLength += PUtilWriteUint32ToBigEndianBuffer(
      nOperatorMessageLength, &pTestEngineInstance->pResponseDataBuffer[nDataLength]);

   if(bSaveState != W_FALSE)
   {
      nFlags |= 0x01;
   }
   if(bCheckUnmodifiedState != W_FALSE)
   {
      nFlags |= 0x02;
   }
   nDataLength += PUtilWriteUint32ToBigEndianBuffer(
      nFlags, &pTestEngineInstance->pResponseDataBuffer[nDataLength]);

   if ( nOperatorMessageLength != 0 )
   {
      CMemoryCopy(
         &pTestEngineInstance->pResponseDataBuffer[nDataLength],
         (void*)pOperatorMessage, nOperatorMessageLength );
      nDataLength += nOperatorMessageLength;
   }

   static_PTestSendResponse(
      pContext,
      P_TEST_RCMD_REMOVE_OBJECT,
      nDataLength);

   return;

return_error:

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback, pCallbackParameter,
      &aErrorCallback );

   PDFCPostContext2(
      &aErrorCallback,
      W_ERROR_CANCEL );
}

/* See Client API Specifications */
static void static_PTestExchangeMessage(
            tContext* pContext,
            uint8_t nCommandCode,
            const uint8_t* pCommandData,
            uint32_t nCommandDataLength,
            uint8_t* pResponseDataBuffer )
{
   /* Generic values */
   tTestEngineInstance* pTestEngineInstance = PContextGetTestEngineInstance( pContext );
   static const uint8_t  pProtocolVersion[] = P_TEST_PROTOCOL_VERSION;
   static const uint8_t  pUUID[] = P_TEST_INTERFACE_UUID;
   static const char* pProductName = P_TEST_PRODUCT_NAME;
   uint32_t nResponseDataLength = 0;

   /* If the test engine has not been initialised */
   /* and the current message is not an initialising one */
   if (  ( pTestEngineInstance->nState == P_TEST_STATE_STOPPED )
      && ( nCommandCode != P_TEST_CMD_INIT)
      && ( nCommandCode != P_TEST_CMD_PING) )
   {
      PDebugError("Exchange Message - initialize the test engine first");
      goto exchange_error;
   }

   /* Get the type of message */
   switch ( nCommandCode )
   {
      case P_TEST_CMD_PING:
         PDebugTrace("Exchange Message - PING");

         /* Check the length of the data */
         if ( nCommandDataLength != 0 )
         {
            PDebugError("Exchange Message - wrong length 0x%08X", nCommandDataLength );
            goto exchange_error;
         }

         /* Send the ping response */
         static_PTestSendStandardResponse(pContext, nCommandCode, P_TEST_ENGINE_RESULT_OK);
         break;

      case P_TEST_CMD_INIT:
         {
            uint32_t nBuildNumber = OPEN_NFC_BUILD_NUMBER;
            uint32_t i;

            PDebugTrace("Exchange Message - INIT");

            if(pTestEngineInstance->nState != P_TEST_STATE_STOPPED)
            {
               PDebugError("Exchange Message - INIT message received in the wrong state");
               goto exchange_error;
            }

            /* Compare the protocol version number with the one received from the test server */
            if ( CMemoryCompare( pCommandData, pProtocolVersion, P_TEST_VERSION_NUMBER_LENGTH ) != 0x00 )
            {
               PDebugError(
                  "Exchange Message - wrong version number %02d.%02d.%02d.%02d",
                  pCommandData[0],
                  pCommandData[1],
                  pCommandData[2],
                  pCommandData[3] );

               /* Set the test engine state */
               static_PTestSwitchState(pTestEngineInstance, P_TEST_STATE_STOPPED);

               goto exchange_error;
            }

            /* Initialize the test memory consumption */
            pTestEngineInstance->nPeakTestMemory = 0;
            pTestEngineInstance->nCurrentTestMemory = 0;
            pTestEngineInstance->nTrueCurrentTestMemory = 0;

            /* Set the test engine state */
            static_PTestSwitchState(pTestEngineInstance, P_TEST_STATE_INIT);

            /* Error code */
            pResponseDataBuffer[nResponseDataLength++] = P_TEST_ENGINE_RESULT_OK;
            /* Processor code */
            pResponseDataBuffer[nResponseDataLength++] = P_TEST_PROCESSOR;
            /* Product name */
            for(i = 0; i < P_TEST_PRODUCT_NAME_LENGTH; i++)
            {
               if(pProductName[i] != 0)
               {
                  pResponseDataBuffer[nResponseDataLength++] = pProductName[i];
               }
               else
               {
                  break;
               }
            }

            while(i++ < P_TEST_PRODUCT_NAME_LENGTH)
            {
               pResponseDataBuffer[nResponseDataLength++] = 0x20;
            }

            /* Version number */
            CMemoryCopy( &pResponseDataBuffer[nResponseDataLength], pProtocolVersion, P_TEST_VERSION_NUMBER_LENGTH );
            nResponseDataLength += P_TEST_VERSION_NUMBER_LENGTH;
            /* Build number */
            nResponseDataLength += PUtilWriteUint32ToBigEndianBuffer( nBuildNumber, &pResponseDataBuffer[nResponseDataLength]);
            /* UUID */
            CMemoryCopy( &pResponseDataBuffer[nResponseDataLength], pUUID, P_TEST_INTERFACE_UUID_LENGTH );
            nResponseDataLength += P_TEST_INTERFACE_UUID_LENGTH;

            /* Send the response */
            static_PTestSendResponse( pContext, nCommandCode, nResponseDataLength );
         }
         break;

      case P_TEST_CMD_DOWNLOAD:
         /* Check the download type */
         if ( nCommandDataLength != 0 )
         {
            PDebugTrace("Exchange Message - DOWNLOAD");
         }
         else
         {
            PDebugTrace("Exchange Message - UNLOAD");
         }

         if((pTestEngineInstance->nState != P_TEST_STATE_INIT) && (pTestEngineInstance->nState != P_TEST_STATE_LOADED))
         {
            PDebugError("Exchange Message - DOWNLOAD message received in the wrong state");
            goto exchange_error;
         }

         /* Unload the current bundle data, if any */
         /* Clear the bundle information, unload the current bundle data, if any */
         static_PTestSwitchState(pTestEngineInstance, P_TEST_STATE_INIT);

         /* If it is not an unload message */
         if ( nCommandDataLength != 0 )
         {
            /* Get the bundle position where starts the UUID */
            uint32_t nUUIDPosition = PUtilReadUint32FromBigEndianBuffer(&pCommandData[0]);
            uint32_t nUncompressedSize = PUtilReadUint32FromBigEndianBuffer(&pCommandData[4]);

            /* Check the bundle is uncompressed */
            if (nCommandDataLength - 8 != nUncompressedSize)
            {
               PDebugError("Exchange Message - compressed bundle data is not supported");
               goto exchange_error;
            }

            /* Get the bundle position in the buffer and store it */
            if ( static_PTestReadBundle( &pTestEngineInstance->sBundle, &pCommandData[8], nUncompressedSize, nUUIDPosition ) == W_FALSE )
            {
               PDebugError("Exchange Message - could not read the bundle data");
               goto exchange_error;
            }

            /* Set the test engine state */
            static_PTestSwitchState(pTestEngineInstance, P_TEST_STATE_LOADED);
         }

         /* Send the download response */
         static_PTestSendStandardResponse(
            pContext,
            nCommandCode,
            P_TEST_ENGINE_RESULT_OK);
         break;

      case P_TEST_CMD_EXECUTE:
         PDebugTrace("Exchange Message - EXECUTE");

         /* Check if a bundle has been loaded */
         if ( pTestEngineInstance->nState != P_TEST_STATE_LOADED )
         {
            PDebugError("Exchange Message - EXECUTE message with the wrong state");
            goto exchange_error;
         }

         /* Check the length of the data */
         if ( nCommandDataLength < 10 )
         {
            PDebugError(
               "Exchange Message - wrong length 0x%08X", nCommandDataLength );
            goto exchange_error;
         }

         {
         /* Get the test identifier */
         uint32_t nNewIdentifier = PUtilReadUint32FromBigEndianBuffer(&pCommandData[1] );
         bool_t bFound = W_FALSE;
         tWTestEntryPoint* pEntryPoint;
         uint32_t nIndex;
         uint32_t nTestParameterLength;
         uint32_t i;
         char16_t aTestName[9];
         uint32_t nTestNameLength;

         /* Search for the test number in the currently loaded bundle */
         for ( nIndex=0; nIndex < pTestEngineInstance->sBundle.pTestLinkTable->nTestNumber; nIndex++ )
         {
            /* Compare the identifier */
            if ( nNewIdentifier == pTestEngineInstance->sBundle.pTestLinkTable->aTestList[nIndex].nIdentifier )
            {
               bFound = W_TRUE;
               break;
            }
         }

         if(bFound == W_FALSE)
         {
            PDebugError(
            "Exchange Message - test 0x%08X unknown", nNewIdentifier );

            goto exchange_error;
         }

         /* Store the test information */
         pTestEngineInstance->nRunningTestId = nNewIdentifier;

         nTestNameLength = PUtilWriteDecimalUint32(aTestName, nNewIdentifier);

         for(i = 0; i < nTestNameLength; i++)
         {
            /* char16_t to char convertion*/
            pTestEngineInstance->aTestName[i] = (uint8_t)aTestName[i];
         }
         pTestEngineInstance->aTestName[nTestNameLength] = 0;

         pTestEngineInstance->nResultDataLength   = 0;

         /* Check if the test is in automatic mode or not */
         if ( pCommandData[0] & P_TEST_MESSAGE_AUTOMATIC )
         {
            pTestEngineInstance->bAutomaticMode = W_TRUE;
         }
         else
         {
            pTestEngineInstance->bAutomaticMode = W_FALSE;
         }

         pEntryPoint = pTestEngineInstance->sBundle.pTestLinkTable->aTestList[nIndex].pEntryPoint;

         /* Reset the test peak memory */
         pTestEngineInstance->nPeakTestMemory = 0;
         pTestEngineInstance->nCurrentTestMemory = 0;
         pTestEngineInstance->nTrueCurrentTestMemory = 0;

         static_PTestGetStatistics(pContext, pCommandData[5],
            &pTestEngineInstance->sStartStatistics);

         if((pTestEngineInstance->sStartStatistics.nFlags & P_TEST_STATISTIC_TIME) != 0)
         {
            pTestEngineInstance->sStartStatistics.nStartTime = PNALServiceDriverGetCurrentTime(pContext);
            pTestEngineInstance->sStartStatistics.nCumulatedTime = 0;
         }

         nTestParameterLength = PUtilReadUint32FromBigEndianBuffer(&pCommandData[6] );

         pTestEngineInstance->sExecuteContext.nInputParameter = 0;
         pTestEngineInstance->sExecuteContext.pInputData = null;
         pTestEngineInstance->sExecuteContext.nInputDataLength = 0;
         pTestEngineInstance->sExecuteContext.pApplicationData = null;

         /* Read the optional test remote parameter */
         /* Get the test identifier */
         if(nTestParameterLength != 0)
         {
            if(nTestParameterLength >= 4)
            {
               pTestEngineInstance->sExecuteContext.nInputParameter = PUtilReadUint32FromBigEndianBuffer(&pCommandData[10] );

               if(nTestParameterLength > 4)
               {
                  pTestEngineInstance->sExecuteContext.nInputDataLength = nTestParameterLength-4;
                  pTestEngineInstance->sExecuteContext.pInputData = (uint8_t*)CMemoryAlloc( pTestEngineInstance->sExecuteContext.nInputDataLength );
                  if (pTestEngineInstance->sExecuteContext.pInputData == null )
                  {
                     PDebugError("PBasicTestExchangeMessage: could not allocate the buffer");
                     goto exchange_error;
                  }
                  else
                  {
                     CMemoryCopy(
                        pTestEngineInstance->sExecuteContext.pInputData,
                        &pCommandData[14],
                        pTestEngineInstance->sExecuteContext.nInputDataLength);
                  }
               }
            }
            else
            {
               PDebugError("PBasicTestExchangeMessage: invalid length for the parameters");
               goto exchange_error;
            }
         }

         static_PTestSwitchState(pTestEngineInstance, P_TEST_STATE_EXECUTION);

         /* Release the context protection before calling the test */
         PContextReleaseLock(pContext);

         /* Launch the corresponding test */
         pEntryPoint( (tTestAPI*)(&g_aTestAPI) );

         /* Get the context protection after calling the test */
         PContextLock(pContext);

         PDebugTrace("Exchange Message - EXECUTE Finish");
         }
         return;

      case P_TEST_CMD_RESET_STATISTICS:

         PDebugTrace("Exchange Message - RESETSTAT");

         if((pTestEngineInstance->nState != P_TEST_STATE_INIT) && (pTestEngineInstance->nState != P_TEST_STATE_LOADED))
         {
            PDebugError("Exchange Message - RESET_STATISTICS message received in the wrong state");
            goto exchange_error;
         }

         /* Check the length of the data */
         if ( nCommandDataLength != 1 )
         {
            PDebugError("Exchange Message - message too long");
            goto exchange_error;
         }

         static_PTestResetStatistics(pContext, pCommandData[0]);

         /* Send the reset statistic response */
         static_PTestSendStandardResponse(pContext, nCommandCode, P_TEST_ENGINE_RESULT_OK);
         break;

      case P_TEST_CMD_GET_STATISTICS:
         {
            tTestStatistics sStatistics;
            uint32_t nStatisticLength;

            PDebugTrace("Exchange Message - GETSTAT");

            if(pTestEngineInstance->nState == P_TEST_STATE_STOPPED)
            {
               PDebugError("Exchange Message - GET_STATISTICS message received in the wrong state");
               goto exchange_error;
            }

            /* Check the length of the data */
            if ( nCommandDataLength != 1 )
            {
               PDebugError("Exchange Message - message too long");
               goto exchange_error;
            }

            static_PTestGetStatistics(pContext, pCommandData[0], &sStatistics);

            pResponseDataBuffer[0] = P_TEST_ENGINE_RESULT_OK;

            /* Get the statistic information */
            if ( (nStatisticLength = static_PTestGetStatisticsData(
                                                pContext,
                                                &sStatistics,
                                                &pResponseDataBuffer[1] ) ) == 0 )
            {
               PDebugError(
                  "Exchange Message - wrong statistic type 0x%02X",
                  pCommandData[0] );
               goto exchange_error;
            }

            static_PTestSendResponse(pContext, nCommandCode, 1 + nStatisticLength );
         }
         break;

      case P_TEST_RCMD_MESSAGE_BOX:
         PDebugTrace("Exchange Message - BOX");

         if((pTestEngineInstance->nState != P_TEST_STATE_EXECUTION)
         && (pTestEngineInstance->nState != P_TEST_STATE_RESULT_SET)
         && (pTestEngineInstance->nState != P_TEST_STATE_ERROR_TEST_IMPLEMENTATION))
         {
            PDebugError("Exchange Message - RCMD_MESSAGE_BOX message received in the wrong state");
            goto exchange_error;
         }

         if(pTestEngineInstance->nRunningInteraction != nCommandCode)
         {
            PDebugError("Exchange Message - RCMD_MESSAGE_BOX message unexptected");
            goto exchange_error;
         }

         /* Reset the displayed message box/card flag */
         pTestEngineInstance->nRunningInteraction = 0;

         /* Check the result or the length of the answer */
         if (  ( pCommandData[0] != P_TEST_ENGINE_RESULT_OK )
            || ( nCommandDataLength != 5 ) )
         {
            PDebugError(
               "Exchange Message - error (0x%02X) or wrong length (0x%08X)",
               pCommandData[0],
               nCommandDataLength );

            /* Call the test request callback */
            PDFCPostContext2(
               &pTestEngineInstance->aInteractionCallback,
               P_TEST_MSG_BOX_RESULT_CANCEL );
         }
         else
         {
            /* Get the code of the button pressed by the operator */
            uint32_t nButtonCode = PUtilReadUint32FromBigEndianBuffer(&pCommandData[1] );

            /* Call the test request callback */
            PDFCPostContext2(
               &pTestEngineInstance->aInteractionCallback,
               nButtonCode );
         }

         if((pTestEngineInstance->sStartStatistics.nFlags & P_TEST_STATISTIC_TIME) != 0)
         {
            pTestEngineInstance->sStartStatistics.nStartTime = PNALServiceDriverGetCurrentTime(pContext);
         }

         break;

      case P_TEST_RCMD_PRESENT_OBJECT:
      case P_TEST_RCMD_REMOVE_OBJECT:
         {
            W_ERROR nError;
            PDebugTrace("Exchange Message - PRESENT OBJECT or REMOVE OBJECT");

            if((pTestEngineInstance->nState != P_TEST_STATE_EXECUTION)
            && (pTestEngineInstance->nState != P_TEST_STATE_RESULT_SET)
            && (pTestEngineInstance->nState != P_TEST_STATE_ERROR_TEST_IMPLEMENTATION))
            {
               PDebugError("Exchange Message - PRESENT/REMOVE_OBJECT message received in the wrong state");
               goto exchange_error;
            }

            if(pTestEngineInstance->nRunningInteraction != nCommandCode)
            {
               PDebugError("Exchange Message - PRESENT/REMOVE_OBJECT message unexptected");
               goto exchange_error;
            }

            /* Reset the displayed message box/card flag */
            pTestEngineInstance->nRunningInteraction = 0;

            /* Check the result or the length of the answer */
            if (  ( pCommandData[0] != P_TEST_ENGINE_RESULT_OK )
               || ( nCommandDataLength != 5 ) )
            {
               PDebugError(
                  "Exchange Message - error (0x%02X) or wrong length (0x%08X)",
                  pCommandData[0],
                  nCommandDataLength );

               nError = W_ERROR_CANCEL;
            }
            else
            {
               /* Get the result */
               uint32_t nButtonCode = PUtilReadUint32FromBigEndianBuffer(&pCommandData[1] );

               /* Get the correct error code */
               switch ( nButtonCode )
               {
                  case P_TEST_MSG_BOX_RESULT_OK_YES:
                     nError = W_SUCCESS;
                     break;
                  case P_TEST_MSG_BOX_RESULT_CANCEL:
                     nError = W_ERROR_CANCEL;
                     break;
                  default:
                     nError = W_ERROR_BAD_PARAMETER;
                     PDebugError(
                        "Exchange Message - wrong result value 0x%08X",
                        nButtonCode );
                     break;
               }
            }

            /* Call the test request callback */
            PDFCPostContext2(
               &pTestEngineInstance->aInteractionCallback,
               nError );

            if((pTestEngineInstance->sStartStatistics.nFlags & P_TEST_STATISTIC_TIME) != 0)
            {
               pTestEngineInstance->sStartStatistics.nStartTime = PNALServiceDriverGetCurrentTime(pContext);
            }
         }
         break;
      case P_TEST_RCMD_REMOTE_EXECUTE:
         {
            uint32_t nReturnParameter;
            uint32_t nReturnBufferLength = 0;
            PDebugTrace("Exchange Message - P_TEST_RCMD_REMOTE_EXECUTE");

            if((pTestEngineInstance->nState != P_TEST_STATE_EXECUTION)
            && (pTestEngineInstance->nState != P_TEST_STATE_RESULT_SET)
            && (pTestEngineInstance->nState != P_TEST_STATE_ERROR_TEST_IMPLEMENTATION))
            {
               PDebugError("Exchange Message - RCMD_REMOTE_EXECUTE message received in the wrong state");
               goto exchange_error;
            }

            if(pTestEngineInstance->nRunningInteraction != nCommandCode)
            {
               PDebugError("Exchange Message - RCMD_REMOTE_EXECUTE message unexptected");
               goto exchange_error;
            }

            /* Reset the displayed message box/card flag */
            pTestEngineInstance->nRunningInteraction = 0;

            /* Check the result or the length of the answer */
            if (  ( pCommandData[0] != P_TEST_ENGINE_RESULT_OK )
               || ( nCommandDataLength < 9 ) )
            {
               PDebugError(
                  "Exchange Message - error (0x%02X) or wrong length (0x%08X)",
                  pCommandData[0],
                  nCommandDataLength );

               nReturnParameter = P_TEST_RESULT_CANCELLED;
            }
            else
            {
               /* Get the return parameter */
               nReturnParameter = PUtilReadUint32FromBigEndianBuffer(&pCommandData[1] );
               nReturnBufferLength = PUtilReadUint32FromBigEndianBuffer(&pCommandData[5] );

               if(nReturnBufferLength + 9 != nCommandDataLength)
               {
                  PDebugError("Error in message length");
                  nReturnParameter = P_TEST_RESULT_CANCELLED;
               }
            }

            if(nReturnBufferLength > pTestEngineInstance->nExecuteRemoteResultResultBufferLength)
            {
               PDebugError("Input buffer too small");
               nReturnParameter = P_TEST_RESULT_CANCELLED;
            }

            if(nReturnParameter != P_TEST_RESULT_PASSED)
            {
               nReturnBufferLength = 0;
            }

            if(nReturnBufferLength > 0)
            {
               CMemoryCopy( pTestEngineInstance->pExecuteRemoteResultBuffer, &pCommandData[9], nReturnBufferLength);
            }

            /* Call the test request callback */
            PDFCPostContext3(
               &pTestEngineInstance->aInteractionCallback,
               nReturnBufferLength,
               nReturnParameter);

            if((pTestEngineInstance->sStartStatistics.nFlags & P_TEST_STATISTIC_TIME) != 0)
            {
               pTestEngineInstance->sStartStatistics.nStartTime = PNALServiceDriverGetCurrentTime(pContext);
            }
         }
         break;

      default:
         PDebugError(
            "Exchange Message - wrong message type 0x%02X",
            nCommandCode );
         goto exchange_error;
   }

   return;

exchange_error:

   static_PTestSendStandardResponse(pContext, nCommandCode, P_TEST_ENGINE_RESULT_ERROR);
}

/* See Client API Specifications */
void PBasicTestExchangeMessage(
            tContext* pContext,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pCommandBuffer,
            uint32_t nCommandBufferLength,
            uint8_t* pResponseBuffer )
{
   /* Generic values */
   tTestEngineInstance* pTestEngineInstance = PContextGetTestEngineInstance( pContext );

   uint8_t nCommandCode = 0;
   uint32_t nCommandDataLength = 0;

   CDebugAssert( pTestEngineInstance != null );
   CDebugAssert( pTestEngineInstance->pResponseBuffer == null );
   CDebugAssert( pTestEngineInstance->pResponseDataBuffer == null );

   PDFCFillCallbackContext(
                  pContext,
                  (tDFCCallback*)pCallback,
                  pCallbackParameter,
                  &pTestEngineInstance->sResponseCallbackContext );

   if(( pCommandBuffer == null) || (pResponseBuffer == null))
   {
      PDebugError("Exchange Message - null buffer");
      /* Call the test server callback */
      PDFCPostContext3(
         &pTestEngineInstance->sResponseCallbackContext,
         0, W_ERROR_BAD_PARAMETER );

      return;
   }

   pTestEngineInstance->pResponseBuffer = pResponseBuffer;
   pTestEngineInstance->pResponseDataBuffer = pResponseBuffer + 5;

   /* Check the parameters */
   if ( nCommandBufferLength < 9 )
   {
      PDebugError("Exchange Message - pCommandBuffer too short");
      goto exchange_error;
   }
   else
   {
      uint32_t nCrc = PUtilReadUint32FromBigEndianBuffer(&pCommandBuffer[nCommandBufferLength - 4] );

      PDebugLog("Read CRC : 0x%04X", nCrc);
      if(PUtilComputeCrc32(0, pCommandBuffer, nCommandBufferLength - 4) != nCrc)
      {
         PDebugError("Exchange Message - Error of CRC");
         goto exchange_error;
      }
   }

   nCommandCode = pCommandBuffer[0];
   nCommandDataLength = PUtilReadUint32FromBigEndianBuffer(&pCommandBuffer[1] );

   if(nCommandDataLength != (nCommandBufferLength - 9))
   {
      PDebugError("Exchange Message - Error of length");
      goto exchange_error;
   }

   static_PTestExchangeMessage(
            pContext,
            nCommandCode,
            pCommandBuffer + 5,
            nCommandDataLength,
            pTestEngineInstance->pResponseDataBuffer );

   return;

exchange_error:

   static_PTestSendStandardResponse(pContext, nCommandCode, P_TEST_ENGINE_RESULT_ERROR);
}

/* See header file */
void PTestEngineCreate(
            tTestEngineInstance* pTestEngineInstance )
{
   CMemoryFill(pTestEngineInstance, 0, sizeof(tTestEngineInstance));

   /* Clear the structure */
   static_PTestSwitchState(pTestEngineInstance, P_TEST_STATE_STOPPED);
}

/* See header file */
void PTestEngineDestroy(
            tTestEngineInstance* pTestEngineInstance )
{
   if ( pTestEngineInstance != null )
   {
      CMemoryFree(pTestEngineInstance->sBundle.pBundleData);
      CMemoryFill(pTestEngineInstance, 0, sizeof(tTestEngineInstance));
   }
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */


#if ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) ) && (! defined P_INCLUDE_TEST_ENGINE)

/* See Client API Specifications */
void PBasicTestExchangeMessage(
            tContext* pContext,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pCommandBuffer,
            uint32_t nCommandBufferLength,
            uint8_t* pResponseBuffer )
{
   tDFCCallbackContext sCallbackContext;

   PDFCFillCallbackContext(pContext, (tDFCCallback *) pCallback, pCallbackParameter, &sCallbackContext);

   PDFCPostContext3(&sCallbackContext, 0, W_ERROR_FUNCTION_NOT_SUPPORTED);
}

#endif /* ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) ) && (! defined P_INCLUDE_TEST_ENGINE) */
