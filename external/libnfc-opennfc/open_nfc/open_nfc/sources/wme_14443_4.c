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
   Contains the ISO14443-4 implementation.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( 14_4 )

#include "wme_context.h"

#ifndef P_READER_14P4_STANDALONE_SUPPORT

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#ifndef P_14443_4_MAX_ATTEMPT
#define P_14443_4_MAX_ATTEMPT                2
#endif /* P_14443_4_MAX_ATTEMPT */

/* The different PCB defines */
#define P_14443_4_PCB_NONE                   0x00
#define P_14443_4_PCB_I                      0x02
#define P_14443_4_PCB_R_ACK                  0xA2
#define P_14443_4_PCB_R_NACK                 0xB2
#define P_14443_4_PCB_S_DESELECT             0xC2
#define P_14443_4_PCB_S_WTXM                 0xE2

/* The different prologue defines */
#define P_14443_4_CID_PRESENT                0x08
#define P_14443_4_NAD_PRESENT                0x04
#define P_14443_4_CHAINING_PRESENT           0x10

/* The different chaining state defines */
#define P_14443_4_CHAINING_COMMAND_NONE      0x00
#define P_14443_4_CHAINING_COMMAND_PROCESS   0x01
#define P_14443_4_CHAINING_COMMAND_DONE      0x02
#define P_14443_4_CHAINING_RESPONSE_NONE     0x03
#define P_14443_4_CHAINING_RESPONSE_PROCESS  0x04
#define P_14443_4_CHAINING_RESPONSE_DONE     0x05

/* Declare a 14443-4 connection card type A*/
/* 11443-4 Command types */
#define P_14443_COMMAND_SEND_RATS            0x01
#define P_14443_COMMAND_SEND_PPS             0x02

/* 14443 defines */
#define P_14443_PPS_START_BYTE               0xD0  /* Fix the CID to 0000 */

typedef struct __tP14P4ConnectionInfoTypeA
{
   /* flags  */
   bool_t                       bIsPPSSupported;
   bool_t                       bIsCIDSupported;
   bool_t                       bIsNADSupported;
   /* Card ID */
   uint8_t                    nCID;
   /*  Node Address */
   uint8_t                    nNAD;
   /*historical bytes*/
   uint8_t                    aApplicationData[W_EMUL_APPLICATION_DATA_MAX_LENGTH];
   uint8_t                   nApplicationDataLength;
    /* card buffer capabilities */
   uint32_t                   nCardInputBufferSize;
   /* Reader buffer capabilities */
   uint32_t                   nReaderInputBufferSize;
   /* time of latency */
   uint8_t                    nFWI_SFGI;
   /* bit rate connection*/
   uint8_t                    nDataRateMaxDiv;
   uint32_t                   nBaudRate;
}tP14P4ConnectionInfoTypeA;

typedef struct __tP14P4ConnectionInfoTypeB
{
   uint8_t nNAD;
}tP14P4ConnectionInfoTypeB;


/* Declare a 14443-4 exchange data structure */
typedef struct __tP14P4Connection
{
   /* Memory handle registry */
   tHandleObjectHeader        sObjectHeader;

   /* Connection information */
   uint8_t                    nProtocol;
   uint8_t                    nAttempt;
   union
   {
      tP14P4ConnectionInfoTypeA sTypeA;
      tP14P4ConnectionInfoTypeB sTypeB;
   }tPart4ConnectionInfo;
#define s14typeA4 tPart4ConnectionInfo.sTypeA
#define s14typeB4 tPart4ConnectionInfo.sTypeB
   /*timeout*/
   uint32_t                   nTimeout;
   /* Chaining state (refer to chaining state defines) */
   bool_t                       bChainingState;
   /* Start index of the currently sent I-block */
   uint32_t                   nChainingIndex;
   /* Command-to-the-card PCB type (refer to PCB defines) */
   uint8_t                    nCommandType;
   /* Reponse-to-the-card PCB type (refer to PCB defines) */
   uint8_t                    nResponseType;
   /* Current block number */
   uint8_t                    nBlockNumber;
   /* Error code */
   W_ERROR                    nError;

   /* APDU command buffer */
   const uint8_t*             pReaderToCardBuffer;
   /* APDU command buffer length */
   uint32_t                   nReaderToCardBufferLength;
   /* APDU response buffer */
   uint8_t*                   pCardToReaderBuffer;
   /* APDU response buffer length */
   uint32_t                   nCardToReaderBufferLength;
   /* APDU response buffer maximum length */
   uint32_t                   nCardToReaderBufferMaxLength;

   /* TCL command buffer */
   uint8_t                    aReaderToCardBufferTCL[P_14443_3_FRAME_MAX_SIZE];
   /* TCL command buffer length */
   uint32_t                   nReaderToCardBufferLengthTCL;
   /* TCL dynamic command buffer */
   uint8_t*                   pAllocatedReaderToCardBufferTCL;
   /* TCL response buffer */
   uint8_t                    aCardToReaderBufferTCL[P_14443_3_FRAME_MAX_SIZE];
   /* TCL response buffer maximum length */
   uint32_t                   nCardToReaderBufferMaxLengthTCL;
   /* TCL dynamic command buffer */
   uint8_t*                   pAllocatedCardToReaderBufferTCL;

   /* Part4 callback context */
   tDFCCallbackContext        sCallbackContext;
} tP14P4Connection;

/* Destroy connection callback */
static void static_P14P4DestroyConnection(
            tContext* pContext,
            void* pObject );

/* Get properties connection callback */
static bool_t static_P14P4GetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray );

/* Check properties connection callback */
static bool_t static_P14P4CheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue );

/* send a frame command*/
static void static_P14P4SendCommand(
            tContext* pContext,
            tP14P4Connection* p14P4Connection,
            W_HANDLE hConnection,
            uint8_t nCommandType );

/* get card response to RATS or PPS command*/
static void static_P14P4GetCardResponseCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            uint32_t nDataLength,
            W_ERROR nError );
/**
 * @brief   Gets the 14443-4 connection property number.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/

static uint32_t static_P14P4UserGetPropertyNumber(
            tContext* pContext,
            void* pObject)
{
   return 1;
}

/* Handle registry 14443-4 type */
tHandleType g_s14P4Connection = { static_P14P4DestroyConnection,
                                  null,
                                  static_P14P4UserGetPropertyNumber,
                                  static_P14P4GetProperties,
                                  static_P14P4CheckProperties,
                                  null, null, null, null };

#define P_HANDLE_TYPE_14443_4_CONNECTION (&g_s14P4Connection)

/**
 * @brief   Destroyes a 14443-4 connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static void static_P14P4DestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tP14P4Connection* p14P4Connection = (tP14P4Connection*)pObject;

   PDebugTrace("static_P14P4DestroyConnection");

   PDFCFlushCall(&p14P4Connection->sCallbackContext);

   /* Free the 14443-4 connection structure */
   CMemoryFree( p14P4Connection );
}

/**
 * @brief   Gets the 14443-4 connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static bool_t static_P14P4GetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray )
{
   tP14P4Connection* p14P4Connection = (tP14P4Connection*)pObject;

   PDebugTrace("static_P14P4GetProperties");

   if ( p14P4Connection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
   {
      PDebugError("static_P14P4GetProperties: no property");
      return W_FALSE;
   }

   pPropertyArray[0] = p14P4Connection->nProtocol;
   return W_TRUE;
}

/**
 * @brief   Checkes the 14443-4 connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  nPropertyValue  The property value.
 **/
static bool_t static_P14P4CheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue )
{
   tP14P4Connection* p14P4Connection = (tP14P4Connection*)pObject;

   PDebugTrace(
      "static_P14P4CheckProperties: nPropertyValue=%s (0x%02X)",
      PUtilTraceConnectionProperty(nPropertyValue), nPropertyValue  );

   if ( p14P4Connection->nProtocol != P_PROTOCOL_STILL_UNKNOWN )
   {
      if ( nPropertyValue == p14P4Connection->nProtocol )
      {
         return W_TRUE;
      }
      else
      {
         return W_FALSE;
      }
   }
   else
   {
      PDebugError("static_P14P4CheckProperties: no property");
      return W_FALSE;
   }
}

/**
 * @brief   Sends an error to the process which has asked for the 14443-4 exchange.
 *
 * @param[in]  p14P4Connection  The 14443-4 connection.
 *
 * @param[in]  nError  The error code of the operation.
 **/
static void static_P14P4SendError(
            tP14P4Connection* p14P4Connection,
            W_ERROR nError )
{
   /* Store the error code */
   p14P4Connection->nError = nError;

   if (( p14P4Connection->nCommandType != P_14443_COMMAND_SEND_RATS )&&
       ( p14P4Connection->nCommandType != P_14443_COMMAND_SEND_PPS ))
   {
      /* Send the error */
      PDFCPostContext3(
         &p14P4Connection->sCallbackContext,
         0,
         nError );

      if ( p14P4Connection->pAllocatedReaderToCardBufferTCL != null )
      {
         /* Free the buffer */
         CMemoryFree( p14P4Connection->pAllocatedReaderToCardBufferTCL );
      }

      if ( p14P4Connection->pAllocatedCardToReaderBufferTCL != null )
      {
         /* Free the buffer */
         CMemoryFree( p14P4Connection->pAllocatedCardToReaderBufferTCL );
      }
   }
   else
   {
      /* Send the result */
      PDFCPostContext2(
         &p14P4Connection->sCallbackContext,
         nError );
   }
}

