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
   Contains the NDEF Type API implementation.
*******************************************************************************/
#define P_MODULE P_MODULE_DEC( NDEF_FORMAT6 )

#include "wme_context.h"

#if ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)) && (defined P_INCLUDE_MIFARE_CLASSIC)

#define P_TYPE7_FORMAT_STATE_START_DETECT                 0
#define P_TYPE7_FORMAT_STATE_CHECK_MAD_SECTOR             1
#define P_TYPE7_FORMAT_STATE_CHECK_MAD_SECTOR_TRAILER     2
#define P_TYPE7_FORMAT_STATE_CHECK_NDEF_SECTOR            3
#define P_TYPE7_FORMAT_STATE_CHECK_MAD_SECTOR_4K          4
#define P_TYPE7_FORMAT_STATE_CHECK_MAD_SECTOR_TRAILER_4K  5
#define P_TYPE7_FORMAT_STATE_CHECK_NDEF_SECTOR_4K         6

#define P_TYPE7_FORMAT_STATE_CONFIGURE_MAD_SECTOR         7
#define P_TYPE7_FORMAT_STATE_CONFIGURE_NDEF_SECTOR        8
#define P_TYPE7_FORMAT_STATE_CONFIGURE_MAD_SECTOR_4K      9
#define P_TYPE7_FORMAT_STATE_CONFIGURE_NDEF_SECTOR_4K     10

#define P_TYPE7_FORMAT_STATE_ERASE_MESSAGE                0xF0


typedef struct tNDEFType7Format
{
   /* Callback context */
   tDFCCallbackContext  sCallbackContext;

   /* The state of the format*/
   uint8_t              nState;

   uint8_t              nTypeTag;
   uint8_t              nCurrentBlock;
   uint8_t              nCurrentSector;
   bool_t               bTmpAuthDone;

   uint8_t              aBuffer[16];

   bool_t               bFirstMADAuthKeyA;
   bool_t               bSecondMADAuthKeyA;
   W_HANDLE             hConnection;

   bool_t               bMessageNDEFWritten;
} tNDEFType7Format;



/* Default key defined in the specification */
const uint8_t g_MifareClassicDefaultKey[] = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};


const uint8_t g_MifareClassicDefaultAccessConditionA[] = {0xFF, 0x07, 0x80};
const uint8_t g_MifareClassicDefaultAccessConditionB[] = {0x7F, 0x07, 0x88};

#define g_MifareClassicDefaultAccessConditionNDEFSector g_MifareClassicDefaultAccessConditionB

/**
 * Mad Access Condition
 *
 * The block 0 is read with key A or B and written with key B
 * The block 1 is read with key A or B and written with key B
 * The block 2 is read with key A or B and written with key B
 *
 * The sector Trailer block :
 * - key A is written with Key B but never read
 * - Access condition is read with key A or B and written with key B
 * - key B is written with key B and never read
 **/
const uint8_t g_MifareClassicMADAccessCondition[] = {0x78, 0x77 ,0x88};

/* Not used */
const uint8_t gMifareClassicMADGPB = 0xFF;


const uint8_t g_MifareClassicMADTrailer[] = { /* Key MAD */
                                               0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5,
                                               /* Access condition */
                                               0x78, 0x77, 0x88,
                                               /* GPB */
                                               0xFF,
                                               /* Default Key B */
                                               0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF
                                             };

const uint8_t g_MifareClassicMADBlock[] = {  /* NDEF AID */
                                             0x03, 0xE1,0x03, 0xE1, 0x03, 0xE1,0x03, 0xE1,0x03, 0xE1,0x03, 0xE1,0x03, 0xE1, 0x03, 0xE1};

/**
 * Mad Access Condition
 *
 * The block 0 is read and written with key A or B
 * The block 1 is read and written with key A or B
 * The block 2 is read and written with key A or B
 *
 * The sector Trailer block :
 * - key A is written with Key B but never read
 * - Access condition is read with key A or B and written with key B
 * - key B is written with key B and never read
 **/
const uint8_t g_MifareClassicNDEFAccessCondition[] = {0x7F, 0x07 ,0x88};

/**
 * GPB contains the NDEF version used to store the NDEF Message, and the R/W access condition :
 * 0100 0000b => 0x40
 * 01 : Major Version
 * 00 : Minor Version
 * 00 : read access wo security
 * 00 : write access wo security
 **/
const uint8_t g_MifareClassicNDEFGPB = 0x40;


const uint8_t g_MifareClassicNDEFTrailer[] = { /* public Key NDEF */
                                               0xD3, 0xF7, 0xD3, 0xF7, 0xD3, 0xF7,
                                               /* Access condition */
                                               0x7F, 0x07, 0x88,
                                               /* GPB */
                                               0x40,
                                               /* Default Key B */
                                               0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF
                                             };


const uint8_t g_MifareClassicEmptyMessage[] = {
/* Empty Message TLV */ 0x03, 0x03,
/* NDEF Message */      0xD0, 0x00, 0x00,
/* NDEF Terminator */   0xFE};

static void static_PNDEFType7FormatAutomaton(
            tContext * pContext,
            void * pCallbackParameter,
            W_ERROR nError)
{
   tNDEFType7Format* pNDEFType7Format = (tNDEFType7Format* )pCallbackParameter;

   switch(pNDEFType7Format->nState)
   {
      case P_TYPE7_FORMAT_STATE_START_DETECT:

         CMemoryFill(pNDEFType7Format->aBuffer,
                     0x00,
                     sizeof(pNDEFType7Format->aBuffer));

         pNDEFType7Format->nCurrentBlock = 0;
         pNDEFType7Format->nCurrentSector = 0;
         pNDEFType7Format->bFirstMADAuthKeyA = W_TRUE;
         pNDEFType7Format->bSecondMADAuthKeyA = W_TRUE;

         pNDEFType7Format->nState = P_TYPE7_FORMAT_STATE_CHECK_MAD_SECTOR;


         PMifareClassicAuthenticate(pContext,
                                    pNDEFType7Format->hConnection,
                                    static_PNDEFType7FormatAutomaton,
                                    pNDEFType7Format,
                                    P_TYPE7_SECTOR_MAD_1,
                                    W_TRUE,
                                    g_MifareClassicDefaultKey,
                                    sizeof(g_MifareClassicDefaultKey));
         return;

      case P_TYPE7_FORMAT_STATE_CHECK_MAD_SECTOR:
         /* if error */
         if(nError != W_SUCCESS)
         {
            nError = W_ERROR_SECURITY;
            goto format_complete;
         }

         pNDEFType7Format->nState = P_TYPE7_FORMAT_STATE_CHECK_MAD_SECTOR_TRAILER;

         /* if Authentication is OK, we read the MAD Sector trailer and the write access*/
         PMifareClassicReadBlock(pContext,
                                 pNDEFType7Format->hConnection,
                                 static_PNDEFType7FormatAutomaton,
                                 pNDEFType7Format,
                                 P_MIFARE_CLASSIC_GET_BLOCK(P_TYPE7_SECTOR_MAD_1 ,
                                                            P_MIFARE_CLASSIC_GET_BLOCK_TRAILER( P_TYPE7_SECTOR_MAD_1)), /* MAD 1 Sector Trailer */
                                 pNDEFType7Format->aBuffer,
                                 sizeof(pNDEFType7Format->aBuffer));

         return;
      case P_TYPE7_FORMAT_STATE_CHECK_MAD_SECTOR_TRAILER:
         if(nError != W_SUCCESS)
         {
            PDebugError("static_PNDEFType7FormatAutomaton Error on the Read");
            goto format_complete;
         }

         /* Check Sector Trailer (Condition Access)*/
         /* if the conditions access are defined to change data of the MAD with the key A */
         if(      CMemoryCompare( &pNDEFType7Format->aBuffer[P_TYPE7_OFFSET_SECTOR_TRAILER_CONDITION_ACCESS],
                                  g_MifareClassicDefaultAccessConditionA,
                                  P_TYPE7_LENGTH_SECTOR_TRAILER_CONDITION_ACCESS) == 0x00)
         {
            pNDEFType7Format->bFirstMADAuthKeyA = W_TRUE;
         }
         /* if the conditions access are defined to change data of the MAD sector with the key B */
         else if( CMemoryCompare( &pNDEFType7Format->aBuffer[P_TYPE7_OFFSET_SECTOR_TRAILER_CONDITION_ACCESS],
                                  g_MifareClassicDefaultAccessConditionB,
                                  P_TYPE7_LENGTH_SECTOR_TRAILER_CONDITION_ACCESS) == 0x00)
         {
            pNDEFType7Format->bFirstMADAuthKeyA = W_FALSE;
         }
         else
         {
            nError = W_ERROR_FEATURE_NOT_SUPPORTED;
            PDebugError("static_PNDEFType7FormatAutomaton Error on the Read");
            goto format_complete;
         }

         /* continue with reading all sector */
         pNDEFType7Format->nCurrentSector = 1;
         pNDEFType7Format->nState = P_TYPE7_FORMAT_STATE_CHECK_NDEF_SECTOR;
         pNDEFType7Format->bTmpAuthDone = W_FALSE;

         PMifareClassicAuthenticate(pContext,
                                    pNDEFType7Format->hConnection,
                                    static_PNDEFType7FormatAutomaton,
                                    pNDEFType7Format,
                                    pNDEFType7Format->nCurrentSector,
                                    pNDEFType7Format->bFirstMADAuthKeyA,
                                    g_MifareClassicDefaultKey,
                                    sizeof(g_MifareClassicDefaultKey));
         return;

      case P_TYPE7_FORMAT_STATE_CHECK_NDEF_SECTOR:
         if(nError != W_SUCCESS)
         {
            nError = W_ERROR_FEATURE_NOT_SUPPORTED;
            PDebugError("static_PNDEFType7FormatAutomaton Error during the check of the sector (1K)");
            goto format_complete;
         }

         if(pNDEFType7Format->bTmpAuthDone == W_FALSE)
         {
            pNDEFType7Format->bTmpAuthDone = W_TRUE;
            PMifareClassicReadBlock(pContext,
                                    pNDEFType7Format->hConnection,
                                    static_PNDEFType7FormatAutomaton,
                                    pNDEFType7Format,
                                    P_MIFARE_CLASSIC_GET_BLOCK(pNDEFType7Format->nCurrentSector,
                                                               P_MIFARE_CLASSIC_GET_BLOCK_TRAILER(pNDEFType7Format->nCurrentSector)),
                                    pNDEFType7Format->aBuffer,
                                    sizeof(pNDEFType7Format->aBuffer));

            return;
         }
         else
         {
            /* Check Sector Trailer (Condition Access)*/
            if(CMemoryCompare( &pNDEFType7Format->aBuffer[P_TYPE7_OFFSET_SECTOR_TRAILER_CONDITION_ACCESS],
                              g_MifareClassicDefaultAccessConditionNDEFSector,
                              P_TYPE7_LENGTH_SECTOR_TRAILER_CONDITION_ACCESS) != 0x00)
            {
               nError = W_ERROR_FEATURE_NOT_SUPPORTED;
               PDebugError("static_PNDEFType7FormatAutomaton : ndef Sector trailer are invalid");
               goto format_complete;
            }


            pNDEFType7Format->bTmpAuthDone = W_FALSE;
            pNDEFType7Format->nCurrentSector++;

            /* if we have to continue */
            if(pNDEFType7Format->nCurrentSector < P_MIFARE_CLASSIC_1K_NUMBER_OF_SECTOR)
            {
               PMifareClassicAuthenticate(pContext,
                                          pNDEFType7Format->hConnection,
                                          static_PNDEFType7FormatAutomaton,
                                          pNDEFType7Format,
                                          pNDEFType7Format->nCurrentSector,
                                          pNDEFType7Format->bFirstMADAuthKeyA,
                                          g_MifareClassicDefaultKey,
                                          sizeof(g_MifareClassicDefaultKey));
               return;
            }

            /* Special case for Mifare 4K */
            if(pNDEFType7Format->nTypeTag == W_PROP_MIFARE_4K)
            {
               pNDEFType7Format->nCurrentBlock = 0;
               pNDEFType7Format->nCurrentSector = 0;

               pNDEFType7Format->nState = P_TYPE7_FORMAT_STATE_CHECK_MAD_SECTOR_4K;


               PMifareClassicAuthenticate(pContext,
                                          pNDEFType7Format->hConnection,
                                          static_PNDEFType7FormatAutomaton,
                                          pNDEFType7Format,
                                          P_TYPE7_SECTOR_MAD_2,
                                          W_TRUE,
                                          g_MifareClassicDefaultKey,
                                          sizeof(g_MifareClassicDefaultKey));
               return;
            }
         }

         /* Detection Complete : The current Mifare Card is a valid Mifare Standart 1K after production phase */
         pNDEFType7Format->nState = P_TYPE7_FORMAT_STATE_CONFIGURE_MAD_SECTOR;
         pNDEFType7Format->bTmpAuthDone = W_FALSE;
         PMifareClassicAuthenticate(pContext,
                                    pNDEFType7Format->hConnection,
                                    static_PNDEFType7FormatAutomaton,
                                    pNDEFType7Format,
                                    P_TYPE7_SECTOR_MAD_1,
                                    pNDEFType7Format->bFirstMADAuthKeyA,
                                    g_MifareClassicDefaultKey,
                                    sizeof(g_MifareClassicDefaultKey));
         return;

      case P_TYPE7_FORMAT_STATE_CHECK_MAD_SECTOR_4K:
         /* if error */
         if(nError != W_SUCCESS)
         {
            nError = W_ERROR_SECURITY;
            goto format_complete;
         }

         pNDEFType7Format->nState = P_TYPE7_FORMAT_STATE_CHECK_MAD_SECTOR_TRAILER_4K;

         /* if Authentication is OK, we read the MAD Sector trailer and the write access*/
         PMifareClassicReadBlock(pContext,
                                 pNDEFType7Format->hConnection,
                                 static_PNDEFType7FormatAutomaton,
                                 pNDEFType7Format,
                                 P_MIFARE_CLASSIC_GET_BLOCK(P_TYPE7_SECTOR_MAD_2,
                                                            P_MIFARE_CLASSIC_GET_BLOCK_TRAILER(P_TYPE7_SECTOR_MAD_2)), /* MAD 2 Sector Trailer */
                                 pNDEFType7Format->aBuffer,
                                 sizeof(pNDEFType7Format->aBuffer));

         return;
      case P_TYPE7_FORMAT_STATE_CHECK_MAD_SECTOR_TRAILER_4K:
         if(nError != W_SUCCESS)
         {
            PDebugError("static_PNDEFType7FormatAutomaton Error on the Read");
            goto format_complete;
         }

         /* Check Sector Trailer (Condition Access)*/
         /* if the conditions access are defined to change data of the MAD with the key A */
         if(      CMemoryCompare( &pNDEFType7Format->aBuffer[P_TYPE7_OFFSET_SECTOR_TRAILER_CONDITION_ACCESS],
                                  g_MifareClassicDefaultAccessConditionA,
                                  P_TYPE7_LENGTH_SECTOR_TRAILER_CONDITION_ACCESS) == 0x00)
         {
            pNDEFType7Format->bSecondMADAuthKeyA = W_TRUE;
         }
         /* if the conditions access are defined to change data of the MAD sector with the key B */
         else if( CMemoryCompare( &pNDEFType7Format->aBuffer[P_TYPE7_OFFSET_SECTOR_TRAILER_CONDITION_ACCESS],
                                  g_MifareClassicDefaultAccessConditionB,
                                  P_TYPE7_LENGTH_SECTOR_TRAILER_CONDITION_ACCESS) == 0x00)
         {
            pNDEFType7Format->bSecondMADAuthKeyA = W_FALSE;
         }
         else
         {
            nError = W_ERROR_FEATURE_NOT_SUPPORTED;
            PDebugError("static_PNDEFType7FormatAutomaton Error on the Read");
            goto format_complete;
         }

         /* continue with reading all sector */
         pNDEFType7Format->nCurrentSector = P_TYPE7_SECTOR_MAD_2 + 1;
         pNDEFType7Format->nState = P_TYPE7_FORMAT_STATE_CHECK_NDEF_SECTOR_4K;
         pNDEFType7Format->bTmpAuthDone = W_FALSE;

         PMifareClassicAuthenticate(pContext,
                                    pNDEFType7Format->hConnection,
                                    static_PNDEFType7FormatAutomaton,
                                    pNDEFType7Format,
                                    pNDEFType7Format->nCurrentSector,
                                    pNDEFType7Format->bFirstMADAuthKeyA,
                                    g_MifareClassicDefaultKey,
                                    sizeof(g_MifareClassicDefaultKey));
         return;

      case P_TYPE7_FORMAT_STATE_CHECK_NDEF_SECTOR_4K:
         if(nError != W_SUCCESS)
         {
            nError = W_ERROR_FEATURE_NOT_SUPPORTED;
            PDebugError("static_PNDEFType7FormatAutomaton Error during the check of the sector (1K)");
            goto format_complete;
         }

         if(pNDEFType7Format->bTmpAuthDone == W_FALSE)
         {
            pNDEFType7Format->bTmpAuthDone = W_TRUE;
            PMifareClassicReadBlock(pContext,
                                    pNDEFType7Format->hConnection,
                                    static_PNDEFType7FormatAutomaton,
                                    pNDEFType7Format,
                                    P_MIFARE_CLASSIC_GET_BLOCK(pNDEFType7Format->nCurrentSector,
                                                               P_MIFARE_CLASSIC_GET_BLOCK_TRAILER(pNDEFType7Format->nCurrentSector)),
                                    pNDEFType7Format->aBuffer,
                                    sizeof(pNDEFType7Format->aBuffer));
            return;
         }
         else
         {

            /* Check Sector Trailer (Condition Access)*/
            if(CMemoryCompare( &pNDEFType7Format->aBuffer[P_TYPE7_OFFSET_SECTOR_TRAILER_CONDITION_ACCESS],
                              g_MifareClassicDefaultAccessConditionNDEFSector,
                              P_TYPE7_LENGTH_SECTOR_TRAILER_CONDITION_ACCESS) != 0x00)
            {
               nError = W_ERROR_FEATURE_NOT_SUPPORTED;
               PDebugError("static_PNDEFType7FormatAutomaton : ndef (4K) Sector trailer are invalid");
               goto format_complete;
            }

            pNDEFType7Format->bTmpAuthDone = W_FALSE;
            pNDEFType7Format->nCurrentSector++;

            /* if we have to continue */
            if(pNDEFType7Format->nCurrentSector < P_MIFARE_CLASSIC_4K_NUMBER_OF_SECTOR)
            {
               PMifareClassicAuthenticate(pContext,
                                          pNDEFType7Format->hConnection,
                                          static_PNDEFType7FormatAutomaton,
                                          pNDEFType7Format,
                                          pNDEFType7Format->nCurrentSector,
                                          pNDEFType7Format->bFirstMADAuthKeyA,
                                          g_MifareClassicDefaultKey,
                                          sizeof(g_MifareClassicDefaultKey));
               return;
            }
         }

         /* Detection Complete : The current Mifare Card is a valid Mifare Standart 4K after production phase
            Start the card configuration
         */
         pNDEFType7Format->nState = P_TYPE7_FORMAT_STATE_CONFIGURE_MAD_SECTOR;
         pNDEFType7Format->bTmpAuthDone = W_FALSE;
         PMifareClassicAuthenticate(pContext,
                                    pNDEFType7Format->hConnection,
                                    static_PNDEFType7FormatAutomaton,
                                    pNDEFType7Format,
                                    P_TYPE7_SECTOR_MAD_1,
                                    pNDEFType7Format->bFirstMADAuthKeyA,
                                    g_MifareClassicDefaultKey,
                                    sizeof(g_MifareClassicDefaultKey));
         return;

      case P_TYPE7_FORMAT_STATE_CONFIGURE_MAD_SECTOR:
         if(nError != W_SUCCESS)
         {
            PDebugError("static_PNDEFType7FormatAutomaton : Error during the card configuration");
            goto format_complete;
         }

         if(pNDEFType7Format->nCurrentBlock < 3)
         {
            if(pNDEFType7Format->bTmpAuthDone == W_FALSE)
            {
               pNDEFType7Format->bTmpAuthDone   = W_TRUE;
               pNDEFType7Format->nCurrentBlock  = 1;
               pNDEFType7Format->nCurrentSector = 0;

               CMemoryCopy(pNDEFType7Format->aBuffer,
                           g_MifareClassicMADBlock,
                           sizeof(g_MifareClassicMADBlock));

               /* CRC / InfoByte */
               pNDEFType7Format->aBuffer[0] = 0x00; /* see on the Tag Mifare used at the CES */
               pNDEFType7Format->aBuffer[1] = 0x00; /* see on the Tag Mifare used at the CES */
            }
            else if(pNDEFType7Format->nCurrentBlock == 1)
            {
               /* Write the second block of the MAD 1*/
               pNDEFType7Format->nCurrentBlock = 2;
               CMemoryCopy(pNDEFType7Format->aBuffer,
                           g_MifareClassicMADBlock,
                           sizeof(g_MifareClassicMADBlock));

            }else
            {
               /* MAD Sector Trailer */
               pNDEFType7Format->nCurrentBlock = 3;
               CMemoryCopy(pNDEFType7Format->aBuffer,
                           g_MifareClassicMADTrailer,
                           sizeof(g_MifareClassicMADTrailer));
            }

            PMifareClassicWriteBlock(pContext,
                                     pNDEFType7Format->hConnection,
                                     static_PNDEFType7FormatAutomaton,
                                     pNDEFType7Format,
                                     pNDEFType7Format->nCurrentBlock,
                                     pNDEFType7Format->aBuffer,
                                     sizeof(pNDEFType7Format->aBuffer));
            return;
         }

         /* End of the treatment */
         pNDEFType7Format->bTmpAuthDone   = W_FALSE;
         pNDEFType7Format->nCurrentBlock  = 0;
         pNDEFType7Format->nCurrentSector = 1;
         pNDEFType7Format->bMessageNDEFWritten = W_FALSE;

         pNDEFType7Format->nState = P_TYPE7_FORMAT_STATE_CONFIGURE_NDEF_SECTOR;

         PMifareClassicAuthenticate(pContext,
                                    pNDEFType7Format->hConnection,
                                    static_PNDEFType7FormatAutomaton,
                                    pNDEFType7Format,
                                    pNDEFType7Format->nCurrentSector,
                                    pNDEFType7Format->bFirstMADAuthKeyA,
                                    g_MifareClassicDefaultKey,
                                    sizeof(g_MifareClassicDefaultKey));
         return;

      case P_TYPE7_FORMAT_STATE_CONFIGURE_NDEF_SECTOR:
         if(nError != W_SUCCESS)
         {
            PDebugError("static_PNDEFType7FormatAutomaton : Error during the card configuration");
            goto format_complete;
         }

         /* If authentication is done*/
         if(pNDEFType7Format->bTmpAuthDone == W_FALSE)
         {
            pNDEFType7Format->bTmpAuthDone = W_TRUE;
            CMemoryCopy(pNDEFType7Format->aBuffer,
                        g_MifareClassicNDEFTrailer,
                        sizeof(g_MifareClassicNDEFTrailer));

            /* Write */
            PMifareClassicWriteBlock(pContext,
                                     pNDEFType7Format->hConnection,
                                     static_PNDEFType7FormatAutomaton,
                                     pNDEFType7Format,
                                     P_MIFARE_CLASSIC_GET_BLOCK(pNDEFType7Format->nCurrentSector,
                                                               P_MIFARE_CLASSIC_GET_BLOCK_TRAILER(pNDEFType7Format->nCurrentSector)),
                                     pNDEFType7Format->aBuffer,
                                     sizeof(pNDEFType7Format->aBuffer));

            return;
         }

         /* Write is done*/
         if(pNDEFType7Format->bMessageNDEFWritten == W_FALSE)
         {
            pNDEFType7Format->bMessageNDEFWritten = W_TRUE;
            CMemoryFill(pNDEFType7Format->aBuffer,
                        0,
                        sizeof(pNDEFType7Format->aBuffer));

            CMemoryCopy(pNDEFType7Format->aBuffer,
                        g_MifareClassicEmptyMessage,
                        sizeof(g_MifareClassicEmptyMessage));

            PMifareClassicWriteBlock(pContext,
                                     pNDEFType7Format->hConnection,
                                     static_PNDEFType7FormatAutomaton,
                                     pNDEFType7Format,
                                     P_MIFARE_CLASSIC_GET_BLOCK(pNDEFType7Format->nCurrentSector, 0),
                                     pNDEFType7Format->aBuffer,
                                     sizeof(pNDEFType7Format->aBuffer));
            return;
         }


         pNDEFType7Format->nCurrentSector += 1;
         /* Go to next sector */
         if(pNDEFType7Format->nCurrentSector < P_MIFARE_CLASSIC_1K_NUMBER_OF_SECTOR)
         {
            pNDEFType7Format->bTmpAuthDone = W_FALSE;
            PMifareClassicAuthenticate(pContext,
                                    pNDEFType7Format->hConnection,
                                    static_PNDEFType7FormatAutomaton,
                                    pNDEFType7Format,
                                    pNDEFType7Format->nCurrentSector,
                                    pNDEFType7Format->bFirstMADAuthKeyA,
                                    g_MifareClassicDefaultKey,
                                    sizeof(g_MifareClassicDefaultKey));
            return;
         }

         /* Special Case for Mifare 4K */
         if(pNDEFType7Format->nTypeTag == W_PROP_MIFARE_4K)
         {
            pNDEFType7Format->bTmpAuthDone   = W_FALSE;
            pNDEFType7Format->nState = P_TYPE7_FORMAT_STATE_CONFIGURE_MAD_SECTOR_4K;


            PMifareClassicAuthenticate(pContext,
                                       pNDEFType7Format->hConnection,
                                       static_PNDEFType7FormatAutomaton,
                                       pNDEFType7Format,
                                       pNDEFType7Format->nCurrentSector,
                                       pNDEFType7Format->bSecondMADAuthKeyA,
                                       g_MifareClassicDefaultKey,
                                       sizeof(g_MifareClassicDefaultKey));

            return;
         }


         nError = W_SUCCESS;
         goto format_complete;
         return;
      case P_TYPE7_FORMAT_STATE_CONFIGURE_MAD_SECTOR_4K:
         if(nError != W_SUCCESS)
         {
            PDebugError("static_PNDEFType7FormatAutomaton : Error during the card configuration");
            goto format_complete;
         }

         if(pNDEFType7Format->bTmpAuthDone == W_FALSE)
         {
            pNDEFType7Format->bTmpAuthDone   = W_TRUE;
            pNDEFType7Format->nCurrentBlock = 0;
            pNDEFType7Format->nCurrentSector = P_TYPE7_SECTOR_MAD_2;

            CMemoryCopy(pNDEFType7Format->aBuffer,
                        g_MifareClassicMADBlock,
                        sizeof(g_MifareClassicMADBlock));

            /* CRC / InfoByte */
            pNDEFType7Format->aBuffer[0] = 0x00; /* see on the Tag Mifare used at the CES */
            pNDEFType7Format->aBuffer[1] = 0x00; /* see on the Tag Mifare used at the CES */

            PMifareClassicWriteBlock(pContext,
                                     pNDEFType7Format->hConnection,
                                     static_PNDEFType7FormatAutomaton,
                                     pNDEFType7Format,
                                     P_MIFARE_CLASSIC_GET_BLOCK(P_TYPE7_SECTOR_MAD_2,  pNDEFType7Format->nCurrentBlock),
                                     pNDEFType7Format->aBuffer,
                                     sizeof(pNDEFType7Format->aBuffer));
            return;
         }

         if(pNDEFType7Format->nCurrentBlock < (P_MIFARE_CLASSIC_BLOCK_NUMBER_PER_SECTOR - 1))
         {
            pNDEFType7Format->nCurrentBlock += 1;


            if(pNDEFType7Format->nCurrentBlock < P_MIFARE_CLASSIC_GET_BLOCK_TRAILER(P_TYPE7_SECTOR_MAD_2))
            {
               /* Write the second block of the MAD 1*/
               CMemoryCopy(pNDEFType7Format->aBuffer,
                           g_MifareClassicMADBlock,
                           sizeof(g_MifareClassicMADBlock));

            }else
            {
               /* Replace MAD Sector Trailer */
               CMemoryCopy(pNDEFType7Format->aBuffer,
                           g_MifareClassicMADTrailer,
                           sizeof(g_MifareClassicMADTrailer));
            }



            PMifareClassicWriteBlock(pContext,
                                     pNDEFType7Format->hConnection,
                                     static_PNDEFType7FormatAutomaton,
                                     pNDEFType7Format,
                                     P_MIFARE_CLASSIC_GET_BLOCK(P_TYPE7_SECTOR_MAD_2,  pNDEFType7Format->nCurrentBlock),
                                     pNDEFType7Format->aBuffer,
                                     sizeof(pNDEFType7Format->aBuffer));
            return;
         }

         /* End of the treatment */
         pNDEFType7Format->bTmpAuthDone   = W_FALSE;
         pNDEFType7Format->nCurrentBlock  = 0;
         pNDEFType7Format->nCurrentSector = P_TYPE7_SECTOR_MAD_2 + 1;
         pNDEFType7Format->bMessageNDEFWritten = W_FALSE;

         pNDEFType7Format->nState = P_TYPE7_FORMAT_STATE_CONFIGURE_NDEF_SECTOR_4K;

         PMifareClassicAuthenticate(pContext,
                                    pNDEFType7Format->hConnection,
                                    static_PNDEFType7FormatAutomaton,
                                    pNDEFType7Format,
                                    pNDEFType7Format->nCurrentSector,
                                    pNDEFType7Format->bFirstMADAuthKeyA,
                                    g_MifareClassicDefaultKey,
                                    sizeof(g_MifareClassicDefaultKey));
         return;


      case P_TYPE7_FORMAT_STATE_CONFIGURE_NDEF_SECTOR_4K:
         if(nError != W_SUCCESS)
         {
            PDebugError("static_PNDEFType7FormatAutomaton : Error during the card configuration");
            goto format_complete;
         }

         /* If authentication is done*/
         if(pNDEFType7Format->bTmpAuthDone == W_FALSE)
         {
            pNDEFType7Format->bTmpAuthDone = W_TRUE;
            CMemoryCopy(pNDEFType7Format->aBuffer,
                        g_MifareClassicNDEFTrailer,
                        sizeof(g_MifareClassicNDEFTrailer));

            /* Write */
            PMifareClassicWriteBlock(pContext,
                                     pNDEFType7Format->hConnection,
                                     static_PNDEFType7FormatAutomaton,
                                     pNDEFType7Format,
                                     P_MIFARE_CLASSIC_GET_BLOCK(pNDEFType7Format->nCurrentSector,
                                                               P_MIFARE_CLASSIC_GET_BLOCK_TRAILER(pNDEFType7Format->nCurrentSector)),
                                     pNDEFType7Format->aBuffer,
                                     sizeof(pNDEFType7Format->aBuffer));

            return;
         }


         pNDEFType7Format->nCurrentSector += 1;
         /* Go to next sector */
         if(pNDEFType7Format->nCurrentSector < P_MIFARE_CLASSIC_4K_NUMBER_OF_SECTOR)
         {
            pNDEFType7Format->bTmpAuthDone = W_FALSE;
            PMifareClassicAuthenticate(pContext,
                                       pNDEFType7Format->hConnection,
                                       static_PNDEFType7FormatAutomaton,
                                       pNDEFType7Format,
                                       pNDEFType7Format->nCurrentSector,
                                       pNDEFType7Format->bSecondMADAuthKeyA,
                                       g_MifareClassicDefaultKey,
                                       sizeof(g_MifareClassicDefaultKey));
            return;
         }

         nError = W_SUCCESS;
         goto format_complete;

      default:
         PDebugError("static_PNDEFType7FormatAutomaton : Error in the format automaton");
         nError = W_ERROR_BAD_STATE;
         goto format_complete;
   }

   return;

format_complete:
   PDFCPostContext2(&pNDEFType7Format->sCallbackContext, nError);
   CMemoryFree(pNDEFType7Format);
}


void PNDEFFormatNDEFType7(tContext* pContext,
                          W_HANDLE hConnection,
                          tPBasicGenericCallbackFunction *pCallback,
                          void *pCallbackParameter,
                          uint8_t nTypeTag)
{
   tNDEFType7Format* pNDEFType7Format = null;
   tDFCCallbackContext sCallbackContext;

   PDebugTrace("PNDEFFormatNDEFType7");

   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   pNDEFType7Format = (tNDEFType7Format*)CMemoryAlloc( sizeof(tNDEFType7Format) );

   if ( pNDEFType7Format == null )
   {
      PDebugError("PNDEFFormatNDEFType7: pNDEFType7Format == null");
      PDFCPostContext2( &sCallbackContext, W_ERROR_OUT_OF_RESOURCE );
      return;
   }

   /* Store the connection information */
   pNDEFType7Format->sCallbackContext   = sCallbackContext;
   pNDEFType7Format->nTypeTag = nTypeTag;
   pNDEFType7Format->nCurrentBlock  = 0;
   pNDEFType7Format->nCurrentSector = 0;
   pNDEFType7Format->hConnection    = hConnection;
   pNDEFType7Format->nState         = P_TYPE7_FORMAT_STATE_START_DETECT;

   static_PNDEFType7FormatAutomaton(pContext,
                                    pNDEFType7Format,
                                    W_SUCCESS);

   return;
}
#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */
