/*
 * Copyright (c) 2011 Inside Secure, All Rights Reserved.
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
   Contains the implementation of the security stack ACL manager functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( SECSTACK )

#include "wme_context.h"

#if ( (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) ) && (defined P_INCLUDE_SE_SECURITY)

#include "wme_security_stack_acl_processor.h"
#include "wme_security_stack_acl_processor_gp.h"

/** The length in bytes of the ACMF refresh tag */
#define P_ACMF_REFRESH_TAG_LENGTH 8

/** @brief Data structure for the GlobalPlatform ACL Processor instance */
struct __tPSecurityStackAclProcessorInstance
{
   /** Whether the ACL may have been modified and need to be read again */
   bool_t bTouched;
   /** Whether the bTouched flag is automatically reset */
   bool_t bAutoResetTouch;
   /** The last time the ACL were updated */
   uint32_t nLastUpdateTime;
   /** The current time */
   uint32_t nNow;
   /** The date&time of the current ACL */
   uint8_t aACLTime[P_ACMF_REFRESH_TAG_LENGTH];
   /** The FID of the PKCS#15 EF(ACMF) file */
   uint16_t nDfPkcs15AcmfFid;
   struct {
   /** The current state of the state machine */
   uint32_t nState;
   /** The buffer containing the APDU command sent to the SE */
   uint8_t aReaderToCardBuffer[20];
   /** The buffer containing the APDU response returned by the SE */
   uint8_t aCardToReaderBuffer[(256+2)+2]; /* 2 padding bytes */
   /** The Refresh tag read from the ACMF file */
   uint8_t aRefreshTag[P_ACMF_REFRESH_TAG_LENGTH];
   /** The contents of the EF(ACRF) file */
   uint8_t* pAcrfBuffer;
   /** The ASN.1 parser used to decode the EF(ACRF) file */
   tAsn1Parser sAcrfParser;
   /** The ACL that is currently being constructed */
   tSecurityStackACL* pAcl;
   /** The ACE that is currently being constructed */
   tACE* pAce;
   } sReadAcl;
   /** Parameters used to read a binary file */
   struct {
   /** Whether the buffer has been dynamically allocated */
   bool_t bAllocated;
   /** The file contents */
   uint8_t* pFileDataBuffer;
   /** The file length (extracted from the answer to the SELECT [file] command */
   uint16_t nFileDataBufferLength;
   /** The number of read bytes (must be less than or equal to nFileDataLength) */
   uint16_t nReadBytes;
   /** The state to be reached when the file is entirely read */
   uint32_t nNextState;
   } sReadBinary;
   /** Parameters used to exchange an APDU with the Secure Element */
   struct {
   tPSecurityStackAclProcessorExchangeApdu* pExchangeApdu;
   void* pCallbackParameter1;
   void* pCallbackParameter2;
   } sExchange;
   /** Parameters used to notify the result of the ACL reading */
   struct {
   tPSecurityStackAclProcessorReadAclCallbackFunction* pCallback;
   void* pCallbackParameter1;
   void* pCallbackParameter2;
   } sResult;
};


/**
 * @brief Extracts a field from an FCP.
 *
 * @param[in]  pAclProcessorInstance  The ACL Processor instance.
 *
 * @param[in]  pFcpBuffer  The FCP buffer.
 *
 * @param[in]  nFcpLength  The length of the FCP buffer.
 *
 * @param[in]  nTag  The tag of the field to be extracted.
 *
 * @return One of the following error codes:
 *          - W_SUCCESS  The FCP has been successfully parsed.
 *          - W_ERROR_BAD_PARAMETER  The FCI is malformed.
 */
static W_ERROR static_PSecurityStackGpAclProcessorExtractFieldFromFcp(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
                  const uint8_t* pFcpBuffer,
                  uint32_t nFcpLength,
                  uint8_t nTag,
                  const uint8_t** ppFieldBuffer,
                  uint32_t* pnFieldLength)
{
   W_ERROR nResult;
   tAsn1Parser sParser;
   tAsn1Parser sIncludedParser;

   PDebugTrace("static_PSecurityStackGpAclProcessorExtractFieldFromFcp()");

   nResult = PAsn1InitializeParser(&sParser, pFcpBuffer, nFcpLength);
   if (nResult != W_SUCCESS)
   {
      PDebugTrace("static_PSecurityStackGpAclProcessorExtractFieldFromFcp: Bad TLV format");
      return nResult;
   }

   if(PAsn1GetTagValue(&sParser) != 0x62)
   {
      PDebugTrace("static_PSecurityStackGpAclProcessorExtractFieldFromFcp: Bad '62' tag");
      return W_ERROR_BAD_PARAMETER;
   }

   nResult = PAsn1ParseIncludedTlv( &sParser, &sIncludedParser);
   if (nResult != W_SUCCESS)
   {
      PDebugTrace("static_PSecurityStackGpAclProcessorExtractFieldFromFcp: Bad included TLV format");
      return nResult;
   }

   do
   {
      if (PAsn1GetTagValue(&sIncludedParser) == nTag)
      {
         PAsn1GetPointerOnBinaryContent(&sIncludedParser, ppFieldBuffer, pnFieldLength);
         return W_SUCCESS;
      }
   } while(PAsn1MoveToNextTlv(&sIncludedParser) == W_SUCCESS);

   PDebugTrace("static_PSecurityStackGpAclProcessorExtractFieldFromFcp: Tag '%02X' not found", nTag);
   return W_ERROR_BAD_PARAMETER;
}


/** See tPSecurityStackAclProcessorCreateInstance */
static W_ERROR static_PSecurityStackGpAclProcessorCreateInstance(
                  const uint8_t* pAnswerToSelect,
                  uint32_t nAnswerToSelectLength,
                  bool_t bIsSelectedByAid,
                  bool_t bAutoResetTouch,
                  tPSecurityStackAclProcessorInstance** ppAclProcessorInstance)
{
   tPSecurityStackAclProcessorInstance* pAclProcessorInstance;

   PDebugTrace("static_PSecurityStackGpAclProcessorCreateInstance()");

   if (pAnswerToSelect == null)
   {
      PDebugError("static_PSecurityStackGpAclProcessorCreateInstance: pAnswerToSelect == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (ppAclProcessorInstance == null)
   {
      PDebugError("static_PSecurityStackGpAclProcessorCreateInstance: ppAclProcessorInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   pAclProcessorInstance = (tPSecurityStackAclProcessorInstance*)CMemoryAlloc(sizeof(*pAclProcessorInstance));
   if (pAclProcessorInstance == null)
   {
      PDebugError("static_PSecurityStackGpAclProcessorCreateInstance: Failed to allocate ACL Processor instance");
      return W_ERROR_OUT_OF_RESOURCE;
   }
   CMemoryFill(pAclProcessorInstance, 0, sizeof(*pAclProcessorInstance));

   /* Answer-to-Select is ignored and always accepted by this ACL processor */

   /* Force reading the ACL */
   pAclProcessorInstance->bTouched = W_TRUE;
   pAclProcessorInstance->nLastUpdateTime = 0;
   pAclProcessorInstance->bAutoResetTouch = bAutoResetTouch;
   pAclProcessorInstance->nDfPkcs15AcmfFid = 0x3F00; /* Ensure no collision with actual FID */

   *ppAclProcessorInstance = pAclProcessorInstance;
   return W_SUCCESS;
}

/** See tPSecurityStackAclProcessorUpdateInstance */
static W_ERROR static_PSecurityStackGpAclProcessorUpdateInstance(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
                  const uint8_t* pAnswerToSelect,
                  uint32_t nAnswerToSelectLength,
                  bool_t bIsSelectedByAid)
{
   PDebugTrace("static_PSecurityStackGpAclProcessorUpdateInstance()");

   if (pAclProcessorInstance == null)
   {
      PDebugError("static_PSecurityStackGpAclProcessorUpdateInstance: pAclProcessorInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pAnswerToSelect == null)
   {
      PDebugTrace("static_PSecurityStackGpAclProcessorUpdateInstance: pAnswerToSelect == null");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Answer-to-Select is ignored and always accepted by this ACL processor */

   if (pAclProcessorInstance->bTouched != W_FALSE)
   {
      PDebugTrace("static_PSecurityStackGpAclProcessorUpdateInstance: Force caller to re-read the ACL");
      return W_ERROR_BAD_STATE;
   }

   PDebugTrace("static_PSecurityStackGpAclProcessorUpdateInstance: No need to read the ACL again");
   return W_SUCCESS;
}


/** The event indicating that the state machine must be started */
#define P_EVENT_INITIAL                    0
/** The event indicating that the state machine has received the answer of the last command */
#define P_EVENT_COMMAND_EXECUTED           1
/* The status indicating that a command is pending */
#define P_STATUS_PENDING_EXECUTED          0x0200
/** The state machine is stopped */
#define P_STATE_IDLE                       0x0000
/** The state machine must select the EF(ODF) file */
#define P_STATE_SELECT_EF_ODF              0x0001
/** The state machine must read the EF(ODF) file */
#define P_STATE_READ_EF_ODF                0x0002
/** The state machine must select the EF(DODF) file */
#define P_STATE_SELECT_EF_DODF             0x0003
/** The state machine must read the EF(DODF) file */
#define P_STATE_READ_EF_DODF               0x0004
/** The state machine must select the EF(ACMF) file */
#define P_STATE_SELECT_EF_ACMF             0x0005
/** The state machine must read the EF(ACMF) file */
#define P_STATE_READ_EF_ACMF               0x0006
/** The state machine must select the EF(ACRF) file */
#define P_STATE_SELECT_EF_ACRF             0x0007
/** The state machine must read the EF(ACRF) file */
#define P_STATE_READ_EF_ACRF               0x0008
/** The state machine must process the contents of the EF(ACRF) file */
#define P_STATE_PROCESS_EF_ACRF            0x0009
/** The state machine must read the next EF(ACCondition) file */
#define P_STATE_READ_EF_ACCONDITION        0x000A
/** The state machine must process the contents of the EF(ACCondition) file */
#define P_STATE_PROCESS_EF_ACCONDITION     0x000B
/** The state machine processes chained READ BINARY commands */
#define P_STATE_READ_BINARY                0x0010
/** The state machine must return the sResult */
#define P_STATE_FINAL                      0x0080
/** The state machine must return an error notification to the caller */
#define P_STATE_ERROR                      0x00FF

/* Forward declaration */
static void static_PSecurityStackGpAclProcessorReadAclStateMachine
(
   tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
   uint32_t nEvent,
   uint32_t nDataLength,
   W_ERROR nResult
);

/** @brief Callback function called when an APDU exchange with the Secure Element completes */
static void static_PSecurityStackGpAclProcessorExchangeApduCompleted(
                  void* pCallbackParameter1,
                  void* pCallbackParameter2,
                  uint32_t nDataLength,
                  W_ERROR nResult)
{
   tPSecurityStackAclProcessorInstance* pAclProcessorInstance = (tPSecurityStackAclProcessorInstance*)pCallbackParameter1;

   PDebugTrace("static_PSecurityStackGpAclProcessorExchangeApduCompleted()");

   CDebugAssert(pAclProcessorInstance != null);
   CDebugAssert(pCallbackParameter2 == null);

   static_PSecurityStackGpAclProcessorReadAclStateMachine(pAclProcessorInstance,
      P_EVENT_COMMAND_EXECUTED, nDataLength, nResult);
}


/**
 * @brief Selects a file by its FID.
 *
 * This command sends a SELECT [file] command to the PKCS#15 DF/application.
 *
 * @param[in]  pSecurityStackInstance  The security stack instance.
 *
 * @param[in]  nFid  The FID of the file to be selected.
 */
static void static_PSecurityStackGpAclProcessorSelectFile
(
   tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
   uint16_t nFid
)
{
   W_ERROR nResult;

   PDebugTrace("static_PSecurityStackGpAclProcessorSelectFile( nFid = 0x%02X)", nFid);

   pAclProcessorInstance->sReadAcl.aReaderToCardBuffer[0] = P_7816SM_CLA;
   pAclProcessorInstance->sReadAcl.aReaderToCardBuffer[1] = P_7816SM_INS_SELECT;
   pAclProcessorInstance->sReadAcl.aReaderToCardBuffer[2] = P_7816SM_P1_SELECT_FILE;
   pAclProcessorInstance->sReadAcl.aReaderToCardBuffer[3] = P_7816SM_P2_SELECT_FILE_WITH_FCP;
   pAclProcessorInstance->sReadAcl.aReaderToCardBuffer[4] = 0x02;
   pAclProcessorInstance->sReadAcl.aReaderToCardBuffer[5] = (uint8_t)(nFid >> 8);
   pAclProcessorInstance->sReadAcl.aReaderToCardBuffer[6] = (uint8_t)(nFid);

   nResult = pAclProcessorInstance->sExchange.pExchangeApdu(
      pAclProcessorInstance->sExchange.pCallbackParameter1,
      pAclProcessorInstance->sExchange.pCallbackParameter2,
      static_PSecurityStackGpAclProcessorExchangeApduCompleted,
      pAclProcessorInstance,
      null,
      pAclProcessorInstance->sReadAcl.aReaderToCardBuffer,
      7,
      pAclProcessorInstance->sReadAcl.aCardToReaderBuffer,
      sizeof(pAclProcessorInstance->sReadAcl.aCardToReaderBuffer));

   if (nResult != W_ERROR_OPERATION_PENDING)
   {
      static_PSecurityStackGpAclProcessorExchangeApduCompleted(
         pAclProcessorInstance->sExchange.pCallbackParameter1,
         pAclProcessorInstance->sExchange.pCallbackParameter2,
         0, nResult);
   }
}

/**
 * @brief Reads binary data from the current file.
 *
 * This command sends a READ BINARY command to the PKCS#15 DF/application.
 *
 * @param[in]  pSecurityStackInstance  The security stack instance.
 *
 * @param[in]  nOffset  The offset from which data are to be read.
 */
static void static_PSecurityStackGpAclProcessorReadBinary
(
   tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
   uint16_t nOffset
)
{
   W_ERROR nResult;

   PDebugTrace("static_PSecurityStackGpAclProcessorReadBinary( nOffset = 0x%02X)", nOffset);

   pAclProcessorInstance->sReadAcl.aReaderToCardBuffer[0] = P_7816SM_CLA;
   pAclProcessorInstance->sReadAcl.aReaderToCardBuffer[1] = P_7816SM_INS_READ_BINARY;
   pAclProcessorInstance->sReadAcl.aReaderToCardBuffer[2] = (uint8_t)(nOffset >> 8);
   pAclProcessorInstance->sReadAcl.aReaderToCardBuffer[3] = (uint8_t)(nOffset);
   pAclProcessorInstance->sReadAcl.aReaderToCardBuffer[4] = 0x00; /* Le */

   nResult = pAclProcessorInstance->sExchange.pExchangeApdu(
      pAclProcessorInstance->sExchange.pCallbackParameter1,
      pAclProcessorInstance->sExchange.pCallbackParameter2,
      static_PSecurityStackGpAclProcessorExchangeApduCompleted,
      pAclProcessorInstance,
      null,
      pAclProcessorInstance->sReadAcl.aReaderToCardBuffer,
      5,
      pAclProcessorInstance->sReadAcl.aCardToReaderBuffer,
      sizeof(pAclProcessorInstance->sReadAcl.aCardToReaderBuffer));

   if (nResult != W_ERROR_OPERATION_PENDING)
   {
      static_PSecurityStackGpAclProcessorExchangeApduCompleted(
         pAclProcessorInstance->sExchange.pCallbackParameter1,
         pAclProcessorInstance->sExchange.pCallbackParameter2,
         0, nResult);
   }
}

/** @brief Reads the currently selected binary file */
static void static_PSecurityStackGpAclProcessorReadBinaryFile
(
   tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
   uint16_t nFileLength
)
{
   uint8_t* pFileDataBuffer;

   PDebugTrace("static_PSecurityStackGpAclProcessorReadBinaryFile( nFileLength = 0x%02X)", nFileLength);

   CDebugAssert(pAclProcessorInstance != null);
   CDebugAssert(pAclProcessorInstance->sReadBinary.pFileDataBuffer == null);

   if (nFileLength > 256)
   {
      pFileDataBuffer = (uint8_t*)CMemoryAlloc(nFileLength);
      if (pFileDataBuffer == null)
      {
         PDebugError("static_PSecurityStackGpAclProcessorReadBinaryFile: Failed to allocate %d bytes", nFileLength);

         static_PSecurityStackGpAclProcessorReadAclStateMachine(pAclProcessorInstance,
            P_EVENT_COMMAND_EXECUTED, 0, W_ERROR_OUT_OF_RESOURCE);
         return;
      }
      CMemoryFill(pFileDataBuffer, 0, nFileLength);

      pAclProcessorInstance->sReadBinary.bAllocated = W_TRUE;
      pAclProcessorInstance->sReadBinary.pFileDataBuffer = pFileDataBuffer;
   }
   else
   {
      pAclProcessorInstance->sReadBinary.bAllocated = W_FALSE;
      pAclProcessorInstance->sReadBinary.pFileDataBuffer = pAclProcessorInstance->sReadAcl.aCardToReaderBuffer;
   }

   pAclProcessorInstance->sReadBinary.nFileDataBufferLength = nFileLength;
   pAclProcessorInstance->sReadBinary.nReadBytes = 0;

   if (nFileLength == 0)
   {
      /*
       * Simulate success, no need to send a READ BINARY command. Moreover, the card returns 6700
       * that is then processed as an error because 9000 is expected.
       */
      static_PSecurityStackGpAclProcessorReadAclStateMachine(pAclProcessorInstance,
         P_EVENT_COMMAND_EXECUTED, 0, W_SUCCESS);
      return;
   }

   /* Save state to be reached */
   pAclProcessorInstance->sReadBinary.nNextState = pAclProcessorInstance->sReadAcl.nState;

   /* Set the next state to be reached (this state shall call static_PSecurityStackGpAclProcessorReadBinaryFileCompleted) */
   pAclProcessorInstance->sReadAcl.nState = P_STATE_READ_BINARY | P_STATUS_PENDING_EXECUTED;

   /* Process the first READ BINARY command */
   static_PSecurityStackGpAclProcessorReadBinary(pAclProcessorInstance, 0);
}

/** @brief Frees memory that may have been allocated for reading a file. Also cleans up data structure */
static void static_PSecurityStackGpAclProcessorReadBinaryFileFree
(
   tPSecurityStackAclProcessorInstance* pAclProcessorInstance
)
{
   PDebugTrace("static_PSecurityStackGpAclProcessorReadBinaryFileFree()");

   CDebugAssert(pAclProcessorInstance != null);

   if (pAclProcessorInstance->sReadBinary.bAllocated != W_FALSE)
   {
      if (pAclProcessorInstance->sReadBinary.pFileDataBuffer != null)
      {
         CMemoryFree(pAclProcessorInstance->sReadBinary.pFileDataBuffer);
      }
   }

   CMemoryFill(&pAclProcessorInstance->sReadBinary, 0, sizeof(pAclProcessorInstance->sReadBinary));
}

/** @brief Processes the completion of a READ BINARY command */
static void static_PSecurityStackGpAclProcessorReadBinaryFileCompleted
(
   tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
   uint16_t nDataLength,
   W_ERROR nResult
)
{
   PDebugTrace("static_PSecurityStackGpAclProcessorReadBinaryFileCompleted(nDataLength = 0x%02X, error=%s)", nDataLength, PUtilTraceError(nResult));

   if (nResult == W_SUCCESS)
   {
      CDebugAssert(pAclProcessorInstance->sReadBinary.pFileDataBuffer != null);

      /* Check answer to the READ BINARY command */
      if ((nDataLength < 2)
            || (pAclProcessorInstance->sReadAcl.aCardToReaderBuffer[nDataLength - 2] != 0x90)
            || (pAclProcessorInstance->sReadAcl.aCardToReaderBuffer[nDataLength - 1] != 0x00))
      {
         PDebugError("static_PSecurityStackGpAclProcessorReadBinaryFileCompleted: error in response APDU");
         nResult = W_ERROR_RF_COMMUNICATION;
         goto return_failure;
      }

      nDataLength -= 2;

      /* Check that there is enough space in the allocated buffer */
      if ((pAclProcessorInstance->sReadBinary.nReadBytes + nDataLength) > pAclProcessorInstance->sReadBinary.nFileDataBufferLength)
      {
         PDebugWarning("static_PSecurityStackGpAclProcessorReadBinaryFileCompleted: Too many bytes have been read");
         nResult = W_ERROR_RF_COMMUNICATION;
         goto return_failure;
      }

      if (pAclProcessorInstance->sReadBinary.bAllocated != W_FALSE)
      {
         CMemoryCopy(pAclProcessorInstance->sReadBinary.pFileDataBuffer + pAclProcessorInstance->sReadBinary.nReadBytes,
            pAclProcessorInstance->sReadAcl.aCardToReaderBuffer, nDataLength);
      }

      pAclProcessorInstance->sReadBinary.nReadBytes += nDataLength;

      /* Check whether all bytes have been read */
      if (pAclProcessorInstance->sReadBinary.nReadBytes == pAclProcessorInstance->sReadBinary.nFileDataBufferLength)
      {
         /* All bytes have been read */

         /* Restore saved state */
         pAclProcessorInstance->sReadAcl.nState = pAclProcessorInstance->sReadBinary.nNextState;

         /* Notify success to the state machine */
         static_PSecurityStackGpAclProcessorReadAclStateMachine(pAclProcessorInstance,
            P_EVENT_COMMAND_EXECUTED, pAclProcessorInstance->sReadBinary.nFileDataBufferLength, W_SUCCESS);
      }
      else
      {
         /* Some bytes still have to be read */

         /* Set the next state to be reached (this state shall call static_PSecurityStackGpAclProcessorReadBinaryFileCompleted) */
         pAclProcessorInstance->sReadAcl.nState = P_STATE_READ_BINARY | P_STATUS_PENDING_EXECUTED;

         /* Process the first READ BINARY command */
         static_PSecurityStackGpAclProcessorReadBinary(pAclProcessorInstance, pAclProcessorInstance->sReadBinary.nReadBytes);
      }

      return;
   }

return_failure:

   /* Delete the internal buffer in case of error */
   static_PSecurityStackGpAclProcessorReadBinaryFileFree(pAclProcessorInstance);

   /* Restore saved state */
   pAclProcessorInstance->sReadAcl.nState = pAclProcessorInstance->sReadBinary.nNextState;

   /* Notify error to the state machine */
   static_PSecurityStackGpAclProcessorReadAclStateMachine(pAclProcessorInstance,
      P_EVENT_COMMAND_EXECUTED, 0, nResult);
}

/**
 * @brief Extracts the file size from an FCP.
 *
 * @param[in]  pAclProcessorInstance  The ACL Processor instance.
 *
 * @param[in]  pFcpBuffer  The FCP buffer.
 *
 * @param[in]  nFcpLength  The length of the FCP buffer.
 *
 * @return One of the following error codes:
 *          - W_SUCCESS  The FCI has been successfully parsed.
 *          - W_ERROR_CONNECTION_COMPATIBILITY  The FCI is malformed.
 *          - W_ERROR_OUT_OF_RESOURCE  Out of memory.
 */
static W_ERROR static_PSecurityStackGpAclProcessorExtractFileSizeFromFcp(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
                  const uint8_t* pFcpBuffer,
                  uint32_t nFcpLength,
                  uint16_t* pnFileSize)
{
   W_ERROR nResult;
   const uint8_t* pFileSizeBuffer;
   uint32_t nFileSizeLength;

   PDebugTrace("static_PSecurityStackGpAclProcessorExtractFileSizeFromFcp()");

   /*
    * The answer to the SELECT[AID] command is (likely) formatted as:
    * 62 xx                FCP template
    *    80 02 NN NN       File size
    *    xx ...
    */

   nResult = static_PSecurityStackGpAclProcessorExtractFieldFromFcp(pAclProcessorInstance,
     pFcpBuffer, nFcpLength, 0x80, &pFileSizeBuffer, &nFileSizeLength);
   if (nResult != W_SUCCESS)
   {
      return nResult;
   }

   if (nFileSizeLength != 2)
   {
      PDebugWarning("static_PSecurityStackGpAclProcessorExtractFileSizeFromFcp: Malformed tag '80'");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pnFileSize != null)
   {
      *pnFileSize = (uint16_t)((256 * pFileSizeBuffer[0]) + pFileSizeBuffer[1]);
   }

   return W_SUCCESS;
}

/**
 * @brief Extracts the FID of the EF(DODF) file stored in the EF(ODF) file.
 *
 * @param[in]  pAclProcessorInstance  The ACL Processor instance.
 *
 * @param[in]  pEfOdfBuffer  The buffer containing the EF(ODF) bytes.
 *
 * @param[in]  nEfOdfLength  The length of the EF(ODF) buffer.
 *
 * @param[out]  pnDodfFid  The extracted FID of the EF(DODF) file.
 *
 * @return One of the following error codes:
 *          - W_SUCCESS  The FCI has been successfully parsed.
 *          - W_ERROR_CONNECTION_COMPATIBILITY  The FCI is malformed.
 *          - W_ERROR_OUT_OF_RESOURCE  Out of memory.
 */
static W_ERROR static_PSecurityStackGpAclProcessorExtractDodfFid(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
                  const uint8_t* pEfOdfBuffer,
                  uint32_t nEfOdfLength,
                  uint16_t* pnDodfFid)
{
   W_ERROR nResult;
   tAsn1Parser sParser;

   PDebugTrace("static_PSecurityStackGpAclProcessorExtractDodfFid()");

   /*
    * Look for the following field:
    *
    *                    PKCS15Objects CHOICE
    * A7 06                   dataObjects : tag = [7] constructed; length = 6
    *                           DataObjects CHOICE
    *    30 04                    path Path SEQUENCE: tag = [UNIVERSAL 16] constructed; length = 4
    *       04 02                   path OCTET STRING: tag = [UNIVERSAL 4] primitive; length = 2
    *          XX XX                = XX XX -> FID of the EF(DODF)
    */

   nResult = PAsn1InitializeParser(&sParser, pEfOdfBuffer, nEfOdfLength);
   if (nResult != W_SUCCESS)
   {
      PDebugWarning("static_PSecurityStackGpAclProcessorExtractDodfFid: Bad TLV format");
      return nResult;
   }

   do
   {
      if (PAsn1GetTagValue(&sParser) == 0xA7)
      {
         const uint8_t* pBuffer;
         uint32_t nLength;
         PAsn1GetPointerOnBinaryContent(&sParser, &pBuffer, &nLength);

         if ((nLength != 6) || (pBuffer[0] != 0x30) || (pBuffer[1] != 0x04) || (pBuffer[2] != 0x04) || (pBuffer[3] != 0x02))
         {
            PDebugWarning("static_PSecurityStackGpAclProcessorExtractDodfFid: Malformed 'A7' tag");
            return W_ERROR_BAD_PARAMETER;
         }

         if (pnDodfFid != null)
         {
            *pnDodfFid = (uint16_t)((256 * pBuffer[4]) + pBuffer[5]);
         }

         return W_SUCCESS;
      }
   } while(PAsn1MoveToNextTlv(&sParser) == W_SUCCESS);

   PDebugWarning("static_PSecurityStackGpAclProcessorExtractDodfFid: Tag 'A7' not found");
   PDebugWarning("static_PSecurityStackGpAclProcessorExtractDodfFid: No DODF EF entry");
   return W_ERROR_BAD_PARAMETER;
}

/** OID for { iso(1) member-body(2) country-USA(840) Global-Platform(114283) device(200) seAccessControl(1) accessControlMainFile(1) } */
static const uint8_t static_OidACMF[] =
{
  0x2A, 0x86, 0x48, 0x86, 0xFC, 0x6B, 0x81, 0x48, 0x01, 0x01
};

/**
 * @brief Extracts the FID of the EF(ACMF) file stored in the EF(DODF) file.
 *
 * @param[in]  pAclProcessorInstance  The ACL Processor instance.
 *
 * @param[in]  pEfDodfBuffer  The buffer containing the EF(DODF) bytes.
 *
 * @param[in]  nEfDodfLength  The length of the EF(DODF) buffer.
 *
 * @param[out]  pnAcmfFid  The extracted FID of the EF(ACMF) file.
 *
 * @return One of the following error codes:
 *          - W_SUCCESS  The FCI has been successfully parsed.
 *          - W_ERROR_CONNECTION_COMPATIBILITY  The FCI is malformed.
 *          - W_ERROR_OUT_OF_RESOURCE  Out of memory.
 */
static W_ERROR static_PSecurityStackGpAclProcessorExtractAcmfFid(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
                  const uint8_t* pEfDodfBuffer,
                  uint32_t nEfDodfLength,
                  uint16_t* pnAcmfFid)
{
   W_ERROR nResult;
   tAsn1Parser sParser;

   PDebugTrace("static_PSecurityStackGpAclProcessorExtractAcmfFid()");

   /*
    * Look for the following field:
    *
    * A1 29                   oidDO SEQUENCE: tag = [1] constructed; length = 41
    *    30 00                  commonObjectAttributes CommonObjectAttributes SEQUENCE: tag = [UNIVERSAL 16] constructed; length = 0
    *    30 0F                  classAttributes CommonDataObjectAttributes SEQUENCE: tag = [UNIVERSAL 16] constructed; length = 15
    *       0C 0D               UTF8String: tag = [UNIVERSAL 12] primitive; length = 13
    *          47 50 20 53 45 20 41 63 63 20 43 74 6C = "GP SE Acc Ctl"
    *    A1 14                  typeAttributes : tag = [1] constructed; length = 20
    *       30 12                 OidDO SEQUENCE: tag = [UNIVERSAL 16]; length = 18
    *          06 0A                   oid OBJECT IDENTIFIER: tag = [UNIVERSAL 6] primitive; length = 10
    *             2A 86 48 86 FC 6B 81 48 01 01 = { iso(1) member-body(2) country-USA(840) Global-Platform(114283) device(200) seAccessControl(1) accessControlMainFile(1) }
    *          30 04              path Path SEQUENCE: tag = [UNIVERSAL 16] constructed; length = 4
    *             04 02             path OCTET STRING: tag = [UNIVERSAL 4] primitive; length = 2
    *                XX XX         = XX XX -> Pointer to EF(ACMF)
    */

   nResult = PAsn1InitializeParser(&sParser, pEfDodfBuffer, nEfDodfLength);
   if (nResult != W_SUCCESS)
   {
      PDebugWarning("static_PSecurityStackGpAclProcessorExtractAcmfFid: Bad TLV format");
      return nResult;
   }

   do
   {
      if (PAsn1GetTagValue(&sParser) == 0xA1)
      {
         tAsn1Parser sIncludedParser;
         tAsn1Parser sOidDoParser;

         const uint8_t* pBuffer;
         uint32_t nLength;
         PAsn1GetPointerOnBinaryContent(&sParser, &pBuffer, &nLength);

         nResult = PAsn1ParseIncludedTlv(&sParser, &sIncludedParser);
         if (nResult != W_SUCCESS)
         {
            PDebugWarning("static_PSecurityStackGpAclProcessorExtractAcmfFid: Bad included TLV format");
            return nResult;
         }

         /* Parse commonObjectAttributes (tag='30') */
         if (PAsn1GetTagValue(&sIncludedParser) != 0x30)
         {
            PDebugWarning("static_PSecurityStackGpAclProcessorExtractAcmfFid: Bad commonObjectAttributes field");
            continue;
         }

         if (PAsn1MoveToNextTlv(&sIncludedParser) != W_SUCCESS)
         {
            PDebugWarning("static_PSecurityStackGpAclProcessorExtractAcmfFid: Malformed classAttributes field");
            continue;
         }

         /* Parse classAttributes (tag='30') */
         if (PAsn1GetTagValue(&sIncludedParser) != 0x30)
         {
            PDebugWarning("static_PSecurityStackGpAclProcessorExtractAcmfFid: Bad classAttributes field");
            continue;
         }

         if (PAsn1MoveToNextTlv(&sIncludedParser) != W_SUCCESS)
         {
            PDebugWarning("static_PSecurityStackGpAclProcessorExtractAcmfFid: Malformed typeAttributes field");
            continue;
         }

         /* Parse typeAttributes (tag='A1') */
         if (PAsn1GetTagValue(&sIncludedParser) != 0xA1)
         {
            PDebugWarning("static_PSecurityStackGpAclProcessorExtractAcmfFid: Bad typeAttributes field");
            continue;
         }

         if (PAsn1ParseIncludedTlv(&sIncludedParser, &sOidDoParser) != W_SUCCESS)
         {
            PDebugWarning("static_PSecurityStackGpAclProcessorExtractAcmfFid: Bad included TLV format (OidDO)");
            continue;
         }

         /* Parse OidDO (tag='30') */
         if (PAsn1GetTagValue(&sOidDoParser) != 0x30)
         {
            PDebugWarning("static_PSecurityStackGpAclProcessorExtractAcmfFid: Bad OidDO field");
            continue;
         }

         if (PAsn1ParseIncludedTlv(&sOidDoParser, &sOidDoParser) != W_SUCCESS)
         {
            PDebugWarning("static_PSecurityStackGpAclProcessorExtractAcmfFid: Bad included TLV format (OidDO)");
            continue;
         }

         /* Parse OidDO Object identifier (tag='06') */
         if (PAsn1GetTagValue(&sOidDoParser) != 0x06)
         {
            PDebugWarning("static_PSecurityStackGpAclProcessorExtractAcmfFid: Bad OidDO field");
            continue;
         }

         /* Check the EF(DODF) OID against the ACMF OID */
         PAsn1GetPointerOnBinaryContent(&sOidDoParser, &pBuffer, &nLength);
         if ((nLength != sizeof(static_OidACMF)) || (CMemoryCompare(static_OidACMF, pBuffer, nLength) != 0))
         {
            PDebugTrace("static_PSecurityStackGpAclProcessorExtractAcmfFid: Not the EF(ACMF) OID, look for the next entry");
            continue;
         }

         if (PAsn1MoveToNextTlv(&sOidDoParser) != W_SUCCESS)
         {
            PDebugWarning("static_PSecurityStackGpAclProcessorExtractAcmfFid: Malformed OidDO/path");
            continue;
         }

         /* Parse the EF(ACMF) File path */
         if (PAsn1GetTagValue(&sOidDoParser) != 0x30)
         {
            PDebugWarning("static_PSecurityStackGpAclProcessorExtractAcmfFid: Bad OidDO/path field");
            continue;
         }

         PAsn1GetPointerOnBinaryContent(&sOidDoParser, &pBuffer, &nLength);
         if ((nLength != 4) || (pBuffer[0] != 0x04) || (pBuffer[1] != 0x02))
         {
            PDebugWarning("static_PSecurityStackGpAclProcessorExtractAcmfFid: Malformed OidDO/path value");
            continue;
         }

         if (pnAcmfFid != null)
         {
            *pnAcmfFid = (uint16_t)((pBuffer[2] * 256) + pBuffer[3]);
         }

         return W_SUCCESS;
      }
   } while(PAsn1MoveToNextTlv(&sParser) == W_SUCCESS);

   PDebugWarning("static_PSecurityStackGpAclProcessorExtractAcmfFid: Tag 'A7' not found");
   return W_ERROR_BAD_PARAMETER;
}

/**
 * @brief Parses the contents of the EF(ACMF) file.
 *
 * @param[in]  pAclProcessorInstance  The ACL Processor instance.
 *
 * @param[in]  pEfAcmfBuffer  The buffer containing the EF(ACMF) bytes.
 *
 * @param[in]  nEfAcmfLength  The length of the EF(ACMF) buffer.
 *
 * @param[out]  pRefreshTag  The extracted refresh tag (must be 8-byte long).
 *
 * @param[out]  pnAcrfFid  The extracted FID of the EF(ACRF) file.
 *
 * @return One of the following error codes:
 *          - W_SUCCESS  The FCI has been successfully parsed.
 *          - W_ERROR_CONNECTION_COMPATIBILITY  The FCI is malformed.
 *          - W_ERROR_OUT_OF_RESOURCE  Out of memory.
 */
static W_ERROR static_PSecurityStackGpAclProcessorParseAcmf(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
                  const uint8_t* pEfAcmfBuffer,
                  uint32_t nEfAcmfLength,
                  uint8_t* pRefreshTag,
                  uint16_t* pnAcrfFid)
{
   W_ERROR nResult;
   tAsn1Parser sParser;
   tAsn1Parser sIncludedParser;
   const uint8_t* pBuffer;
   uint32_t nLength;

   PDebugTrace("static_PSecurityStackGpAclProcessorParseAcmf()");

   /*
    * Look for the following field:
    *
    * 30 10
    *    04 08
    *       XX XX XX XX XX XX XX XX = ##refresh-tag##
    *    30 04              path Path SEQUENCE: tag = [UNIVERSAL 16] constructed; length = 4
    *       04 02             path OCTET STRING: tag = [UNIVERSAL 4] primitive; length = 2
    *          XX XX         = XX XX -> Pointer to EF(ACRF)
    */

   nResult = PAsn1InitializeParser(&sParser, pEfAcmfBuffer, nEfAcmfLength);
   if (nResult != W_SUCCESS)
   {
      PDebugWarning("static_PSecurityStackGpAclProcessorParseAcmf: Bad TLV format");
      return nResult;
   }

   if (PAsn1GetTagValue(&sParser) != 0x30)
   {
      PDebugWarning("static_PSecurityStackGpAclProcessorParseAcmf: Bad ACMF field");
      return W_ERROR_BAD_PARAMETER;
   }

   nResult = PAsn1ParseIncludedTlv(&sParser, &sIncludedParser);
   if (nResult != W_SUCCESS)
   {
      PDebugWarning("static_PSecurityStackGpAclProcessorParseAcmf: Bad included TLV format");
      return nResult;
   }

   /* Parse the refreshTag field (tag='04', length='08') */
   if ((PAsn1GetTagValue(&sIncludedParser) != 0x04) || (PAsn1GetTagLength(&sIncludedParser) != 0x08))
   {
      PDebugWarning("static_PSecurityStackGpAclProcessorParseAcmf: Malformed refreshTag field");
      return W_ERROR_BAD_PARAMETER;
   }

   PAsn1GetPointerOnBinaryContent(&sIncludedParser, &pBuffer, &nLength);
   if(nLength != P_ACMF_REFRESH_TAG_LENGTH)
   {
      PDebugWarning("static_PSecurityStackGpAclProcessorParseAcmf: Bad refresh tag length");
      return W_ERROR_BAD_PARAMETER;
   }
   CMemoryCopy(pRefreshTag, pBuffer, nLength);

   if (PAsn1MoveToNextTlv(&sIncludedParser) != W_SUCCESS)
   {
      PDebugWarning("static_PSecurityStackGpAclProcessorParseAcmf: Bad rulesPath field");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Parse the rulesPath field (tag='30', length='04') */
   if ((PAsn1GetTagValue(&sIncludedParser) != 0x30) || (PAsn1GetTagLength(&sIncludedParser) != 0x04))
   {
      PDebugWarning("static_PSecurityStackGpAclProcessorParseAcmf: Malformed rulesPath field");
      return W_ERROR_BAD_PARAMETER;
   }

   PAsn1GetPointerOnBinaryContent(&sIncludedParser, &pBuffer, &nLength);

   if ((pBuffer[0] != 0x04) || (pBuffer[1] != 0x02))
   {
      PDebugWarning("static_PSecurityStackGpAclProcessorParseAcmf: Malformed rulesPath field");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pnAcrfFid != null)
   {
      *pnAcrfFid = (uint16_t)((pBuffer[2] * 256) + pBuffer[3]);
   }

   return W_SUCCESS;
}

/**
 * @brief Reads the next EF(ACCondition) file.
 *
 * @param[in]  pAclProcessorInstance  The ACL Processor instance.
 */
void static_PSecurityStackGpAclProcessorReadNextAcConditionFile
(
   tPSecurityStackAclProcessorInstance* pAclProcessorInstance
)
{
   W_ERROR nResult;
   tAsn1Parser* pParser;
   tAsn1Parser sIncludedParser;
   const uint8_t* pAid;
   uint32_t nAidLength;
   const uint8_t* pBuffer;
   uint32_t nLength;
   uint16_t nFid;
   bool_t bParseNextAcRule;

   PDebugTrace("static_PSecurityStackGpAclProcessorReadNextAcConditionFile()");

parse_next_ac_rule:

   pParser = &pAclProcessorInstance->sReadAcl.sAcrfParser;
   bParseNextAcRule = W_FALSE;

   if ((PAsn1CheckEndOfTlv(pParser) != W_FALSE) || (PAsn1GetTagValue(pParser) == 0xFF))
   {
      goto return_success;
   }

   if (PAsn1GetTagValue(pParser) != 0x30)
   {
      goto return_failure;
   }

   nResult = PAsn1ParseIncludedTlv(pParser, &sIncludedParser);
   if (nResult != W_SUCCESS)
   {
      goto return_failure;
   }

   /* Move to the next TLV so that the parser is ready when this function is called again */
   PAsn1MoveToNextTlv(pParser);
   pParser = (tAsn1Parser*)null;

   /* First tag must be either:
      - T='A0' - Specific AID (Octet String TLV)
      - T='81' (length='00') - Default AID
      - T='82' (length='00') - All AID
   */
   switch(PAsn1GetTagValue(&sIncludedParser))
   {
      case 0xA0:
      {
         tAsn1Parser sAidParser;

         nResult = PAsn1ParseIncludedTlv(&sIncludedParser, &sAidParser);
         if (nResult != W_SUCCESS)
         {
            goto return_failure;
         }

         nResult = PAsn1ParseOctetString(&sAidParser, &pAid, &nAidLength);
         if (nResult != W_SUCCESS)
         {
            goto return_failure;
         }

         if ((nAidLength != P_SECSTACK_MFID_LENGTH) && ((nAidLength < P_SECSTACK_AID_MIN_LENGTH) || (nAidLength > P_SECSTACK_AID_MAX_LENGTH)))
         {
            goto return_failure;
         }
         break;
      }

      case 0x81: /* Default AID */
      {
         if (PAsn1GetTagLength(&sIncludedParser) != 0x00)
         {
            goto return_failure;
         }

         pAid = (const uint8_t*)null;
         nAidLength = 0;
         break;
      }

      case 0x82: /* All AID */
      {
         if (PAsn1GetTagLength(&sIncludedParser) != 0x00)
         {
            goto return_failure;
         }

         pAid = P_SECSTACK_BUFFER_AID_ALL;
         nAidLength = P_SECSTACK_LENGTH_AID_ALL;
         break;
      }

      default:
         goto return_failure;
   }

   if (PAsn1MoveToNextTlv(&sIncludedParser) != W_SUCCESS)
   {
      goto return_failure;
   }

   /*
    * Second tag must '30' '04' '04' '02' 'xx' 'xx'
    */
   if ((PAsn1GetTagValue(&sIncludedParser) != 0x30) || (PAsn1GetTagLength(&sIncludedParser) != 0x04))
   {
      goto return_failure;
   }

   PAsn1GetPointerOnBinaryContent(&sIncludedParser, &pBuffer, &nLength);

   if ((pBuffer[0] != 0x04) || (pBuffer[1] != 0x02))
   {
      goto return_failure;
   }

   nFid = (uint16_t)((pBuffer[2] * 256) + pBuffer[3]);

   /* The file index/length is not known at this point, but it doesn't matter */
   nResult = PSecurityStackAclCreateAce(pAclProcessorInstance->sReadAcl.pAcl, nFid, /*dummy*/0, /*dummy*/(uint32_t)(-1),
      &pAclProcessorInstance->sReadAcl.pAce);
   if ((nResult != W_SUCCESS) && (nResult != W_ERROR_ITEM_NOT_FOUND))
   {
      goto return_failure;
   }

   if (nResult == W_SUCCESS)
   {
      /* The EF(ACCondition) file has already been read */
      bParseNextAcRule = W_TRUE;
   }

   nResult = PSecurityStackAclCreateAcie(pAclProcessorInstance->sReadAcl.pAcl, pAid, nAidLength,
      pAclProcessorInstance->sReadAcl.pAce);
   if (nResult != W_SUCCESS)
   {
      goto return_failure;
   }

   if (bParseNextAcRule != W_FALSE)
   {
      /* The EF(ACCondition) file has already been read */
      pAclProcessorInstance->sReadAcl.pAce = (tACE*)null;
      goto parse_next_ac_rule;
   }

   /* Set the next state to be reached */
   pAclProcessorInstance->sReadAcl.nState = P_STATE_READ_EF_ACCONDITION | P_STATUS_PENDING_EXECUTED;

   /* Select the EF(ACCondition) file' */
   static_PSecurityStackGpAclProcessorSelectFile(pAclProcessorInstance, nFid);
   return;

return_success:
   pAclProcessorInstance->sReadAcl.pAce = (tACE*)null;

   /* Set the next state to be reached */
   pAclProcessorInstance->sReadAcl.nState = P_STATE_FINAL | P_STATUS_PENDING_EXECUTED;

   static_PSecurityStackGpAclProcessorReadAclStateMachine(pAclProcessorInstance,
      P_EVENT_COMMAND_EXECUTED, 0, W_SUCCESS);
   return;

return_failure:
   PDebugWarning("static_PSecurityStackGpAclProcessorReadNextAcConditionFile: Malformed file");

   pAclProcessorInstance->sReadAcl.pAce = (tACE*)null;

   /* Set the next state to be reached */
   pAclProcessorInstance->sReadAcl.nState = P_STATE_FINAL | P_STATUS_PENDING_EXECUTED;

   static_PSecurityStackGpAclProcessorReadAclStateMachine(pAclProcessorInstance,
      P_EVENT_COMMAND_EXECUTED, 0, W_ERROR_RF_COMMUNICATION);
   return;
}

/** Parses a list of Principals */
static W_ERROR static_PSecurityStackGpAclProcessorParsePrincipals
(
  const tAsn1Parser* pAceParser,
  uint32_t* pnPrincipals,
  tPrincipal* pPrincipals,
  bool_t* pbGrantAll
)
{
   tAsn1Parser sPrincipalsParser = *pAceParser;
   uint32_t nPrincipals = 0;
   bool_t bGrantAll = W_FALSE;
   bool_t bEndOfTlv;
   uint8_t nTagValue;

   PDebugTrace("static_PSecurityStackGpAclProcessorParsePrincipals()");

   /*
    * The list of "Principal" contains:
    * - 30 00 -- Grant access to all terminal applications
    * - 30 16 04 14 <CertHash>
    */
   bEndOfTlv = PAsn1CheckEndOfTlv(&sPrincipalsParser);
   nTagValue = PAsn1GetTagValue(&sPrincipalsParser);
   while((bEndOfTlv == W_FALSE) && (nTagValue != 0xFF))
   {
      uint32_t nLength = 0;
      const uint8_t* pValue = (uint8_t*)null;

      if (nTagValue != 0x30)
      {
         PDebugError("static_PSecurityStackGpAclProcessorParsePrincipals: Unexpected tag");
         return W_ERROR_BAD_PARAMETER;
      }

      PAsn1GetPointerOnBinaryContent(&sPrincipalsParser, &pValue, &nLength);

      if ((nLength == 0) && (pValue != null))
      {
         bGrantAll = W_TRUE;
      }
      else if ((nLength == (2 + P_SECSTACK_PRINCIPALID_LENGTH)) && (pValue[0] == 0x04) && (pValue[1] == 0x14))
      {
         if ((pPrincipals != null) && (pnPrincipals != null))
         {
            if (nPrincipals >= (*pnPrincipals))
            {
               /* There is not enough space in the passed array */
               PDebugError("static_PSecurityStackGpAclProcessorParsePrincipals: Buffer too short");
               return W_ERROR_BUFFER_TOO_SHORT;
            }

            /* +2 to skip 0x04, 0x14 */
            CMemoryCopy(pPrincipals[nPrincipals].id, pValue + 2, nLength - 2);
         }

         nPrincipals++;
      }
      else
      {
         PDebugError("static_PSecurityStackGpAclProcessorParsePrincipals: Malformed tag");
         return W_ERROR_BAD_PARAMETER;
      }

      /* Move to the next TLV. An error is returned if the end is reached. */
      if (PAsn1MoveToNextTlv(&sPrincipalsParser) != W_SUCCESS)
      {
         bEndOfTlv = PAsn1CheckEndOfTlv(&sPrincipalsParser);
         if (bEndOfTlv == W_FALSE)
         {
            nTagValue = PAsn1GetTagValue(&sPrincipalsParser);
            if (nTagValue != 0xFF)
            {
               /* An error has been returned and the end is not reached */
               PDebugError("static_PSecurityStackGpAclProcessorParsePrincipals: Malformed tag");
               return W_ERROR_BAD_PARAMETER;
            }
         }
      }
      else
      {
         nTagValue = PAsn1GetTagValue(&sPrincipalsParser);
      }
   }

   if (pnPrincipals != null)
   {
      *pnPrincipals = nPrincipals;
   }

   if (pbGrantAll != null)
   {
      *pbGrantAll = bGrantAll;
   }

   return W_SUCCESS;
}

/** Parses the contents of the EF(ACCondition) file */
static void static_PSecurityStackGpAclProcessorParseAcConditionFile
(
   tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
   const uint8_t* pAcConditionFile,
   uint32_t nAcConditionFileLength
)
{
   W_ERROR nResult;
   tAsn1Parser sParser;
   uint32_t nPrincipals = 0;
   tPrincipal* pPrincipals = (tPrincipal*)null;
   bool_t bGrantAll = W_FALSE;

   PDebugTrace("static_PSecurityStackGpAclProcessorParseAcConditionFile()");

   if (nAcConditionFileLength == 0)
   {
      /* If the file is empty, access is denied to all terminal applications */
      /* Note: This is the default state of the ACE */
      goto return_success;
   }

   nResult = PAsn1InitializeParser(&sParser, pAcConditionFile, nAcConditionFileLength);
   if (nResult != W_SUCCESS)
   {
      PDebugWarning("static_PSecurityStackGpAclProcessorParseAcConditionFile: Bad TLV format");

      /* If the file is invalid, access is denied to all terminal applications */
      /* Note: This is the default state of the ACE */
      goto return_success;
   }

   nResult = static_PSecurityStackGpAclProcessorParsePrincipals(&sParser, &nPrincipals, (tPrincipal*)null, &bGrantAll);
   if (nResult != W_SUCCESS)
   {
      /* If the file is invalid, access is denied to all terminal applications */
      /* Note: This is the default state of the ACE */
      goto return_success;
   }

   if (bGrantAll != W_FALSE)
   {
      nResult = PSecurityStackAclCreateAcePrincipals(pAclProcessorInstance->sReadAcl.pAcl,
         pAclProcessorInstance->sReadAcl.pAce, 0, (tPrincipal**)null);
      if (nResult != W_SUCCESS)
      {
         goto return_failure;
      }
   }
   else
   {
      nResult = PSecurityStackAclCreateAcePrincipals(pAclProcessorInstance->sReadAcl.pAcl,
         pAclProcessorInstance->sReadAcl.pAce, nPrincipals, &pPrincipals);
      if (nResult != W_SUCCESS)
      {
         goto return_failure;
      }

      nResult = static_PSecurityStackGpAclProcessorParsePrincipals(&sParser, &nPrincipals, pPrincipals, (bool_t*)null);
      if (nResult != W_SUCCESS)
      {
         goto return_failure;
      }
   }

   /* Allow all APDU commands */
   nResult = PSecurityStackAclCreateAcePermissions(pAclProcessorInstance->sReadAcl.pAcl,
         pAclProcessorInstance->sReadAcl.pAce, 0, (tPermission**)null);

   if (nResult != W_SUCCESS)
   {
      goto return_failure;
   }

   goto return_success;

return_success:
   pAclProcessorInstance->sReadAcl.pAce = (tACE*)null;

   /* Free internal READ BINARY buffer */
   static_PSecurityStackGpAclProcessorReadBinaryFileFree(pAclProcessorInstance);

   /* Set the next state to be reached */
   pAclProcessorInstance->sReadAcl.nState = P_STATE_READ_EF_ACCONDITION | P_STATUS_PENDING_EXECUTED;

   /* Read the next EF(ACCondition) refered to from EF(ACRF) */
   static_PSecurityStackGpAclProcessorReadNextAcConditionFile(pAclProcessorInstance);
   return;

return_failure:

   PDebugWarning("static_PSecurityStackGpAclProcessorParseAcConditionFile: returning an error");

   pAclProcessorInstance->sReadAcl.pAce = (tACE*)null;

   /* Free internal READ BINARY buffer */
   static_PSecurityStackGpAclProcessorReadBinaryFileFree(pAclProcessorInstance);

   /* Set the next state to be reached */
   pAclProcessorInstance->sReadAcl.nState = P_STATE_FINAL | P_STATUS_PENDING_EXECUTED;

   static_PSecurityStackGpAclProcessorReadAclStateMachine(pAclProcessorInstance,
      P_EVENT_COMMAND_EXECUTED, 0, W_ERROR_RF_COMMUNICATION);
   return;
}

/**
 * @brief The GlobalPlatform ACL reading state machine.
 */
static void static_PSecurityStackGpAclProcessorReadAclStateMachine
(
   tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
   uint32_t nEvent,
   uint32_t nDataLength,
   W_ERROR nResult
)
{
   tSecurityStackACL* pAcl = (tSecurityStackACL*)null;

   PDebugTrace("static_PSecurityStackGpAclProcessorReadAclStateMachine()");

   switch (nEvent)
   {
      case P_EVENT_INITIAL:
         pAclProcessorInstance->sReadAcl.nState = P_STATE_SELECT_EF_ODF;
         break;

      case P_EVENT_COMMAND_EXECUTED:
         pAclProcessorInstance->sReadAcl.nState &= ~P_STATUS_PENDING_EXECUTED;
         break;
   }

   if(nResult != W_SUCCESS)
   {
      PDebugError("Security GP SM: receiving error %s", PUtilTraceError(nResult));

      if (nResult == W_ERROR_TIMEOUT) /* The SE is not responding */
      {
         goto notify_failure;
      }

      pAclProcessorInstance->sReadAcl.nState = P_STATE_ERROR;
      /* FALL THROUGH */
   }

   switch(pAclProcessorInstance->sReadAcl.nState)
   {
      case P_STATE_SELECT_EF_ODF:
      {
         PDebugTrace("Security GP SM: SELECT_EF_ODF");

         /* Set the next state to be reached */
         pAclProcessorInstance->sReadAcl.nState = P_STATE_READ_EF_ODF | P_STATUS_PENDING_EXECUTED;

         /* Select the EF(ODF) file - FID='5031' */
         static_PSecurityStackGpAclProcessorSelectFile(pAclProcessorInstance, 0x5031);
         return;
      }

      case P_STATE_READ_EF_ODF:
      {
         uint16_t nFileSize;

         PDebugTrace("Security GP SM: READ_EF_ODF");

         /* Check answer to the SELECT EF(ODF) command */
         if ((nDataLength < 2)
             || (pAclProcessorInstance->sReadAcl.aCardToReaderBuffer[nDataLength - 2] != 0x90)
             || (pAclProcessorInstance->sReadAcl.aCardToReaderBuffer[nDataLength - 1] != 0x00))
         {
            PDebugError("Security GP SM: READ_EF_ODF: error in APDU");
            nResult = W_ERROR_RF_COMMUNICATION;
            goto notify_failure;
         }

         nDataLength -= 2; /* Remove SW */

         nResult = static_PSecurityStackGpAclProcessorExtractFileSizeFromFcp(pAclProcessorInstance,
            pAclProcessorInstance->sReadAcl.aCardToReaderBuffer, nDataLength, &nFileSize);
         if (nResult != W_SUCCESS)
         {
            PDebugError("Security GP SM: READ_EF_ODF: error returned by static_PSecurityStackGpAclProcessorExtractFileSizeFromFcp()");
            nResult = W_ERROR_RF_COMMUNICATION;
            goto notify_failure;
         }

         /* Set the next state to be reached */
         pAclProcessorInstance->sReadAcl.nState = P_STATE_SELECT_EF_DODF | P_STATUS_PENDING_EXECUTED;

         /* Read the EF(ODF) file */
         static_PSecurityStackGpAclProcessorReadBinaryFile(pAclProcessorInstance, nFileSize);
         return;
      }

      case P_STATE_SELECT_EF_DODF:
      {
         uint16_t nDodfFid;

         PDebugTrace("Security GP SM: SELECT_EF_DODF");

         /* Parse the EF(ODF) file contents to extract the FID of the EF(DODF) file */
         nResult = static_PSecurityStackGpAclProcessorExtractDodfFid(pAclProcessorInstance,
            pAclProcessorInstance->sReadBinary.pFileDataBuffer, nDataLength, &nDodfFid);

         /* Free internal buffer, whatever result */
         static_PSecurityStackGpAclProcessorReadBinaryFileFree(pAclProcessorInstance);

         if (nResult != W_SUCCESS)
         {
            PDebugError("Security GP SM: SELECT_EF_DODF: error returned by static_PSecurityStackGpAclProcessorExtractDodfFid()");
            nResult = W_ERROR_RF_COMMUNICATION;
            goto notify_failure;
         }

         /* Set the next state to be reached */
         pAclProcessorInstance->sReadAcl.nState = P_STATE_READ_EF_DODF | P_STATUS_PENDING_EXECUTED;

         /* Select the EF(DODF) file */
         static_PSecurityStackGpAclProcessorSelectFile(pAclProcessorInstance, nDodfFid);
         return;
      }

      case P_STATE_READ_EF_DODF:
      {
         uint16_t nFileSize;

         PDebugTrace("Security GP SM: READ_EF_DODF");

         /* Check answer to the SELECT EF(DODF) command */
         if ((nDataLength < 2)
             || (pAclProcessorInstance->sReadAcl.aCardToReaderBuffer[nDataLength - 2] != 0x90)
             || (pAclProcessorInstance->sReadAcl.aCardToReaderBuffer[nDataLength - 1] != 0x00))
         {
            PDebugError("Security GP SM: READ_EF_DODF: error in R-APDU");
            nResult = W_ERROR_RF_COMMUNICATION;
            goto notify_failure;
         }

         nDataLength -= 2; /* Remove SW */

         nResult = static_PSecurityStackGpAclProcessorExtractFileSizeFromFcp(pAclProcessorInstance,
            pAclProcessorInstance->sReadAcl.aCardToReaderBuffer, nDataLength, &nFileSize);
         if (nResult != W_SUCCESS)
         {
            PDebugError("Security GP SM: READ_EF_DODF: error returned by static_PSecurityStackGpAclProcessorExtractFileSizeFromFcp()");
            nResult = W_ERROR_RF_COMMUNICATION;
            goto notify_failure;
         }

         /* Set the next state to be reached */
         pAclProcessorInstance->sReadAcl.nState = P_STATE_SELECT_EF_ACMF | P_STATUS_PENDING_EXECUTED;

         /* Read the EF(ACMF) file */
         static_PSecurityStackGpAclProcessorReadBinaryFile(pAclProcessorInstance, nFileSize);
         return;
      }

      case P_STATE_SELECT_EF_ACMF:
      {
         uint16_t nAcmfFid;

         PDebugTrace("Security GP SM: SELECT_EF_ACMF");

         /* Parse the EF(DODF) file contents to extract the FID of the ACMF file */
         nResult = static_PSecurityStackGpAclProcessorExtractAcmfFid(pAclProcessorInstance,
            pAclProcessorInstance->sReadBinary.pFileDataBuffer, nDataLength, &nAcmfFid);

         /* Free internal buffer, whatever result */
         static_PSecurityStackGpAclProcessorReadBinaryFileFree(pAclProcessorInstance);

         if (nResult != W_SUCCESS)
         {
            PDebugError("Security GP SM: SELECT_EF_ACMF: error returned by static_PSecurityStackGpAclProcessorReadBinaryFileFree()");
            nResult = W_ERROR_RF_COMMUNICATION;
            goto notify_failure;
         }

         /* Remember the FID of the EF(ACMF) file to manage the STK REFRESH command */
         pAclProcessorInstance->nDfPkcs15AcmfFid = nAcmfFid;

         /* Set the next state to be reached */
         pAclProcessorInstance->sReadAcl.nState = P_STATE_READ_EF_ACMF | P_STATUS_PENDING_EXECUTED;

         /* Select the EF(ACMF) file */
         static_PSecurityStackGpAclProcessorSelectFile(pAclProcessorInstance, nAcmfFid);
         return;
      }

      case P_STATE_READ_EF_ACMF:
      {
         uint16_t nFileSize;

         PDebugTrace("Security GP SM: READ_EF_ACMF");

         /* Check answer to the SELECT EF(ACMF) command */
         if ((nDataLength < 2)
             || (pAclProcessorInstance->sReadAcl.aCardToReaderBuffer[nDataLength - 2] != 0x90)
             || (pAclProcessorInstance->sReadAcl.aCardToReaderBuffer[nDataLength - 1] != 0x00))
         {
            PDebugError("Security GP SM: READ_EF_ACMF; error in R-APDU");
            nResult = W_ERROR_RF_COMMUNICATION;
            goto notify_failure;
         }

         nDataLength -= 2; /* Remove SW */

         nResult = static_PSecurityStackGpAclProcessorExtractFileSizeFromFcp(pAclProcessorInstance,
            pAclProcessorInstance->sReadAcl.aCardToReaderBuffer, nDataLength, &nFileSize);
         if (nResult != W_SUCCESS)
         {
            PDebugError("Security GP SM: READ_EF_ACMF; error returned static_PSecurityStackGpAclProcessorExtractFileSizeFromFcp()");
            nResult = W_ERROR_RF_COMMUNICATION;
            goto notify_failure;
         }

         /* Set the next state to be reached */
         pAclProcessorInstance->sReadAcl.nState = P_STATE_SELECT_EF_ACRF | P_STATUS_PENDING_EXECUTED;

         /* Read the EF(ACRF) file */
         static_PSecurityStackGpAclProcessorReadBinaryFile(pAclProcessorInstance, nFileSize);
         return;
      }

      case P_STATE_SELECT_EF_ACRF:
      {
         uint16_t nAcrfFid;

         PDebugTrace("Security GP SM: SELECT_EF_ACRF");

         nResult = static_PSecurityStackGpAclProcessorParseAcmf(pAclProcessorInstance,
            pAclProcessorInstance->sReadBinary.pFileDataBuffer, nDataLength,
            pAclProcessorInstance->sReadAcl.aRefreshTag, &nAcrfFid);

         /* Free internal buffer, whatever result */
         static_PSecurityStackGpAclProcessorReadBinaryFileFree(pAclProcessorInstance);

         if (nResult != W_SUCCESS)
         {
            PDebugError("Security GP SM: SELECT_EF_ACRF: error returned static_PSecurityStackGpAclProcessorReadBinaryFileFree()");
            nResult = W_ERROR_RF_COMMUNICATION;
            goto notify_failure;
         }

         /* Check whether the ACL timestamp has changed since the last read */
         if (CMemoryCompare(pAclProcessorInstance->aACLTime, pAclProcessorInstance->sReadAcl.aRefreshTag, P_ACMF_REFRESH_TAG_LENGTH) == 0)
         {
            /* The timestamp has not changed - No need to read the ACL again */
            PDebugLog("Security GP SM: SELECT_EF_ACRF: The ACL has not changed. No need to read it again.");
            pAclProcessorInstance->nLastUpdateTime = pAclProcessorInstance->nNow;
            goto notify_success_nochange;
         }

         /* Force reading the ACL */
         pAclProcessorInstance->bTouched = W_TRUE;
         pAclProcessorInstance->nLastUpdateTime = 0;
         pAclProcessorInstance->nDfPkcs15AcmfFid = 0x3F00; /* Ensure no collision with actual FID */

         /* Set the next state to be reached */
         pAclProcessorInstance->sReadAcl.nState = P_STATE_READ_EF_ACRF | P_STATUS_PENDING_EXECUTED;

         /* Select the EF(ACMF) file */
         static_PSecurityStackGpAclProcessorSelectFile(pAclProcessorInstance, nAcrfFid);
         return;
      }

      case P_STATE_READ_EF_ACRF:
      {
         uint16_t nFileSize;

         PDebugTrace("Security GP SM: READ_EF_ACRF");

         /* Check answer to the SELECT EF(ACRF) command */
         if ((nDataLength < 2)
             || (pAclProcessorInstance->sReadAcl.aCardToReaderBuffer[nDataLength - 2] != 0x90)
             || (pAclProcessorInstance->sReadAcl.aCardToReaderBuffer[nDataLength - 1] != 0x00))
         {
            PDebugError("Security GP SM: READ_EF_ACRF: error in R-APDU");
            nResult = W_ERROR_RF_COMMUNICATION;
            goto notify_failure;
         }

         nDataLength -= 2; /* Remove SW */

         nResult = static_PSecurityStackGpAclProcessorExtractFileSizeFromFcp(pAclProcessorInstance,
            pAclProcessorInstance->sReadAcl.aCardToReaderBuffer, nDataLength, &nFileSize);
         if (nResult != W_SUCCESS)
         {
            PDebugError("Security GP SM: READ_EF_ACRF: error returned by static_PSecurityStackGpAclProcessorExtractFileSizeFromFcp()");
            nResult = W_ERROR_RF_COMMUNICATION;
            goto notify_failure;
         }

         /* Set the next state to be reached */
         pAclProcessorInstance->sReadAcl.nState = P_STATE_PROCESS_EF_ACRF | P_STATUS_PENDING_EXECUTED;

         /* Read the EF(ACRF) file */
         static_PSecurityStackGpAclProcessorReadBinaryFile(pAclProcessorInstance, nFileSize);
         return;
      }

      case P_STATE_PROCESS_EF_ACRF:
      {
         PDebugTrace("Security GP SM: PROCESS_EF_ACRF");

         if (nDataLength == 0)
         {
            /* The EF(ACRF) file is empty */
            PDebugTrace("Security GP SM: PROCESS_EF_ACRF: The EF(ACRF) file is empty");
            nResult = W_ERROR_SECURITY;
            goto notify_failure;
         }

         if (pAclProcessorInstance->sReadBinary.bAllocated != W_FALSE)
         {
            /* Transfer buffer ownership */
            pAclProcessorInstance->sReadAcl.pAcrfBuffer = pAclProcessorInstance->sReadBinary.pFileDataBuffer;
            pAclProcessorInstance->sReadBinary.pFileDataBuffer = (uint8_t*)null;
            pAclProcessorInstance->sReadBinary.bAllocated = W_FALSE;
         }
         else
         {
            CDebugAssert(pAclProcessorInstance->sReadAcl.pAcrfBuffer == null);
            pAclProcessorInstance->sReadAcl.pAcrfBuffer = (uint8_t*)CMemoryAlloc(nDataLength);
            if (pAclProcessorInstance->sReadAcl.pAcrfBuffer == null)
            {
               PDebugWarning("Security GP SM: PROCESS_EF_ACRF: Failed to allocate %d bytes", nDataLength);
               nResult = W_ERROR_RF_COMMUNICATION;
               goto notify_failure;
            }
            CMemoryCopy(pAclProcessorInstance->sReadAcl.pAcrfBuffer, pAclProcessorInstance->sReadBinary.pFileDataBuffer, nDataLength);
         }

         /* Free internal buffer, whatever result */
         static_PSecurityStackGpAclProcessorReadBinaryFileFree(pAclProcessorInstance);

         /* Initialize the ASN.1 parser for decoding the EF(ACRF) file */
         nResult = PAsn1InitializeParser(&pAclProcessorInstance->sReadAcl.sAcrfParser,
            pAclProcessorInstance->sReadAcl.pAcrfBuffer, nDataLength);

         if (nResult != W_SUCCESS)
         {
            PDebugWarning("Security GP SM: PROCESS_EF_ACRF: Failed to parse the EF(ACRF) file");
            nResult = W_ERROR_RF_COMMUNICATION;
            goto notify_failure;
         }

         CDebugAssert(pAclProcessorInstance->sReadAcl.pAcl == null);
         nResult = PSecurityStackAclCreateInstance(W_TRUE, &pAclProcessorInstance->sReadAcl.pAcl);

         if (nResult != W_SUCCESS)
         {
            PDebugError("Security GP SM: PROCESS_EF_ACRF: Failed to allocate ACL");
            goto notify_failure;
         }

         /* Set the next state to be reached */
         pAclProcessorInstance->sReadAcl.nState = P_STATE_READ_EF_ACCONDITION | P_STATUS_PENDING_EXECUTED;

         /* Read the first EF(ACCondition) refered to from EF(ACRF) */
         static_PSecurityStackGpAclProcessorReadNextAcConditionFile(pAclProcessorInstance);
         return;
      }

      case P_STATE_READ_EF_ACCONDITION:
      {
         uint16_t nFileSize;

         PDebugTrace("Security GP SM: READ_EF_ACCONDITION");

         /* Check answer to the SELECT EF(ACCondition) command */
         if ((nDataLength < 2)
             || (pAclProcessorInstance->sReadAcl.aCardToReaderBuffer[nDataLength - 2] != 0x90)
             || (pAclProcessorInstance->sReadAcl.aCardToReaderBuffer[nDataLength - 1] != 0x00))
         {
            PDebugTrace("Security GP SM: READ_EF_ACCONDITION: Failed to select EF(ACCondition). Using deny-all rule.");
            goto read_ef_accondition_failure;
         }

         nDataLength -= 2; /* Remove SW */

         nResult = static_PSecurityStackGpAclProcessorExtractFileSizeFromFcp(pAclProcessorInstance,
            pAclProcessorInstance->sReadAcl.aCardToReaderBuffer, nDataLength, &nFileSize);
         if (nResult != W_SUCCESS)
         {
            PDebugTrace("Security GP SM: READ_EF_ACCONDITION: Failed to extract EF(ACCondition) file size from FCP. Using deny-all rule.");
            goto read_ef_accondition_failure;
         }

         /* Set the next state to be reached */
         pAclProcessorInstance->sReadAcl.nState = P_STATE_PROCESS_EF_ACCONDITION | P_STATUS_PENDING_EXECUTED;

         /* Read the EF(ACCondition) file */
         static_PSecurityStackGpAclProcessorReadBinaryFile(pAclProcessorInstance, nFileSize);
         return;

read_ef_accondition_failure:

         /* The deny-all rule has already been generated in the ACL (in static_PSecurityStackGpAclProcessorReadNextAcConditionFile) */

         /* Set the next state to be reached */
         pAclProcessorInstance->sReadAcl.nState = P_STATE_READ_EF_ACCONDITION | P_STATUS_PENDING_EXECUTED;

         /* Read the next EF(ACCondition) refered to from EF(ACRF) */
         static_PSecurityStackGpAclProcessorReadNextAcConditionFile(pAclProcessorInstance);
         return;
      }

      case P_STATE_PROCESS_EF_ACCONDITION:
      {
         PDebugTrace("Security GP SM: PROCESS_EF_ACCONDITION");
         static_PSecurityStackGpAclProcessorParseAcConditionFile(pAclProcessorInstance,
            pAclProcessorInstance->sReadBinary.pFileDataBuffer, nDataLength);
         return;
      }

      case P_STATE_READ_BINARY:
      {
         PDebugTrace("Security GP SM: READ_BINARY");
         static_PSecurityStackGpAclProcessorReadBinaryFileCompleted(pAclProcessorInstance, (uint16_t)nDataLength, nResult);
         return;
      }

      case P_STATE_FINAL:
      {
         PDebugTrace("Security GP SM: FINAL: The PKCS#15 application ACL has been successfully parsed");

         /* Remember the timestamp of this ACL */
         CMemoryCopy(pAclProcessorInstance->aACLTime, pAclProcessorInstance->sReadAcl.aRefreshTag, P_ACMF_REFRESH_TAG_LENGTH);

         /* ACL has been read */
         if(pAclProcessorInstance->bAutoResetTouch != W_FALSE)
         {
            pAclProcessorInstance->bTouched = W_FALSE;
         }
         pAclProcessorInstance->nLastUpdateTime = pAclProcessorInstance->nNow;

         /* Return newly parsed ACL */
         pAcl = pAclProcessorInstance->sReadAcl.pAcl;
         pAclProcessorInstance->sReadAcl.pAcl = (tSecurityStackACL*)null;

         goto notify_success;
      }

      case P_STATE_ERROR:
      {
         PDebugTrace("Security GP SM: ERROR");
         goto notify_failure;
      }
   }

   nResult = W_ERROR_SECURITY;
   /* FALL THROUGH */

notify_failure:
   CDebugAssert(pAcl == null);
   CDebugAssert(nResult != W_SUCCESS);

   if (pAclProcessorInstance->sReadAcl.pAcl != null)
   {
      PSecurityStackAclDestroyInstance(pAclProcessorInstance->sReadAcl.pAcl);
      pAclProcessorInstance->sReadAcl.pAcl = (tSecurityStackACL*)null;
      pAclProcessorInstance->sReadAcl.pAce = (tACE*)null;
   }

   /* Force reading the ACL the next time this ACL Processor is called */
   pAclProcessorInstance->bTouched = W_TRUE;
   pAclProcessorInstance->nLastUpdateTime = 0;
   pAclProcessorInstance->nDfPkcs15AcmfFid = 0x3F00; /* Ensure no collision with actual FID */

   goto notify_result;

notify_success_nochange:
   CDebugAssert(pAcl == null);
   CDebugAssert(nResult == W_SUCCESS);
   goto notify_result;

notify_success:
   CDebugAssert(pAcl != null);
   CDebugAssert(nResult == W_SUCCESS);
   /* FALL THROUGH */

notify_result:
   pAclProcessorInstance->sReadAcl.nState = P_STATE_IDLE;

   if (pAclProcessorInstance->sReadAcl.pAcrfBuffer != null)
   {
      CMemoryFree(pAclProcessorInstance->sReadAcl.pAcrfBuffer);
      pAclProcessorInstance->sReadAcl.pAcrfBuffer = (uint8_t*)null;
   }

   CDebugAssert(pAclProcessorInstance->sReadAcl.pAcrfBuffer == null);
   CDebugAssert(pAclProcessorInstance->sReadAcl.pAcl == null);
   CDebugAssert(pAclProcessorInstance->sReadAcl.pAce == null);

   if(nResult == W_SUCCESS)
   {
      PDebugTrace("Security GP SM: Sending success result");
   }
   else
   {
      PDebugError("Security GP SM: Sending error result %s", PUtilTraceError(nResult));
   }

   /* Notify sResult to the caller */
   pAclProcessorInstance->sResult.pCallback(
      pAclProcessorInstance->sResult.pCallbackParameter1,
      pAclProcessorInstance->sResult.pCallbackParameter2,
      pAcl, nResult);
}

/* Best Before timeout */
#define P_SEC_STACK_TIMEOUT  60000

/** See tPSecurityStackAclProcessorReadAcl */
static W_ERROR static_PSecurityStackGpAclProcessorReadAcl(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
                  tPSecurityStackAclProcessorExchangeApdu pExchangeApdu,
                  void* pExchangeApduCallbackParameter1,
                  void* pExchangeApduCallbackParameter2,
                  tPSecurityStackAclProcessorReadAclCallbackFunction pCallback,
                  void* pCallbackParameter1,
                  void* pCallbackParameter2,
                  uint32_t nNow)
{
   PDebugTrace("static_PSecurityStackGpAclProcessorReadAcl()");

   if (pAclProcessorInstance == null)
   {
      PDebugError("static_PSecurityStackGpAclProcessorReadAcl: pAclProcessorInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pExchangeApdu == null)
   {
      PDebugError("static_PSecurityStackGpAclProcessorReadAcl: pExchangeApdu == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pCallback == null)
   {
      PDebugError("static_PSecurityStackGpAclProcessorReadAcl: pCallback == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pAclProcessorInstance->bTouched == W_FALSE)
   {
      PDebugError("static_PSecurityStackGpAclProcessorReadAcl: bTouched == W_FALSE, No need to read the ACL again");
      return W_SUCCESS;
   }

   if(pAclProcessorInstance->nLastUpdateTime != 0)
   {
      uint32_t nExpirationTime = pAclProcessorInstance->nLastUpdateTime + P_SEC_STACK_TIMEOUT;
      uint32_t nDelta;

      if (nExpirationTime <= nNow)
      {
         nDelta = nNow - nExpirationTime;
      }
      else
      {
         nDelta  = 0xFFFFFFFF - nExpirationTime + nNow;
      }

      if (nDelta > 0x7FFFFFFF)
      {
         PDebugTrace("static_PSecurityStackGpAclProcessorReadAcl: Timeout not expired, No need to read the ACL again");
         return W_SUCCESS;
      }
   }
   pAclProcessorInstance->nNow = nNow;

   /* Remember callbacks and callback parameters */
   pAclProcessorInstance->sExchange.pExchangeApdu = pExchangeApdu;
   pAclProcessorInstance->sExchange.pCallbackParameter1 = pExchangeApduCallbackParameter1;
   pAclProcessorInstance->sExchange.pCallbackParameter2 = pExchangeApduCallbackParameter2;

   pAclProcessorInstance->sResult.pCallback = pCallback;
   pAclProcessorInstance->sResult.pCallbackParameter1 = pCallbackParameter1;
   pAclProcessorInstance->sResult.pCallbackParameter2 = pCallbackParameter2;

   /* Start reading the ACL */
   PDebugTrace("static_PSecurityStackGpAclProcessorReadAcl: Starting to read the ACF...");

   /* Initialize the state machine */
   static_PSecurityStackGpAclProcessorReadAclStateMachine(pAclProcessorInstance,
      P_EVENT_INITIAL, 0, W_SUCCESS);

   return W_ERROR_OPERATION_PENDING;
}

/** See tPSecurityStackAclProcessorNotifyStkRefresh */
static W_ERROR static_PSecurityStackGpAclProcessorNotifyStkRefresh(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
                  uint32_t nCommand,
                  const uint8_t* pRefreshFileList,
                  uint32_t nRefreshFileListLength)
{
   uint32_t nValueLength;
   uint32_t nNumberOfFiles;
   uint16_t nEfFid;

   /* @todo - This code must be tested with a real UICC card! */

   PDebugTrace("static_PSecurityStackGpAclProcessorNotifyStkRefresh()");

   if (pAclProcessorInstance == null)
   {
      PDebugError("static_PSecurityStackGpAclProcessorNotifyStkRefresh: pAclProcessorInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((pRefreshFileList == null) || (nRefreshFileListLength == 0))
   {
      PDebugError("static_PSecurityStackGpAclProcessorNotifyStkRefresh: pRefreshFileList == null || nRefreshFileListLength == 0");
      return W_ERROR_BAD_PARAMETER;
   }

   /*
    * Reference: ETSI TS 102 223, Section 8.18 (File List) and Annex C.
    */

   /* Check "file list tag" */
   if ((pRefreshFileList[0] & 0x7F) != 0x12)
   {
      PDebugError("static_PSecurityStackGpAclProcessorNotifyStkRefresh: Tag field (file list tag) is incorrect");
      return W_ERROR_BAD_PARAMETER;
   }

   pRefreshFileList++;
   nRefreshFileListLength--;

   /* Check file list length (encoded as par simple-TLV) */
   if (nRefreshFileListLength == 0)
   {
      PDebugError("static_PSecurityStackGpAclProcessorNotifyStkRefresh: Length field is absent");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((pRefreshFileList[0] & 0x7F) == 0)
   {
      /* 1-byte length field */
      nValueLength = pRefreshFileList[0];

      pRefreshFileList++;
      nRefreshFileListLength--;
   }
   else if ((pRefreshFileList[0] == 0x81) && (nRefreshFileListLength >= 2))
   {
      /* 2-byte length field */
      nValueLength = pRefreshFileList[1];

      pRefreshFileList += 2;
      nRefreshFileListLength -= 2;
   }
   else
   {
      PDebugError("static_PSecurityStackGpAclProcessorNotifyStkRefresh: Length field is incorrect");
      return W_ERROR_BAD_PARAMETER;
   }

   if (nValueLength != nRefreshFileListLength)
   {
      PDebugError("static_PSecurityStackGpAclProcessorNotifyStkRefresh: Length field value is inconsistent with the file list length");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Get the number of files */
   if (nRefreshFileListLength == 0)
   {
      PDebugError("static_PSecurityStackGpAclProcessorNotifyStkRefresh: The Number of Files field is absent");
      return W_ERROR_BAD_PARAMETER;
   }

   nNumberOfFiles = pRefreshFileList[0];

   pRefreshFileList++;
   nRefreshFileListLength--;

   if ((nRefreshFileListLength % 1) != 0)
   {
      PDebugError("static_PSecurityStackGpAclProcessorNotifyStkRefresh: The Files path length must be even");
      return W_ERROR_BAD_PARAMETER;
   }

   while(nNumberOfFiles-->0)
   {
      if (nRefreshFileListLength == 0)
      {
         PDebugError("static_PSecurityStackGpAclProcessorNotifyStkRefresh: The Files path is inconsistent with the number of files");
         return W_ERROR_BAD_PARAMETER;
      }

      CDebugAssert(nRefreshFileListLength >= 2);

      /* Path must start with P_7816SM_MF_FIRST_BYTE */
      if (pRefreshFileList[0] != P_7816SM_MF_FIRST_BYTE)
      {
         PDebugError("static_PSecurityStackGpAclProcessorNotifyStkRefresh: The Files path is inconsistent with the number of files");
         return W_ERROR_BAD_PARAMETER;
      }

      pRefreshFileList += 2;
      nRefreshFileListLength -= 2;

      if (nRefreshFileListLength == 0)
      {
         PDebugError("static_PSecurityStackGpAclProcessorNotifyStkRefresh: The Files path does not contain a DF/EF after the MF");
         return W_ERROR_BAD_PARAMETER;
      }

      CDebugAssert(nRefreshFileListLength >= 2);

      /* There must be a DF or an EF (must not start with P_7816SM_MF_FIRST_BYTE) */
      if (pRefreshFileList[0] == P_7816SM_MF_FIRST_BYTE)
      {
         PDebugError("static_PSecurityStackGpAclProcessorNotifyStkRefresh: The Files path does not contain a DF/EF after the MF");
         return W_ERROR_BAD_PARAMETER;
      }

      nEfFid = ((pRefreshFileList[0] << 8) + pRefreshFileList[1]);

      pRefreshFileList += 2;
      nRefreshFileListLength -= 2;

      /* Look for the next file path (which starts with an MF) */
      while (nRefreshFileListLength > 0)
      {
         if (pRefreshFileList[0] == P_7816SM_MF_FIRST_BYTE)
         {
            /* The next MF has been found */
            break;
         }

         nEfFid = ((pRefreshFileList[0] << 8) + pRefreshFileList[1]);

         pRefreshFileList += 2;
         nRefreshFileListLength -= 2;
      }

      if (nEfFid == pAclProcessorInstance->nDfPkcs15AcmfFid)
      {
         PDebugError("static_PSecurityStackGpAclProcessorNotifyStkRefresh: The Files path contains the EF(ACMF) FID. Update flag set.");
         pAclProcessorInstance->bTouched = W_TRUE;
         pAclProcessorInstance->nLastUpdateTime = 0;
      }
   }

   if (nRefreshFileListLength != 0)
   {
      PDebugError("static_PSecurityStackGpAclProcessorNotifyStkRefresh: The Files path is too long");
      return W_ERROR_BAD_PARAMETER;
   }

   return W_SUCCESS;
}

/** See tPSecurityStackAclProcessorDestroyInstance */
static W_ERROR static_PSecurityStackGpAclProcessorDestroyInstance(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance)
{
   PDebugTrace("static_PSecurityStackGpAclProcessorDestroyInstance()");

   if (pAclProcessorInstance == null)
   {
      PDebugError("static_PSecurityStackGpAclProcessorDestroyInstance: pAclProcessorInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   CMemoryFree(pAclProcessorInstance);
   pAclProcessorInstance = (tPSecurityStackAclProcessorInstance*)null;

   return W_SUCCESS;
}

/** See header file */
tPSecurityStackAclProcessorInterface PSecurityStackGpAclProcessor =
{
   static_PSecurityStackGpAclProcessorCreateInstance,
   static_PSecurityStackGpAclProcessorUpdateInstance,
   static_PSecurityStackGpAclProcessorReadAcl,
   (tPSecurityStackAclProcessorFilterApdu*)null,
   static_PSecurityStackGpAclProcessorNotifyStkRefresh,
   static_PSecurityStackGpAclProcessorDestroyInstance
};

#endif /* (P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC) && P_INCLUDE_SE_SECURITY */