/* See tWBasicGenericDataCallbackFunction */
static void static_P14P4ExchangeDataCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            uint32_t nDataLength,
            W_ERROR nError)
{
   W_HANDLE hConnection;
   tP14P4Connection* p14P4Connection  = (tP14P4Connection*)PReaderUserConvertPointerToConnection(
      pContext, pCallbackParameter, P_HANDLE_TYPE_14443_4_CONNECTION, &hConnection);

   bool_t     bIsCIDFromCardSupported;
   bool_t     bIsNADFromCardSupported;
   uint8_t  nPCB = 0x00;
   uint8_t  nPrologueLength = 1;
   uint8_t  nWTXM = 1;
   uint8_t  nChainingFlag = 0x00;
   uint8_t  bSendNextBlockNumber = W_FALSE;
   uint32_t nNewTimeout = 0;
   uint32_t nReaderToCardBufferLengthCL = 0;
   W_ERROR  nErrorProcess = W_ERROR_RF_COMMUNICATION;
   tW14Part3ConnectionInfo p14Part3ConnectionInfo;
   bool_t bIsCIDSupported = W_FALSE;
   bool_t bIsNADSupported  = W_FALSE;
   uint8_t nCID = 0;
   uint8_t nNAD = 0;
   uint32_t nTimeout = 0;
   uint32_t nCardInputBufferSize = P_14443_3_BUFFER_MAX_SIZE;
   uint8_t* pCardToReaderBuffer;

   if(p14P4Connection->pAllocatedCardToReaderBufferTCL != null )
   {
      pCardToReaderBuffer = p14P4Connection->pAllocatedCardToReaderBufferTCL;
   }
   else
   {
      pCardToReaderBuffer = p14P4Connection->aCardToReaderBufferTCL;
   }

   /* Get the connection information */
   /* Get the connection information */
      if(p14P4Connection->nProtocol == W_PROP_ISO_14443_4_A)
      {
         bIsCIDSupported =      p14P4Connection->s14typeA4.bIsCIDSupported;
         bIsNADSupported =      p14P4Connection->s14typeA4.bIsNADSupported;
         nCID            =      p14P4Connection->s14typeA4.nCID;
         nNAD            =      p14P4Connection->s14typeA4.nNAD;
         nCardInputBufferSize = p14P4Connection->s14typeA4.nCardInputBufferSize;
         nTimeout             =  p14P4Connection->nTimeout;

      }
      else if(p14P4Connection->nProtocol == W_PROP_ISO_14443_4_B)
      {
         nErrorProcess = P14Part3GetConnectionInfo(
                                       pContext,
                                       hConnection,
                                       &p14Part3ConnectionInfo);
         if ( nErrorProcess != W_SUCCESS )
         {
            PDebugWarning(
               "static_P14P4ExchangeDataCompleted: could not get the connection information 0x%08X",
               nErrorProcess );
            goto error;
         }
         bIsCIDSupported =      p14Part3ConnectionInfo.sW14TypeB.bIsCIDSupported;
         bIsNADSupported =      p14Part3ConnectionInfo.sW14TypeB.bIsNADSupported;
         nCID            =      (p14Part3ConnectionInfo.sW14TypeB.nMBLI_CID & 0x0F);
         nCardInputBufferSize = p14Part3ConnectionInfo.sW14TypeB.nCardInputBufferSize;
         nTimeout        =       p14Part3ConnectionInfo.sW14TypeB.nTimeout;
         nNAD            =      p14P4Connection->s14typeB4.nNAD;
      }
      else
      {
         nErrorProcess = W_ERROR_CONNECTION_COMPATIBILITY;
         PDebugWarning("static_P14P4ExchangeDataCompleted: unknow protocol 0x%02X", p14P4Connection->nProtocol );
         goto error;
      }

   /* Check if an answer was received */
   if ( nDataLength != 0x00 )
   {
      /* Check if the CID is used */
      if ( bIsCIDSupported != W_FALSE )
      {
         /* Check if the CID is present in the response */
         bIsCIDFromCardSupported = pCardToReaderBuffer[0] & P_14443_4_CID_PRESENT;
         /* Get the correct APDU start index */
         if ( bIsCIDFromCardSupported != W_FALSE )
         {
            /* Increase the PCB length */
            nPrologueLength ++;

            /* Compare the two CID */
            if ( nCID != ( pCardToReaderBuffer[1] & 0x0F ) )
            {
               PDebugWarning(
                  "static_P14P4ExchangeDataCompleted: nCID %d != CID in response %d",
                  nCID,
                  ( pCardToReaderBuffer[1] & 0x0F ));

               goto error;
            }

            /* Check if the CID is correct */
            if ( ( pCardToReaderBuffer[1] & 30 ) != 0x00 ) /* 0011 0000 */
            {
               PDebugWarning("static_P14P4ExchangeDataCompleted: ( pCardToReaderBuffer[1] & 30 ) != 0x00");

               goto error;
            }
         }
      }

      /* Check if the NAD is used */
      if ( bIsNADSupported != W_FALSE )
      {
         /* Check if the NAD is present in the response */
         bIsNADFromCardSupported = pCardToReaderBuffer[0] & P_14443_4_NAD_PRESENT;
         /* Get the correct APDU start index */
         if ( bIsNADFromCardSupported != W_FALSE )
         {
            /* Increase the PCB length */
            nPrologueLength ++;

            /* Compare the two NAD */
            if ( nNAD != pCardToReaderBuffer[2] )
            {
               PDebugWarning(
                  "static_P14P4ExchangeDataCompleted: nNAD %d != NAD in response %d",
                  nNAD,
                  pCardToReaderBuffer[1] );

               goto error;
            }

            /* Check if the NAD is correct */
            if ( ( pCardToReaderBuffer[2] & 0x77 ) != 0x00 ) /* 0111 0111 */
            {
               PDebugWarning("static_P14P4ExchangeDataCompleted: ( pCardToReaderBuffer[2] & 0x77 ) != 0x00");

               goto error;
            }
         }
      }
   }

   /* Check the error code return */
   switch ( nError )
   {
      case W_ERROR_BUFFER_TOO_LARGE:
         PDebugError("static_P14P4ExchangeDataCompleted: W_ERROR_BUFFER_TOO_LARGE");
         /* Check the maximum length again */
         if ( nCardInputBufferSize > p14P4Connection->nCardToReaderBufferMaxLengthTCL )
         {
            PDebugError("static_P14P4ExchangeDataCompleted: card buffer-in size should be big enough");
         }
         else
         {
            PDebugError("static_P14P4ExchangeDataCompleted: bug in the chaining process");
         }
         break;

      case W_SUCCESS:
         PDebugTrace("static_P14P4ExchangeDataCompleted: W_SUCCESS");

         /* Get the response type */
         nPCB = pCardToReaderBuffer[0] & 0xE2; /* 11100010 */

         /* Check the previous command sent for protocol error */
         /* If a deselect command has been sent but the command received is not a deselect response */
         if (  ( p14P4Connection->nCommandType == P_14443_4_PCB_S_DESELECT )
            && ( nPCB != P_14443_4_PCB_S_DESELECT ) )
         {
            PDebugWarning("static_P14P4ExchangeDataCompleted: P_14443_4_PCB_S_DESELECT problem");

            goto error;
         }

         /* Process the response */
         switch ( nPCB )
         {
            /* I-block */
            case P_14443_4_PCB_I:
               PDebugTrace("static_P14P4ExchangeDataCompleted: I-block");

               /* Check the block number */
               if ( ( pCardToReaderBuffer[0] & 0x01 ) == p14P4Connection->nBlockNumber )
               {
                  /* Check if the response arrives during a chaining I-block on PCD side */
                  if (  ( p14P4Connection->nCommandType == P_14443_4_PCB_I )
                     && ( p14P4Connection->bChainingState == P_14443_4_CHAINING_COMMAND_PROCESS ) )
                  {
                     PDebugWarning("static_P14P4ExchangeDataCompleted: I-block received while chaining");

                     goto error;
                  }

                  /* If the output buffer is too short */
                  if ( ( p14P4Connection->nChainingIndex + nDataLength - nPrologueLength ) >
                           p14P4Connection->nCardToReaderBufferMaxLength )
                  {
                     PDebugWarning("static_P14P4ExchangeDataCompleted: buffer to short");

                     nErrorProcess = W_ERROR_BUFFER_TOO_SHORT;
                     goto error;
                  }
                  else
                  {
                     /* Copy the response */
                     CMemoryCopy(
                        &p14P4Connection->pCardToReaderBuffer[p14P4Connection->nChainingIndex],
                        &pCardToReaderBuffer[nPrologueLength],
                        ( nDataLength - nPrologueLength /* PCB */ ) );

                     /* Check the chaining index */
                     if ( ( pCardToReaderBuffer[0] & P_14443_4_CHAINING_PRESENT ) == 0x00 )
                     {
                        PDebugTrace("static_P14P4ExchangeDataCompleted: chaining OFF");

                        /* Check the chaining value */
                        if ( p14P4Connection->bChainingState == P_14443_4_CHAINING_RESPONSE_PROCESS )
                        {
                           /* Set the chaining state to DONE */
                           p14P4Connection->bChainingState = P_14443_4_CHAINING_RESPONSE_DONE;
                        }
                        else
                        {
                           /* Set the chaining state to NONE */
                           p14P4Connection->bChainingState = P_14443_4_CHAINING_RESPONSE_NONE;
                        }

                        /* Store the response length */
                        p14P4Connection->nCardToReaderBufferLength  = p14P4Connection->nChainingIndex
                                                                     + nDataLength
                                                                     - nPrologueLength /* PCB */;

                        /* Store the error code */
                        p14P4Connection->nError = W_SUCCESS;

                        /* Provide the card response */
                        PDFCPostContext3(
                           &p14P4Connection->sCallbackContext,
                           p14P4Connection->nCardToReaderBufferLength,
                           W_SUCCESS );

                        /* Clear the command information */
                        p14P4Connection->nChainingIndex              = 0;
                        p14P4Connection->pReaderToCardBuffer         = null;
                        p14P4Connection->nReaderToCardBufferLength   = 0;
                        p14P4Connection->pCardToReaderBuffer         = null;
                        p14P4Connection->nCardToReaderBufferLength   = 0;
                        if ( p14P4Connection->pAllocatedReaderToCardBufferTCL != null )
                        {
                           /* Free the buffer */
                           CMemoryFree( p14P4Connection->pAllocatedReaderToCardBufferTCL );
                        }

                        if ( p14P4Connection->pAllocatedCardToReaderBufferTCL != null )
                        {
                           /* Free the buffer */
                           CMemoryFree( p14P4Connection->pAllocatedCardToReaderBufferTCL );
                        }

                        /* Send no more commands */
                        p14P4Connection->nResponseType = P_14443_4_PCB_NONE;
                     }
                     else
                     {
                        PDebugTrace("static_P14P4ExchangeDataCompleted: chaining ON");

                        /* Increase the chaining index accordingly */
                        p14P4Connection->nChainingIndex += nDataLength - nPrologueLength /* PCB */;

                        /* Send the R(ACK) command */
                        p14P4Connection->nResponseType = P_14443_4_PCB_R_ACK;

                        /* Set the chaining state to PROCESS */
                        p14P4Connection->bChainingState = P_14443_4_CHAINING_RESPONSE_PROCESS;
                     }
                  }

                  /* Switch the block number */
                  p14P4Connection->nBlockNumber ^= 0x01;
               }
               else
               {
                  PDebugWarning("static_P14P4ExchangeDataCompleted: wrong block number");

                  goto error;
               }
               break;

            /* R(NACK)-block: equivalent to a timeout */
            case P_14443_4_PCB_R_NACK:
               PDebugTrace("static_P14P4ExchangeDataCompleted: R(NACK)-block");

               goto error;

            /* R(ACK)-block */
            case P_14443_4_PCB_R_ACK:
               PDebugTrace("static_P14P4ExchangeDataCompleted: R(ACK)-block");

               /* Send an I-block */
               p14P4Connection->nResponseType = P_14443_4_PCB_I;

               /* Check the block number */
               if ( ( pCardToReaderBuffer[0] & 0x01 ) == p14P4Connection->nBlockNumber )
               {
                  PDebugTrace("static_P14P4ExchangeDataCompleted: same block number");

                  /* Check if the R(ACK) is received but we are not on chaining I-block process */
                  if (  ( p14P4Connection->nCommandType    == P_14443_4_PCB_I )
                     && ( p14P4Connection->bChainingState  != P_14443_4_CHAINING_COMMAND_PROCESS ) )
                  {
                     PDebugWarning("static_P14P4ExchangeDataCompleted: not chaining I-block");

                     goto error;
                  }

                  /* Send the next I-block */
                  bSendNextBlockNumber = W_TRUE;

                  /* Switch the block number */
                  p14P4Connection->nBlockNumber ^= 0x01;
               }
               else
               {
                  PDebugWarning("static_P14P4ExchangeDataCompleted: different block number");

                  /* Send the last I-block previously sent */
                  bSendNextBlockNumber = W_FALSE;
               }
               break;

            /* S-block */
            case P_14443_4_PCB_S_DESELECT:
            case P_14443_4_PCB_S_WTXM:
               PDebugTrace("static_P14P4ExchangeDataCompleted: S-block");

               /* Check the S-block type */
               if ( nPCB == P_14443_4_PCB_S_WTXM )
               {
                  /* Check if the previous command was not a R(ACK) */
                  if ( p14P4Connection->nResponseType == P_14443_4_PCB_R_ACK )
                  {
                     PDebugWarning("static_P14P4ExchangeDataCompleted: S-block response after a R(ACK) command");

                     goto error;
                  }

                  /* Get the new timeout value */
                  /* @note: the power level is not supported (00) */
                  nWTXM = pCardToReaderBuffer[nPrologueLength] & 0x3F;

                  /* Check the waiting time extension value */
                  if (  ( nWTXM > 0)
                     && ( nWTXM < 60) )
                  {
                     /* Calculate the new timeout */
                     nNewTimeout = nTimeout * nWTXM;
                     PDebugTrace(
                        "static_P14P4ExchangeDataCompleted: new timeout %d",
                        nNewTimeout);
                     /* Check if the new timeout is compliant */
                     if ( nNewTimeout > 14 )
                     {
                        PDebugWarning("static_P14P4ExchangeDataCompleted: set timeout to 14");
                        nNewTimeout = 14;
                     }
                     if ( nNewTimeout != nTimeout )
                     {
                        /* Set the temporary timeout at 14443-3 level */
                        P14Part3SetTimeout(
                           pContext,
                           hConnection,
                           nNewTimeout );
                     }
                     /* Send the WTXM response */
                     p14P4Connection->nResponseType = P_14443_4_PCB_S_WTXM;
                  }
                  else
                  {
                     PDebugWarning(
                        "static_P14P4ExchangeDataCompleted: wrong nWTXM 0x%02X",
                        nWTXM);

                     goto error;
                  }
               }
               else
               {
                  PDebugTrace("static_P14P4ExchangeDataCompleted: DESELECT response received");

                  /* Check the previous command sent */
                  if ( p14P4Connection->nCommandType == P_14443_4_PCB_S_DESELECT )
                  {
                     /* Reset the block number */
                     p14P4Connection->nBlockNumber = 0x00;

                     /* Reset the response type */
                     p14P4Connection->nResponseType = P_14443_4_PCB_NONE;

                     /* Send the error */
                     static_P14P4SendError(
                        p14P4Connection,
                        p14P4Connection->nError );
                  }
                  else
                  {
                     PDebugTrace("static_P14P4ExchangeDataCompleted: DESELECT command not sent");

                     goto error;
                  }
               }
               break;

            default:
               PDebugWarning(
                  "static_P14P4ExchangeDataCompleted: wrong response 0x%08X",
                  pCardToReaderBuffer[0] );

               goto error;
         }
         break;

      case W_ERROR_TIMEOUT:
         /* Check the number of attempt left */
         if ( p14P4Connection->nAttempt < P_14443_4_MAX_ATTEMPT )
         {
            /* Increase the number of attempt */
            p14P4Connection->nAttempt ++;

            /* Check the type of the last command sent */
            /* I-block & chaining */
            if (  ( p14P4Connection->nCommandType     == P_14443_4_PCB_I )
               && ( p14P4Connection->bChainingState   != P_14443_4_CHAINING_COMMAND_NONE) )
            {
               /* Send the R(ACK) command */
               p14P4Connection->nResponseType = P_14443_4_PCB_R_ACK;
            }
            else
            {
               /* Deselect S-block */
               if ( p14P4Connection->nCommandType == P_14443_4_PCB_S_DESELECT )
               {
                  /* Send the deselect command */
                  p14P4Connection->nResponseType = P_14443_4_PCB_S_DESELECT;
               }
               else
               {
                  /* Send the R(NACK) command */
                  p14P4Connection->nResponseType = P_14443_4_PCB_R_NACK;
               }
            }
         }
         else
         {
            PDebugWarning("static_P14P4ExchangeDataCompleted: maximum number of attempt reached");

            /* Check the command type */
            if ( p14P4Connection->nCommandType != P_14443_4_PCB_S_DESELECT )
            {
               nErrorProcess = nError;

               goto error;
            }
            else
            {
               p14P4Connection->nResponseType = P_14443_4_PCB_NONE;

               /* Send the error */
               static_P14P4SendError(
                  p14P4Connection,
                  p14P4Connection->nError );
            }
         }
         break;

      case W_ERROR_CANCEL:
         /* Check the command type */
         if ( p14P4Connection->nCommandType != P_14443_4_PCB_S_DESELECT )
         {
            nErrorProcess = nError;

            goto error;
         }
         else
         {
            p14P4Connection->nResponseType = P_14443_4_PCB_NONE;

            /* Send the error */
            static_P14P4SendError(
               p14P4Connection,
               p14P4Connection->nError );
         }
         break;

      default:
         PDebugWarning(
            "static_P14P4ExchangeDataCompleted: unknown error 0x%08X",
            nError );
         if ( p14P4Connection->nResponseType != P_14443_4_PCB_S_DESELECT )
         {
            nErrorProcess = nError;
            goto error;
         }
         else
         {
            p14P4Connection->nResponseType = P_14443_4_PCB_NONE;

            /* Send the error */
            static_P14P4SendError(
               p14P4Connection,
               p14P4Connection->nError );
         }
         break;
   }

   /* Check if we need to send a response */
   if ( p14P4Connection->nResponseType == P_14443_4_PCB_NONE )
   {
      PDebugTrace("static_P14P4ExchangeDataCompleted: no response/command to send");

      return;
   }

   goto send_command;

/* Send an error */
error:
   PDebugTrace(
      "static_P14P4ExchangeDataCompleted: return error 0x%08X",
      nErrorProcess );

   /* Store the error code */
   p14P4Connection->nError = nErrorProcess;

   /* Send a deselect command */
   p14P4Connection->nResponseType = P_14443_4_PCB_S_DESELECT;

   /* Reset the number of attempt */
   p14P4Connection->nAttempt = 1;

send_command:
   /* Set the prologue length */
   nPrologueLength = 1;
   /* Check if the CID is present */
   if ( bIsCIDSupported != W_FALSE )
   {
      /* Increase the PCB length */
      nPrologueLength ++;
   }
   /* Allocate the command buffer */
   switch ( p14P4Connection->nResponseType )
   {
      /* Wait time S-block */
      case P_14443_4_PCB_S_WTXM:
         /* Reset the number of attempt */
         p14P4Connection->nAttempt = 0;
         nReaderToCardBufferLengthCL = nPrologueLength + 1;
         break;
      /* I-block */
      case P_14443_4_PCB_I:
         /* Reset the number of attempt */
         p14P4Connection->nAttempt = 0;
         /* Check if the NAD is present */
         if ( bIsNADSupported != W_FALSE )
         {
            /* Increase the PCB length */
            nPrologueLength ++;
         }
         /* In case of I-block next */
         if ( bSendNextBlockNumber != W_FALSE )
         {
            /* Increase the chaining index */
            p14P4Connection->nChainingIndex += nCardInputBufferSize - 2 /*EDC*/ - 1 /* Control byte */ - nPrologueLength;
         }

         /* Get the command length */
         nReaderToCardBufferLengthCL   = nPrologueLength
                                       + p14P4Connection->nReaderToCardBufferLength
                                       - p14P4Connection->nChainingIndex;

         /* Check the command length */
         if ( ( nReaderToCardBufferLengthCL + 2 /*EDC*/ + 1 /* Control byte */ ) > nCardInputBufferSize )
         {
            /* Chaining ON */
            nChainingFlag = P_14443_4_CHAINING_PRESENT;
            nReaderToCardBufferLengthCL = nCardInputBufferSize - 2 /*EDC*/ - 1 /* Control byte */;
         }
         else
         {
            /* Check the current chaining state */
            if (p14P4Connection->bChainingState != P_14443_4_CHAINING_RESPONSE_NONE)
            {
               p14P4Connection->bChainingState = P_14443_4_CHAINING_RESPONSE_DONE;
            }
         }
         break;
      /* Other commands */
      default:
         nReaderToCardBufferLengthCL = nPrologueLength;
         break;
   }

   /* Set the PCB */
   p14P4Connection->aReaderToCardBufferTCL[0] = p14P4Connection->nResponseType;
   if ( p14P4Connection->nResponseType == P_14443_4_PCB_S_WTXM )
   {
      p14P4Connection->aReaderToCardBufferTCL[0] |= 0x10;
   }

   /* Check the type of response */
   switch ( p14P4Connection->nResponseType )
   {
      /* Deselect S-block */
      case P_14443_4_PCB_S_DESELECT:
         PDebugTrace("static_P14P4ExchangeDataCompleted: send a deselect");
         p14P4Connection->nCommandType = P_14443_4_PCB_S_DESELECT;
         p14P4Connection->nCardToReaderBufferMaxLengthTCL = nReaderToCardBufferLengthCL;
         break;
      /* R-block */
      case P_14443_4_PCB_R_ACK:
      case P_14443_4_PCB_R_NACK:
         if ( p14P4Connection->nResponseType == P_14443_4_PCB_R_ACK )
         {
            PDebugTrace("static_P14P4ExchangeDataCompleted: send a R(ACK)");
         }
         else
         {
            PDebugTrace("static_P14P4ExchangeDataCompleted: send a R(NACK)");
         }
         /* Add the block number */
         p14P4Connection->aReaderToCardBufferTCL[0] |= p14P4Connection->nBlockNumber;
         p14P4Connection->nCardToReaderBufferMaxLengthTCL =
            p14P4Connection->nCardToReaderBufferMaxLength + nPrologueLength;
         break;
      /* Wait time S-block */
      case P_14443_4_PCB_S_WTXM:
         PDebugTrace("static_P14P4ExchangeDataCompleted: send a WTXM");
         /* @note: the power level is not supported (00) */
         p14P4Connection->aReaderToCardBufferTCL[nPrologueLength] = nWTXM;
         p14P4Connection->nCardToReaderBufferMaxLengthTCL =
            p14P4Connection->nCardToReaderBufferMaxLength + nPrologueLength;
         break;
      /* I-block */
      case P_14443_4_PCB_I:
         /* Check if the NAD is present */
         if ( bIsNADSupported != W_FALSE )
         {
            p14P4Connection->aReaderToCardBufferTCL[0] |= P_14443_4_NAD_PRESENT;
            p14P4Connection->aReaderToCardBufferTCL[nPrologueLength - 1] = nNAD;
         }

         /* Set the chaining flag */
         p14P4Connection->aReaderToCardBufferTCL[0] |= nChainingFlag;
         /* Add the block number */
         p14P4Connection->aReaderToCardBufferTCL[0] |= p14P4Connection->nBlockNumber;

         /* Check the I-block type */
         if ( bSendNextBlockNumber != W_FALSE )
         {
            PDebugTrace("static_P14P4ExchangeDataCompleted: send the next I-block");
         }
         else
         {
            PDebugTrace("static_P14P4ExchangeDataCompleted: send the current I-block");
         }

         /* Copy the APDU */
         CMemoryCopy(
            &p14P4Connection->aReaderToCardBufferTCL[nPrologueLength],
            &p14P4Connection->pReaderToCardBuffer[p14P4Connection->nChainingIndex],
            nReaderToCardBufferLengthCL - nPrologueLength );
         break;
   }

   /* Insert the CID if needed */
   if ( bIsCIDSupported != W_FALSE )
   {
      p14P4Connection->aReaderToCardBufferTCL[0] |= P_14443_4_CID_PRESENT;
      p14P4Connection->aReaderToCardBufferTCL[1] = nCID;
   }

   /* Store the length */
   p14P4Connection->nReaderToCardBufferLengthTCL = nReaderToCardBufferLengthCL;

   /* Send the command */
   P14P3UserExchangeData(
      pContext,
      hConnection,
      static_P14P4ExchangeDataCompleted,
      pCallbackParameter,
      p14P4Connection->aReaderToCardBufferTCL,
      nReaderToCardBufferLengthCL,
      pCardToReaderBuffer,
      p14P4Connection->nCardToReaderBufferMaxLengthTCL,
      null,
      W_TRUE );

   /* Reset the timeout value */
   if ( p14P4Connection->nResponseType == P_14443_4_PCB_S_WTXM )
   {
      P14Part3SetTimeout(
         pContext,
         hConnection,
         nTimeout );
   }
}

/* See Header file */
void P14P4CreateConnection(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericCallbackFunction* pCallback,
            void* pCallbackParameter,
            uint8_t nProtocol )
{
   tP14P4Connection* p14P4Connection;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Create the 14443-4 buffer */
   p14P4Connection = (tP14P4Connection*)CMemoryAlloc( sizeof(tP14P4Connection) );
   if ( p14P4Connection == null )
   {
      PDebugError("P14P4CreateConnection: p14P4Connection == null");
      /* Send the result */
      PDFCPostContext2(
         &sCallbackContext,
         W_ERROR_OUT_OF_RESOURCE );
      return;
   }
   CMemoryFill(p14P4Connection, 0, sizeof(tP14P4Connection));

   /* Check if the card is compliant with 14443-4 */
   if ( P14P3UserCheckPart4(
            pContext,
            hConnection ) != W_SUCCESS )
   {
      PDebugError("P14P4CreateConnection: not 14443-4 compliant");
      CMemoryFree(p14P4Connection);
       /* Send the result */
      PDFCPostContext2(
         &sCallbackContext,
         W_ERROR_CONNECTION_COMPATIBILITY );
      return;
   }

   /* Add the 14443-4 structure */
   if ( ( nError = PHandleAddHeir(
                     pContext,
                     hConnection,
                     p14P4Connection,
                     P_HANDLE_TYPE_14443_4_CONNECTION ) ) != W_SUCCESS )
   {
      PDebugError("P14P4CreateConnection: could not add the 14443-4 buffer");
      CMemoryFree(p14P4Connection);
       /* Send the result */
      PDFCPostContext2(
         &sCallbackContext,
         nError );
      return;
   }

   /* Store the callback context */
   p14P4Connection->sCallbackContext = sCallbackContext;

   /* Store the connection information */
   p14P4Connection->nProtocol = nProtocol;
   p14P4Connection->nBlockNumber = 0x00;

   if(nProtocol == W_PROP_ISO_14443_4_A)
   {
      p14P4Connection->s14typeA4.nReaderInputBufferSize = P_14443_3_BUFFER_MAX_SIZE;
      /* Send the RATS */
      static_P14P4SendCommand(
                        pContext,
                        p14P4Connection,
                        hConnection,
                        P_14443_COMMAND_SEND_RATS );
   }
   else
   {
      /* Send the result */
      PDFCPostContext2(
         &sCallbackContext,
         W_SUCCESS );
   }

   return;
}

/* See Header file*/
W_ERROR P14Part4ListenToCardDetectionTypeB(
                     tContext* pContext,
                     tPReaderCardDetectionHandler* pHandler,
                     void* pHandlerParameter,
                     uint8_t nPriority,
                     uint8_t nAFI,
                     bool_t bUseCID,
                     uint8_t nCID,
                     uint32_t nBaudRate,
                     const uint8_t* pHigherLayerDataBuffer,
                     uint8_t  nHigherLayerDataLength,
                     W_HANDLE* phEventRegistry)
{
   tP14P3DetectionConfigurationTypeB sDetectionConfiguration, *pDetectionConfigurationBuffer;
   static const uint8_t nProtocol = W_PROP_ISO_14443_4_B;
   uint32_t nDetectionConfigurationBufferLength =
      sizeof(tP14P3DetectionConfigurationTypeB) - W_EMUL_HIGHER_LAYER_DATA_MAX_LENGTH;

   if (((pHigherLayerDataBuffer != null) && (nHigherLayerDataLength == 0)) ||
       ((pHigherLayerDataBuffer == null) && (nHigherLayerDataLength != 0)))
   {
      return W_ERROR_BAD_PARAMETER;
   }

   if ((nPriority == W_PRIORITY_SE) || (nPriority == W_PRIORITY_SE_FORCED))
   {
      return W_ERROR_BAD_PARAMETER;
   }

   /* The anti-collision parameters shall be zero if W_PRIORITY_EXCLUSIVE is not used*/
   if((nAFI != 0x00)
   ||(bUseCID != W_FALSE)
   ||(nCID != 0x00)
   ||(nBaudRate != 0x00)
   ||(pHigherLayerDataBuffer != null)
   ||(nHigherLayerDataLength != 0x00))
   {
      if(nPriority != W_PRIORITY_EXCLUSIVE)
      {
          PDebugError("P14Part4ListenToCardDetectionTypeB: Bad Detection configuration parameters or bad priorty type passed");
         return W_ERROR_BAD_PARAMETER;
      }
      else
      {
         /* set the pointer to configuration buffer*/
         pDetectionConfigurationBuffer = &sDetectionConfiguration;
         /* set the AFI */
         sDetectionConfiguration.nAFI = nAFI;
         /* set the CID */
         sDetectionConfiguration.bUseCID = bUseCID;
         sDetectionConfiguration.nCID = nCID;
         /* set the bit rate */
         sDetectionConfiguration.nBaudRate = nBaudRate;
         /* set the high layer data length */
         sDetectionConfiguration.nHigherLayerDataLength = nHigherLayerDataLength;

         /* set the hayer layer data */
         if((nHigherLayerDataLength > 0)&&
            (nHigherLayerDataLength <= W_EMUL_HIGHER_LAYER_DATA_MAX_LENGTH)&&
            (pHigherLayerDataBuffer != null))
         {
            CMemoryCopy(
               sDetectionConfiguration.aHigherLayerData,
               pHigherLayerDataBuffer,
               nHigherLayerDataLength);
            nDetectionConfigurationBufferLength += nHigherLayerDataLength;

         }
      }
   }
   else
   {
      pDetectionConfigurationBuffer = null;
      nDetectionConfigurationBufferLength = 0x00;
   }

   /* Launch the card detection request */
   return PReaderUserListenToCardDetection(
                     pContext,
                     pHandler, pHandlerParameter,
                     nPriority,
                     &nProtocol, 1,
                     &sDetectionConfiguration, nDetectionConfigurationBufferLength,
                     phEventRegistry );
}

/* See Client API Specifications */
void P14Part4ExchangeData(
            tContext* pContext,
            W_HANDLE hConnection,
            tPBasicGenericDataCallbackFunction* pCallback,
            void* pCallbackParameter,
            const uint8_t* pReaderToCardBuffer,
            uint32_t nReaderToCardBufferLength,
            uint8_t* pCardToReaderBuffer,
            uint32_t nCardToReaderBufferMaxLength,
            W_HANDLE* phOperation )
{
   tP14P4Connection* p14P4Connection;
   tDFCCallbackContext sCallbackContext;
   uint8_t nPrologueLength = 1;
   W_ERROR nError = W_SUCCESS;
   tW14Part3ConnectionInfo p14Part3ConnectionInfo;
    bool_t bIsCIDSupported = W_FALSE;
   bool_t bIsNADSupported  = W_FALSE;
   uint8_t nCID = 0;
   uint8_t nNAD = 0;
   uint32_t nCardInputBufferSize = P_14443_3_BUFFER_MAX_SIZE;

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_14443_4_CONNECTION, (void**)&p14P4Connection);
   if ( nError == W_SUCCESS )
   {
      /* Check if an operation is still pending */
      if ( p14P4Connection->nError == P_ERROR_STILL_PENDING )
      {
         PDebugError("P14Part4ExchangeData: W_ERROR_BAD_STATE");
         /* Send the error */
         PDFCPostContext3(
            &sCallbackContext,
            0,
            W_ERROR_BAD_STATE );
         return;
      }

      /* Store the callback context */
      p14P4Connection->sCallbackContext       = sCallbackContext;
      /* Store the callback buffer */
      p14P4Connection->pCardToReaderBuffer    = pCardToReaderBuffer;
      /* Reset the dynamic buffer addresses */
      p14P4Connection->pAllocatedReaderToCardBufferTCL = null;
      p14P4Connection->pAllocatedCardToReaderBufferTCL = null;

      /* Check the protocol type */
      if ( p14P4Connection->nProtocol == P_PROTOCOL_STILL_UNKNOWN )
      {
         PDebugError("P14Part4ExchangeData: W_ERROR_CONNECTION_COMPATIBILITY");
         /* Send the error */
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
         goto send_error;
      }

      /* Check the parameters */
      if (  (  ( pReaderToCardBuffer == null )
            && ( nReaderToCardBufferLength != 0 ) )
         || (  ( pReaderToCardBuffer != null )
            && ( nReaderToCardBufferLength == 0 ) )
         || (  ( pCardToReaderBuffer == null )
            && ( nCardToReaderBufferMaxLength != 0 ) )
         || (  ( pCardToReaderBuffer != null )
            && ( nCardToReaderBufferMaxLength == 0 ) ) )
      {
         PDebugError("P14Part4ExchangeData: W_ERROR_BAD_PARAMETER");
         /* Send the error */
         nError = W_ERROR_BAD_PARAMETER;
         goto send_error;
      }

      /* Get the connection information */
      if(p14P4Connection->nProtocol == W_PROP_ISO_14443_4_A)
      {
         bIsCIDSupported =      p14P4Connection->s14typeA4.bIsCIDSupported;
         bIsNADSupported =      p14P4Connection->s14typeA4.bIsNADSupported;
         nCID            =      p14P4Connection->s14typeA4.nCID;
         nNAD            =      p14P4Connection->s14typeA4.nNAD;
         nCardInputBufferSize = p14P4Connection->s14typeA4.nCardInputBufferSize;

      }
      else if(p14P4Connection->nProtocol == W_PROP_ISO_14443_4_B)
      {
         nError = P14Part3GetConnectionInfo(
                                       pContext,
                                       hConnection,
                                       &p14Part3ConnectionInfo);
         if ( nError != W_SUCCESS )
         {
            PDebugWarning(
               "P14Part4ExchangeData: could not get the connection information 0x%08X",
               nError );
            goto send_error;
         }
         bIsCIDSupported =      p14Part3ConnectionInfo.sW14TypeB.bIsCIDSupported;
         bIsNADSupported =      p14Part3ConnectionInfo.sW14TypeB.bIsNADSupported;
         nCID            =      (p14Part3ConnectionInfo.sW14TypeB.nMBLI_CID & 0x0F);
         nCardInputBufferSize = p14Part3ConnectionInfo.sW14TypeB.nCardInputBufferSize;
         nNAD            =      p14P4Connection->s14typeB4.nNAD;
      }
      else
      {
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
         PDebugWarning("P14Part4ExchangeData: unknow protocol 0x%02X", p14P4Connection->nProtocol );
         goto send_error;
      }

      /* Generate the prologue field according to the connection information */
      if ( bIsCIDSupported != W_FALSE )
      {
         nPrologueLength ++;
      }
      if ( bIsNADSupported != W_FALSE )
      {
         nPrologueLength ++;
      }

      /* Check if the command does not need to be chained */
      if ( ( nPrologueLength + nReaderToCardBufferLength + 2 /* EDC */ + 1 /* Control Byte */) <= nCardInputBufferSize )
      {
         /* Set the chaining state */
         p14P4Connection->bChainingState = P_14443_4_CHAINING_COMMAND_NONE;

         /* Set the length */
         p14P4Connection->nReaderToCardBufferLengthTCL = nPrologueLength /* PCB */ + nReaderToCardBufferLength /* APDU */;
      }
      else
      {
         /* Set the chaining state */
         p14P4Connection->bChainingState = P_14443_4_CHAINING_COMMAND_PROCESS;

         /* Set the length */
         p14P4Connection->nReaderToCardBufferLengthTCL = nCardInputBufferSize - 2 /* EDC */ - 1 /* Control Byte */;
      }

      /* Check if the command buffer is big enough */
      if ( p14P4Connection->nReaderToCardBufferLengthTCL > P_14443_3_FRAME_MAX_SIZE )
      {
         /* Allocate a dynamic buffer */
         p14P4Connection->pAllocatedReaderToCardBufferTCL = (uint8_t*)CMemoryAlloc( p14P4Connection->nReaderToCardBufferLengthTCL );
         if ( p14P4Connection->pAllocatedReaderToCardBufferTCL == null )
         {
            PDebugError("P14Part4ExchangeData: pAllocatedReaderToCardBufferTCL == null");
            /* Send the error */
            nError = W_ERROR_OUT_OF_RESOURCE;
            goto send_error;
         }
      }

      /* Fix the PCB to I-block with block number equals to 0 */
      p14P4Connection->aReaderToCardBufferTCL[0] = (P_14443_4_PCB_I | p14P4Connection->nBlockNumber);

      /* Check if the CID is present */
      if ( bIsCIDSupported != W_FALSE )
      {
         p14P4Connection->aReaderToCardBufferTCL[0] |= P_14443_4_CID_PRESENT;
         p14P4Connection->aReaderToCardBufferTCL[1] = nCID;
         /* @note: the power level is not supported (00) */
      }
      /* Check if the NAD is present */
      if ( bIsNADSupported != W_FALSE )
      {
         p14P4Connection->aReaderToCardBufferTCL[0] |= P_14443_4_NAD_PRESENT;
         p14P4Connection->aReaderToCardBufferTCL[nPrologueLength - 1] = nNAD;
      }

      /* Set the connection information */
      p14P4Connection->pReaderToCardBuffer           = pReaderToCardBuffer;
      p14P4Connection->nReaderToCardBufferLength     = nReaderToCardBufferLength;
      p14P4Connection->nCardToReaderBufferMaxLength  = nCardToReaderBufferMaxLength;
      p14P4Connection->nError                        = P_ERROR_STILL_PENDING;
      p14P4Connection->nCommandType                  = P_14443_4_PCB_I;
      p14P4Connection->nResponseType                 = P_14443_4_PCB_NONE;
      p14P4Connection->nChainingIndex                = 0x00;
      p14P4Connection->nAttempt                      = 0x00;

      /* Create the response buffer */
      p14P4Connection->nCardToReaderBufferMaxLengthTCL =  nPrologueLength /* PCB */
                                                         +  nCardToReaderBufferMaxLength /* APDU */;

      /* Check if the response buffer is big enough */
      if ( p14P4Connection->nCardToReaderBufferMaxLengthTCL > P_14443_3_FRAME_MAX_SIZE )
      {
         /* Allocate a dynamic buffer */
         p14P4Connection->pAllocatedCardToReaderBufferTCL = (uint8_t*)CMemoryAlloc( p14P4Connection->nCardToReaderBufferMaxLengthTCL );
         if ( p14P4Connection->pAllocatedCardToReaderBufferTCL == null )
         {
            PDebugError("P14Part4ExchangeData: pAllocatedCardToReaderBufferTCL == null");
            /* Send the error */
            nError = W_ERROR_OUT_OF_RESOURCE;
            goto send_error;
         }
      }

      /* Check if the command does not need to be chained */
      if (p14P4Connection->bChainingState == P_14443_4_CHAINING_COMMAND_NONE)
      {
         /* Copy the APDU */
         CMemoryCopy(
            &p14P4Connection->aReaderToCardBufferTCL[nPrologueLength],
            pReaderToCardBuffer,
            nReaderToCardBufferLength );
      }
      else
      {
         /* Copy the APDU */
         p14P4Connection->aReaderToCardBufferTCL[0] |= P_14443_4_CHAINING_PRESENT;
         CMemoryCopy(
            &p14P4Connection->aReaderToCardBufferTCL[nPrologueLength],
            pReaderToCardBuffer,
            nCardInputBufferSize - nPrologueLength - 2 /* EDC */ - 1 /* Control Byte */);
      }

      /* Send the command asynchronously */
      P14P3UserExchangeData(
         pContext,
         hConnection,
         static_P14P4ExchangeDataCompleted,
         PUtilConvertHandleToPointer(hConnection),
         ( (p14P4Connection->pAllocatedReaderToCardBufferTCL != null ) ?
            p14P4Connection->pAllocatedReaderToCardBufferTCL :
            p14P4Connection->aReaderToCardBufferTCL ),
         p14P4Connection->nReaderToCardBufferLengthTCL,
         ( (p14P4Connection->pAllocatedCardToReaderBufferTCL != null ) ?
            p14P4Connection->pAllocatedCardToReaderBufferTCL :
            p14P4Connection->aCardToReaderBufferTCL ),
         p14P4Connection->nCardToReaderBufferMaxLengthTCL,
         phOperation,
         W_TRUE );
      return;
   }
   else
   {
      PDebugError("P14Part4ExchangeData: could not get p14P4Connection buffer");
      /* Send the error */
      PDFCPostContext3(
         &sCallbackContext,
         0,
         nError );
      return;
   }
send_error:
   /* Send the error */
   static_P14P4SendError(
               p14P4Connection,
               nError );
}

/* See Client API Specifications */
W_ERROR P14Part4ListenToCardDetectionTypeA(
                  tContext* pContext,
                  tPReaderCardDetectionHandler* pHandler,
                  void* pHandlerParameter,
                  uint8_t nPriority,
                  bool_t bUseCID,
                  uint8_t nCID,
                  uint32_t nBaudRate,
                  W_HANDLE* phEventRegistry)
{
   tP14P3DetectionConfigurationTypeA sDetectionConfiguration, *pDetectionConfigurationBuffer;
   uint32_t nDetectionConfigurationBufferLength;
   static const uint8_t nProtocol = W_PROP_ISO_14443_4_A;

   if ((nPriority == W_PRIORITY_SE) || (nPriority == W_PRIORITY_SE_FORCED))
   {
      return W_ERROR_BAD_PARAMETER;
   }

    /* The anti-collision parameters shall be zero if W_PRIORITY_EXCLUSIVE is not used*/
   if((bUseCID != W_FALSE)
    ||(nCID != 0x00)
    ||(nBaudRate != 0x00))
   {
      if(nPriority != W_PRIORITY_EXCLUSIVE)
      {
         PDebugError("P14Part4ListenToCardDetectionTypeA: Bad Detection configuration parameters or bad priorty type passed");
         return W_ERROR_BAD_PARAMETER;
      }
      else
      {
         /* set the pointer to configuration buffer and length*/
         pDetectionConfigurationBuffer = &sDetectionConfiguration;
         nDetectionConfigurationBufferLength = sizeof(tP14P3DetectionConfigurationTypeA);
         /* set the CID*/
         sDetectionConfiguration.bUseCID = bUseCID;
         sDetectionConfiguration.nCID = nCID;
         /* set the bit rate*/
         sDetectionConfiguration.nBaudRate = nBaudRate;
      }
   }
   else
   {
      pDetectionConfigurationBuffer = null;
      nDetectionConfigurationBufferLength = 0x00;
   }

   /* Launch the card detection request */
   return PReaderUserListenToCardDetection(
                     pContext,
                     pHandler,
                     pHandlerParameter,
                     nPriority,
                     &nProtocol, 1,
                     &sDetectionConfiguration, nDetectionConfigurationBufferLength,
                     phEventRegistry );
}

/* See Client API Specifications */
W_ERROR P14Part4GetConnectionInfo(
                  tContext* pContext,
                  W_HANDLE hConnection,
                  tW14Part4ConnectionInfo* p14Part4ConnectionInfo )
{
   tW14Part3ConnectionInfo p14Part3ConnectionInfo;
   tP14P4Connection* p14P4Connection;
   W_ERROR nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_14443_4_CONNECTION, (void**)&p14P4Connection);

   if ( nError == W_SUCCESS )
   {
      if (p14Part4ConnectionInfo == null)
      {
         return W_ERROR_BAD_PARAMETER;
      }

      CMemoryFill(p14Part4ConnectionInfo, 0, sizeof(tW14Part4ConnectionInfo));

      switch (p14P4Connection->nProtocol)
      {
         case W_PROP_ISO_14443_4_A:
            /* CID */
            p14Part4ConnectionInfo->sW14TypeA.bIsCIDSupported    = p14P4Connection->s14typeA4.bIsCIDSupported;
            p14Part4ConnectionInfo->sW14TypeA.nCID               = p14P4Connection->s14typeA4.nCID;
            /* NAD */
            p14Part4ConnectionInfo->sW14TypeA.bIsNADSupported    = p14P4Connection->s14typeA4.bIsNADSupported;
            if ( p14P4Connection->s14typeA4.bIsNADSupported != W_FALSE )
            {
               p14Part4ConnectionInfo->sW14TypeA.nNAD            = p14P4Connection->s14typeA4.nNAD;
            }
            else
            {
               /* Set to a default value */
               p14Part4ConnectionInfo->sW14TypeA.nNAD            = 0x00;
            }
             /* application data */
            p14Part4ConnectionInfo->sW14TypeA.nApplicationDataLength = p14P4Connection->s14typeA4.nApplicationDataLength;
            CMemoryCopy(
               p14Part4ConnectionInfo->sW14TypeA.aApplicationData,
               p14P4Connection->s14typeA4.aApplicationData,
               p14P4Connection->s14typeA4.nApplicationDataLength );
            /* reader Input buffer size */
            p14Part4ConnectionInfo->sW14TypeA.nReaderInputBufferSize  = p14P4Connection->s14typeA4.nReaderInputBufferSize;
            /* card Input buffer size */
            p14Part4ConnectionInfo->sW14TypeA.nCardInputBufferSize   = p14P4Connection->s14typeA4.nCardInputBufferSize;
            /* FWI_SFGI */
            p14Part4ConnectionInfo->sW14TypeA.nFWI_SFGI = p14P4Connection->s14typeA4.nFWI_SFGI;
            /* Timeout */
            p14Part4ConnectionInfo->sW14TypeA.nTimeout           = p14P4Connection->nTimeout;
            /*DATA_RATE_MAX */
            p14Part4ConnectionInfo->sW14TypeA.nDataRateMaxDiv    = p14P4Connection->s14typeA4.nDataRateMaxDiv;
             /* Baud rate */
            p14Part4ConnectionInfo->sW14TypeA.nBaudRate          = p14P4Connection->s14typeA4.nBaudRate;
         break;

         case W_PROP_ISO_14443_4_B:
            nError = P14Part3GetConnectionInfo(
                                          pContext,
                                          hConnection,
                                          &p14Part3ConnectionInfo);
            if(nError != W_SUCCESS)
            {
               return nError;
            }
            if ( p14Part3ConnectionInfo.sW14TypeB.bIsNADSupported != W_FALSE )
            {
               p14Part4ConnectionInfo->sW14TypeB.nNAD            = p14P4Connection->s14typeB4.nNAD;
            }
            else
            {
               /* Set to a default value */
               p14Part4ConnectionInfo->sW14TypeA.nNAD            = 0x00;
            }
         break;

         default:
            nError = W_ERROR_CONNECTION_COMPATIBILITY;
            PDebugError("P14Part4GetConnectionInfo: unknow protocol 0x%02X", p14P4Connection->nProtocol);
         break;
      }
   }
   else
   {
      if (p14Part4ConnectionInfo != null)
      {
         CMemoryFill(p14Part4ConnectionInfo, 0, sizeof(tW14Part4ConnectionInfo));
      }
   }

   return nError;
}
/**
 * @brief   Sends a command frame.
 *
 * @param[in]   pContext  The context.
 *
 * @param[in]   p14443Connection  The 14443-4 connection structure.
 *
 * @param[in]   hConnection  The connection handle.
 *
 * @param[in]   nCommandType  The command type.
 **/
static void static_P14P4SendCommand(
            tContext* pContext,
            tP14P4Connection* p14P4Connection,
            W_HANDLE hConnection,
            uint8_t nCommandType )
{
   static const uint8_t pSendRATS[] =  {  0xE0,       /* Fixed value */
                                          0x80 };     /* Maximum frame size at PCD level: 256 bytes */
                                                      /* the 4 least significant bits are the CID: default 0000 */
   static const uint8_t pSendPPS[] =   {  P_14443_PPS_START_BYTE,
                                          0x01 };     /* Do not send the PPS1 */
   /* Store the command type */
   p14P4Connection->nCommandType = nCommandType;


   /* Send the corresponding command */
   switch ( nCommandType )
   {
      case P_14443_COMMAND_SEND_RATS:
         /* Prepare the command */
         CMemoryCopy(
            p14P4Connection->aReaderToCardBufferTCL,
            pSendRATS,
            sizeof(pSendRATS) );

         /* set Maximum Reader Input frame size : 256 bytes */
          p14P4Connection->aReaderToCardBufferTCL[1] = 0x80;

         /* Send the command */
         P14P3UserExchangeData(
            pContext,
            hConnection,
            static_P14P4GetCardResponseCompleted,
            PUtilConvertHandleToPointer(hConnection),
            p14P4Connection->aReaderToCardBufferTCL,
            sizeof(pSendRATS),
            p14P4Connection->aCardToReaderBufferTCL,
            P_14443_3_FRAME_MAX_SIZE,
            null,
            W_TRUE);
      break;
      case P_14443_COMMAND_SEND_PPS:
         /* Prepare the command */
         CMemoryCopy(
             p14P4Connection->aReaderToCardBufferTCL,
            pSendPPS,
            sizeof(pSendPPS) );

         /* Check if the PPS is supported */
         if ( p14P4Connection->s14typeA4.bIsPPSSupported != W_FALSE )
         {
             p14P4Connection->aReaderToCardBufferTCL[1] |= 0x10;
             p14P4Connection->aReaderToCardBufferTCL[sizeof(pSendPPS)] = 0x00;
         }
          /* Send the command */
         P14P3UserExchangeData(
            pContext,
            hConnection,
            static_P14P4GetCardResponseCompleted,
            PUtilConvertHandleToPointer(hConnection),
            p14P4Connection->aReaderToCardBufferTCL,
            sizeof(pSendPPS) + ((p14P4Connection->s14typeA4.bIsPPSSupported != W_FALSE) ? 1 : 0),
            p14P4Connection->aCardToReaderBufferTCL,
            P_14443_3_FRAME_MAX_SIZE,
            null,
            W_TRUE);
         break;
   }
}

/**
 * @brief   Parses the Type A card response to a RATS command (ATS frame) .
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  p14P4Connection  The 14443-4  connection structure.
 *
 * @param[in]  hConnection  The connection handle.
 *
 * @param[in]  pBuffer  The response buffer.
 *
 * @param[in]  nLength  The length of the response buffer.
 **/
static W_ERROR static_P14P4ParseATS(
            tContext* pContext,
            tP14P4Connection* p14P4Connection,
            W_HANDLE hConnection,
            uint8_t* pBuffer,
            uint32_t nLength )
{
   uint8_t nOffset = 0;

   /* Check the length */
   if (pBuffer[0] > 1 )
   {
      /* Increase the offset: TO is present */
      nOffset ++;

      /* Check the format byte coherence */
      if ( pBuffer[1] & 0x80 )
      {
         return W_ERROR_RF_COMMUNICATION;
      }

      /* Maximum PICC buffer */
      switch ( pBuffer[1] & 0x0F )
      {
         case 0:
            p14P4Connection->s14typeA4.nCardInputBufferSize = 16;
            break;
         case 1:
            p14P4Connection->s14typeA4.nCardInputBufferSize = 24;
            break;
         case 2:
            p14P4Connection->s14typeA4.nCardInputBufferSize = 32;
            break;
         case 3:
            p14P4Connection->s14typeA4.nCardInputBufferSize = 40;
            break;
         case 4:
            p14P4Connection->s14typeA4.nCardInputBufferSize = 48;
            break;
         case 5:
            p14P4Connection->s14typeA4.nCardInputBufferSize = 64;
            break;
         case 6:
            p14P4Connection->s14typeA4.nCardInputBufferSize = 96;
            break;
         case 7:
            p14P4Connection->s14typeA4.nCardInputBufferSize = 128;
            break;
         case 8:
            p14P4Connection->s14typeA4.nCardInputBufferSize = 256;
            break;
         default:
            PDebugWarning(
               "static_P14P4ParseATS: wrong card input buffer size value %d",
               pBuffer[1] & 0x0F );
            /* A received value of FSDI = '9'-'F' should be interpreted as FSDI = '8' (FSD = 256 bytes). */
            p14P4Connection->s14typeA4.nCardInputBufferSize = 256;
            break;
      }
      PDebugTrace(
         "static_P14P4ParseATS: max card input buffer %d bytes",
         p14P4Connection->s14typeA4.nCardInputBufferSize );

      /* Check the format byte TA value */
      if ( pBuffer[1] & 0x10 )
      {
         /* Increase the offset */
         nOffset ++;

         if ( pBuffer[2] & 0x08 )
         {
            return W_ERROR_RF_COMMUNICATION;
         }

         /* Different divisors for each direction */
         if ( pBuffer[2] & 0x80 )
         {
            /* The PPS mode is not supported */
            p14P4Connection->s14typeA4.bIsPPSSupported = W_FALSE;
         }
         else
         {
            /* We need top manage the PPS mode */
            p14P4Connection->s14typeA4.bIsPPSSupported = W_TRUE;
         }

         /* Baud rate from PCD to PICC */
         if ( pBuffer[2] & 0x01 )
         {
            p14P4Connection->s14typeA4.nBaudRate = 212;
         }
         else
         {
            if ( pBuffer[9] & 0x02 )
            {
               p14P4Connection->s14typeA4.nBaudRate = 424;
            }
            else
            {
               if ( pBuffer[9] & 0x04 )
               {
                  p14P4Connection->s14typeA4.nBaudRate = 847;
               }
            }
         }
      }
      else
      {
         p14P4Connection->s14typeA4.nBaudRate = 106;
         p14P4Connection->s14typeA4.bIsPPSSupported = W_FALSE;
      }
      PDebugTrace(
         "static_P14P4ParseATS: baud rate %d kbit/s",
         p14P4Connection->s14typeA4.nBaudRate );
      if ( p14P4Connection->s14typeA4.bIsPPSSupported != W_FALSE )
      {
         PDebugTrace( "static_P14P4ParseATS: PPS mode supported" );
      }
      else
      {
         PDebugTrace( "static_P14P4ParseATS: PPS mode not supported" );
      }

      /* Check the format byte TB value */
      if ( pBuffer[1] & 0x60 )
      {
         /* Increase the offset */
         nOffset ++;

         /* Timeout */
         p14P4Connection->nTimeout = ( ( pBuffer[nOffset] >> 4 ) & 0x0F );
         if ( p14P4Connection->nTimeout > 14 )
         {
            PDebugWarning(
               "static_P14P4ParseATS: wrong timeout value %d",
               p14P4Connection->nTimeout );
            p14P4Connection->nTimeout = 14;
         }
         else
         {
            /* This limitation is removed */
            /*
            if ( p14P4Connection->nTimeout < 9 )
            {
               p14P4Connection->nTimeout = 9;
            }
            */
         }

         /* SFGI */
         /* nSFGI = p14P4Connection->aCardToReaderSpecialBuffer[nOffset] & 0x0F;*/
      }
      else
      {
         p14P4Connection->nTimeout = 14;
      }
      PDebugTrace(
         "static_P14P4ParseATS: timeout %d",
         p14P4Connection->nTimeout );
      /* Set the timeout at level 3*/
      P14Part3SetTimeout(
         pContext,
         hConnection,
         p14P4Connection->nTimeout );

      /* Check the format byte TC value */
      if ( pBuffer[1] & 0x40 )
      {
         /* Increase the offset */
         nOffset ++;

         if ( pBuffer[nOffset] & 0xFC )
         {
            return W_ERROR_RF_COMMUNICATION;
         }

         /* CID */
         p14P4Connection->s14typeA4.bIsCIDSupported = ( ( pBuffer[nOffset] & 0x02 ) == 0x02 ) ? W_TRUE: W_FALSE;
         /* Set the default CID to 0 */
         p14P4Connection->s14typeA4.nCID = 0x00;
         /* NAD */
         p14P4Connection->s14typeA4.bIsNADSupported = ( ( pBuffer[nOffset] & 0x01 ) == 0x01 ) ? W_TRUE: W_FALSE;
      }
      else
      {
         p14P4Connection->s14typeA4.bIsCIDSupported = W_FALSE;
         p14P4Connection->s14typeA4.nCID = 0x00;
         p14P4Connection->s14typeA4.bIsNADSupported = W_FALSE;
      }
      if ( p14P4Connection->s14typeA4.bIsCIDSupported != W_FALSE )
      {
         PDebugTrace( "static_P14P4ParseATS: CID supported" );
      }
      else
      {
         PDebugTrace( "static_P14P4ParseATS: CID not supported" );
      }
      if ( p14P4Connection->s14typeA4.bIsNADSupported != W_FALSE )
      {
         PDebugTrace( "static_P14P4ParseATS: NAD supported" );
      }
      else
      {
         PDebugTrace( "static_P14P4ParseATS: NAD not supported" );
      }
      /*store application data*/
      p14P4Connection->s14typeA4.nApplicationDataLength = pBuffer[0] - nOffset - 1;
      if((p14P4Connection->s14typeA4.nApplicationDataLength > 0)
       &&(p14P4Connection->s14typeA4.nApplicationDataLength <= W_EMUL_APPLICATION_DATA_MAX_LENGTH))
      {
         CMemoryCopy(
            p14P4Connection->s14typeA4.aApplicationData,
            &pBuffer[nOffset],
            p14P4Connection->s14typeA4.nApplicationDataLength );
      }
   }

   return W_SUCCESS;
}

/* See tWBasicGenericDataCallbackFunction */
static void static_P14P4GetCardResponseCompleted(
            tContext* pContext,
            void *pCallbackParameter,
            uint32_t nDataLength,
            W_ERROR nError )
{
   W_HANDLE hConnection;
   tP14P4Connection* p14P4Connection  = (tP14P4Connection*)PReaderUserConvertPointerToConnection(
      pContext, pCallbackParameter, P_HANDLE_TYPE_14443_4_CONNECTION, &hConnection);

   uint8_t* pCardToReaderBuffer = p14P4Connection->aCardToReaderBufferTCL;

   /* Check the error code */
   if ( nError != W_SUCCESS )
   {
      PDebugError(
         "static_P14P4GetCardResponseCompleted: nError 0x%02X",
         nError );
      static_P14P4SendError(
         p14P4Connection,
         nError );
      return;
   }

   /* Send the corresponding command */
   switch ( p14P4Connection->nCommandType )
   {
      case P_14443_COMMAND_SEND_RATS:
         /* Parse the card answer */
         if ( ( nError = static_P14P4ParseATS(
                           pContext,
                           p14P4Connection,
                           hConnection,
                           pCardToReaderBuffer,
                           nDataLength ) ) != W_SUCCESS )
         {
            PDebugError(
               "static_P14P4GetCardResponseCompleted: wrong response to RATS 0x%08X",
               nError );
            static_P14P4SendError(
               p14P4Connection,
               nError );
         }
         else
         {
            /* Check if the PPS is supported */
            if (p14P4Connection->s14typeA4.bIsPPSSupported != W_FALSE )
            {
               /* Send the PPS */
               static_P14P4SendCommand(
                  pContext,
                  p14P4Connection,
                  hConnection,
                  P_14443_COMMAND_SEND_PPS );
            }
            else
            {
               static_P14P4SendError(
                  p14P4Connection,
                  W_SUCCESS );
            }
         }
         break;
      case P_14443_COMMAND_SEND_PPS:
         /* Check the start byte */
         if ( pCardToReaderBuffer[0] != P_14443_PPS_START_BYTE )
         {
            PDebugError(
               "static_P14P4GetCardResponseCompleted: wrong response to PPS 0x%02X",
               pCardToReaderBuffer[0] );
            static_P14P4SendError(
               p14P4Connection,
               W_ERROR_RF_COMMUNICATION );
         }
         else
         {
            static_P14P4SendError(
               p14P4Connection,
               W_SUCCESS );
         }
         break;
   }
}

/* See Client API Specifications */
W_ERROR P14Part4SetNAD(
            tContext* pContext,
            W_HANDLE hConnection,
            uint8_t nNAD )
{
   tW14Part3ConnectionInfo p14Part3ConnectionInfo;
   tP14P4Connection* p14P4Connection;
   W_ERROR nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_14443_4_CONNECTION, (void**)&p14P4Connection);

   if ( nError == W_SUCCESS )
   {
      switch(p14P4Connection->nProtocol)
      {
         case W_PROP_ISO_14443_4_A:
            if ( p14P4Connection->s14typeA4.bIsNADSupported != W_FALSE )
            {
               p14P4Connection->s14typeA4.nNAD = nNAD;
            }
         break;
         case W_PROP_ISO_14443_4_B:
            nError = P14Part3GetConnectionInfo(
                                          pContext,
                                          hConnection,
                                          &p14Part3ConnectionInfo);
            if(nError != W_SUCCESS)
            {
               return nError;
            }
            if ( p14Part3ConnectionInfo.sW14TypeB.bIsNADSupported != W_FALSE )
            {
               p14P4Connection->s14typeB4.nNAD = nNAD;
            }
         break;
         default:
         nError = W_ERROR_CONNECTION_COMPATIBILITY;
         PDebugError("P14Part4SetNAD: unknow protocol 0x%02X", p14P4Connection->nProtocol);
         break;
      }
   }

   return nError;
}

/* See Header file */
W_ERROR P14P4UserCheckMifare(
            tContext* pContext,
            W_HANDLE hConnection,
            uint8_t* pUID,
            uint8_t* pnUIDLength,
            uint8_t* pnType )

{
   return P14P3UserCheckMifare(pContext, hConnection, pUID, pnUIDLength, pnType);
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */

#endif /* P_READER_14P4_STANDALONE_SUPPORT */
