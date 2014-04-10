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
#include "wme_security_stack_acl_processor_pkcs15.h"

/** The expected major version of the PKCS#15 application */
#define P_SECSTACK_PKCS15_MAJOR_VERSION 0x01

/** The expected minor version of the PKCS#15 application */
#define P_SECSTACK_PKCS15_MINOR_VERSION 0x00

/** The length of the EF(SE-LastUpdate) file */
#define P_SECSTACK_LASTUPDATE_LENGTH 0x11

struct __tPSecurityStackAclProcessorInstance
{
   /** Whether the ACL may have been modified and need to be read again */
   bool_t bTouched;
   /** The date&time of the current ACL */
   uint8_t aACLTime[((P_SECSTACK_LASTUPDATE_LENGTH + 3) & ~3)]; /* Aligned on 32-bit boundary */
   struct {
   /** Whether the ACL has been read at least once */
   bool_t bIsInitialized;
   /** The current state of the state machine */
   uint32_t nState;
   /** The buffer containing the APDU command sent to the SE */
   uint8_t aReaderToCardBuffer[20];
   /** The buffer containing the APDU response returned by the SE */
   uint8_t aCardToReaderBuffer[(256+2)+2]; /* 2 padding bytes */

   /** The contents of the EF(SE-LastUpdate) file */
   uint8_t aSELastUpdate[((P_SECSTACK_LASTUPDATE_LENGTH + 3) & ~3)];
   /** The contents of the EF(SE-ACF) file */
   uint8_t* pSEACF;
   /** The size (in bytes) of the EF(SE-ACF) file */
   uint32_t nSEACFLength;
   /** The number of bytes currently read from the EF(SE-ACF) file */
   uint32_t nSEACFReadLength;
   } sReadAcf;
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
 * @brief Extracts the next TLV from the answer to the SELECT command sent to the PKCS#15 application.
 *
 * @param[inout]  ppBuffer  A pointer to the pointer to the next available data to be parsed
 *                This pointer is updated in case a TLV has been successfully parsed.
 *
 * @param[inout]  pnBufferLength  A pointer to the length of the next available data to be parsed.
 *                This length is updated in case a TLV has been successfully parsed.
 *
 * @param[in]  nExpectedTag  The expected tag
 *
 * @param[in]  nExpectedLength  The expected length in bytes of the tag
 *
 * @param[out]  ppValue  A pointer to the tag value
 *
 * @param[in]  bLastTag  Whether this must be the last tag
 *
 * @return One of the following error codes:
 *          - W_SUCCESS  The TLV has been successfully parsed and a pointer to the value is returned.
 *          - W_ERROR_BAD_PARAMETER  The passed data are malformed.
 */
static W_ERROR static_PSecurityStackPkcs15AclProcessorParseAnswerToSelectTLV
(
   const uint8_t** ppBuffer,
   uint32_t* pnBufferLength,
   uint16_t nExpectedTag,
   uint8_t nExpectedLength,
   const uint8_t** ppValue,
   bool_t bLastTag
)
{
   const uint8_t* pBuffer;
   uint32_t nBufferLength;

   CDebugAssert(ppBuffer != null);
   CDebugAssert(pnBufferLength != null);
   CDebugAssert(ppValue != null);

   pBuffer = *ppBuffer;
   nBufferLength = *pnBufferLength;

   /* Parse tag */
   if ((nExpectedTag & 0xFF00) == 0x0000)
   {
      /* 1-byte tag */
      if ((nBufferLength < 1) || (pBuffer[0] != (uint8_t)nExpectedTag))
      {
         return W_ERROR_BAD_PARAMETER;
      }

      pBuffer++;
      nBufferLength--;
   }
   else
   {
      /* 2-byte tag */
      if ((nBufferLength < 2) || (pBuffer[0] != (uint8_t)(nExpectedTag>>8)) || (pBuffer[1] != (uint8_t)nExpectedTag))
      {
         return W_ERROR_BAD_PARAMETER;
      }

      pBuffer += 2;
      nBufferLength -= 2;
   }

   /* Parse length (1-byte length is assumed) */
   if ((nBufferLength < 1) || (pBuffer[0] != nExpectedLength))
   {
      return W_ERROR_BAD_PARAMETER;
   }

   pBuffer++;
   nBufferLength--;

   /* Check whether this is (optionally) actually the last tag */
   if (bLastTag && (nBufferLength != nExpectedLength))
   {
      return W_ERROR_BAD_PARAMETER;
   }

   /* Remember the pointer to the value */
   if (ppValue != null)
   {
      *ppValue = pBuffer;
   }

   if (bLastTag)
   {
      /* Return pointer to the tag value */
      *ppBuffer = pBuffer;
      *pnBufferLength = nBufferLength;
   }
   else
   {
      /* Return pointer to the next tag-length-value */
      *ppBuffer = pBuffer + nExpectedLength;
      *pnBufferLength = nBufferLength - nExpectedLength;
   }

   return W_SUCCESS;
}

/**
 * @brief Parses the answer (FCI) to the SELECT command sent to the PKCS#15 application.
 *
 * @param[in]  pAclProcessorInstance  The ACL Processor instance.
 *
 * @param[in]  pAnswerToSelect  The Answer-to-Select buffer.
 *
 * @param[in]  nAnswerToSelectLength  The length of the Answer-to-Select buffer.
 *
 * @return One of the following error codes:
 *          - W_SUCCESS  The FCI has been successfully parsed.
 *          - W_ERROR_CONNECTION_COMPATIBILITY  The FCI is malformed.
 *          - W_ERROR_OUT_OF_RESOURCE  Out of memory.
 */
static W_ERROR static_PSecurityStackPkcs15AclProcessorParseAnswerToSelect(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
                  const uint8_t* pAnswerToSelect,
                  uint32_t nAnswerToSelectLength)
{
   uint32_t nResult;

   const uint8_t* pBuffer = pAnswerToSelect;
   uint32_t nBufferLength = nAnswerToSelectLength;
   const uint8_t* pValue;

   if (nBufferLength < 2)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorParseAnswerToSelect: Buffer is too short (%d), must be at least 2", nBufferLength);
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   nBufferLength -= 2; /* Remove SW */

   /*
    * The answer to the SELECT[AID] command is formatted as:
    * 6F 2F                FCI template
    *    84 0C xx-xx       PKCS#15 application AID
    *    A5 1F             Proprietary data
    *       9F-18 02 xx-xx PKCS#15 Application Version Number
    *       BF-0C 17       FCI Issuer Discretionary Data
    *          51 11 xx.xx Contents of the EF(SE-LastUpdate) file
    *          52 02 xx.xx Size of the EF(SE-ACF) file
    */

   /* Parse '6F' tag */
   nResult = static_PSecurityStackPkcs15AclProcessorParseAnswerToSelectTLV(&pBuffer, &nBufferLength, 0x6F, 0x2F, &pValue, W_TRUE);
   if (nResult != W_SUCCESS)
   {
      PDebugWarning("static_PSecurityStackPkcs15AclProcessorParseAnswerToSelect: Bad '6F' tag");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Parse '84' tag */
   nResult = static_PSecurityStackPkcs15AclProcessorParseAnswerToSelectTLV(&pBuffer, &nBufferLength, 0x84, 0x0C, &pValue, W_FALSE);
   if (nResult != W_SUCCESS)
   {
      PDebugWarning("static_PSecurityStackPkcs15AclProcessorParseAnswerToSelect: Bad '84' tag");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Parse 'A5' tag */
   nResult = static_PSecurityStackPkcs15AclProcessorParseAnswerToSelectTLV(&pBuffer, &nBufferLength, 0xA5, 0x1F, &pValue, W_TRUE);
   if (nResult != W_SUCCESS)
   {
      PDebugWarning("static_PSecurityStackPkcs15AclProcessorParseAnswerToSelect: Bad 'A5' tag");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Parse '9F08' tag */
   nResult = static_PSecurityStackPkcs15AclProcessorParseAnswerToSelectTLV(&pBuffer, &nBufferLength, 0x9F08, 0x02, &pValue, W_FALSE);
   if (nResult != W_SUCCESS)
   {
      PDebugWarning("static_PSecurityStackPkcs15AclProcessorParseAnswerToSelect: Bad '9F08' tag");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* The version must be 0x01 0x00 */
   if ((pValue[0] != P_SECSTACK_PKCS15_MAJOR_VERSION) || (pValue[1] != P_SECSTACK_PKCS15_MINOR_VERSION))
   {
      PDebugWarning("static_PSecurityStackPkcs15AclProcessorParseAnswerToSelect: Bad version number");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Parse 'BF0C' tag */
   nResult = static_PSecurityStackPkcs15AclProcessorParseAnswerToSelectTLV(&pBuffer, &nBufferLength, 0xBF0C, 0x17, &pValue, W_TRUE);
   if (nResult != W_SUCCESS)
   {
      PDebugWarning("static_PSecurityStackPkcs15AclProcessorParseAnswerToSelect: Bad 'BF0C' tag");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Parse '51' tag */
   nResult = static_PSecurityStackPkcs15AclProcessorParseAnswerToSelectTLV(&pBuffer, &nBufferLength, 0x51, 0x11, &pValue, W_FALSE);
   if (nResult != W_SUCCESS)
   {
      PDebugWarning("static_PSecurityStackPkcs15AclProcessorParseAnswerToSelect: Bad '51' tag");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }
   else
   {
      /* Copy the EF(SE-LastUpdate) file contents */
      CMemoryCopy(pAclProcessorInstance->sReadAcf.aSELastUpdate, pValue, P_SECSTACK_LASTUPDATE_LENGTH);
   }

   /* Parse '52' tag */
   nResult = static_PSecurityStackPkcs15AclProcessorParseAnswerToSelectTLV(&pBuffer, &nBufferLength, 0x52, 0x02, &pValue, W_TRUE);
   if (nResult != W_SUCCESS)
   {
      PDebugWarning("static_PSecurityStackPkcs15AclProcessorParseAnswerToSelect: Bad '52' tag");
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }
   else
   {
      /* Allocate memory for the EF(SE-ACF) file */

      if (pAclProcessorInstance->sReadAcf.pSEACF != null)
      {
         CMemoryFree(pAclProcessorInstance->sReadAcf.pSEACF);
         pAclProcessorInstance->sReadAcf.pSEACF = (uint8_t*)null;
      }

      pAclProcessorInstance->sReadAcf.nSEACFLength = (256*pValue[0]+pValue[1]);

      if ((pAclProcessorInstance->sReadAcf.pSEACF = (uint8_t*)CMemoryAlloc(pAclProcessorInstance->sReadAcf.nSEACFLength)) == null)
      {
         pAclProcessorInstance->sReadAcf.nSEACFLength = 0;
         return W_ERROR_OUT_OF_RESOURCE;
      }

      CMemoryFill(pAclProcessorInstance->sReadAcf.pSEACF, 0, pAclProcessorInstance->sReadAcf.nSEACFLength);
   }

   return W_SUCCESS;
}

/** See tPSecurityStackAclProcessorCreateInstance */
static W_ERROR static_PSecurityStackPkcs15AclProcessorCreateInstance(
                  const uint8_t* pAnswerToSelect,
                  uint32_t nAnswerToSelectLength,
                  bool_t bIsSelectedByAid,
                  bool_t bAutoResetTouch,
                  tPSecurityStackAclProcessorInstance** ppAclProcessorInstance)
{
   tPSecurityStackAclProcessorInstance* pAclProcessorInstance;

   PDebugTrace("static_PSecurityStackPkcs15AclProcessorCreateInstance");

   if (pAnswerToSelect == null)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorCreateInstance: pAnswerToSelect == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (ppAclProcessorInstance == null)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorCreateInstance: ppAclProcessorInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   pAclProcessorInstance = (tPSecurityStackAclProcessorInstance*)CMemoryAlloc(sizeof(*pAclProcessorInstance));
   if (pAclProcessorInstance == null)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorCreateInstance: Failed to allocate ACL Processor instance");
      return W_ERROR_OUT_OF_RESOURCE;
   }
   CMemoryFill(pAclProcessorInstance, 0, sizeof(*pAclProcessorInstance));

   /* Check whether the Answer-to-Select is recognized by this ACL processor */
   if (static_PSecurityStackPkcs15AclProcessorParseAnswerToSelect(pAclProcessorInstance, pAnswerToSelect, nAnswerToSelectLength) != W_SUCCESS)
   {
      CMemoryFree(pAclProcessorInstance);
      pAclProcessorInstance = (tPSecurityStackAclProcessorInstance*)null;

      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Force reading the ACL */
   pAclProcessorInstance->bTouched = W_TRUE;

   *ppAclProcessorInstance = pAclProcessorInstance;
   return W_SUCCESS;
}

/** See tPSecurityStackAclProcessorUpdateInstance */
static W_ERROR static_PSecurityStackPkcs15AclProcessorUpdateInstance(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
                  const uint8_t* pAnswerToSelect,
                  uint32_t nAnswerToSelectLength,
                  bool_t bIsSelectedByAid)
{
   PDebugTrace("static_PSecurityStackPkcs15AclProcessorUpdateInstance");

   if (pAclProcessorInstance == null)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorUpdateInstance: pAclProcessorInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pAnswerToSelect == null)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorUpdateInstance: pAnswerToSelect == null");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Check whether the Answer-to-Select is recognized by this ACL processor */
   if (static_PSecurityStackPkcs15AclProcessorParseAnswerToSelect(pAclProcessorInstance, pAnswerToSelect, nAnswerToSelectLength) != W_SUCCESS)
   {
      return W_ERROR_CONNECTION_COMPATIBILITY;
   }

   /* Did the EF(SE-LastUpdate) file change ? */
   if (CMemoryCompare(pAclProcessorInstance->aACLTime, pAclProcessorInstance->sReadAcf.aSELastUpdate, P_SECSTACK_LASTUPDATE_LENGTH) != 0)
   {
      /* The file changed. Please read the ACL again */
      return W_ERROR_BAD_STATE;
   }

   if (pAclProcessorInstance->bTouched != W_FALSE)
   {
      /* Force caller to re-read the ACL */
      return W_ERROR_BAD_STATE;
   }

   /* No need to read the ACL again */
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
/** The state machine must send the first READ BINARY command */
#define P_STATE_READ_EF_SE_ACF_FIRST       0x0001
/** The state machine must send subsequent READ BINARY command */
#define P_STATE_READ_EF_SE_ACF_NEXT        0x0002
/** The state machine must return an error notification to the caller */
#define P_STATE_ERROR                      0x00FF

/* Forward declaration */
static void static_PSecurityStackPkcs15AclProcessorReadAclStateMachine
(
   tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
   uint32_t nEvent,
   uint32_t nDataLength,
   W_ERROR nResult
);

/** @brief Callback function called when an APDU exchange with the Secure Element completes */
static void static_PSecurityStackPkcs15AclProcessorExchangeApduCompleted(
                  void* pCallbackParameter1,
                  void* pCallbackParameter2,
                  uint32_t nDataLength,
                  W_ERROR nResult)
{
   tPSecurityStackAclProcessorInstance* pAclProcessorInstance = (tPSecurityStackAclProcessorInstance*)pCallbackParameter1;

   CDebugAssert(pAclProcessorInstance != null);
   CDebugAssert(pCallbackParameter2 == null);

   static_PSecurityStackPkcs15AclProcessorReadAclStateMachine(pAclProcessorInstance,
      P_EVENT_COMMAND_EXECUTED, nDataLength, nResult);
}

/**
 * @brief Reads next available data from the EF(SE-ACF) file of the PKCS#15 application.
 *
 * This command sends a READ BINARY command to the PKCS#15 application.
 *
 * @param[in]  pContext  The current context.
 *
 * @param[in]  pSecurityStackInstance  The security stack instance.
 */
static void static_PSecurityStackPkcs15AclProcessorReadAcf
(
   tPSecurityStackAclProcessorInstance* pAclProcessorInstance
)
{
   W_ERROR nResult;

   pAclProcessorInstance->sReadAcf.aReaderToCardBuffer[0] = P_7816SM_CLA;
   pAclProcessorInstance->sReadAcf.aReaderToCardBuffer[1] = P_7816SM_INS_READ_BINARY;
   pAclProcessorInstance->sReadAcf.aReaderToCardBuffer[2] = (uint8_t)(pAclProcessorInstance->sReadAcf.nSEACFReadLength >> 8);
   pAclProcessorInstance->sReadAcf.aReaderToCardBuffer[3] = (uint8_t)(pAclProcessorInstance->sReadAcf.nSEACFReadLength);
   pAclProcessorInstance->sReadAcf.aReaderToCardBuffer[4] = 0x00; /* Le */

   nResult = pAclProcessorInstance->sExchange.pExchangeApdu(
      pAclProcessorInstance->sExchange.pCallbackParameter1,
      pAclProcessorInstance->sExchange.pCallbackParameter2,
      static_PSecurityStackPkcs15AclProcessorExchangeApduCompleted,
      pAclProcessorInstance,
      null,
      pAclProcessorInstance->sReadAcf.aReaderToCardBuffer,
      5,
      pAclProcessorInstance->sReadAcf.aCardToReaderBuffer,
      sizeof(pAclProcessorInstance->sReadAcf.aCardToReaderBuffer));

   if (nResult != W_ERROR_OPERATION_PENDING)
   {
      static_PSecurityStackPkcs15AclProcessorExchangeApduCompleted(
         pAclProcessorInstance->sExchange.pCallbackParameter1,
         pAclProcessorInstance->sExchange.pCallbackParameter2,
         0, nResult);
   }
}

/* Forward declaration */
static W_ERROR static_PSecurityStackPkcs15AclProcessorParseAcf
(
   const uint8_t* pBuffer, /* EF(SE-ACF) file */
   uint32_t nBuffer,
   tSecurityStackACL** ppAcl
);

/**
 * @brief The PKCS#15 Applet ACL reading state machine.
 */
static void static_PSecurityStackPkcs15AclProcessorReadAclStateMachine
(
   tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
   uint32_t nEvent,
   uint32_t nDataLength,
   W_ERROR nResult
)
{
   tSecurityStackACL* pAcl = (tSecurityStackACL*)null;

   switch (nEvent)
   {
      case P_EVENT_INITIAL:
         pAclProcessorInstance->sReadAcf.nState = P_STATE_READ_EF_SE_ACF_FIRST;
         break;

      case P_EVENT_COMMAND_EXECUTED:
         pAclProcessorInstance->sReadAcf.nState &= ~P_STATUS_PENDING_EXECUTED;
         break;
   }

   if (nResult == W_ERROR_TIMEOUT) /* The SE is not responding */
   {
      goto notify_failure;
   }
   else if (nResult != W_SUCCESS)
   {
      /* Close the logical channel (if need be) */
      switch(pAclProcessorInstance->sReadAcf.nState)
      {
         case P_STATE_READ_EF_SE_ACF_FIRST:
            PDebugLog("static_PSecurityStackPkcs15AclProcessorReadAclStateMachine: The PKCS#15 application is not present in the SE");
            /* FALL THROUGH */

         case P_STATE_READ_EF_SE_ACF_NEXT:
            pAclProcessorInstance->sReadAcf.nState = P_STATE_ERROR;
            break;
      }
   }

   switch(pAclProcessorInstance->sReadAcf.nState)
   {
      case P_STATE_READ_EF_SE_ACF_FIRST:
      {
         PDebugTrace("static_PSecurityStackPkcs15AclProcessorReadAclStateMachine: A PKCS#15 application is present in the SE");

         /* Check whether the ACL timestamp has changed since the last read */
         if ((pAclProcessorInstance->sReadAcf.bIsInitialized != W_FALSE)
         &&  (CMemoryCompare(pAclProcessorInstance->aACLTime, pAclProcessorInstance->sReadAcf.aSELastUpdate, P_SECSTACK_LASTUPDATE_LENGTH) == 0))
         {
            /* The timestamp has not changed - No need to read the ACL again */
            PDebugLog("static_PSecurityStackPkcs15AclProcessorReadAclStateMachine: The ACL has not changed. No need to read it again.");

            pAclProcessorInstance->bTouched = W_FALSE;
            goto notify_success_nochange;
         }

         /* Force reading the ACL */
         pAclProcessorInstance->bTouched = W_TRUE;

         /* No data have been read yet */
         pAclProcessorInstance->sReadAcf.nSEACFReadLength = 0;

         /* Set the next state to be reached */
         pAclProcessorInstance->sReadAcf.nState = P_STATE_READ_EF_SE_ACF_NEXT | P_STATUS_PENDING_EXECUTED;

         /* Read next data */
         static_PSecurityStackPkcs15AclProcessorReadAcf(pAclProcessorInstance);
         return;
      }

      case P_STATE_READ_EF_SE_ACF_NEXT:
      {
         if ((nDataLength < 2)
             || (pAclProcessorInstance->sReadAcf.aCardToReaderBuffer[nDataLength - 2] != 0x90)
             || (pAclProcessorInstance->sReadAcf.aCardToReaderBuffer[nDataLength - 1] != 0x00))
         {
            nResult = W_ERROR_RF_COMMUNICATION;
            goto notify_failure;
         }

         /* Copy returned bytes */
         CMemoryCopy(pAclProcessorInstance->sReadAcf.pSEACF + pAclProcessorInstance->sReadAcf.nSEACFReadLength,
            pAclProcessorInstance->sReadAcf.aCardToReaderBuffer,
            nDataLength - 2
         );
         pAclProcessorInstance->sReadAcf.nSEACFReadLength += nDataLength - 2;

         /* Check whether all bytes have been read */
         if ((nDataLength > 2) && (pAclProcessorInstance->sReadAcf.nSEACFReadLength < pAclProcessorInstance->sReadAcf.nSEACFLength))
         {
            /* Set the next state to be reached */
            pAclProcessorInstance->sReadAcf.nState = P_STATE_READ_EF_SE_ACF_NEXT | P_STATUS_PENDING_EXECUTED;

            /* Read next data */
            static_PSecurityStackPkcs15AclProcessorReadAcf(pAclProcessorInstance);
            return;
         }
         else
         {
            if ((nResult = static_PSecurityStackPkcs15AclProcessorParseAcf(pAclProcessorInstance->sReadAcf.pSEACF, pAclProcessorInstance->sReadAcf.nSEACFLength, &pAcl)) != W_SUCCESS)
            {
               nResult = W_ERROR_SECURITY;
               goto notify_failure;
            }

            PDebugTrace("static_PSecurityStackPkcs15AclProcessorReadAclStateMachine: The PKCS#15 application ACL has been successfully parsed");

            /* Remember the timestamp of this ACL */
            CMemoryCopy(pAclProcessorInstance->aACLTime, pAclProcessorInstance->sReadAcf.aSELastUpdate, P_SECSTACK_LASTUPDATE_LENGTH);

            /* ACL has been initialized */
            pAclProcessorInstance->sReadAcf.bIsInitialized = W_TRUE;

            /* ACL has been read */
            pAclProcessorInstance->bTouched = W_FALSE;

            goto notify_success;
         }
      }

      case P_STATE_ERROR:
      {
         goto notify_failure;
      }
   }

   nResult = W_ERROR_SECURITY;
   /* FALL THROUGH */

notify_failure:
   CDebugAssert(pAcl == null);
   CDebugAssert(nResult != W_SUCCESS);

   /* Free working memory */
   if (pAclProcessorInstance->sReadAcf.pSEACF != null)
   {
      CMemoryFree(pAclProcessorInstance->sReadAcf.pSEACF);
      pAclProcessorInstance->sReadAcf.pSEACF = (uint8_t*)null;

      pAclProcessorInstance->sReadAcf.nSEACFLength = 0;
      pAclProcessorInstance->sReadAcf.nSEACFReadLength = 0;
   }

   /* Force reading the ACL the next time this ACL Processor is called */
   pAclProcessorInstance->bTouched = W_TRUE;

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
   pAclProcessorInstance->sReadAcf.nState = P_STATE_IDLE;

   /* Notify result to the caller */
   pAclProcessorInstance->sResult.pCallback(
      pAclProcessorInstance->sResult.pCallbackParameter1,
      pAclProcessorInstance->sResult.pCallbackParameter2,
      pAcl, nResult);
}

/** See tPSecurityStackAclProcessorReadAcl */
static W_ERROR static_PSecurityStackPkcs15AclProcessorReadAcl(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
                  tPSecurityStackAclProcessorExchangeApdu pExchangeApdu,
                  void* pExchangeApduCallbackParameter1,
                  void* pExchangeApduCallbackParameter2,
                  tPSecurityStackAclProcessorReadAclCallbackFunction pCallback,
                  void* pCallbackParameter1,
                  void* pCallbackParameter2,
                  uint32_t nNow)
{
   PDebugTrace("static_PSecurityStackPkcs15AclProcessorReadAcl()");

   if (pAclProcessorInstance == null)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorReadAcl: pAclProcessorInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pExchangeApdu == null)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorReadAcl: pExchangeApdu == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pCallback == null)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorReadAcl: pCallback == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pAclProcessorInstance->bTouched == W_FALSE)
   {
      PDebugTrace("static_PSecurityStackPkcs15AclProcessorReadAcl: No need to read the ACL again");
      return W_SUCCESS;
   }

   /* Remember callbacks and callback parameters */
   pAclProcessorInstance->sExchange.pExchangeApdu = pExchangeApdu;
   pAclProcessorInstance->sExchange.pCallbackParameter1 = pExchangeApduCallbackParameter1;
   pAclProcessorInstance->sExchange.pCallbackParameter2 = pExchangeApduCallbackParameter2;

   pAclProcessorInstance->sResult.pCallback = pCallback;
   pAclProcessorInstance->sResult.pCallbackParameter1 = pCallbackParameter1;
   pAclProcessorInstance->sResult.pCallbackParameter2 = pCallbackParameter2;

   /* Start reading the ACL */
   PDebugTrace("static_PSecurityStackPkcs15AclProcessorReadAcl: Starting to read the ACF...");

   /* Initialize the state machine */
   static_PSecurityStackPkcs15AclProcessorReadAclStateMachine(pAclProcessorInstance,
      P_EVENT_INITIAL, 0, W_SUCCESS);

   return W_ERROR_OPERATION_PENDING;
}

/** See tPSecurityStackAclProcessorFilterApdu */
static W_ERROR static_PSecurityStackPkcs15AclProcessorFilterApdu(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance,
                  const uint8_t* pAid,
                  uint32_t nAidLength,
                  const uint8_t* pApdu,
                  uint32_t nApduLength)
{
   PDebugTrace("static_PSecurityStackPkcs15AclProcessorFilterApdu()");

   if (pAclProcessorInstance == null)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorFilterApdu: pAclProcessorInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (pAid == null)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorFilterApdu: pAid == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if ((nAidLength < 5) || (nAidLength > 16))
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorFilterApdu: nAidLength (%d) not in range 5..16", nAidLength);
      return W_ERROR_BAD_PARAMETER;
   }

   if (pApdu == null)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorFilterApdu: pApdu == null");
      return W_ERROR_BAD_PARAMETER;
   }

   if (nApduLength < 4)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorFilterApdu: nApduLength (%d) < 4", nApduLength);
      return W_ERROR_BAD_PARAMETER;
   }

   if (pAclProcessorInstance->bTouched != W_FALSE)
   {
      /* The ACL must already be read again. Nothing to be done */
      return W_SUCCESS;
   }

   if (pApdu[1] != P_7816SM_INS_UPDATE_BINARY)
   {
      /* This is not an UPDATE BINARY command */
      return W_SUCCESS;
   }

   if ((nAidLength != g_nSecurityStackPkcs15AidSize) || (CMemoryCompare(g_aSecurityStackPkcs15Aid, pAid, nAidLength) != 0))
   {
      /* The AID is not that of the PKCS#15 application */
      return W_SUCCESS;
   }

   /* We may assume that the ACL has been changed... */
   pAclProcessorInstance->bTouched = W_TRUE;

   PDebugWarning("static_PSecurityStackPkcs15AclProcessorFilterApdu: The ACL is assumed to have changed!");

   return W_SUCCESS;
}

/** See tPSecurityStackAclProcessorDestroyInstance */
static W_ERROR static_PSecurityStackPkcs15AclProcessorDestroyInstance(
                  tPSecurityStackAclProcessorInstance* pAclProcessorInstance)
{
   PDebugTrace("static_PSecurityStackPkcs15AclProcessorDestroyInstance()");

   if (pAclProcessorInstance == null)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorDestroyInstance: pAclProcessorInstance == null");
      return W_ERROR_BAD_PARAMETER;
   }

   CMemoryFree(pAclProcessorInstance);
   pAclProcessorInstance = (tPSecurityStackAclProcessorInstance*)null;

   return W_SUCCESS;
}

/** See header file */
tPSecurityStackAclProcessorInterface PSecurityStackPkcs15AclProcessor =
{
   static_PSecurityStackPkcs15AclProcessorCreateInstance,
   static_PSecurityStackPkcs15AclProcessorUpdateInstance,
   static_PSecurityStackPkcs15AclProcessorReadAcl,
   static_PSecurityStackPkcs15AclProcessorFilterApdu,
   (tPSecurityStackAclProcessorNotifyStkRefresh*)null,
   static_PSecurityStackPkcs15AclProcessorDestroyInstance
};


/** FID of the EF(SE-LastUpdate) file */
#define P_SECSTACK_FID_EF_SE_LASTUPDATE ((uint16_t)0x5035)

/** FID of the EF(SE-ACF) file */
#define P_SECSTACK_FID_EF_SE_ACF ((uint16_t)0x5036)

/* Allowed value ranges */
#define P_SECSTACK_ACE_INDEX_MIN 0x0000
#define P_SECSTACK_ACE_INDEX_MAX 0x7FFF
#define P_SECSTACK_ACE_LENGTH_MIN 0x0000
#define P_SECSTACK_ACE_LENGTH_MAX 0x7FFF

/** Parses a list of Principals */
static W_ERROR static_PSecurityStackPkcs15AclProcessorParseAceSequenceOfPrincipals
(
  const tAsn1Parser* pAceParser,
  uint32_t* pnPrincipals,
  tPrincipal* pPrincipals
)
{
   W_ERROR nError = W_SUCCESS;
   tAsn1Parser sPrincipalsParser;
   uint32_t nPrincipals = 0;

   /*
    * The sequence of "Principal" is a list of:
    * - 81 - [1] endEntityID (OCTET STRING)
    */

   /* Initialize the principals parser and parse the first TLV */
   nError = PAsn1ParseIncludedTlv(pAceParser, &sPrincipalsParser);
   while (nError == W_SUCCESS)
   {
      uint32_t nLength = 0;
      const uint8_t* pValue = (uint8_t*)null;

      if (PAsn1GetTagValue(&sPrincipalsParser) != P_ASN1_TAG(P_ASN1_TAG_CONTEXT_SPECIFIC, P_ASN1_TAG_PRIMITIVE, 1))
      {
         PDebugError("static_PSecurityStackPkcs15AclProcessorParseAceSequenceOfPrincipals: Unexpected tag");
         nError = W_ERROR_BAD_PARAMETER;
         break;
      }

      PAsn1GetPointerOnBinaryContent(&sPrincipalsParser, &pValue, &nLength);

      if (nLength != P_SECSTACK_PRINCIPALID_LENGTH)
      {
         PDebugError("static_PSecurityStackPkcs15AclProcessorParseAceSequenceOfPrincipals: Invalid PrincipalID length");
         nError = W_ERROR_BAD_PARAMETER;
         break;
      }

      if ((pPrincipals != null) && (pnPrincipals != null))
      {
         if (nPrincipals >= (*pnPrincipals))
         {
            /* There is not enough space in the passed array */
            PDebugError("static_PSecurityStackPkcs15AclProcessorParseAceSequenceOfPrincipals: Buffer too short");
            return W_ERROR_BUFFER_TOO_SHORT;
         }

         CMemoryCopy(pPrincipals[nPrincipals].id, pValue, nLength);
      }

      nPrincipals++;

      /* Move to the next TLV. An error is returned if end of principals is reached. */
      nError = PAsn1MoveToNextTlv(&sPrincipalsParser);
   }

   /* Check if end of principals is reached */
   /* If not, an error occurs before the end of principals */
   if (PAsn1CheckEndOfTlv(&sPrincipalsParser))
   {
      /* End of principals is reached, thus the error returned by PAsn1MoveToNextTlv is normal */
      nError = W_SUCCESS;
   }

   if (pnPrincipals != null)
   {
      *pnPrincipals = nPrincipals;
   }

   return nError;
}

/** Parses a list of APDU permissions */
static W_ERROR static_PSecurityStackPkcs15AclProcessorParseAceSequenceOfApduMaskPermissions
(
  const tAsn1Parser* pAceParser,
  uint32_t* pnPermissions,
  tPermission** ppPermissions
)
{
   W_ERROR nError = W_SUCCESS;
   tAsn1Parser sPermissionsParser;
   tAsn1Parser sAPDUMaskParser;
   uint32_t nPermissions = 0;

   /*
    * The sequence of "APDUMaskPermission" is a list of:
    * - A0 - [0] APDUMaskPermission (SEQUENCE)
    * -    04 - apduHeader (OCTET STRING)
    * -    04 - mask (OCTET STRING)
    */

   /* Initialize the principals parser and parse the first TLV */
   nError = PAsn1ParseIncludedTlv(pAceParser, &sPermissionsParser);
   while (nError == W_SUCCESS)
   {
      const uint8_t* pAPDUHeader = (uint8_t*)null;
      const uint8_t* pAPDUMask = (uint8_t*)null;
      uint32_t nLength = 0;

      if (PAsn1GetTagValue(&sPermissionsParser) != P_ASN1_TAG(P_ASN1_TAG_CONTEXT_SPECIFIC, P_ASN1_TAG_CONSTRUCTED, 0))
      {
         PDebugError("static_PSecurityStackPkcs15AclProcessorParseAceSequenceOfApduMaskPermissions: Unexpected tag");
         nError = W_ERROR_BAD_PARAMETER;
         break;
      }

      /* Initialize the APDUMask parser and parse the first TLV */
      nError = PAsn1ParseIncludedTlv(&sPermissionsParser, &sAPDUMaskParser);
      if (nError != W_SUCCESS)
         { return nError; }

      /* Parse the current TLV (which must be an OCTET STRING for apduHeader) */
      if (PAsn1ParseOctetString(&sAPDUMaskParser, &pAPDUHeader, &nLength) != W_SUCCESS)
      {
         PDebugError("static_PSecurityStackPkcs15AclProcessorParseAceSequenceOfApduMaskPermissions: Invalid TLV (apduHeader)");
         nError = W_ERROR_BAD_PARAMETER;
         break;
      }
      if (nLength != P_SECSTACK_APDU_PERMISSION_LENGTH)
      {
         PDebugError("static_PSecurityStackPkcs15AclProcessorParseAceSequenceOfApduMaskPermissions: Invalid length (apduHeader)");
         nError = W_ERROR_BAD_PARAMETER;
         break;
      }

      /* Move to the next TLV (apduMask) */
      nError = PAsn1MoveToNextTlv(&sAPDUMaskParser);
      if (nError != W_SUCCESS)
         { return nError; }

      /* Parse the current TLV (which must be an OCTET STRING for apduMask) */
      if (PAsn1ParseOctetString(&sAPDUMaskParser, &pAPDUMask, &nLength) != W_SUCCESS)
      {
         PDebugError("static_PSecurityStackPkcs15AclProcessorParseAceSequenceOfApduMaskPermissions: Invalid TLV (apduMask)");
         nError = W_ERROR_BAD_PARAMETER;
         break;
      }
      if (nLength != P_SECSTACK_APDU_PERMISSION_LENGTH)
      {
         PDebugError("static_PSecurityStackPkcs15AclProcessorParseAceSequenceOfApduMaskPermissions: Invalid length (apduMask)");
         nError = W_ERROR_BAD_PARAMETER;
         break;
      }

      /* No data must follow in APDUMask sequence */
      PAsn1MoveToNextTlv(&sAPDUMaskParser);
      if (PAsn1CheckEndOfTlv(&sAPDUMaskParser) == W_FALSE)
      {
         PDebugError("static_PSecurityStackPkcs15AclProcessorParseAce: Too much data in APDUMask sequence");
         nError = W_ERROR_BAD_PARAMETER;
         break;
      }

      if ((ppPermissions != null) && (pnPermissions != null))
      {
         if (nPermissions >= (*pnPermissions))
         {
            /* There is not enough space in the passed array */
            return W_ERROR_BUFFER_TOO_SHORT;
         }

         CMemoryCopy((*ppPermissions)[nPermissions].apduHeader,
            pAPDUHeader, P_SECSTACK_APDU_PERMISSION_LENGTH
            );
         CMemoryCopy((*ppPermissions)[nPermissions].apduMask,
            pAPDUMask, P_SECSTACK_APDU_PERMISSION_LENGTH
            );
      }

      nPermissions++;

      /* Move to the next TLV. An error is returned if end of permissions is reached. */
      nError = PAsn1MoveToNextTlv(&sPermissionsParser);
   }

   /* Check if end of permissions is reached */
   /* If not, an error occurs before the end of permissions */
   if (PAsn1CheckEndOfTlv(&sPermissionsParser))
   {
      /* End of permissions is reached, thus the error returned by PAsn1MoveToNextTlv is normal */
      nError = W_SUCCESS;
   }

   if (pnPermissions != null)
   {
      *pnPermissions = nPermissions;
   }

   return nError;
}

/** Parses an ACE */
static W_ERROR static_PSecurityStackPkcs15AclProcessorParseAce
(
  tSecurityStackACL* pAcl,
  const uint8_t* pAceBuffer, /* ACE data buffer */
  uint32_t nAceBufferLength, /* ACE data length */
  tACE* pAce
)
{
   W_ERROR nError = W_SUCCESS;

   tAsn1Parser sBufferParser;
   tAsn1Parser sAceParser;

   /*
    * The ACIE is a SEQUENCE formated as:
    * - 30 SEQUENCE
    * -    A0 - [0] Principals (SEQUENCE OF) OPTIONAL
    * -       81 - [1] endEntityID (OCTET STRING)
    * -       n times
    * -    A1 - [1] Permissions (SEQUENCE OF) OPTIONAL
    * -       A0 - [0] APDUMaskPermission (SEQUENCE)
    * -          04 - apduHeader (OCTET STRING)
    * -          04 - mask (OCTET STRING)
    * -       n times
    */

   /* Initialize the buffer parser and parse the first TLV */
   nError = PAsn1InitializeParser(&sBufferParser, pAceBuffer, nAceBufferLength);
   if (nError != W_SUCCESS)
      { return nError; }

   /* Check that this a SEQUENCE and that data don't follow */
   if (PAsn1GetTagValue(&sBufferParser) != P_ASN1_TAG_UNIVERSAL_SEQUENCE)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorParseAce: Unexpected tag (must be a SEQUENCE)");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Initialize the ACE parser and parse the first TLV */
   nError = PAsn1ParseIncludedTlv(&sBufferParser, &sAceParser);
   if (nError != W_SUCCESS)
      { return nError; }

   /* Is the next tag the SEQUENCE OF "Principal" ? */
   if (PAsn1GetTagValue(&sAceParser) == P_ASN1_TAG(P_ASN1_TAG_CONTEXT_SPECIFIC, P_ASN1_TAG_CONSTRUCTED, 0))
   {
      uint32_t nPrincipals = 0;
      tPrincipal* pPrincipals = (tPrincipal*)null;

      if (PAsn1GetTagLength(&sAceParser) == 0)
      {
         /* The SEQUENCE of "Principal" is empty = All terminal applications are granted */
         nError = PSecurityStackAclCreateAcePrincipals(pAcl, pAce, 0, (tPrincipal**)null);
         if (nError != W_SUCCESS)
            { return nError; }
      }
      else
      {
         /* The SEQUENCE of "Principal" is not empty */
         nError = static_PSecurityStackPkcs15AclProcessorParseAceSequenceOfPrincipals(
           &sAceParser, &nPrincipals, (tPrincipal*)null
         );
         if (nError != W_SUCCESS)
            { return nError; }

         nError = PSecurityStackAclCreateAcePrincipals(pAcl, pAce, nPrincipals, &pPrincipals);
         if (nError != W_SUCCESS)
            { return nError; }

         nError = static_PSecurityStackPkcs15AclProcessorParseAceSequenceOfPrincipals(
           &sAceParser, &nPrincipals, pPrincipals
         );
         if (nError != W_SUCCESS)
            { return nError; }
      }

      /* Get the next tag (which should the SEQUENCE of "Permission") */
      nError = PAsn1MoveToNextTlv(&sAceParser);
      if (nError != W_SUCCESS)
         { return nError; }
   }
   else
   {
      /* The SEQUENCE of "Principal" is absent = All terminal applications are granted */
      nError = PSecurityStackAclCreateAcePrincipals(pAcl, pAce, 0, (tPrincipal**)null);
      if (nError != W_SUCCESS)
         { return nError; }
   }


   /* Is the next tag the SEQUENCE OF "Permission" ? */
   if (PAsn1GetTagValue(&sAceParser) == P_ASN1_TAG(P_ASN1_TAG_CONTEXT_SPECIFIC, P_ASN1_TAG_CONSTRUCTED, 1))
   {
      if (PAsn1GetTagLength(&sAceParser) == 0)
      {
         /* The SEQUENCE of "Permission" is empty = All APDU commands are granted */
         nError = PSecurityStackAclCreateAcePermissions(pAcl, pAce, 0, (tPermission**)null);
         if (nError != W_SUCCESS)
            { return nError; }
      }
      else
      {
         uint32_t nPermissions = 0;
         tPermission* pPermissions = (tPermission*)null;

         /* The SEQUENCE of "Permission" is not empty */
         nError = static_PSecurityStackPkcs15AclProcessorParseAceSequenceOfApduMaskPermissions(
           &sAceParser, &nPermissions, (tPermission**)null
         );
         if (nError != W_SUCCESS)
            { return nError; }

         nError = PSecurityStackAclCreateAcePermissions(pAcl, pAce, nPermissions, &pPermissions);
         if (nError != W_SUCCESS)
            { return nError; }

         nError = static_PSecurityStackPkcs15AclProcessorParseAceSequenceOfApduMaskPermissions(
           &sAceParser, &nPermissions, &pPermissions
         );
         if (nError != W_SUCCESS)
            { return nError; }
      }
   }
   else
   {
      /* The SEQUENCE of "Permission" is absent = All APDU commands are granted */
      nError = PSecurityStackAclCreateAcePermissions(pAcl, pAce, 0, (tPermission**)null);
      if (nError != W_SUCCESS)
         { return nError; }
   }

   /* No data must follow in the ACE sequence */
   PAsn1MoveToNextTlv(&sAceParser);
   if (PAsn1CheckEndOfTlv(&sAceParser) == W_FALSE)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorParseAce: Too much data in ACE sequence");
      return W_ERROR_BAD_PARAMETER;
   }

   /* No data must follow after this ACE sequence */
   PAsn1MoveToNextTlv(&sBufferParser);
   if (PAsn1CheckEndOfTlv(&sBufferParser) == W_FALSE)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorParseAce: incorrect ACE length");
      return W_ERROR_BAD_PARAMETER;
   }

   return W_SUCCESS;
}

/**
 * @brief Parses an ACIE.
 *
 * @param[inout]  pAcl  The pointer to the ACL structure.
 *
 * @param[in]  pACFParser  The pointer to the ACF parser which encloses this ACIE (ACF cursor points to this ACIE).
 *
 * @param[in]  pBuffer  The pointer to the buffer which contains ACF data (needed to access ACE with index).
 *
 * @param[in]  nBuffer  The length of the ACF data in pBuffer.
 **/
static W_ERROR static_PSecurityStackPkcs15AclProcessorParseAcie
(
  tSecurityStackACL* pAcl,
  const tAsn1Parser* pACFParser,
  const uint8_t* pBuffer,
  uint32_t nBuffer
)
{
   static const uint8_t aTlvSeAcfPath[] = { P_ASN1_TAG_UNIVERSAL_OCTET_STRING, 2, (uint8_t)(P_SECSTACK_FID_EF_SE_ACF >> 8), (uint8_t)(P_SECSTACK_FID_EF_SE_ACF & 0xFF) };

   W_ERROR nError = W_SUCCESS;

   const uint8_t* pAid = P_SECSTACK_BUFFER_AID_ALL;
   uint32_t nAidLength = P_SECSTACK_LENGTH_AID_ALL;

   uint32_t nAceIndex = 0;
   uint32_t nAceLength = 0;
   tACE* pAce = (tACE*)null;

   tAsn1Parser sACIEParser;
   tAsn1Parser sPathParser;

   /*
    * The ACIE is a SEQUENCE that contains the following fields:
    * - 04 aid    (OCTET STRING) OPTIONAL
    *   30 SEQUENCE
    * -    04 - path   (OCTET STRING) -- must be '5036'
    * -    02 - index  (INTEGER)
    * -    80 - [0] length (INTEGER)
    */

   /* Initialize the ACIE parser and parse the first TLV in ACIE */
   nError = PAsn1ParseIncludedTlv(pACFParser, &sACIEParser);
   if (nError != W_SUCCESS)
      {  return nError; }

   /* Is the next tag an octet string, ie. the (optional) AID ? */
   if ((PAsn1GetTagValue(&sACIEParser) == P_ASN1_TAG_UNIVERSAL_OCTET_STRING) && (PAsn1ParseOctetString(&sACIEParser, &pAid, &nAidLength) == W_SUCCESS))
   {
      /* The AID length must be between 5 and 16 */
      if ((nAidLength < 5) || (nAidLength > 16))
      {
         PDebugError("static_PSecurityStackPkcs15AclProcessorParseAcie: Invalid AID length");
         return W_ERROR_BAD_PARAMETER;
      }

      /* Move to the next TLV */
      nError = PAsn1MoveToNextTlv(&sACIEParser);
      if (nError != W_SUCCESS)
         { return nError; }
   }

   /* Is the next tag a SEQUENCE of {path, index, length} ? */
   if (PAsn1GetTagValue(&sACIEParser) != P_ASN1_TAG_UNIVERSAL_SEQUENCE)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorParseAcie: Unexpected tag (must be a SEQUENCE)");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Initialize the Path parser and parse the first TLV path in Path sequence */
   nError = PAsn1ParseIncludedTlv(&sACIEParser, &sPathParser);
   if (nError != W_SUCCESS)
      { return nError; }

   /* Check tag 'path' that must contain the FID of the EF(SE-ACF) file, ie. '5036' */
   if (PAsn1CompareTlvToBuffer(&sPathParser, aTlvSeAcfPath, sizeof(aTlvSeAcfPath)) == W_FALSE)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorParseAcie: Invalid path");
      return nError;
   }

   /* Get the second tag of the SEQUENCE of {path, index, length}, ie. index */
   nError = PAsn1MoveToNextTlv(&sPathParser);
   if (nError != W_SUCCESS)
      { return nError; }

   nError = PAsn1ParseInteger(&sPathParser, (int32_t*)&nAceIndex);
   if (nError != W_SUCCESS)
      { return nError; }

   /* The ACE index must be in range 0 to 32767 */
   if ((nAceIndex < P_SECSTACK_ACE_INDEX_MIN) || (nAceIndex > P_SECSTACK_ACE_INDEX_MAX))
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorParseAcie: Invalid index value (not in allowed range)");
      return W_ERROR_BAD_PARAMETER;
   }

   /* Get the third tag of the SEQUENCE of {path, index, length}, ie. length */
   nError = PAsn1MoveToNextTlv(&sPathParser);
   if (nError != W_SUCCESS)
      { return nError; }

   if (PAsn1GetTagValue(&sPathParser) != P_ASN1_TAG(P_ASN1_TAG_CONTEXT_SPECIFIC, P_ASN1_TAG_PRIMITIVE, 0))
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorParseAcie: Unexpected tag for the length field");
      return W_ERROR_BAD_PARAMETER;
   }

   nError = PAsn1ParseIntegerValue(&sPathParser, (int32_t*)&nAceLength);
   if (nError != W_SUCCESS)
      { return nError; }

   /* The ACE length must be in range 0 to 32767 */
   if ((nAceLength < P_SECSTACK_ACE_LENGTH_MIN) || (nAceLength > P_SECSTACK_ACE_LENGTH_MAX))
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorParseAcie: Invalid length value (not in allowed range)");
      return W_ERROR_BAD_PARAMETER;
   }

   /* No data must follow in Path sequence */
   PAsn1MoveToNextTlv(&sPathParser);
   if (PAsn1CheckEndOfTlv(&sPathParser) == W_FALSE)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorParseAcie: Too much data in Path sequence");
      return W_ERROR_BAD_PARAMETER;
   }

   /* No data must follow in ACIE sequence */
   PAsn1MoveToNextTlv(&sACIEParser);
   if (PAsn1CheckEndOfTlv(&sACIEParser) == W_FALSE)
   {
      PDebugError("static_PSecurityStackPkcs15AclProcessorParseAcie: Too much data in ACIE sequence");
      return W_ERROR_BAD_PARAMETER;
   }

   /*
    * At this point, the ACIE entry has been successfully parsed.
    */

   nError = PSecurityStackAclCreateAce(pAcl,
      P_SECSTACK_FID_EF_SE_ACF, nAceIndex, nAceLength,
      &pAce
   );
   if ((nError != W_SUCCESS) && (nError != W_ERROR_ITEM_NOT_FOUND))
      { return nError; }

   if (nError == W_ERROR_ITEM_NOT_FOUND)
   {
      /* Check that ACE index and length are not out of bounds */
      if ((nAceIndex + nAceLength) > nBuffer)
      {
         PDebugError("static_PSecurityStackPkcs15AclProcessorParseAcie: index/length are out of bounds");
         return W_ERROR_BAD_PARAMETER;
      }

      nError = static_PSecurityStackPkcs15AclProcessorParseAce(pAcl,
        pBuffer + nAceIndex, nAceLength, pAce);
      if (nError != W_SUCCESS)
         { return nError; }
   }

   return PSecurityStackAclCreateAcie(pAcl, pAid, nAidLength, pAce);
}

/**
 * @brief Parses the ACL stored in the EF(SE-ACF) file.
 *
 * @param[in]  pBuffer  A pointer to the buffer storing the contents EF(SE-ACF) file
 *
 * @param[in]  nBuffer  The length in bytes of the EF(SE-ACF) file
 *
 * @param[out] ppAcl    A pointer to the returned pointer to the parsed ACL
 *
 * @return One of the following error codes:
 *          - W_SUCCESS  The ACL has been successfully parsed
 *          - W_ERROR_OUT_OF_RESOURCE  Out of memory
 *          - W_ERROR_BAD_PARAMETER  The EF(SE-ACF) file is malformed.
 */
static W_ERROR static_PSecurityStackPkcs15AclProcessorParseAcf
(
   const uint8_t* pBuffer, /* EF(SE-ACF) file */
   uint32_t nBuffer,
   tSecurityStackACL** ppAcl
)
{
   W_ERROR nError = W_SUCCESS;

   tSecurityStackACL* pAcl = (tSecurityStackACL*)null;

   tAsn1Parser sACFParser;

   if (pBuffer == null)
   {
      return W_ERROR_BAD_PARAMETER;
   }

   nError = PSecurityStackAclCreateInstance(W_FALSE, &pAcl);
   if (nError != W_SUCCESS)
   {
      return nError;
   }

   /*
   * The ACL is a list of:
   * - tags '30' containing an ACIE
   * - tags '00' (Empty element) containing the ACE
   */

   /* Initialize the ACF parser and parse the first TLV */
   nError = PAsn1InitializeParser(&sACFParser, pBuffer, nBuffer);
   while (nError == W_SUCCESS)
   {
      uint8_t nTag = PAsn1GetTagValue(&sACFParser);

      /* Check that the next tag is a SEQUENCE (containing an ACIE) */
      if (nTag == P_ASN1_TAG_UNIVERSAL_SEQUENCE)
      {
         nError = static_PSecurityStackPkcs15AclProcessorParseAcie(pAcl,
                                                 &sACFParser,
                                                 pBuffer, nBuffer  /* Needed to access ACE with index */
                                                 );
         if (nError != W_SUCCESS)
            { break; }
      }
      /* Skip Empty elements */
      else if (PAsn1GetTagValue(&sACFParser) != P_ASN1_TAG_EMPTY)
      {
         PDebugError("PSecurityStackParseACL: Unexpected tag");
         nError = W_ERROR_BAD_PARAMETER;
         break;
      }

      /* Move to the next TLV. An error is returned if end of file is reached. */
      nError = PAsn1MoveToNextTlv(&sACFParser);
   }

   /* Check if end of file is reached */
   /* If not, an error occurs before the end of file */
   if (PAsn1CheckEndOfTlv(&sACFParser))
   {
      /* End of file is reached, thus the error returned by PAsn1MoveToNextTlv is normal */
      nError = W_SUCCESS;
   }

   if (nError != W_SUCCESS)
   {
      if (pAcl != null)
      {
         PSecurityStackAclDestroyInstance(pAcl);
         pAcl = (tSecurityStackACL*)null;
      }
   }
   else
   {
      *ppAcl = pAcl;
   }

   return nError;
}


#endif /* (P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC) && P_INCLUDE_SE_SECURITY */
