
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
   Contains the NDEF Type 6 API implementation.
*******************************************************************************/


#define P_MODULE P_MODULE_DEC( NDEFA7 )

#include "wme_context.h"


#if ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (defined P_INCLUDE_MIFARE_CLASSIC)

#define P_TYPE7_GPB_OFFSET                9
#define P_TYPE7_GPB_MASK_VERSION          0xF0
#define P_TYPE7_GPB_MASK_READ_ACCESS      0x0C
#define P_TYPE7_GPB_MASK_WRITE_ACCESS     0x03

#define P_TYPE7_GPB_READ_ACCESS_GRANTED   0x00
#define P_TYPE7_GPB_READ_ACCESS_REFUSED   0x0C

#define P_TYPE7_GPB_WRITE_ACCESS_GRANTED  0x00
#define P_TYPE7_GPB_WRITE_ACCESS_REFUSED  0x03

#define P_TYPE7_GPB_VERSION_SUPPORTED     0x40

#define P_TYPE7_TLV_NULL_TYPE             0x00
#define P_TYPE7_TLV_NDEF_TYPE             0x03
#define P_TYPE7_TLV_NDEF_TERMINATOR_TYPE  0xFE
/* ------------------------------------------------------- */
/*                       MACRO                             */
/* ------------------------------------------------------- */
#define P_TYPE7_GET_TLV_LENGTH(nMessageLength)( 1                                /* Type of TLV => 0x03 */ \
                                              + ((nMessageLength < 255) ? 1 : 3) /* Length */ \
                                              + nMessageLength )                 /* Value of TLV */

#define P_TYPE7_GET_CURRENT_BLOCK(pNDEFConnection) ( P_MIFARE_CLASSIC_GET_BLOCK( \
                                                                     pNDEFConnection->sType.t7.nSectorOffset, \
                                                                     pNDEFConnection->sType.t7.nBlockIndex))

#define MAX(a,b) ((a > b) ? a : b)
#define MIN(a,b) ((a < b) ? a : b)


/* ------------------------------------------------------- */
/*                CONSTANT DEFINITION                      */
/* ------------------------------------------------------- */
const static uint8_t g_Type7MADKey[]   = {0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5};
const static uint8_t g_Type7NdefKey[]  = {0xD3, 0xF7, 0xD3, 0xF7, 0xD3, 0xF7};



/* -------------------------------------------------------- */
/*                Definition of function                    */
/* -------------------------------------------------------- */
static void    static_PNDEFType7OperationRead(tContext* pContext, tNDEFConnection * pNDEFConnection);
static void    static_PNDEFType7ReadFirstMAD(tContext* pContext, void* pCallbackParameter, W_ERROR nError );
static void    static_PNDEFType7ReadSecondMAD(tContext* pContext, void* pCallbackParameter, W_ERROR nError );
static void    static_PNDEFType7ReadNDEF(tContext* pContext, void* pCallbackParameter, W_ERROR nError );
/* YWA !!!!
   static W_ERROR  static_PNDEFType7IsSectorLocked(tContext* pContext, uint8_t nSectorNumber, uint8_t * pSectorTrailer, uint32_t nLength, bool_t * pbIsLocked); */

static W_ERROR static_PNDEFType7ParseTLV(tContext * pContext, tNDEFConnection * pNDEFConnection, uint8_t * pBuffer, uint32_t nLength, uint8_t * pOutputBuffer);





static void static_PNDEFType7SendError(tContext * pContext,
                                       tNDEFConnection* pNDEFConnection,
                                       W_ERROR nError)
{
   PDebugTrace("static_PNDEFType7SendError");


   switch(pNDEFConnection->nCommandType)
   {
      case P_NDEF_COMMAND_READ_NDEF:
      case P_NDEF_COMMAND_WRITE_NDEF:
      case P_NDEF_COMMAND_LOCK_TAG:
         PNDEFSendError(pContext,
                        pNDEFConnection,
                        nError);
         break;

      /* For Create Connection */
      default:
         if(nError != W_SUCCESS)
         {
            nError = W_ERROR_CONNECTION_COMPATIBILITY;
         }
         PDFCPostContext2(&pNDEFConnection->sCallbackContext,
                          nError);

         break;
   }
}


/* ------------------------------------------------------- */
/*                CONNECTION CREATION                      */
/* ------------------------------------------------------- */

/**
 * @brief   Creates a NDEF TAG Type 6 connection
 *
 * @param[in]  pContext  The context.
 *
 * @param[in]  pNDEFConnection  The PNDEF connection
 *
 * @return W_SUCCESS if the current connection is NDEF type 5 capable
 *
 * The NDEF connection is completed when the PNDEFSendError() is called
 **/
static W_ERROR static_PNDEFType7CreateConnection(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection )
{

   /* Initialize the NDEF Connection object */
   pNDEFConnection->nCCLength    = 0;

   static_PNDEFType7OperationRead(pContext, pNDEFConnection);

   return W_SUCCESS;
}

/* ------------------------------------------------------- */
/*                COMMAND PROCESSING                       */
/* ------------------------------------------------------- */


/**** READ ****/
static void static_PNDEFType7OperationRead(tContext* pContext, tNDEFConnection * pNDEFConnection)
{
   pNDEFConnection->bIsLockable  = W_TRUE;
   pNDEFConnection->bIsLocked    = W_FALSE;

   /* will be set after reading the MAD(s) */
   pNDEFConnection->nMaximumSpaceSize = 0;
   pNDEFConnection->sType.t7.nNdefNumberOfSector = 0;
   pNDEFConnection->sType.t7.nNdefStartSectorNumber = 0;

   /* Set after reading the Message NDEF */
   pNDEFConnection->nNDEFFileLength = 0;
   pNDEFConnection->nFreeSpaceSize  = 0;
   pNDEFConnection->nByteLength     = 1;

   pNDEFConnection->sType.t7.bSearchNdefAidCompleted = W_FALSE;

   /* Start with read MAD 1 */
   pNDEFConnection->sType.t7.bAuthenticationDone = W_FALSE;
   PMifareClassicAuthenticate(pContext,
                              pNDEFConnection->hConnection,
                              static_PNDEFType7ReadFirstMAD,
                              pNDEFConnection,
                              P_TYPE7_SECTOR_MAD_1,
                              W_TRUE,
                              g_Type7MADKey,
                              sizeof(g_Type7MADKey));
}

static void static_PNDEFType7ReadFirstMAD(tContext* pContext,
                                                   void* pCallbackParameter,
                                                   W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection* ) pCallbackParameter;
   uint8_t i;

   uint8_t * pnNDEFStartSector;
   uint8_t * pnNDEFNumberOfSector;
   uint8_t * pReceivedBuffer;

   if( nError != W_SUCCESS)
   {
      goto ndef_creation_complete;
   }

   /* Authentication */
   if(pNDEFConnection->sType.t7.bAuthenticationDone == W_FALSE)
   {
      pNDEFConnection->sType.t7.bAuthenticationDone = W_TRUE;
      /* begin the MAD Reading by the sector 1.
         The sector 0 contains only Manufacturer data*/

      pNDEFConnection->sType.t7.nBlockIndex = 1;

      PMifareClassicReadBlock(pContext,
                              pNDEFConnection->hConnection,
                              static_PNDEFType7ReadFirstMAD,
                              pNDEFConnection,
                              P_MIFARE_CLASSIC_GET_BLOCK(P_TYPE7_SECTOR_MAD_1,  pNDEFConnection->sType.t7.nBlockIndex),
                              pNDEFConnection->pReceivedBuffer,
                              P_MIFARE_CLASSIC_BLOCK_SIZE);

      return;
   }

   pnNDEFStartSector = &(pNDEFConnection->sType.t7.nNdefStartSectorNumber);
   pnNDEFNumberOfSector = &(pNDEFConnection->sType.t7.nNdefNumberOfSector);
   pReceivedBuffer = pNDEFConnection->pReceivedBuffer;

   i = (pNDEFConnection->sType.t7.nBlockIndex == 1)?  2 /* Skip the two first byte (CRC + InfoByte) of the Block 1*/
                                                   :  0;

   /* Search NDEF AID */
   for(; i < P_MIFARE_CLASSIC_BLOCK_SIZE; i += 2 )
   {
      /* We find AID */
      if( (pReceivedBuffer[i] == 0x03)
         && (pReceivedBuffer[i + 1] == 0xE1))
      {
         /* In case of the first occurrence */
         if(*pnNDEFNumberOfSector == 0)
         {
            /* We must save the sector number */
            *pnNDEFStartSector = (pNDEFConnection->sType.t7.nBlockIndex == 1)?  i / 2
                                                                             :  8 + (i / 2);
         }

         *pnNDEFNumberOfSector += 1;
      }else
      {
         /* If we have already found the NDEF AID */
         if(*pnNDEFNumberOfSector > 0)
         {
            /* We need to notify the end of the NDEF Sector container */
            pNDEFConnection->sType.t7.bSearchNdefAidCompleted = W_TRUE;
            break;/* exit from loop */
         }
      }

   }

   /* If the operation (Read Ndef sector from MAD(s)) is not complete */
   if(pNDEFConnection->sType.t7.bSearchNdefAidCompleted == W_FALSE)
   {
      /* If we need to continue to get the NDEF AID from the first MAD */
      if(pNDEFConnection->sType.t7.nBlockIndex < 2)
      {
         pNDEFConnection->sType.t7.nBlockIndex += 1;

         PMifareClassicReadBlock(pContext,
                              pNDEFConnection->hConnection,
                              static_PNDEFType7ReadFirstMAD,
                              pNDEFConnection,
                              P_MIFARE_CLASSIC_GET_BLOCK(P_TYPE7_SECTOR_MAD_1,  pNDEFConnection->sType.t7.nBlockIndex),
                              pNDEFConnection->pReceivedBuffer,
                              P_MIFARE_CLASSIC_BLOCK_SIZE);
         return;
      }

      /* If we need to get NDEF AID on the second MAD (only for Mifare 4K)*/
      if(PBasicCheckConnectionProperty(pContext, pNDEFConnection->hConnection, W_PROP_MIFARE_4K) == W_SUCCESS)
      {
         pNDEFConnection->sType.t7.bAuthenticationDone = W_FALSE;
         PMifareClassicAuthenticate(pContext,
                                    pNDEFConnection->hConnection,
                                    static_PNDEFType7ReadSecondMAD,
                                    pNDEFConnection,
                                    P_TYPE7_SECTOR_MAD_2,
                                    W_TRUE,
                                    g_Type7MADKey,
                                    sizeof(g_Type7MADKey));
         return;


      }
   }

   /* In slot have been found for the NDEF Message */
   if(*pnNDEFNumberOfSector == 0)
   {
      /* return error */
      PDebugError("static_PNDEFType7ReadFirstMAD");
      nError = W_ERROR_FEATURE_NOT_SUPPORTED;
      goto ndef_creation_complete;
   }

   /* Read MAD Complete. Start the read of the NDEF Message */
   pNDEFConnection->nMaximumSpaceSize = *pnNDEFNumberOfSector * 48; /* 48 = 3 block of 16 byte per sector */
   pNDEFConnection->nIndex = 0; /* Used to count the number of sector read */
   pNDEFConnection->sType.t7.nSectorOffset = *pnNDEFStartSector;
   pNDEFConnection->sType.t7.bAuthenticationDone = W_FALSE;
   pNDEFConnection->nOffset = 0; /* Offset of the received buffer */

   PMifareClassicAuthenticate(pContext,
                              pNDEFConnection->hConnection,
                              static_PNDEFType7ReadNDEF,
                              pNDEFConnection,
                              pNDEFConnection->sType.t7.nSectorOffset,
                              W_TRUE,
                              g_Type7NdefKey,
                              sizeof(g_Type7NdefKey));


   return;
ndef_creation_complete:
   PDebugError("static_PNDEFType7ReadFirstMAD error during the first MAD reading");
   static_PNDEFType7SendError(pContext,
                              pNDEFConnection,
                              nError);
}

/**
 * @brief read and extract ndef data from the MAD Sector 2 (only for Mifare 4K)
 *
 * @param[in] pContext  the current context
 * @param[in/out] pCallbackParameter
 * @param[in] nError the error returned
 **/
static void static_PNDEFType7ReadSecondMAD(tContext* pContext,
                                                    void* pCallbackParameter,
                                                    W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection* ) pCallbackParameter;
   uint8_t i;

   uint8_t * pnNDEFStartSector;
   uint8_t * pnNDEFNumberOfSector;
   uint8_t * pReceivedBuffer;

   uint8_t nSkipMadSector = 0; /* Used to calcultate the maximum length */

   if( nError != W_SUCCESS)
   {
      goto ndef_creation_complete;
   }

   /* Authentication */
   if(pNDEFConnection->sType.t7.bAuthenticationDone == W_FALSE)
   {
      pNDEFConnection->sType.t7.bAuthenticationDone = W_TRUE;
      /* begin the MAD Reading by the block 1.
         The sector 0 contains only Manufacturer data*/

      pNDEFConnection->sType.t7.nBlockIndex = 0;

      PMifareClassicReadBlock(pContext,
                              pNDEFConnection->hConnection,
                              static_PNDEFType7ReadSecondMAD,
                              pNDEFConnection,
                              P_MIFARE_CLASSIC_GET_BLOCK(P_TYPE7_SECTOR_MAD_2, pNDEFConnection->sType.t7.nBlockIndex),
                              pNDEFConnection->pReceivedBuffer,
                              P_MIFARE_CLASSIC_BLOCK_SIZE);

      return;
   }

   pnNDEFStartSector = &(pNDEFConnection->sType.t7.nNdefStartSectorNumber);
   pnNDEFNumberOfSector = &(pNDEFConnection->sType.t7.nNdefNumberOfSector);
   pReceivedBuffer = pNDEFConnection->pReceivedBuffer;

   i = (pNDEFConnection->sType.t7.nBlockIndex == 0 )?  2 /* Skip the two first byte (CRC + InfoByte) of the Block 0 of the Sector MAD 2 (16)*/
                                                    :  0;

   /* Search NDEF AID */
   for(; i < P_MIFARE_CLASSIC_BLOCK_SIZE; i += 2 )
   {
      /* We find AID */
      if( (pReceivedBuffer[i] == 0x03)
         && (pReceivedBuffer[i + 1] == 0xE1))
      {
         /* In case of the first occurrence */
         if(*pnNDEFNumberOfSector == 0)
         {
            /* We must save the sector number */
            *pnNDEFStartSector =  (uint8_t)(P_TYPE7_SECTOR_MAD_2 +  (8 * pNDEFConnection->sType.t7.nBlockIndex)  + (i / 2));
         }

         *pnNDEFNumberOfSector += 1;
      }else
      {
         /* If we have already found the NDEF AID */
         if(*pnNDEFNumberOfSector > 0)
         {
            /* We need to notify the end of the NDEF Sector container */
            pNDEFConnection->sType.t7.bSearchNdefAidCompleted = W_TRUE;
            break;/* exit from loop */
         }
      }

   }

   /* If the operation (Read Ndef sector from MAD(s)) is not complete */
   if(   (pNDEFConnection->sType.t7.bSearchNdefAidCompleted == W_FALSE)
      && (pNDEFConnection->sType.t7.nBlockIndex < 2) )
   {
      pNDEFConnection->sType.t7.nBlockIndex += 1;

      PMifareClassicReadBlock(pContext,
                              pNDEFConnection->hConnection,
                              static_PNDEFType7ReadSecondMAD,
                              pNDEFConnection,
                              P_MIFARE_CLASSIC_GET_BLOCK(P_TYPE7_SECTOR_MAD_2,  pNDEFConnection->sType.t7.nBlockIndex),
                              pNDEFConnection->pReceivedBuffer,
                              P_MIFARE_CLASSIC_BLOCK_SIZE);
         return;
   }

    /* In slot have been found for the NDEF Message */
   if(*pnNDEFNumberOfSector == 0)
   {
      /* return error */
      PDebugError("static_PNDEFType7ReadSecondMAD");
      nError = W_ERROR_FEATURE_NOT_SUPPORTED;
      goto ndef_creation_complete;
   }

   pNDEFConnection->nMaximumSpaceSize = 0;

   if(    (*pnNDEFStartSector < P_TYPE7_SECTOR_MAD_2)
      &&  (*pnNDEFStartSector + *pnNDEFNumberOfSector) >= P_MIFARE_CLASSIC_4K_EXTENDED_SECTOR)
   {
      nSkipMadSector = 1;
   }

   /* Read MAD Complete. Start the read of the NDEF Message */
   if( (*pnNDEFStartSector + *pnNDEFNumberOfSector) > P_MIFARE_CLASSIC_4K_EXTENDED_SECTOR)
   {
      /* 240 = 16 byte per block and there are 15 block containing data per sector */
      pNDEFConnection->nMaximumSpaceSize += 240 * ( (*pnNDEFStartSector + *pnNDEFNumberOfSector + nSkipMadSector)
                                                   - MAX(P_MIFARE_CLASSIC_4K_EXTENDED_SECTOR , *pnNDEFStartSector));
   }

   if( *pnNDEFStartSector < P_MIFARE_CLASSIC_4K_EXTENDED_SECTOR)
   {
      /* 48 = 16 byte per block and there are 3 block containing data per sector */
      pNDEFConnection->nMaximumSpaceSize += 48 * ( MIN(P_MIFARE_CLASSIC_4K_EXTENDED_SECTOR , (*pnNDEFStartSector + *pnNDEFNumberOfSector))
                                                   - (*pnNDEFStartSector + nSkipMadSector));
   }

   pNDEFConnection->nIndex = 0; /* Used to count the number of sector read */
   pNDEFConnection->sType.t7.nSectorOffset = *pnNDEFStartSector;
   pNDEFConnection->sType.t7.bAuthenticationDone = W_FALSE;
   pNDEFConnection->nOffset = 0; /* Offset of the received buffer */

   PMifareClassicAuthenticate(pContext,
                              pNDEFConnection->hConnection,
                              static_PNDEFType7ReadNDEF,
                              pNDEFConnection,
                              pNDEFConnection->sType.t7.nSectorOffset,
                              W_TRUE,
                              g_Type7NdefKey,
                              sizeof(g_Type7NdefKey));

   return;

ndef_creation_complete:
   PDebugError("static_PNDEFType7ReadSecondMAD error during the second MAD reading");
   static_PNDEFType7SendError(pContext,
                              pNDEFConnection,
                              nError);
}

static void static_PNDEFType7ReadNDEF( tContext* pContext,
                                       void* pCallbackParameter,
                                       W_ERROR nError )
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection* ) pCallbackParameter;

   uint8_t * pnSectorOffset   = &pNDEFConnection->sType.t7.nSectorOffset;
   uint8_t * pnBlockIndex     = &pNDEFConnection->sType.t7.nBlockIndex;
   uint8_t * pReceivedBuffer  = pNDEFConnection->pReceivedBuffer;

   if( nError != W_SUCCESS)
   {
      goto ndef_creation_complete;
   }

   /* --------------  Authentication --------------- */
   if(pNDEFConnection->sType.t7.bAuthenticationDone == W_FALSE)
   {
      pNDEFConnection->sType.t7.bAuthenticationDone = W_TRUE;
      /* begin the MAD Reading by the block 1.
         The sector 0 contains only Manufacturer data*/

      *pnBlockIndex = P_MIFARE_CLASSIC_GET_BLOCK_TRAILER(*pnSectorOffset); /* Block of sector trailer */

      PMifareClassicReadBlock(pContext,
                              pNDEFConnection->hConnection,
                              static_PNDEFType7ReadNDEF,
                              pNDEFConnection,
                              P_MIFARE_CLASSIC_GET_BLOCK(*pnSectorOffset,*pnBlockIndex),
                              &pReceivedBuffer[ pNDEFConnection->nOffset ],
                              P_MIFARE_CLASSIC_BLOCK_SIZE);

      return;
   }

   /* Authentication done, we receive some data from a block */

   /* ------------------ Read Sector Trailer ---------------- */
   if( *pnBlockIndex == P_MIFARE_CLASSIC_GET_BLOCK_TRAILER(*pnSectorOffset))
   {
      tPMifareClassicAccessConditions sMifareClassicAccessConditions;
      uint8_t nIndex = 0;
      uint8_t nGPB = pReceivedBuffer[ pNDEFConnection->nOffset + P_TYPE7_GPB_OFFSET];

      nError =  PMifareClassicGetAccessConditions(*pnSectorOffset,
                                                   W_TRUE,
                                                   &pReceivedBuffer[ pNDEFConnection->nOffset + P_MIFARE_CLASSIC_ACCESS_BITS_OFFSET],
                                                   3,
                                                   &sMifareClassicAccessConditions);

      if(nError != W_SUCCESS)
      {
         PDebugError("static_PNDEFType7ReadNDEF : Access bytes error");
         goto ndef_creation_complete;
      }

      /* Verify the GPB (version number, access conditions, lock data) */
      if((nGPB & P_TYPE7_GPB_MASK_VERSION) != P_TYPE7_GPB_VERSION_SUPPORTED)
      {
         nError  = W_ERROR_VERSION_NOT_SUPPORTED;
         PDebugError("static_PNDEFType7ReadNDEF : Version not supported");
         goto ndef_creation_complete;
      }

      if( (nGPB & P_TYPE7_GPB_MASK_READ_ACCESS) != P_TYPE7_GPB_READ_ACCESS_GRANTED)
      {
         nError  = W_ERROR_VERSION_NOT_SUPPORTED;
         PDebugError("static_PNDEFType7ReadNDEF : Version not supported");
         goto ndef_creation_complete;
      }

      if( ((nGPB & P_TYPE7_GPB_MASK_WRITE_ACCESS) != P_TYPE7_GPB_WRITE_ACCESS_GRANTED)
       && ((nGPB & P_TYPE7_GPB_MASK_WRITE_ACCESS) != P_TYPE7_GPB_WRITE_ACCESS_REFUSED))
      {
         nError  = W_ERROR_VERSION_NOT_SUPPORTED;
         PDebugError("static_PNDEFType7ReadNDEF : Version not supported");
         goto ndef_creation_complete;
      }

      if((nGPB & P_TYPE7_GPB_MASK_WRITE_ACCESS) == P_TYPE7_GPB_WRITE_ACCESS_REFUSED)
      {
         pNDEFConnection->bIsLocked    = W_TRUE;
         pNDEFConnection->bIsLockable  = W_FALSE;
      }


      /* If we cannot write access bytes we cannot locked the tag.
         If we are able to write the key B, we cannot change it, so we cannot change the sector trailer */
      if (  ( ( sMifareClassicAccessConditions.nSectorTrailer & P_MIFARE_CLASSIC_ACCESS_WRITE_ACCESS_BYTE) == 0x00)
         || ( ( sMifareClassicAccessConditions.nSectorTrailer & P_MIFARE_CLASSIC_ACCESS_WRITE_KEY_B) != 0x00)
         )
      {
         pNDEFConnection->bIsLockable  = W_FALSE;
      }

      for(nIndex = 0 ; nIndex < sMifareClassicAccessConditions.nNumberOfBlock; ++nIndex)
      {
         /* Verify access byte for reading */
         if( (sMifareClassicAccessConditions.aBlock[nIndex] & P_MIFARE_CLASSIC_ACCESS_READ) == 0x00)
         {
            nError  = W_ERROR_VERSION_NOT_SUPPORTED;
            PDebugError("static_PNDEFType7ReadNDEF : Cannot read block %d", P_MIFARE_CLASSIC_GET_BLOCK(*pnSectorOffset, nIndex));
            goto ndef_creation_complete;
         }

         if( (sMifareClassicAccessConditions.aBlock[nIndex] & P_MIFARE_CLASSIC_ACCESS_WRITE) == 0x00)
         {
            pNDEFConnection->bIsLocked    = W_TRUE;
            pNDEFConnection->bIsLockable  = W_FALSE;
         }
      }

      /* Read block */
      *pnBlockIndex = 0;
      PMifareClassicReadBlock(pContext,
                              pNDEFConnection->hConnection,
                              static_PNDEFType7ReadNDEF,
                              pNDEFConnection,
                              /* read the first block of the current sector (which contains a part of the NDEF MEssage */
                              P_MIFARE_CLASSIC_GET_BLOCK(*pnSectorOffset,*pnBlockIndex),
                              &pReceivedBuffer[ pNDEFConnection->nOffset ],
                              P_MIFARE_CLASSIC_BLOCK_SIZE);

      return;
   }

   /* ---------------- Other Block ------------------------- */
   /* set the new position of the received buffer offset */
   pNDEFConnection->nOffset += P_MIFARE_CLASSIC_BLOCK_SIZE;


   if(*pnBlockIndex < ( P_MIFARE_CLASSIC_GET_BLOCK_TRAILER(*pnSectorOffset) - 1 ) )
   {
      *pnBlockIndex += 1;
      PMifareClassicReadBlock(pContext,
                              pNDEFConnection->hConnection,
                              static_PNDEFType7ReadNDEF,
                              pNDEFConnection,
                              /* read the first block of the current sector (which contains a part of the NDEF MEssage */
                              P_MIFARE_CLASSIC_GET_BLOCK(*pnSectorOffset,*pnBlockIndex),
                              &pReceivedBuffer[ pNDEFConnection->nOffset ],
                              P_MIFARE_CLASSIC_BLOCK_SIZE);

      return;
   }

   /* Increment the number of sector read */
   pNDEFConnection->nIndex += 1;
   if(pNDEFConnection->nIndex < pNDEFConnection->sType.t7.nNdefNumberOfSector)
   {
      /* jump to the next sector */
      pNDEFConnection->sType.t7.bAuthenticationDone = W_FALSE;

      *pnSectorOffset += 1;
      if( *pnSectorOffset == P_TYPE7_SECTOR_MAD_2)
      {
         /* skip the MAD 2 Sector */
         *pnSectorOffset += 1;
      }

      PMifareClassicAuthenticate(pContext,
                                 pNDEFConnection->hConnection,
                                 static_PNDEFType7ReadNDEF,
                                 pNDEFConnection,
                                 *pnSectorOffset,
                                 W_TRUE,
                                 g_Type7NdefKey,
                                 sizeof(g_Type7NdefKey));
      return;
   }

   /* end of reading */
   /* start  parsing */
   nError = static_PNDEFType7ParseTLV(pContext,
                                      pNDEFConnection,
                                      pReceivedBuffer,
                                      pNDEFConnection->nOffset,
                                      pNDEFConnection->pBuffer);

ndef_creation_complete:
   static_PNDEFType7SendError(pContext,
                              pNDEFConnection,
                              nError);
}

static W_ERROR static_PNDEFType7ParseTLV(tContext * pContext,
                                         tNDEFConnection * pNDEFConnection,
                                         uint8_t * pBuffer,
                                         uint32_t nLength,
                                         uint8_t * pOutputBuffer)
{
   uint32_t nMessageLength = 0;
   uint32_t nMessageOffset = 0;
   W_ERROR nError;

   /* Locate the NDEF TLV */
   while(nMessageOffset < nLength)
   {
      if(pBuffer[nMessageOffset] == P_TYPE7_TLV_NULL_TYPE)
      {
         nMessageOffset++;
         continue;
      }

      if( pBuffer[nMessageOffset] == P_TYPE7_TLV_NDEF_TYPE)
      {
         nMessageOffset++;
         break;
      }

      /* Wait only type TLV Ndef */
      return W_ERROR_BAD_TAG_FORMAT;
   }

   if(pBuffer[nMessageOffset]  != 0xFF)
   {
      nMessageLength = pBuffer[nMessageOffset];
      pNDEFConnection->nByteLength = 1;
   }else
   {
      nMessageLength = ( pBuffer[nMessageOffset + 1] << 8 ) + pBuffer[nMessageOffset + 2];
      pNDEFConnection->nByteLength = 3;
   }

   nMessageOffset += pNDEFConnection->nByteLength;

   /* Check NDEF Terminator */
   if( (pBuffer[nMessageOffset + nMessageLength] != P_TYPE7_TLV_NDEF_TERMINATOR_TYPE)
      && ( (nMessageOffset + nMessageLength) < nLength))
   {
      return W_ERROR_BAD_TAG_FORMAT;
   }


   pNDEFConnection->nNDEFId = (uint16_t) nMessageOffset;

   pNDEFConnection->nNDEFFileLength = nMessageLength;
   pNDEFConnection->nFreeSpaceSize = pNDEFConnection->nMaximumSpaceSize
                                       - ( 1 + pNDEFConnection->nByteLength + nMessageLength);

   if( pNDEFConnection->nCommandType == P_NDEF_COMMAND_READ_NDEF)
   {
      nError = PNDEFParseFile(
               pContext,
               pNDEFConnection->nTNF,
               pNDEFConnection->aTypeString,
               pNDEFConnection->aIdentifierString,
               &pBuffer[nMessageOffset],
               pNDEFConnection->nNDEFFileLength,
               &pNDEFConnection->hMessage );
   }else
   {
      nError = W_SUCCESS;
   }


   return nError;

}


/* --------------------------------------------- */
/* -------------- WRITE Operation  ------------- */
/* --------------------------------------------- */

#define P_TYPE7_WRITE_EMPTY                  0 /* LENTGH equals 0 */
#define P_TYPE7_WRITE_MESSAGE                1 /* LENTGH equals 0 */
#define P_TYPE7_WRITE_MESSAGE_LENGTH         3 /* Set the length  */
#define P_TYPE7_WRITE_MESSAGE_COMPLETED      4 /* Write complete  */



static void static_PNDEFType7WriteNDEFAutomaton(
            tContext* pContext,
            void * pCallbackParameter,
            W_ERROR nError)
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection* ) pCallbackParameter;
   uint8_t * pnState = &pNDEFConnection->sType.t7.nState;
   uint8_t aTmpBuffer[P_MIFARE_CLASSIC_BLOCK_SIZE];
   uint8_t nBufferOffset = 0;
   uint8_t nDataToCopy = 0;

   /* Fill the buffer to write */
   CMemoryFill(aTmpBuffer,
               0x00,
               P_MIFARE_CLASSIC_BLOCK_SIZE);

   if(nError != W_SUCCESS)
   {
      static_PNDEFType7SendError(pContext,
                                 pNDEFConnection,
                                 nError);
      return;
   }

   switch(*pnState)
   {
      case P_TYPE7_WRITE_EMPTY:
         CDebugAssert(pNDEFConnection->sType.t7.bAuthenticationDone == W_FALSE);

         pNDEFConnection->sType.t7.bAuthenticationDone = W_TRUE;
         nBufferOffset = 0;


         /*---------- Special case, only one write operation is needed to write the NDEF Message -----------*/
         if(pNDEFConnection->nMessageLength <= 13)
         {
            /* Only one write operation to complete the write */
            aTmpBuffer[nBufferOffset++] = P_TYPE7_TLV_NDEF_TYPE;
            aTmpBuffer[nBufferOffset++] = (uint8_t) pNDEFConnection->nMessageLength ;

            CMemoryCopy(&aTmpBuffer[nBufferOffset],
                        pNDEFConnection->pMessageBuffer,
                        pNDEFConnection->nMessageLength);

            nBufferOffset += (uint8_t) pNDEFConnection->nMessageLength;
            aTmpBuffer[nBufferOffset ++] = P_TYPE7_TLV_NDEF_TERMINATOR_TYPE;

            /* Set the new state (Complete ) */
            *pnState = P_TYPE7_WRITE_MESSAGE_COMPLETED;

            PMifareClassicWriteBlock(pContext,
                                     pNDEFConnection->hConnection,
                                     static_PNDEFType7WriteNDEFAutomaton,
                                     pNDEFConnection,
                                     P_TYPE7_GET_CURRENT_BLOCK(pNDEFConnection),
                                     aTmpBuffer,
                                     sizeof(aTmpBuffer));

            return;
         }
         /*-------------------------------------------------------------------------------------------------*/

         aTmpBuffer[nBufferOffset++] = P_TYPE7_TLV_NDEF_TYPE;
         aTmpBuffer[nBufferOffset++] = 0 ; /* Length */
         aTmpBuffer[nBufferOffset++] = P_TYPE7_TLV_NDEF_TERMINATOR_TYPE;

         if(pNDEFConnection->nMessageLength < 255)
         {
            pNDEFConnection->nOffset = 14; /* 1 byte for TLV Length */
         }else
         {
            pNDEFConnection->nOffset = 12; /* 3 bytes for TLV Length */
         }

         PMifareClassicWriteBlock(pContext,
                                  pNDEFConnection->hConnection,
                                  static_PNDEFType7WriteNDEFAutomaton,
                                  pNDEFConnection,
                                  P_TYPE7_GET_CURRENT_BLOCK(pNDEFConnection),
                                  aTmpBuffer,
                                  sizeof(aTmpBuffer));

         pNDEFConnection->sType.t7.nBlockIndex = 1;
         *pnState = P_TYPE7_WRITE_MESSAGE;

         return;

      case P_TYPE7_WRITE_MESSAGE:

         /* if we have to continue */
         if( (pNDEFConnection->nOffset < pNDEFConnection->nMessageLength)
           || (   /* Test if we can add the TLV Terminator */
                  ( pNDEFConnection->nOffset == pNDEFConnection->nMessageLength)
               && ( P_TYPE7_GET_TLV_LENGTH( pNDEFConnection->nMessageLength) < pNDEFConnection->nMaximumSpaceSize)))
         {
            /* Need reauthentication */
            if(pNDEFConnection->sType.t7.nBlockIndex >= P_MIFARE_CLASSIC_GET_BLOCK_TRAILER(pNDEFConnection->sType.t7.nSectorOffset))
            {
               pNDEFConnection->sType.t7.nBlockIndex = 0;

               /* Increment Offset */
               pNDEFConnection->sType.t7.nSectorOffset += 1;
               if( pNDEFConnection->sType.t7.nSectorOffset == P_TYPE7_SECTOR_MAD_2)
               {
                  /* skip mad sector */
                  pNDEFConnection->sType.t7.nSectorOffset += 1;
               }

               pNDEFConnection->sType.t7.bAuthenticationDone = W_FALSE;

               /* Authenticate on the new sector */
               PMifareClassicAuthenticate(pContext,
                                          pNDEFConnection->hConnection,
                                          static_PNDEFType7WriteNDEFAutomaton,
                                          pNDEFConnection,
                                          pNDEFConnection->sType.t7.nSectorOffset,
                                          W_TRUE,
                                          g_Type7NdefKey,
                                          sizeof(g_Type7NdefKey));
               return;
            }

            if(pNDEFConnection->sType.t7.bAuthenticationDone == W_FALSE)
            {
               pNDEFConnection->sType.t7.bAuthenticationDone = W_TRUE;
            }

            nDataToCopy = (uint8_t) MIN(P_MIFARE_CLASSIC_BLOCK_SIZE, (pNDEFConnection->nMessageLength - pNDEFConnection->nOffset));

            if(nDataToCopy > 0)
            {
               CMemoryCopy(aTmpBuffer,
                           &pNDEFConnection->pMessageBuffer[ pNDEFConnection->nOffset],
                           nDataToCopy);
            }

            /* We need to Write the TLV */
            if(nDataToCopy != P_MIFARE_CLASSIC_BLOCK_SIZE)
            {
               aTmpBuffer[nDataToCopy] = P_TYPE7_TLV_NDEF_TERMINATOR_TYPE;
            }


            PMifareClassicWriteBlock(pContext,
                                     pNDEFConnection->hConnection,
                                     static_PNDEFType7WriteNDEFAutomaton,
                                     pNDEFConnection,
                                     P_TYPE7_GET_CURRENT_BLOCK(pNDEFConnection),
                                     aTmpBuffer,
                                     sizeof(aTmpBuffer));

            pNDEFConnection->sType.t7.nBlockIndex += 1;

            pNDEFConnection->nOffset += P_MIFARE_CLASSIC_BLOCK_SIZE;

            return;
         }

         pNDEFConnection->sType.t7.bAuthenticationDone = W_FALSE;
         *pnState = P_TYPE7_WRITE_MESSAGE_LENGTH;

         pNDEFConnection->sType.t7.nSectorOffset = pNDEFConnection->sType.t7.nNdefStartSectorNumber;
         pNDEFConnection->sType.t7.nBlockIndex   = 0;


         PMifareClassicAuthenticate(pContext,
                                    pNDEFConnection->hConnection,
                                    static_PNDEFType7WriteNDEFAutomaton,
                                    pNDEFConnection,
                                    pNDEFConnection->sType.t7.nSectorOffset,
                                    W_TRUE,
                                    g_Type7NdefKey,
                                    sizeof(g_Type7NdefKey));

         return;

      case P_TYPE7_WRITE_MESSAGE_LENGTH:

         CDebugAssert(pNDEFConnection->sType.t7.bAuthenticationDone == W_FALSE);

         nBufferOffset = 0;
         pNDEFConnection->sType.t7.bAuthenticationDone = W_TRUE;

         aTmpBuffer[nBufferOffset ++] = P_TYPE7_TLV_NDEF_TYPE;

         if(pNDEFConnection->nMessageLength > 254)
         {
            aTmpBuffer[nBufferOffset ++] = 0xFF;
            aTmpBuffer[nBufferOffset ++] = (uint8_t) ((0x0000FF00 & pNDEFConnection->nMessageLength) >> 8);
            aTmpBuffer[nBufferOffset ++] = (uint8_t)  (0x000000FF & pNDEFConnection->nMessageLength);
         }else
         {
            aTmpBuffer[nBufferOffset ++] = (uint8_t)  pNDEFConnection->nMessageLength;
         }

         CMemoryCopy(&aTmpBuffer[nBufferOffset],
                     pNDEFConnection->pMessageBuffer,
                     P_MIFARE_CLASSIC_BLOCK_SIZE - nBufferOffset);

         *pnState = P_TYPE7_WRITE_MESSAGE_COMPLETED;

         PMifareClassicWriteBlock(pContext,
                                    pNDEFConnection->hConnection,
                                    static_PNDEFType7WriteNDEFAutomaton,
                                    pNDEFConnection,
                                    P_TYPE7_GET_CURRENT_BLOCK(pNDEFConnection),
                                    aTmpBuffer,
                                    sizeof(aTmpBuffer));
         return;

      case P_TYPE7_WRITE_MESSAGE_COMPLETED:
         static_PNDEFType7SendError(pContext,
                                 pNDEFConnection,
                                 nError);
         return;

      default:
         CDebugAssert(0);
         static_PNDEFType7SendError(pContext,
                                    pNDEFConnection,
                                    W_ERROR_BAD_STATE);
         return;
   }

}

/**
 * @brief Write the message into a MifareClassic Cards
 *
 * @param[in] pContext  the current context
 * @param[in/out] pNDEFConnection   the current Connection with the Card
 **/
static void static_PNDEFType7WriteNDEF(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection)
{
   if(P_TYPE7_GET_TLV_LENGTH(pNDEFConnection->nMessageLength) > pNDEFConnection->nMaximumSpaceSize)
   {
      PDebugError("static_PNDEFType7WriteNDEF : Cannot write the Message");
      static_PNDEFType7SendError(pContext,
                                 pNDEFConnection,
                                 W_ERROR_TAG_FULL);
      return;
   }

   pNDEFConnection->sType.t7.nState = P_TYPE7_WRITE_EMPTY;
   pNDEFConnection->sType.t7.bAuthenticationDone = W_FALSE;
   pNDEFConnection->sType.t7.nSectorOffset = pNDEFConnection->sType.t7.nNdefStartSectorNumber;
   pNDEFConnection->sType.t7.nBlockIndex   = 0;

   PMifareClassicAuthenticate(pContext,
                              pNDEFConnection->hConnection,
                              static_PNDEFType7WriteNDEFAutomaton,
                              pNDEFConnection,
                              pNDEFConnection->sType.t7.nSectorOffset,
                              W_TRUE,
                              g_Type7NdefKey,
                              sizeof(g_Type7NdefKey));
}

/* --------------------------------------------- */
/* -------------- LOCK Operation  -------------- */
/* --------------------------------------------- */

#define P_TYPE7_LOCK_STATE_AUTHENTICATE            0x00
#define P_TYPE7_LOCK_STATE_READ_SECTOR_TRAILER     0x01
#define P_TYPE7_LOCK_STATE_WRITE_SECTOR_TRAILER    0x02

