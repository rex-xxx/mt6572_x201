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

/*******************************************************************************
   Contains the NDEF Type API implementation.
*******************************************************************************/
#define P_MODULE P_MODULE_DEC( NDEF_FORMAT1 )

#include "wme_context.h"

#if (P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC)

const uint8_t g_Type1StaticFormat[] = { /* CC */               0xE1, 0x10, 0x0E, 0x00,
                                       /* NDEF TLV */         0x03, 0x00,
                                       /* TERMINATOR TLV */   0xFE };

const uint8_t g_Type1Dynamic512Format[] = { /* CC */               0xE1, 0x10, 0x3F, 0x00,
                                            /* LOCK CTRL TLV */    0x01, 0x03, 0xF2, 0x30, 0x33,
                                            /* RES MEM TLV */      0x02, 0x03, 0xF0, 0x02, 0x03,
                                            /* NDEF TLV */         0x03, 0x00,
                                            /* TERMINATOR TLV */   0xFE };


typedef struct tType1Format
{
   /* Callback context */
   tDFCCallbackContext        sCallbackContext;

} tType1Format;

/* write operation completed callback */
static void static_PNDEFFormatNDEFType1WriteCompleted (
            tContext* pContext,
            void *pCallbackParameter,
            W_ERROR nError )
{
   /* Get operation context  */
   tType1Format* pType1Format = (tType1Format*)pCallbackParameter;

   PDebugTrace("static_WType1WriteCompleted nError = 0x%02x",nError);

   PDFCPostContext2( &pType1Format->sCallbackContext, nError);
   CMemoryFree(pType1Format);
}


/** See header */
void PNDEFFormatNDEFType1(tContext* pContext,
                          W_HANDLE hConnection,
                          tPBasicGenericCallbackFunction *pCallback,
                          void *pCallbackParameter,
                          uint8_t nTagType)
{
   tType1Format* pType1Format = null;
   tDFCCallbackContext sCallbackContext;
   tWType1ChipConnectionInfo sConnectionInfo;
   W_ERROR nError;


   PDebugTrace("PNDEFFormatNDEFType1");

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext( pContext, (tDFCCallback*)pCallback, pCallbackParameter, &sCallbackContext );

   pType1Format = (tType1Format*)CMemoryAlloc( sizeof(tType1Format) );

   if ( pType1Format == null )
   {
      PDebugError("PNDEFFormatNDEFType1: pType1Format == null");
      PDFCPostContext2( &sCallbackContext, W_ERROR_OUT_OF_RESOURCE );
      return;
   }

   /* Store the callback context information */
   pType1Format->sCallbackContext = sCallbackContext;

   if ((nError = PType1ChipGetConnectionInfo(pContext, hConnection, &sConnectionInfo)) != W_SUCCESS)
   {
      PDebugError("PNDEFFormatNDEFType1: PType1ChipGetConnectionInfo failed");
      goto error;
   }

   switch (sConnectionInfo.aHeaderRom[0])
   {
      case 0x11 :     /* Topaz */
         PType1ChipWriteInternal(pContext, hConnection, static_PNDEFFormatNDEFType1WriteCompleted, pType1Format, g_Type1StaticFormat, 8, sizeof(g_Type1StaticFormat), W_FALSE );
         return;

      case 0x12 :     /* Topaz 512 */
         PType1ChipWriteInternal(pContext, hConnection, static_PNDEFFormatNDEFType1WriteCompleted, pType1Format, g_Type1Dynamic512Format, 8, sizeof(g_Type1Dynamic512Format), W_FALSE );
         return;
   }

error:

   PDFCPostContext2( &sCallbackContext, nError );
   CMemoryFree(pType1Format);

}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */
