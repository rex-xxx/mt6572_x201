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
#define P_MODULE P_MODULE_DEC( NDEF_FORMAT5 )

#include "wme_context.h"

#if (defined P_INCLUDE_PICOPASS) && ((P_BUILD_CONFIG == P_CONFIG_USER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC))

typedef struct tPicoFormat
{
   /* Callback context */
   tDFCCallbackContext        sCallbackContext;

} tPicoFormat;


/* Pico 2K */
const uint8_t g_Pico2KFormat[]  = { /* CC */          0x4E, 0x44, 0x45, 0x46, 0x10, 0x00, 0xDE, 0x00,
                                    /* NDEF */        0x00, 0x00 };

/* Pico 32k */
const uint8_t g_Pico32KFormat[] = { /* CC */          0x4E, 0x44, 0x45, 0x46, 0x10, 0x0F, 0xC6, 0x00,
                                    /* NDEF */        0x00, 0x00 };


/* write operation completed callback */
static void static_WPicoWriteCompleted (
            tContext* pContext,
            void *pCallbackParameter,
            W_ERROR nError )
{
   tPicoFormat* pPicoFormat = (tPicoFormat*)pCallbackParameter;

   PDebugTrace("static_WPicoWriteCompleted nError = 0x%02x",nError);

   /* Post the result */
   PDFCPostContext2( &pPicoFormat->sCallbackContext, nError );

   CMemoryFree(pPicoFormat);
}

/* see header */
void PNDEFFormatNDEFType5(tContext* pContext,
                          W_HANDLE hConnection,
                          tPBasicGenericCallbackFunction *pCallback,
                          void *pCallbackParameter,
                          uint8_t nTagType)
{
   tPicoFormat* pPicoFormat = null;
   tDFCCallbackContext sCallbackContext;

   PDebugTrace("PNDEFFormatNDEFType5");

   /* Get the connection property number to check if it exists */
   PDFCFillCallbackContext(
      pContext,
      (tDFCCallback*)pCallback,
      pCallbackParameter,
      &sCallbackContext );

   pPicoFormat = (tPicoFormat*)CMemoryAlloc( sizeof(tPicoFormat) );

   if ( pPicoFormat == null )
   {
      PDebugError("PNDEFFormatNDEFType5: pPicoFormat == null");
      PDFCPostContext2( &sCallbackContext, W_ERROR_OUT_OF_RESOURCE );
      return;
   }

   pPicoFormat->sCallbackContext = sCallbackContext;

   switch (nTagType)
   {
      case W_PROP_PICOPASS_2K :
         PPicoWriteInternal(pContext, hConnection, static_WPicoWriteCompleted, pPicoFormat, g_Pico2KFormat, 0x18, sizeof(g_Pico2KFormat), W_FALSE, null);
         break;

      case W_PROP_PICOPASS_32K :
         PPicoWriteInternal(pContext, hConnection, static_WPicoWriteCompleted, pPicoFormat, g_Pico32KFormat, 0x18, sizeof(g_Pico32KFormat), W_FALSE, null);
         break;
   }
}

#endif /* P_CONFIG_USER || P_CONFIG_MONOLITHIC */