static void static_PNDEFType7LockNDEFAutomaton(
            tContext* pContext,
            void * pCallbackParameter,
            W_ERROR nError)
{
   tNDEFConnection* pNDEFConnection = (tNDEFConnection* ) pCallbackParameter;

   uint8_t aTmpBuffer[P_MIFARE_CLASSIC_BLOCK_SIZE];

   if(nError != W_SUCCESS)
   {
      static_PNDEFType7SendError(pContext,
                                 pNDEFConnection,
                                 nError);
      return;
   }

   switch(pNDEFConnection->sType.t7.nState)
   {
      case P_TYPE7_LOCK_STATE_AUTHENTICATE:
         CDebugAssert(pNDEFConnection->sType.t7.bAuthenticationDone == W_FALSE);

         /* Authentication done */
         pNDEFConnection->sType.t7.bAuthenticationDone = W_TRUE;

         pNDEFConnection->sType.t7.nState = P_TYPE7_LOCK_STATE_READ_SECTOR_TRAILER;

         /* Read the current sector trailer */
         PMifareClassicReadBlock(pContext,
                                 pNDEFConnection->hConnection,
                                 static_PNDEFType7LockNDEFAutomaton,
                                 pNDEFConnection,
                                 P_MIFARE_CLASSIC_GET_BLOCK(pNDEFConnection->sType.t7.nSectorOffset,
                                                            P_MIFARE_CLASSIC_GET_BLOCK_TRAILER(pNDEFConnection->sType.t7.nSectorOffset)),
                                 pNDEFConnection->pReceivedBuffer,
                                 P_MIFARE_CLASSIC_BLOCK_SIZE);

         return;

      case P_TYPE7_LOCK_STATE_READ_SECTOR_TRAILER:
         CDebugAssert(pNDEFConnection->sType.t7.bAuthenticationDone == W_TRUE);

         /* Make the new sector trailer */
         CMemoryCopy(aTmpBuffer,
                     g_Type7NdefKey,
                     sizeof(g_Type7NdefKey));

         CMemoryCopy(&aTmpBuffer[6],
                     pNDEFConnection->pReceivedBuffer,
                     (P_MIFARE_CLASSIC_BLOCK_SIZE - 6));

         aTmpBuffer[P_TYPE7_GPB_OFFSET] = P_TYPE7_GPB_VERSION_SUPPORTED | P_TYPE7_GPB_WRITE_ACCESS_REFUSED;

         /* Write the new sector trailer */
         pNDEFConnection->sType.t7.nState = P_TYPE7_LOCK_STATE_WRITE_SECTOR_TRAILER;

         PMifareClassicWriteBlock(pContext,
                                  pNDEFConnection->hConnection,
                                  static_PNDEFType7LockNDEFAutomaton,
                                  pNDEFConnection,
                                  P_MIFARE_CLASSIC_GET_BLOCK(pNDEFConnection->sType.t7.nSectorOffset,
                                                            P_MIFARE_CLASSIC_GET_BLOCK_TRAILER(pNDEFConnection->sType.t7.nSectorOffset)),
                                  aTmpBuffer,
                                  P_MIFARE_CLASSIC_BLOCK_SIZE);

         return;

      case P_TYPE7_LOCK_STATE_WRITE_SECTOR_TRAILER:
         CDebugAssert(pNDEFConnection->sType.t7.bAuthenticationDone == W_TRUE);


         pNDEFConnection->nIndex += 1;

         /* If we need to continue the lock */
         if( pNDEFConnection->nIndex < pNDEFConnection->sType.t7.nNdefNumberOfSector)
         {
            pNDEFConnection->sType.t7.nSectorOffset += 1;

            /* Skip the MAD Sector */
            if(pNDEFConnection->sType.t7.nSectorOffset == P_TYPE7_SECTOR_MAD_2)
            {
               pNDEFConnection->sType.t7.nSectorOffset += 1;
            }

            pNDEFConnection->sType.t7.bAuthenticationDone = W_FALSE;
            pNDEFConnection->sType.t7.nState = P_TYPE7_LOCK_STATE_AUTHENTICATE;

            PMifareClassicAuthenticate(pContext,
                                       pNDEFConnection->hConnection,
                                       static_PNDEFType7LockNDEFAutomaton,
                                       pNDEFConnection,
                                       pNDEFConnection->sType.t7.nSectorOffset,
                                       W_TRUE,
                                       g_Type7NdefKey,
                                       sizeof(g_Type7NdefKey));

            return;
         }

         static_PNDEFType7SendError(pContext,
                                    pNDEFConnection,
                                    W_SUCCESS);

         break;

      default:
         CDebugAssert(0);
         static_PNDEFType7SendError(pContext,
                                    pNDEFConnection,
                                    W_ERROR_BAD_STATE);
         return;
   }
}

/**
 * @brief Write the message into a MifareClassic Cards
 *
 * @param[in] pContext  the current context
 * @param[in/out] pNDEFConnection   the current Connection with the Card
 **/
static void static_PNDEFType7LockNDEF(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection)
{
   /* if it is not lockable */
   if(pNDEFConnection->bIsLockable == W_FALSE)
   {
      static_PNDEFType7SendError(pContext,
                                 pNDEFConnection,
                                 W_ERROR_BAD_STATE);
      return;
   }

   pNDEFConnection->sType.t7.bAuthenticationDone = W_FALSE;
   pNDEFConnection->sType.t7.nSectorOffset = pNDEFConnection->sType.t7.nNdefStartSectorNumber;
   pNDEFConnection->sType.t7.nState        = P_TYPE7_LOCK_STATE_AUTHENTICATE;
   pNDEFConnection->sType.t7.nBlockIndex   = 0;
   pNDEFConnection->nIndex                 = 0;

   PMifareClassicAuthenticate(pContext,
                              pNDEFConnection->hConnection,
                              static_PNDEFType7LockNDEFAutomaton,
                              pNDEFConnection,
                              pNDEFConnection->sType.t7.nSectorOffset,
                              W_TRUE,
                              g_Type7NdefKey,
                              sizeof(g_Type7NdefKey));
}


/**
  * @brief Processes the different NDEF commands
  *
  * @param[in] pContext The context
  *
  * @param[in] pNDEFConnection The NDEF connection
  *
  * @param[in] nOffset The offset, for read / write operations
  *
  * @param[in] nLength The length, for read / write operations
  *
  * @return W_SUCCESS on success
  */
static W_ERROR static_PNDEFType7SendCommand(
            tContext* pContext,
            tNDEFConnection* pNDEFConnection,
            uint32_t nOffset,
            uint32_t nLength )
{
   /* Send the corresponding command */
   switch ( pNDEFConnection->nCommandType )
   {

      case P_NDEF_COMMAND_READ_NDEF:
         /* Hoping the pNDEFConnection->pBuffer was set before , The length is not used because we get the NDEF Length directly */
         static_PNDEFType7OperationRead(pContext, pNDEFConnection);
         break;

      case P_NDEF_COMMAND_WRITE_NDEF:
         static_PNDEFType7WriteNDEF( pContext , pNDEFConnection );
         break;

      case P_NDEF_COMMAND_LOCK_TAG:
         static_PNDEFType7LockNDEF(pContext, pNDEFConnection);
         break;

      default:
         return W_ERROR_BAD_PARAMETER;
   }

   return W_SUCCESS;
}


/* The NDEF type information structure */
tNDEFTypeEntry g_sPNDEFType7Info = {
   W_PROP_NFC_TAG_TYPE_7,
   static_PNDEFType7CreateConnection,
   static_PNDEFType7SendCommand,
   null};

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */
