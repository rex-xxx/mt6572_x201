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
   Contains the implementation of the Virtual Tag functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( VTAG )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

#define W_NDEF_ACTION_BITMASK                     0x1F

/* Generic defines */
#define P_VIRTUAL_TAG_STATE_STOP                0x00
#define P_VIRTUAL_TAG_STATE_START_PENDING       0x01
#define P_VIRTUAL_TAG_STATE_ACTIVE              0x02
#define P_VIRTUAL_TAG_STATE_STOP_PENDING        0x03
#define P_VIRTUAL_TAG_STATE_AID_SELECTED        0x04
#define P_VIRTUAL_TAG_STATE_CC_SELECTED         0x05
#define P_VIRTUAL_TAG_STATE_NDEF_SELECTED       0x06


/*

 NFC HAL commands are 263 bytes maximum
 NFC HAL event command from reader
 Write binary command on 260 bytes max (only case 3S is supported)

 <----- 263 ------------------------------------------------------------->
 [svc]  [evt] [ APDU Command 260                              ] [RF error]
              [CLA] [INS] [P1] [P2] [Lc max 255] [data 255 max]


 NFC HAL event response to reader

 <----- 263 ------------------------------------------------------------->
 [svc]  [evt] [ APDU Command 261                                         ]
              [ Data 259                                     ] [SW1] [SW2]

*/

/* Maximum buffer size */
#define P_VIRTUAL_TAG_MAX_MESSAGE_SIZE      261

/* Maximum read "Le" value */
#define P_VIRTUAL_TAG_MAX_READ_LE_VALUE     259

/* Maximum read "Lc" value */
#define P_VIRTUAL_TAG_MAX_WRITE_LC_VALUE    255

static const uint8_t g_aCCFile[] = { 0x00, 0x0F,    /*   0 - Size of this file */
                                     0x10,          /*   2 - Version of the format */
                                     0x00, 0x00,    /*   3 - Maximum Le value */
                                     0x00, 0x00,    /*   5 - Maximum Lc value */
                                     0x04, 0x06,    /*   7 - NDEF TLV */
                                     0x00, 0x01,    /*   9 - File identifier */
                                     0x00, 0x00,    /*  11 - maximum file size */
                                     0x00,          /*  13 - Read access : read granted */
                                     0x00,          /*  14 - Write access : write granted */
                                     };
/* The size of the CC file */
#define P_VIRTUAL_TAG_CC_FILE_LENGTH  sizeof(g_aCCFile)

/* The maximum length of the messages stored in the virtual tag */
#define P_VIRTUAL_TAG_MAXIMUM_NDEF_MESSAGE   0x80FC

/* Reader actions */
#define  P_VIRTUAL_TAG_ACTION_NONE        0
#define  P_VIRTUAL_TAG_ACTION_READ_ONLY   1
#define  P_VIRTUAL_TAG_ACTION_READ_WRITE  2

/* Declare a Virtual Tag structure */
typedef struct __tVirtualTag
{
   /* Operation header */
   tHandleObjectHeader  sObjectHeader;
   /* Emulation connection handle */
   W_HANDLE  hCardEmulation;

   /* Connection information */
   uint8_t  nTagType;
   uint8_t  nVariant;
   uint8_t  nState;
   uint8_t  nReaderAction;
   uint8_t  nIdentifierLength;
   uint8_t  aIdentifier[10];

   /* NDEF file information */
   uint32_t  nMaximumMessageLength;
   bool_t  bIsLocked;
   uint8_t*  pNDEFFile;
   uint8_t  aCCFile[P_VIRTUAL_TAG_CC_FILE_LENGTH];

   /* Message buffer */
   uint8_t  aMessagerBuffer[P_VIRTUAL_TAG_MAX_MESSAGE_SIZE];

   /* Callback contexts */
   tDFCCallbackContext  sStartCallbackContext;
   tDFCCallbackContext  sStopCallbackContext;
   tDFCCallbackContext  sEventCallbackContext;

} tVirtualTag;

/* Destroy connection callback */
static uint32_t static_PVirtualTagDestroyConnection(
            tContext* pContext,
            void* pObject );

/* Get properties connection callback */
static uint32_t static_PVirtualTagGetPropertyNumber(
            tContext* pContext,
            void* pObject)
{
   return 1;
}

/* Get properties connection callback */
static bool_t static_PVirtualTagGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray );

/* Check properties connection callback */
static bool_t static_PVirtualTagCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue );

/* Handle of the virtual tag connection type */
tHandleType g_sVirtualTagConnection = {  static_PVirtualTagDestroyConnection,
                                    null,
                                    static_PVirtualTagGetPropertyNumber,
                                    static_PVirtualTagGetProperties,
                                    static_PVirtualTagCheckProperties,
                                    null, null, null, null };

#define P_HANDLE_TYPE_VIRTUAL_TAG_CONNECTION (&g_sVirtualTagConnection)

/* The emptiy NDEF file */
static const uint8_t g_aNDEFEmptyFile[] = { 0x00, 0x03, 0xD0, 0x00, 0x00 };

static uint32_t static_PVirtualRead16(const uint8_t* pBuffer)
{
   return ( ( ((uint32_t)(pBuffer[0])) << 8 ) & 0xFF00 ) | (( pBuffer[1] ) & 0x00FF);
}

static void static_PVirtualWrite16(uint8_t* pBuffer, uint32_t nValue)
{
   pBuffer[0] = (uint8_t)((nValue >> 8) & 0x00FF);
   pBuffer[1] = (uint8_t)(nValue & 0x00FF);
}

/**
 * @brief   Destroyes a virtual tag connection object.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object to destroy.
 **/
static uint32_t static_PVirtualTagDestroyConnection(
            tContext* pContext,
            void* pObject )
{
   tVirtualTag* pVirtualTag = (tVirtualTag*)pObject;

   PDebugTrace("static_PVirtualTagDestroyConnection");

   PDFCFlushCall(&pVirtualTag->sStartCallbackContext);
   PDFCFlushCall(&pVirtualTag->sEventCallbackContext);
   PDFCFlushCall(&pVirtualTag->sStopCallbackContext);

   PHandleClose(
      pContext,
      pVirtualTag->hCardEmulation );

   CMemoryFree( pVirtualTag->pNDEFFile );

   CMemoryFree( pVirtualTag );

   return P_HANDLE_DESTROY_DONE;
}

/**
 * @brief   Gets the virtual tag connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  pPropertyArray  The property array.
 **/
static bool_t static_PVirtualTagGetProperties(
            tContext* pContext,
            void* pObject,
            uint8_t* pPropertyArray )
{
   PDebugTrace("static_PVirtualTagGetProperties");

   pPropertyArray[0] = W_PROP_VIRTUAL_TAG;
   return W_TRUE;
}

/**
 * @brief   Checkes the virtual tag connection properties.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pObject  The object.
 *
 * @param[in]  nPropertyValue  The property value.
 **/
static bool_t static_PVirtualTagCheckProperties(
            tContext* pContext,
            void* pObject,
            uint8_t nPropertyValue )
{
   PDebugTrace(
      "static_PVirtualTagCheckProperties: nPropertyValue=%s (0x%02X)",
      PUtilTraceConnectionProperty(nPropertyValue), nPropertyValue  );

   return ( nPropertyValue == W_PROP_VIRTUAL_TAG )?W_TRUE:W_FALSE;
}

/* APDU status value */
#define P_APDU_STATUS_SUCCESS          0x9000
#define P_APDU_STATUS_EXECUTION_ERROR  0x6400
#define P_APDU_STATUS_MEMORY_FAILURE   0x6581
#define P_APDU_STATUS_WRONG_LENGTH     0x6700
#define P_APDU_STATUS_COMMAND_UNKNOWN  0x6800
#define P_APDU_STATUS_EF_READ_ONLY     0x6900
#define P_APDU_STATUS_TAG_LOCKED       0x6981
#define P_APDU_STATUS_BAD_STATE        0x6985
#define P_APDU_STATUS_NO_EF_SELECTED   0x6986
#define P_APDU_STATUS_FILE_NOT_FOUND   0x6A82
#define P_APDU_STATUS_BAD_PARAMETER    0x6B00

/**
 * @brief   Called when a connection is opened.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter  The callback parameter.
 *
 * @param[in]  nCommandLength  The command buffer length.
 **/
static void static_PVirtualTagCommandReceived(
                  tContext* pContext,
                  void * pCallbackParameter,
                  uint32_t nCommandLength )
{
   static const uint8_t aType4AID[]        = { 0xD2, 0x76, 0x00, 0x00, 0x85, 0x01, 0x00 };
   static const uint8_t aType4AIDV20[]     = { 0xD2, 0x76, 0x00, 0x00, 0x85, 0x01, 0x01 };
   static const uint8_t aType4AIDV20_Le[]  = { 0xD2, 0x76, 0x00, 0x00, 0x85, 0x01, 0x01, 0x00 };  /* including optional LE */
   static const uint8_t aType4CCID[]   = { 0xE1, 0x03 };
   tVirtualTag* pVirtualTag = (tVirtualTag*)pCallbackParameter;
   uint32_t nAnswerLength = 0;
   uint32_t nOffset;
   uint32_t nLength;
   uint8_t* pCommandBuffer = pVirtualTag->aMessagerBuffer;
   uint32_t nAPDUStatus = P_APDU_STATUS_SUCCESS;
   W_ERROR nError;

   /* Check the parameters */
   if ( (nCommandLength < 5) || (nCommandLength >= P_VIRTUAL_TAG_MAX_MESSAGE_SIZE) )
   {
      PDebugError("static_PVirtualTagCommandReceived: wrong command length");
      nAPDUStatus = P_APDU_STATUS_COMMAND_UNKNOWN;
      goto return_status;
   }

   if((pVirtualTag->nState != P_VIRTUAL_TAG_STATE_ACTIVE)
   && (pVirtualTag->nState != P_VIRTUAL_TAG_STATE_AID_SELECTED)
   && (pVirtualTag->nState != P_VIRTUAL_TAG_STATE_CC_SELECTED)
   && (pVirtualTag->nState != P_VIRTUAL_TAG_STATE_NDEF_SELECTED))
   {
      PDebugError("static_PVirtualTagCommandReceived: bad state");
      nAPDUStatus = P_APDU_STATUS_BAD_STATE;
      goto return_status;   }

   if ( PEmulGetMessageData(
               pContext,
               pVirtualTag->hCardEmulation,
               pCommandBuffer,
               nCommandLength,
               &nLength ) != W_SUCCESS )
   {
      PDebugError("static_PVirtualTagCommandReceived: PEmulGetMessageData error");
      nAPDUStatus = P_APDU_STATUS_EXECUTION_ERROR;
      goto return_status;
   }

   CDebugAssert(nCommandLength == nLength);
   nCommandLength = nLength;

   /* Check the type of APDU */
   /* Select */
   if (  ( pCommandBuffer[0] == P_7816SM_CLA )
      && ( pCommandBuffer[1] == P_7816SM_INS_SELECT )
      && (  ( pCommandBuffer[3] == P_7816SM_P2_SELECT_AID )
         || ( pCommandBuffer[3] == P_7816SM_P2_SELECT_FILE_NO_RESPONSE_DATA )
         || ( pCommandBuffer[3] == P_7816SM_P2_SELECT_FILE_WITH_FCI)))
   {
      switch ( pCommandBuffer[2] )
      {
         /* AID */
         case P_7816SM_P1_SELECT_AID:

            if (nCommandLength == (uint32_t) (5 + pCommandBuffer[4]))
            {
               /* Select AID without Le */

               /* Check with NDEF TypE 4 v 1.0 */
               if (pCommandBuffer[4] == sizeof(aType4AID))
               {
                  if ( CMemoryCompare( &pCommandBuffer[5], aType4AID, sizeof(aType4AID) ) == 0 )
                  {
                     PDebugError("static_PVirtualTagEmulExchangeDataReceived: NDEF v10 application selected");
                     pVirtualTag->nVariant = P_NDEF_4_VERSION_10;
                     pVirtualTag->nState = P_VIRTUAL_TAG_STATE_AID_SELECTED;

                     /* update the version of the tag in the NDEF file */
                     pVirtualTag->aCCFile[2] = 0x10;
                     goto return_status;
                  }
               }

               /* Check with NDEF TypE 4 v 2.0 */

               if (pCommandBuffer[4] == sizeof(aType4AIDV20))
               {
                  if ( CMemoryCompare( &pCommandBuffer[5], aType4AIDV20, sizeof(aType4AIDV20) ) == 0 )
                  {
                     PDebugError("static_PVirtualTagEmulExchangeDataReceived: NDEF v20 application selected");
                     pVirtualTag->nVariant = P_NDEF_4_VERSION_20;
                     pVirtualTag->nState = P_VIRTUAL_TAG_STATE_AID_SELECTED;

                     /* update the version of the tag in the NDEF file */
                     pVirtualTag->aCCFile[2] = 0x20;
                     goto return_status;
                  }
               }
            } else if (nCommandLength == (uint32_t) (5 + pCommandBuffer[4] + 1)) {

               /* Select AID with Le */
               if (pCommandBuffer[4] == sizeof(aType4AIDV20_Le) - 1)
               {
                  if ( CMemoryCompare( &pCommandBuffer[5], aType4AIDV20_Le, sizeof(aType4AIDV20_Le) ) == 0 )
                  {
                     PDebugError("static_PVirtualTagEmulExchangeDataReceived: NDEF v20 application selected");
                     pVirtualTag->nVariant = P_NDEF_4_VERSION_20;
                     pVirtualTag->nState = P_VIRTUAL_TAG_STATE_AID_SELECTED;

                     /* update the version of the tag in the NDEF file */
                     pVirtualTag->aCCFile[2] = 0x20;
                     goto return_status;
                  }
               }
            }
            else
            {
               PDebugError("static_PVirtualTagCommandReceived: wrong select AID length");
            }

            pVirtualTag->nState = P_VIRTUAL_TAG_STATE_ACTIVE;
            nAPDUStatus = P_APDU_STATUS_FILE_NOT_FOUND;
            break;

         /* DF/EF */
         case P_7816SM_P1_SELECT_FILE:
            /* Check the current state */
            if ( pVirtualTag->nState == P_VIRTUAL_TAG_STATE_ACTIVE )
            {
               PDebugError("static_PVirtualTagCommandReceived: AID not selected");
               nAPDUStatus = P_APDU_STATUS_FILE_NOT_FOUND;
            }
            else
            {
               if ( (nCommandLength != 7) || (pCommandBuffer[4] != sizeof(aType4CCID)) )
               {
                  PDebugTrace("static_PVirtualTagCommandReceived: wrong length");
                  nAPDUStatus = P_APDU_STATUS_WRONG_LENGTH;
               }
               else
               {
                  if ( CMemoryCompare( &pCommandBuffer[5], aType4CCID, sizeof(aType4CCID) ) == 0 )
                  {
                     PDebugTrace("static_PVirtualTagCommandReceived: CC file selected");
                     pVirtualTag->nState = P_VIRTUAL_TAG_STATE_CC_SELECTED;
                     goto return_status;
                  }
                  else
                  {
                     if ( CMemoryCompare( &pCommandBuffer[5], &pVirtualTag->aCCFile[9], sizeof(aType4CCID) ) == 0 )
                     {
                        PDebugTrace("static_PVirtualTagCommandReceived: NDEF file selected");
                        pVirtualTag->nState = P_VIRTUAL_TAG_STATE_NDEF_SELECTED;
                        goto return_status;
                     }
                  }
                  nAPDUStatus = P_APDU_STATUS_FILE_NOT_FOUND;
               }
            }
            break;
         default:
            /* Wrong parameter */
            nAPDUStatus = P_APDU_STATUS_BAD_PARAMETER;
            break;
      }
      goto return_status;
   }
   /* Read or Write */
   else if (  ( pCommandBuffer[0] == P_7816SM_CLA )
      && (( pCommandBuffer[1] == P_7816SM_INS_READ_BINARY ) || ( pCommandBuffer[1] == P_7816SM_INS_UPDATE_BINARY )))
   {
      if ( pVirtualTag->nState == P_VIRTUAL_TAG_STATE_ACTIVE )
      {
         PDebugError("static_PVirtualTagCommandReceived: read/write command but AID not selected");
         nAPDUStatus = P_APDU_STATUS_FILE_NOT_FOUND;
         goto return_status;
      }

      if((pCommandBuffer[2] & 0x80) != 0)  /* If b8=1 in P1 */
      {
         uint32_t nFileIdentifier = pCommandBuffer[2] & 0x1F;
         if(nFileIdentifier != static_PVirtualRead16(&pVirtualTag->aCCFile[9]))
         {
            nAPDUStatus = P_APDU_STATUS_FILE_NOT_FOUND;
            goto return_status;
         }
         pVirtualTag->nState = P_VIRTUAL_TAG_STATE_NDEF_SELECTED;
         nOffset = pCommandBuffer[3];
      }
      else
      {
         nOffset = static_PVirtualRead16(&pCommandBuffer[2]);
      }

      if ( pVirtualTag->nState == P_VIRTUAL_TAG_STATE_AID_SELECTED )
      {
         PDebugError("static_PVirtualTagCommandReceived: read/write command but no EF selected");
         nAPDUStatus = P_APDU_STATUS_NO_EF_SELECTED;
         goto return_status;
      }

      /* Get the length */
      if((nCommandLength == 5) || ( pCommandBuffer[1] == P_7816SM_INS_UPDATE_BINARY ))
      {
         nLength = pCommandBuffer[4];
         if(nLength == 0)
         {
            nLength = 0x0100;
         }
      }
      else if(nCommandLength == 7)
      {
         if(pCommandBuffer[4] == 0)
         {
            nLength = static_PVirtualRead16(&pCommandBuffer[5]);
            if(nLength == 0)
            {
               nLength = 0x00010000;
            }
         }
         else
         {
            nAPDUStatus = P_APDU_STATUS_BAD_PARAMETER;
            goto return_status;
         }
      }
      else
      {
         nAPDUStatus = P_APDU_STATUS_BAD_PARAMETER;
         goto return_status;
      }

      if ( pVirtualTag->nState == P_VIRTUAL_TAG_STATE_CC_SELECTED)
      {
         /* Check the offset */
         if ( nOffset < P_VIRTUAL_TAG_CC_FILE_LENGTH )
         {
            if ( ( nOffset + nLength ) <= P_VIRTUAL_TAG_CC_FILE_LENGTH )
            {
               goto continue_operation;
            }
            else
            {
               nAPDUStatus = P_APDU_STATUS_WRONG_LENGTH;
            }
         }
         else
         {
            /* Wrong offset */
            nAPDUStatus = P_APDU_STATUS_BAD_PARAMETER;
         }
      }
      else
      {
         /* Check the offset */
         if ( nOffset < (pVirtualTag->nMaximumMessageLength + 2) )
         {
            if ( ( nOffset + nLength ) <= (pVirtualTag->nMaximumMessageLength + 2) )
            {
               goto continue_operation;
            }
            else
            {
               nAPDUStatus = P_APDU_STATUS_WRONG_LENGTH;
            }
         }
         else
         {
            /* Wrong offset */
            nAPDUStatus = P_APDU_STATUS_BAD_PARAMETER;
         }
      }
      goto return_status;
   }

   /* Unsupported command */
   PDebugError("static_PVirtualTagCommandReceived: unknown command");
   nAPDUStatus = P_APDU_STATUS_COMMAND_UNKNOWN;
   goto return_status;

continue_operation:

   /* Read */
   if (  pCommandBuffer[1] == P_7816SM_INS_READ_BINARY )
   {
      if( nLength <= P_VIRTUAL_TAG_MAX_READ_LE_VALUE)
      {
         if ( pVirtualTag->nState == P_VIRTUAL_TAG_STATE_CC_SELECTED)
         {
            CMemoryCopy(
               pVirtualTag->aMessagerBuffer,
               &pVirtualTag->aCCFile[nOffset],
               nLength );
         }
         else
         {
            CMemoryCopy(
               pVirtualTag->aMessagerBuffer,
               &pVirtualTag->pNDEFFile[nOffset],
               nLength );

            if(pVirtualTag->nReaderAction == P_VIRTUAL_TAG_ACTION_NONE)
            {
               pVirtualTag->nReaderAction = P_VIRTUAL_TAG_ACTION_READ_ONLY;
            }
         }
         nAnswerLength = nLength;
      }
      else
      {
         nAPDUStatus = P_APDU_STATUS_WRONG_LENGTH;
      }
   }
   else /* Write */
   {
      if( nLength <= P_VIRTUAL_TAG_MAX_WRITE_LC_VALUE)
      {
         if ( pVirtualTag->nState == P_VIRTUAL_TAG_STATE_CC_SELECTED)
         {
            PDebugError("static_PVirtualTagCommandReceived: cannot write in CC file");
            nAPDUStatus = P_APDU_STATUS_EF_READ_ONLY;
         }
         else
         {
            if ( pVirtualTag->bIsLocked == W_FALSE )
            {
               CMemoryCopy(
                  &pVirtualTag->pNDEFFile[nOffset],
                  &pCommandBuffer[5],
                  nLength );

               /* Remember the write operation */
               pVirtualTag->nReaderAction = P_VIRTUAL_TAG_ACTION_READ_WRITE;
            }
            else
            {
               PDebugError("static_PVirtualTagCommandReceived: cannot write, tag locked");
               nAPDUStatus = P_APDU_STATUS_TAG_LOCKED;
            }
         }
      }
      else
      {
         nAPDUStatus = P_APDU_STATUS_WRONG_LENGTH;
      }
   }

return_status:

   static_PVirtualWrite16(&pVirtualTag->aMessagerBuffer[nAnswerLength], nAPDUStatus);

   /* Send the command */
   nError = PEmulSendAnswer(
      pContext,
      pVirtualTag->hCardEmulation,
      pVirtualTag->aMessagerBuffer,
      nAnswerLength + 2 );

   if (nError != W_SUCCESS)
   {
      PDebugError("static_PVirtualTagCommandReceived : PEmulSendAnswer() failed %d\n", nError);

      /* @todo what can we do here if the IOCTL failed */
   }

}

