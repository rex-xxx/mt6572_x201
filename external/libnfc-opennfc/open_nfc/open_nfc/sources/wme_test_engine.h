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

#ifndef __WME_TEST_ENGINE_H
#define __WME_TEST_ENGINE_H

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

/*******************************************************************************
  Contains the declaration of the test engine implementation
*******************************************************************************/
/* Error code value */
#define P_TEST_ENGINE_RESULT_OK     0x00
#define P_TEST_ENGINE_RESULT_ERROR  0x01

/* The test statistics */
typedef struct __tTestStatistics
{
   uint32_t nCurrentUserMemory;
   uint32_t nPeakUserMemory;
   uint32_t nCurrentDriverMemory;
   uint32_t nPeakDriverMemory;
   uint32_t nPeakTestMemory;
   uint32_t nCurrentTestMemory;
   uint32_t nBundleDataLength;
   uint32_t nUserHandleNumber;
   uint32_t nDriverHandleNumber;

   uint32_t nStartTime;
   uint32_t nCumulatedTime;

   tNALProtocolStatistics sProtocolStatistics;

   uint8_t nFlags;
} tTestStatistics;

typedef struct __tTestBundle
{
   /* Bundle configuration data */
   uint8_t*    pBundleData;
   /* Bundle data length */
   uint32_t nBundleDataLength;
   /* Address of the test function structure */
   tTestLinkTable* pTestLinkTable;
   /* Starting address of the buffer */
   uintptr_t    npStartAddress;
   /* Positive or negative offset */
   bool_t        bOffsetOrder;
   /* Value of the offset */
   uint32_t    nOffset;
} tTestBundle;

/* Describes a test engine instance - Do not use directly */
typedef struct __tTestEngineInstance
{
/* Test information */
   /* Test engine state */
   uint32_t     nState;

/* Response data */
   tDFCCallbackContext sResponseCallbackContext;
   uint8_t* pResponseBuffer;
   uint8_t* pResponseDataBuffer;

/* Bundle data */
   tTestBundle sBundle;

/* Test information */
   /* Identifier of the current running test, if not null */
   uint32_t nRunningTestId;
   /* Mode of the test: automatic or not */
   bool_t bAutomaticMode;
   /* Name of the current running test */
   char     aTestName[10];

/* Interaction information */
   /* Interaction flag */
   uint32_t nRunningInteraction;
   /* Interaction callback context */
   tDFCCallbackContext aInteractionCallback;
   /* Remote execution */
   uint8_t* pExecuteRemoteResultBuffer;
   uint32_t nExecuteRemoteResultResultBufferLength;

/* Result information */
   uint32_t nResultErrorCode;
   uint8_t aResultData[W_TEST_SERVER_RESPONSE_MAX_LENGTH];
   uint32_t nResultDataLength;

/* The test statistics */
   uint32_t nPeakTestMemory;
   uint32_t nCurrentTestMemory;
   uint32_t nTrueCurrentTestMemory;
   tTestStatistics sStartStatistics;
   tTestExecuteContext sExecuteContext;

/* The timer stuff */
	uint32_t nCurrentExpiryTime;
	tDFCCallbackContext sTimerCallbackContext;



} tTestEngineInstance;

/**
 * @brief   Creates a test engine instance.
 *
 * @pre  Only one test engine instance is created at a given time.
 *
 * @param[in]  pTestEngine  The test engine instance to initialize.
 **/
void PTestEngineCreate(
         tTestEngineInstance* pTestEngineInstance );

/**
 * @brief   Destroyes a test engine instance.
 *
 * If the instance is already destroyed, the function does nothing and returns.
 *
 * @post  PTestEngineDestroy() does not return any error. The caller should always
 *        assume that the test engine instance is destroyed after this call.
 *
 * @post  The caller should never re-use the test engine instance value.
 *
 * @param[in]  pTestEngine  The test engine instance to destroy.
 **/
void PTestEngineDestroy(
         tTestEngineInstance* pTestEngineInstance );

#endif /* P_CONFIG_USER ||P_CONFIG_MONOLITHIC */

#endif /* __WME_TEST_ENGINE_H */