/**
 * @brief   Called when a card emulation is opened.
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pCallbackParameter  The callback parameter.
 *
 * @param[in]  nError  The error code.
 **/
static void static_PVirtualTagCardEmulationOpenCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  W_ERROR nError )
{
   tVirtualTag* pVirtualTag = (tVirtualTag*)pCallbackParameter;

   if((pVirtualTag->nState != P_VIRTUAL_TAG_STATE_START_PENDING)
   && (pVirtualTag->nState != P_VIRTUAL_TAG_STATE_STOP_PENDING))
   {
      PDebugWarning("static_PVirtualTagCardEmulationOpenCompleted: wrong state");
      return;
   }

   if ( nError == W_SUCCESS )
   {
      /* Set the connection information */
      pVirtualTag->nState = P_VIRTUAL_TAG_STATE_ACTIVE;
      pVirtualTag->nReaderAction = P_VIRTUAL_TAG_ACTION_NONE;
   }
   else
   {
      pVirtualTag->nState = P_VIRTUAL_TAG_STATE_STOP;
      PDebugError(
         "static_PVirtualTagCardEmulationOpenCompleted: returning error %s", PUtilTraceError(nError));
   }

   PDFCPostContext2( &pVirtualTag->sStartCallbackContext, nError );
}

/* See header file */
W_ERROR PVirtualTagReadMessage(
            tContext* pContext,
            W_HANDLE hConnection,
            W_HANDLE* phMessage,
            uint8_t nTNF,
            const char16_t* pTypeString )
{
   tVirtualTag* pVirtualTag;
   uint32_t nMessageLength;
   W_ERROR nError;
   char16_t aIdentifierString[256];

   aIdentifierString[0] = '\0';

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_VIRTUAL_TAG_CONNECTION, (void**)&pVirtualTag);
   if ( nError == W_SUCCESS )
   {
      /* Check the read parameters */
      if ( ( nError = PNDEFCheckReadParameters(
                        pContext,
                        nTNF,
                        pTypeString ) ) != W_SUCCESS )
      {
         goto return_error;
      }

      if(pVirtualTag->nState != P_VIRTUAL_TAG_STATE_STOP)
      {
         PDebugWarning("PVirtualTagReadMessage: Wrong state");
         nError = W_ERROR_BAD_STATE;
         goto return_error;
      }

      /* Check the NDEF message size */
      nMessageLength = static_PVirtualRead16( pVirtualTag->pNDEFFile );
      if ( (nMessageLength < 3) || (nMessageLength > pVirtualTag->nMaximumMessageLength))
      {
         nError = W_ERROR_ITEM_NOT_FOUND;
         goto return_error;
      }

      /* Parse the information */
      nError = PNDEFParseFile(
                  pContext,
                  nTNF,
                  pTypeString,
                  aIdentifierString,
                  &pVirtualTag->pNDEFFile[2],
                  nMessageLength,
                  phMessage );
      if ( nError != W_SUCCESS )
      {
         goto return_error;
      }

      return W_SUCCESS;
   }

return_error:

   PDebugError("PVirtualTagReadMessage: returning %s", PUtilTraceError(nError));

   return nError;
}

#define W_NDEF_ACTION_BIT_FORMAK_MASK  (W_NDEF_ACTION_BIT_FORMAT_ALL | W_NDEF_ACTION_BIT_FORMAT_BLANK_TAG | W_NDEF_ACTION_BIT_FORMAT_NON_NDEF_TAG)

/* see header */
W_ERROR PVirtualTagWriteMessage(
            tContext* pContext,
            W_HANDLE hConnection,
            W_HANDLE hMessage,
            uint32_t nActionMask )
{
   tVirtualTag* pVirtualTag = null;
   uint32_t nCurrentMessageLength;
   uint32_t nNewMessageLength;
   W_ERROR nError;

   nError = PReaderUserGetConnectionObject(pContext, hConnection, P_HANDLE_TYPE_VIRTUAL_TAG_CONNECTION, (void**)&pVirtualTag);

   if ( nError != W_SUCCESS )
   {
      PDebugError("PVirtualTagWriteMessage: PReaderUserGetConnectionObject returned  %s", PUtilTraceError(nError));
      return (nError);
   }

   if ((nActionMask & W_NDEF_ACTION_BITMASK) != nActionMask)
   {
      PDebugError("PVirtualTagWriteMessage : invalid nActionMask value");
      return (W_ERROR_BAD_PARAMETER);
   }

   if (nActionMask & W_NDEF_ACTION_BIT_FORMAK_MASK)
   {
      PDebugWarning("PVirtualTagWriteMessage : format option is not supported on a virtual tag... ignore it");
      nActionMask &= ~W_NDEF_ACTION_BIT_FORMAK_MASK;
   }

   if (nActionMask & W_NDEF_ACTION_BIT_LOCK)
   {
      PDebugWarning("PVirtualTagWriteMessage : lock option is not supported on a virtual tag... ignore it");
      nActionMask &= ~W_NDEF_ACTION_BIT_LOCK;
   }

   if(pVirtualTag->nState != P_VIRTUAL_TAG_STATE_STOP)
   {
      PDebugWarning("PVirtualTagWriteMessage: Wrong state");
      return (W_ERROR_BAD_STATE);
   }

   if (hMessage != W_NULL_HANDLE)
   {
      if ( (nNewMessageLength = PNDEFGetMessageLength( pContext, hMessage )) == 0 )
      {
         PDebugError("PVirtualTagWriteMessage: unknown hMessage");
         return (W_ERROR_BAD_HANDLE);
      }
   }
   else
   {
      nNewMessageLength = 0;
   }

   if (nActionMask & W_NDEF_ACTION_BIT_ERASE)
   {
      nCurrentMessageLength = 0;
   }
   else
   {
      nCurrentMessageLength = static_PVirtualRead16( pVirtualTag->pNDEFFile );
   }

   if ( nCurrentMessageLength + nNewMessageLength > pVirtualTag->nMaximumMessageLength )
   {
      PDebugError("PVirtualTagWriteMessage: The tag is full");
      return (W_ERROR_TAG_FULL);
   }

   if (hMessage != W_NULL_HANDLE)
   {
      /* Copy the message in the */
      if ( (nError = PNDEFGetMessageContent(
                        pContext,
                        hMessage,
                        &pVirtualTag->pNDEFFile[nCurrentMessageLength + 2],   /* skip 2 bytes for the length */
                        nNewMessageLength,
                        &nNewMessageLength )) != W_SUCCESS )
      {
         PDebugError("PVirtualTagWriteMessage : unable to get the message contents");
         return (nError);
      }
   }

   /* write the new NDEF message length */
   static_PVirtualWrite16(pVirtualTag->pNDEFFile, nCurrentMessageLength + nNewMessageLength);

   return (W_SUCCESS);
}


/* See Client API Specifications */
W_ERROR PVirtualTagCreate(
            tContext* pContext,
            uint8_t nTagType,
            const uint8_t* pIdentifier,
            uint32_t nIdentifierLength,
            uint32_t nMaximumMessageLength,
            W_HANDLE* phHandle )
{
   tVirtualTag* pVirtualTag = null;
   W_HANDLE hHandle = W_NULL_HANDLE;
   W_ERROR nError;

   /* Check the parameters */
   if (  (  ( pIdentifier == null )
         && ( nIdentifierLength != 0 ) )
      || (  ( pIdentifier != null )
         && ( nIdentifierLength == 0 ) ) )
   {
      PDebugError("PVirtualTagCreate: bad identifier");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }
   if( phHandle == null)
   {
      PDebugError("PVirtualTagCreate: handle pointer");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }
   if((nMaximumMessageLength < 3) || (nMaximumMessageLength > P_VIRTUAL_TAG_MAXIMUM_NDEF_MESSAGE))
   {
      PDebugError("PVirtualTagCreate: bad maximum message length");
      nError = W_ERROR_BAD_PARAMETER;
      goto return_error;
   }

   /* Check the tag type */
   switch ( nTagType )
   {
      case W_PROP_NFC_TAG_TYPE_4_A:

         /* Check if the property is supported */
         if (PEmulIsPropertySupported(pContext, nTagType) == W_FALSE)
         {
            PDebugError("PVirtualTagCreate: Card 14443-A not supported");
            nError = W_ERROR_RF_PROTOCOL_NOT_SUPPORTED;
            goto return_error;
         }
          /* Check the parameters */
         if (  ( nIdentifierLength != 0 )
            && ( nIdentifierLength != 4 )
            && ( nIdentifierLength != 7 )
            && ( nIdentifierLength != 10 ) )
         {
            PDebugError("PVirtualTagCreate: bad identifier for type A");
            nError = W_ERROR_BAD_PARAMETER;
            goto return_error;
         }
         break;

      case W_PROP_NFC_TAG_TYPE_4_B:

         /* Check if the property is supported */
         if (PEmulIsPropertySupported(pContext, nTagType) == W_FALSE)
         {
            PDebugError("PVirtualTagCreate: Card 14443-B not supported");
            nError = W_ERROR_RF_PROTOCOL_NOT_SUPPORTED;
            goto return_error;
         }
         /* Check the parameters */
         if (  ( nIdentifierLength != 0 )
            && ( nIdentifierLength != 4 ) )
         {
            PDebugError("PVirtualTagCreate: bad identifier for type B");
            nError = W_ERROR_BAD_PARAMETER;
            goto return_error;
         }
         break;
      default:
         PDebugError("PVirtualTagCreate: bad tag type");
         nError = W_ERROR_BAD_PARAMETER;
         goto return_error;
   }

   /* Create the virtual tag */
   pVirtualTag = (tVirtualTag*)CMemoryAlloc( sizeof(tVirtualTag) );
   if ( pVirtualTag == null )
   {
      PDebugError("PVirtualTagCreate: cannot allocate the virtual tag");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }
   CMemoryFill(pVirtualTag, 0, sizeof(tVirtualTag));

   /* Create the NDEF file */
   pVirtualTag->pNDEFFile = (uint8_t*)CMemoryAlloc( nMaximumMessageLength + 2 );
   if ( pVirtualTag->pNDEFFile == null )
   {
      PDebugError("PVirtualTagCreate: cannot allocate the NDEF file");
      nError = W_ERROR_OUT_OF_RESOURCE;
      goto return_error;
   }
   /* Copy the empty NDEF file */
   CMemoryCopy(
      pVirtualTag->pNDEFFile,
      g_aNDEFEmptyFile,
      sizeof(g_aNDEFEmptyFile) );

   /* Get a user connection handle */
   if ( ( nError = PHandleRegister(
                     pContext,
                     pVirtualTag,
                     P_HANDLE_TYPE_VIRTUAL_TAG_CONNECTION,
                     &hHandle ) ) != W_SUCCESS )
   {
      PDebugError(
         "PVirtualTagCreate: Cannot register the object" );
      goto return_error;
   }

   /* Store the connection information */
   pVirtualTag->nTagType = nTagType;
   pVirtualTag->nMaximumMessageLength = nMaximumMessageLength;

   if ( nIdentifierLength != 0 )
   {
      CMemoryCopy(
         pVirtualTag->aIdentifier,
         pIdentifier,
         nIdentifierLength );
   }
   pVirtualTag->nIdentifierLength = (uint8_t)nIdentifierLength;
   pVirtualTag->nState = P_VIRTUAL_TAG_STATE_STOP;

   /* Initialize the CC file */
   CMemoryCopy(
      pVirtualTag->aCCFile,
      g_aCCFile,
      sizeof(g_aCCFile) );
   /* Maximum Le value */

   static_PVirtualWrite16(&pVirtualTag->aCCFile[3], P_VIRTUAL_TAG_MAX_READ_LE_VALUE);
   /* Maximum Lc value */
   static_PVirtualWrite16(&pVirtualTag->aCCFile[5], P_VIRTUAL_TAG_MAX_WRITE_LC_VALUE);
   /* Maximum File Size */
   static_PVirtualWrite16(&pVirtualTag->aCCFile[11], nMaximumMessageLength + 2);

   *phHandle = hHandle;

   return W_SUCCESS;

return_error:

   PDebugError(
         "PVirtualTagCreate: PHandleRegister return error %s", PUtilTraceError(nError));

   if(pVirtualTag != null)
   {
      CMemoryFree( pVirtualTag->pNDEFFile );
      CMemoryFree( pVirtualTag );
   }

   if(phHandle != null)
   {
      *phHandle = W_NULL_HANDLE;
   }

   return nError;
}

/* See Client API Specifications */
W_ERROR WVirtualTagStartSync(
                  W_HANDLE hVirtualTag,
                  tWBasicGenericEventHandler* pEventCallback,
                  void* pEventCallbackParameter,
                  bool_t bReadOnly )
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WVirtualTagStart(
         hVirtualTag,
         PBasicGenericSyncCompletion, &param,
         pEventCallback, pEventCallbackParameter,
         bReadOnly);
   }

   return PBasicGenericSyncWaitForResult(&param);
}

/* Receive the card emeulation events */
static void static_PVirtualTagEventReceived(
                  tContext* pContext,
                  void *pCallbackParameter,
                  uint32_t nEventCode)
{
   tVirtualTag* pVirtualTag = (tVirtualTag*)pCallbackParameter;
   uint32_t nVirtualTagEvent;

   switch(nEventCode)
   {
      case W_EMUL_EVENT_SELECTION:
         PDebugTrace("static_PVirtualTagEventReceived: Receive W_EMUL_EVENT_SELECTION");
         nVirtualTagEvent = W_VIRTUAL_TAG_EVENT_SELECTION;
         break;
      case W_EMUL_EVENT_DEACTIVATE:
      default:
         PDebugTrace("static_PVirtualTagEventReceived: Receive W_EMUL_EVENT_DEACTIVATE");
         nVirtualTagEvent = W_VIRTUAL_TAG_EVENT_READER_LEFT;
         break;
   }

   if(nVirtualTagEvent == W_VIRTUAL_TAG_EVENT_READER_LEFT)
   {
      if(pVirtualTag->nReaderAction == P_VIRTUAL_TAG_ACTION_READ_ONLY)
      {
         pVirtualTag->nReaderAction = P_VIRTUAL_TAG_ACTION_NONE;
         nVirtualTagEvent = W_VIRTUAL_TAG_EVENT_READER_READ_ONLY;
      }
      else if(pVirtualTag->nReaderAction == P_VIRTUAL_TAG_ACTION_READ_WRITE)
      {
         pVirtualTag->nReaderAction = P_VIRTUAL_TAG_ACTION_NONE;
         nVirtualTagEvent = W_VIRTUAL_TAG_EVENT_READER_WRITE;
      }

      CDebugAssert(pVirtualTag->nReaderAction == P_VIRTUAL_TAG_ACTION_NONE);
   }

   if((pVirtualTag->nState == P_VIRTUAL_TAG_STATE_ACTIVE)
   || (pVirtualTag->nState == P_VIRTUAL_TAG_STATE_AID_SELECTED)
   || (pVirtualTag->nState == P_VIRTUAL_TAG_STATE_CC_SELECTED)
   || (pVirtualTag->nState == P_VIRTUAL_TAG_STATE_NDEF_SELECTED))
   {
      /* Send the event */
      PDFCPostContext2(
         &pVirtualTag->sEventCallbackContext,
         nVirtualTagEvent );
   }
   else
   {
      PDebugWarning("static_PVirtualTagEventReceived: wrong state, event ignored");
   }
}

/* See Client API Specifications */
void PVirtualTagStart(
                  tContext *pContext,
                  W_HANDLE hVirtualTag,
                  tPBasicGenericCallbackFunction* pCompletionCallback,
                  void* pCompletionCallbackParameter,
                  tPBasicGenericEventHandler* pEventCallback,
                  void* pEventCallbackParameter,
                  bool_t bReadOnly )
{
   tVirtualTag* pVirtualTag;
   tWEmulConnectionInfo sEmulConnectionInfo;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;
   uint8_t nUIDSize;

   nError = PReaderUserGetConnectionObject(pContext, hVirtualTag, P_HANDLE_TYPE_VIRTUAL_TAG_CONNECTION, (void**)&pVirtualTag);

   if(nError != W_SUCCESS)
   {
      PDebugError("PVirtualTagStart: could not get pVirtualTag buffer");
      goto return_error;
   }

   /* Check if the virtual tag is in the right state */
   if ( pVirtualTag->nState != P_VIRTUAL_TAG_STATE_STOP )
   {
      nError = W_ERROR_BAD_STATE;
      PDebugWarning("PVirtualTagStart: wrong state");
      goto return_error;
   }

   /* Set the new state */
   pVirtualTag->nState = P_VIRTUAL_TAG_STATE_START_PENDING;

   /* Set the read-only state */
   pVirtualTag->bIsLocked = bReadOnly;
   if(bReadOnly != W_FALSE)
   {
      /* Lock the CC file */
      pVirtualTag->aCCFile[14] = 0xFF;
   }
   else
   {
      pVirtualTag->aCCFile[14] = 0x00;
   }

   /* Store the connection information */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCompletionCallback,
      pCompletionCallbackParameter,
      &pVirtualTag->sStartCallbackContext );

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pEventCallback,
      pEventCallbackParameter,
      &pVirtualTag->sEventCallbackContext );

   /* Call the card emulation */

   /* Check the tag type */
   switch ( pVirtualTag->nTagType )
   {
      case W_PROP_NFC_TAG_TYPE_4_A:

       sEmulConnectionInfo.nCardType = W_PROP_ISO_14443_4_A;
         if ( pVirtualTag->nIdentifierLength == 0 )
         {
            sEmulConnectionInfo.sCardInfo.s14A.UID[0] = 0x08;
            sEmulConnectionInfo.sCardInfo.s14A.nUIDLength = 0x01;
            nUIDSize=0; /* simple */
         }
         else
         {
            CMemoryCopy(
               sEmulConnectionInfo.sCardInfo.s14A.UID,
               pVirtualTag->aIdentifier,
               pVirtualTag->nIdentifierLength );
            sEmulConnectionInfo.sCardInfo.s14A.nUIDLength = (uint8_t)pVirtualTag->nIdentifierLength;

            switch ( pVirtualTag->nIdentifierLength )
            {
               default:
               case 4:
                  nUIDSize = 0; /* simple */
                  break;
               case 7:
                  nUIDSize = 1; /* double */
                  break;
               case 10:
                  nUIDSize = 2; /* triple */
                  break;
            }

         }
         /* Store the other default values */
         sEmulConnectionInfo.sCardInfo.s14A.nSAK = 0x20;
         sEmulConnectionInfo.sCardInfo.s14A.nATQA = 0x0010 | (nUIDSize << 6);
         sEmulConnectionInfo.sCardInfo.s14A.nFWI_SFGI = 0xB0;
         sEmulConnectionInfo.sCardInfo.s14A.nDataRateMax = 0x00;
         sEmulConnectionInfo.sCardInfo.s14A.bSetCIDSupport = W_FALSE;
         sEmulConnectionInfo.sCardInfo.s14A.nNAD = 0;
         sEmulConnectionInfo.sCardInfo.s14A.nApplicationDataLength = 0;
         break;

      case W_PROP_NFC_TAG_TYPE_4_B:

       sEmulConnectionInfo.nCardType = W_PROP_ISO_14443_4_B;
         if ( pVirtualTag->nIdentifierLength == 0 )
         {
            sEmulConnectionInfo.sCardInfo.s14B.nPUPILength = 0x00;
         }
         else
         {
            CMemoryCopy(
               sEmulConnectionInfo.sCardInfo.s14B.PUPI,
               pVirtualTag->aIdentifier,
               pVirtualTag->nIdentifierLength );
            sEmulConnectionInfo.sCardInfo.s14B.nPUPILength = (uint8_t)pVirtualTag->nIdentifierLength;
         }
         /* Store the other default values */
         /* 00Bx -> FWI = B corresponds to a value FWT = (256  16/ fc) x 2 FWI = 600 ms */
         sEmulConnectionInfo.sCardInfo.s14B.nATQB = 0x000000B4;
         sEmulConnectionInfo.sCardInfo.s14B.nAFI = 0x00;
         sEmulConnectionInfo.sCardInfo.s14B.bSetCIDSupport = W_FALSE;
         sEmulConnectionInfo.sCardInfo.s14B.nNAD = 0;
         sEmulConnectionInfo.sCardInfo.s14B.nHigherLayerResponseLength = 0x00;
         break;
   }

   PEmulOpenConnectionDriver1Ex(
      pContext,
      static_PVirtualTagCardEmulationOpenCompleted, pVirtualTag,
      &sEmulConnectionInfo, sizeof(tWEmulConnectionInfo),
      &pVirtualTag->hCardEmulation );

   nError = PContextGetLastIoctlError(pContext);

   if (nError != W_SUCCESS)
   {
      PDebugError("PVirtualTagStart: PEmulOpenConnectionDriver1Ex() failed %d", nError);

      PDFCFillCallbackContext(pContext, (tDFCCallback *) static_PVirtualTagCardEmulationOpenCompleted, pVirtualTag, &sCallbackContext);
      PDFCPostContext2(&sCallbackContext, nError);
      return;
   }

   if (pVirtualTag->hCardEmulation != W_NULL_HANDLE)
   {
      PEmulOpenConnectionDriver2Ex(
         pContext, pVirtualTag->hCardEmulation,
         static_PVirtualTagEventReceived, pVirtualTag);

      nError = PContextGetLastIoctlError(pContext);

      if (nError != W_SUCCESS)
      {
         PDebugError("PVirtualTagStart: PEmulOpenConnectionDriver2Ex() failed %d", nError);

         PBasicCloseHandle(pContext, pVirtualTag->hCardEmulation);
         pVirtualTag->hCardEmulation = W_NULL_HANDLE;

         PDFCFillCallbackContext(pContext, (tDFCCallback *) static_PVirtualTagCardEmulationOpenCompleted, pVirtualTag, &sCallbackContext);
         PDFCPostContext2(&sCallbackContext, nError);
         return;
      }

      PEmulOpenConnectionDriver3Ex(
         pContext, pVirtualTag->hCardEmulation,
         static_PVirtualTagCommandReceived, pVirtualTag);

      nError = PContextGetLastIoctlError(pContext);

      if (nError != W_SUCCESS)
      {
         PDebugError("PVirtualTagStart: PEmulOpenConnectionDriver3Ex() failed %d", nError);

         PBasicCloseHandle(pContext, pVirtualTag->hCardEmulation);
         pVirtualTag->hCardEmulation = W_NULL_HANDLE;

         PDFCFillCallbackContext(pContext, (tDFCCallback *) static_PVirtualTagCardEmulationOpenCompleted, pVirtualTag, &sCallbackContext);
         PDFCPostContext2(&sCallbackContext, nError);
         return;
      }
   }
   else
   {
      /* Reset the state */
      PDebugError("PVirtualTagStart: error returned by WEmulOpenConnectionDriver1()");

      /* The error is returned in the callback function */
   }

   return;

return_error:

   PDebugError("PVirtualTagStart: return the error %s", PUtilTraceError(nError));

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCompletionCallback,
      pCompletionCallbackParameter,
      &sCallbackContext );

   /* Send the error */
   PDFCPostContext2(
      &sCallbackContext,
      nError );
}

static void static_PVirtualTagCloseCompleted(
                  tContext* pContext,
                  void* pCallbackParameter,
                  W_ERROR nError)
{
   tVirtualTag* pVirtualTag = (tVirtualTag*)pCallbackParameter;

   pVirtualTag->nState = P_VIRTUAL_TAG_STATE_STOP;

   if(nError != W_SUCCESS)
   {
      PDebugError("static_PVirtualTagCloseCompleted: return the error %s", PUtilTraceError(nError));
   }

   PDFCPostContext2(
      &pVirtualTag->sStopCallbackContext,
      nError );
}

/* See Client API Specifications */
void PVirtualTagStop(
                  tContext *pContext,
                  W_HANDLE hVirtualTag,
                  tPBasicGenericCallbackFunction* pCompletionCallback,
                  void* pCallbackParameter )
{
   tVirtualTag* pVirtualTag;
   tDFCCallbackContext sCallbackContext;
   W_ERROR nError;

   nError = PReaderUserGetConnectionObject(pContext, hVirtualTag, P_HANDLE_TYPE_VIRTUAL_TAG_CONNECTION, (void**)&pVirtualTag);

   if(nError != W_SUCCESS)
   {
      PDebugError("PVirtualTagStop: could not get pVirtualTag buffer");
      goto return_error;
   }

   /* Check if the virtual tag is in the right state */
   if((pVirtualTag->nState != P_VIRTUAL_TAG_STATE_ACTIVE)
   && (pVirtualTag->nState != P_VIRTUAL_TAG_STATE_START_PENDING)
   && (pVirtualTag->nState != P_VIRTUAL_TAG_STATE_AID_SELECTED)
   && (pVirtualTag->nState != P_VIRTUAL_TAG_STATE_CC_SELECTED)
   && (pVirtualTag->nState != P_VIRTUAL_TAG_STATE_NDEF_SELECTED))
   {
      nError = W_ERROR_BAD_STATE;
      PDebugWarning("PVirtualTagStop: wrong state");
      goto return_error;
   }

   if ( pVirtualTag->hCardEmulation == W_NULL_HANDLE )
   {
      nError = W_ERROR_BAD_STATE;
      PDebugWarning("PVirtualTagStop: hCardEmulation is null");
      goto return_error;
   }

   /* Set the new state */
   pVirtualTag->nState = P_VIRTUAL_TAG_STATE_STOP_PENDING;

   PDFCFlushCall(&pVirtualTag->sEventCallbackContext);

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCompletionCallback,
      pCallbackParameter,
      &pVirtualTag->sStopCallbackContext );

   PEmulCloseDriver(
      pContext, pVirtualTag->hCardEmulation,
      static_PVirtualTagCloseCompleted,
      pVirtualTag);

   nError = PContextGetLastIoctlError(pContext);

   if (nError != W_SUCCESS)
   {
      PDebugError("PVirtualTagStop : PEmulCloseDriver() failed : trouble ahead");

      /* @todo what can we do here to solve this problem ? */
      goto return_error;
   }

   pVirtualTag->hCardEmulation = W_NULL_HANDLE;

   return;

return_error:

   PDebugError("PVirtualTagStop: return the error %s", PUtilTraceError(nError));

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCompletionCallback,
      pCallbackParameter,
      &sCallbackContext );

   /* Send the error */
   PDFCPostContext2(
      &sCallbackContext,
      nError );
}

/* See Client API Specifications */
W_ERROR WVirtualTagStopSync(
                  W_HANDLE hVirtualTag )
{
   tPBasicGenericSyncParameters param;

   if(WBasicGenericSyncPrepare(&param) != W_FALSE)
   {
      WVirtualTagStop(
         hVirtualTag,
         PBasicGenericSyncCompletion, &param);
   }

   return PBasicGenericSyncWaitForResult(&param);
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */
