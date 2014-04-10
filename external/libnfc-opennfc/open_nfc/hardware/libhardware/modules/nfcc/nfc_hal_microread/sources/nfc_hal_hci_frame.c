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
   Contains the implementation of the HCI frame functions.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( H_FRM )

#include "nfc_hal_binding.h"

#define HCI_FRAME_CHAINING_MSK      0x80

#define  HCI_FRAME_STATE_DISCONNECTED  0x00
#define  HCI_FRAME_STATE_CONNECTED     0x01

static void static_PHCIFrameAddReadContextToList(
         tHCIFrameReadContext** pListHead,
         tHCIFrameReadContext* pReadContext);

static tHCIFrameReadContext* static_PHCIFrameFindReadContextFromList(
         uint8_t               nPipeId,
         tHCIFrameReadContext* pReadContext );

static void static_PHCIFrameRemoveReadContextFromList(
         tHCIFrameReadContext*  pReadContext,
         tHCIFrameReadContext** pListHead);

static void static_PHCIFrameAddWriteContextToList(
         tHCIFrameWriteContext*  pWriteContext,
         tHCIFrameWriteContext** pListHead);

static void static_PHCIFrameCleanContextLists( tHCIFrameInstance* );

static void static_PHCIFrameSetStateConnected( tHCIFrameInstance* );
static void static_PHCIFrameSetStateDisconnected( tHCIFrameInstance* );
static bool_t static_PHCIFrameIsStateConnected( tHCIFrameInstance* );

/*****************
 * WRITE Machine *
 *****************/
static void static_PHCIFrameWriteMachine(
         tNALBindingContext* pBindingContext,
         uint32_t  nSHDLCFrameReceptionCounter)
{
   tHCIFrameInstance* pHCIFrame = PNALContextGetHCIFrameInstance( pBindingContext );
   tHCIFrameWriteContext* pWriteContext;

   /* If the SHDLC frame counter is different from 0, a complete message is acknowledged */
   if(nSHDLCFrameReceptionCounter != 0)
   {
      tHCIFrameWriteContext* pPrevWriteContext = null;

      /* At least one write context should be waiting in the list */
      pWriteContext = pHCIFrame->pSentWriteContextListHead;
      CNALDebugAssert(pWriteContext != null);

      /* The oldest write context is at the end of the list */
      while(pWriteContext->pNext != null)
      {
         pPrevWriteContext = pWriteContext;
         pWriteContext = pWriteContext->pNext;
      }

      /* Remove it from the list */
      if(pPrevWriteContext == null)
      {
         pHCIFrame->pSentWriteContextListHead = null;
      }
      else
      {
         pPrevWriteContext->pNext = null;
      }

      CNALDebugAssert( pWriteContext->pCallbackFunction != null );

      /* Post the write completed callback */
      PNALDFCPost2(
         pBindingContext,
         P_DFC_TYPE_HCI_FRAME,
         pWriteContext->pCallbackFunction,
         pWriteContext->pCallbackParameter,
         PNALUtilConvertUint32ToPointer(nSHDLCFrameReceptionCounter));
   }

   while ( (pWriteContext = pHCIFrame->pWriteContextListHead) != null )
   {
      uint32_t nLength;

      while ( (nLength=(pWriteContext->nLength - pWriteContext->nCounter)) > 0 )
      {
         /* Format the packet */
         if ( nLength > (PSHDLC_PAYLOAD_MAX_SIZE-1) )
         {
            /* Fragmented packet */
            nLength = (PSHDLC_PAYLOAD_MAX_SIZE-1);
            pHCIFrame->aTxPacket[ 0 ] = pWriteContext->nPipeIdentifier;
         }
         else
         {
            /* Unic or last packet */
            pHCIFrame->aTxPacket[ 0 ] = (pWriteContext->nPipeIdentifier | HCI_FRAME_CHAINING_MSK);
         }

         /* Case of Streaming */
         if ( pWriteContext->pBuffer == null)
         {
            /* Get Data */
            pWriteContext->pSreamCallbackFunction(
               pBindingContext,
               pWriteContext->pStreamCallbackParameter,
               &pHCIFrame->aTxPacket[1],
               pWriteContext->nCounter,
               nLength);
         }
         else
         {
            /* Copy payload data */
            CNALMemoryCopy(
               &pHCIFrame->aTxPacket[ 1 ],
               (uint8_t*)( pWriteContext->pBuffer + pWriteContext->nCounter),
               nLength);
         }

         /* Send Packet */
         if ( PSHDLCWrite(
            pBindingContext,
            pHCIFrame->aTxPacket,
            (nLength+1) ) == W_FALSE )
         {
            /* wait for write acknowledgement */
            return;
         }

         pWriteContext->nCounter += nLength;
      }

      /* Remove Write Context from the List */
      pHCIFrame->pWriteContextListHead = pWriteContext->pNext;

      /* then add it at the head of the wait list for acknowledgement */
      pWriteContext->pNext = pHCIFrame->pSentWriteContextListHead;
      pHCIFrame->pSentWriteContextListHead = pWriteContext;
   }
}

/****************
 * READ Machine *
 ****************/
static void static_PHCIFrameReadMachine(
         tNALBindingContext* pBindingContext)
{
   uint32_t nLength;

   tHCIFrameInstance* pHCIFrame = PNALContextGetHCIFrameInstance( pBindingContext );
   tHCIFrameReadContext *pReadContext;

   for (;;)
   {
      bool_t bEnd;
      uint32_t nSHDLCFrameReceptionCounter;

      /* Read packet */
      if ( (nLength=PSHDLCRead(
         pBindingContext,
         pHCIFrame->aRxPacket,
         &nSHDLCFrameReceptionCounter )) == 0 )
      {
         /* wait for Data_read */
         return;
      }

      if (nLength == 1)
      {
         PNALDebugError("The received frame is to short to contain a whole HCI message");
         ++pHCIFrame->nReadMessageLost;
         pHCIFrame->nReadByteErrorCount += nLength;
         continue;
      }

      /* Search corresponding pipe_id from the list */
      if ( (pReadContext=static_PHCIFrameFindReadContextFromList(
         (pHCIFrame->aRxPacket[ 0 ] & ~HCI_FRAME_CHAINING_MSK),
         pHCIFrame->pReadContextListHead)) == null)
      {
         /* No corresponding Pipe_id found */
         PNALDebugWarning( "No corresponding Pipe id %d found", pHCIFrame->aRxPacket[ 0 ] & ~HCI_FRAME_CHAINING_MSK );
         /* Update statistics */
         ++pHCIFrame->nReadMessageLost;
         pHCIFrame->nReadByteErrorCount += nLength;
         continue;
      }

      bEnd = ( pHCIFrame->aRxPacket[ 0 ] & HCI_FRAME_CHAINING_MSK ) ? W_TRUE : W_FALSE;

      /* Record the SHDLC frame reception counter of the first frame as the
         HCI message reception counter */
      if(pReadContext->nCounter == 0)
      {
         pReadContext->nHCIMessageReceptionCounter = nSHDLCFrameReceptionCounter;
      }

      /* Case of Steaming */
      if ( pReadContext->pBuffer == null )
      {
         uint32_t nHCIMessageReceptionCounter = pReadContext->nHCIMessageReceptionCounter;
         uint32_t nCounter;

         if ( bEnd == W_FALSE )
         {
            nCounter = pReadContext->nCounter;
            pReadContext->nCounter += (nLength-1);
            nHCIMessageReceptionCounter = 0;
         }
         else
         {
            nCounter = pReadContext->nCounter;
            pReadContext->nCounter = 0;
            nHCIMessageReceptionCounter = pReadContext->nHCIMessageReceptionCounter;
         }

         PNALDFCPost5(
            pBindingContext,
            P_DFC_TYPE_HCI_FRAME,
            pReadContext->pCallbackFunction,
            pReadContext->pCallbackParameter,
            &pHCIFrame->aRxPacket[1],
            nCounter,
            (nLength-1),
            nHCIMessageReceptionCounter);
      }
      else
      {
         /* Prevent Buffer Overflow */
         if( pReadContext->nCounter == (uint32_t)-1 )
         {
           /* Update statistics */
            ++pHCIFrame->nReadMessageLost;
            pHCIFrame->nReadByteErrorCount += (nLength-1);
         }
         else if ( (pReadContext->nCounter + (nLength-1)) > pReadContext->nBufferLength )
         {
            PNALDebugWarning( "Input buffer overflow" );
            /* Update statistics */
            ++pHCIFrame->nReadMessageLost;
            pHCIFrame->nReadByteErrorCount += pReadContext->nCounter + (nLength-1);

            /* Set the size to -1 to notify the error */
            pReadContext->nCounter = (uint32_t)-1;
         }
         else
         {
            /* Copy payload data */
            CNALMemoryCopy(
               (uint8_t*)(pReadContext->pBuffer + pReadContext->nCounter),
               &pHCIFrame->aRxPacket[ 1 ],
               (nLength-1) );

            pReadContext->nCounter += (nLength-1);
         }

         if(bEnd != W_FALSE)
         {
            if( pReadContext->nCounter == (uint32_t)-1 )
            {
               pReadContext->nCounter = 0;
            }

            /* Read Completed ! */
            PNALDFCPost4(
               pBindingContext,
               P_DFC_TYPE_HCI_FRAME,
               pReadContext->pCallbackFunction,
               pReadContext->pCallbackParameter,
               pReadContext->pBuffer,
               pReadContext->nCounter,
               pReadContext->nHCIMessageReceptionCounter);

            /* Remove Read_Context from List */
            static_PHCIFrameRemoveReadContextFromList(
               pReadContext,
               &pHCIFrame->pReadContextListHead );
         }
      }
      /* continue infinite loop */
   }
}

/**
 *   ===================================
 *   SHDLC Callback implementation
 *   ===================================
 **/

/**
 * Open Completed Handler
 **/
static void static_PHCIFrameSHDLCConnectCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter)
{
   tHCIFrameInstance* pHCIFrame = PNALContextGetHCIFrameInstance( pBindingContext );

   /* State = CONNECTED */
   static_PHCIFrameSetStateConnected( pHCIFrame );

   PNALDFCPost1(
      pBindingContext,
      P_DFC_TYPE_HCI_FRAME,
      pHCIFrame->pCallbackFunction,
      pHCIFrame->pCallbackParameter);
}

/**
 * Reset indication Handler
 **/
static void static_PHCIFrameSHDLCResetIndication(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter)
{
   tHCIFrameInstance* pHCIFrame = PNALContextGetHCIFrameInstance( pBindingContext );

   PNALDebugTrace("static_PHCIFrameSHDLCResetIndication");

   PNALDFCPost1(
      pBindingContext,
      P_DFC_TYPE_HCI_FRAME,
      pHCIFrame->pReconnectIndicationFunction,
      pHCIFrame->pReconnectIndicationCallbackParameter);
}

/**
 * Write Acknowledged Handler
 **/
static void static_PHCIFrameSHDLCWriteAcknowledged(
         tNALBindingContext* pBindingContext,
         void* pWriteHandlerParameter,
         uint32_t nSHDLCFrameReceptionCounter)
{
   /* Only if State is CONNECTED */
   if ( static_PHCIFrameIsStateConnected( PNALContextGetHCIFrameInstance( pBindingContext ) ) != W_FALSE )
   {
      static_PHCIFrameWriteMachine( pBindingContext, nSHDLCFrameReceptionCounter );
   }
}

/**
 * Data Ready
 **/
static void static_PHCIFrameSHDLCDataReady(
         tNALBindingContext* pBindingContext,
         void* pDataReadyHandlerParameter )
{
   /* Only if State is CONNECTED */
   if ( static_PHCIFrameIsStateConnected( PNALContextGetHCIFrameInstance( pBindingContext ) ) != W_FALSE )
   {
      static_PHCIFrameReadMachine( pBindingContext );
   }
}

/**
 * ========================
 * HCI Frame implementation
 * ========================
 **/

/**
 * ------------
 * Frame Create
 * ------------
 * See header file
 **/
NFC_HAL_INTERNAL void PHCIFrameCreate(
         tHCIFrameInstance* pHCIFrame)
{
   /* State is DISCONNECTED */
   static_PHCIFrameSetStateDisconnected( pHCIFrame );

   /* reset lists */
   pHCIFrame->pWriteContextListHead = null;
   pHCIFrame->pSentWriteContextListHead = null;
   pHCIFrame->pReadContextListHead  = null;

   /* Reset statistics */
   pHCIFrame->nReadMessageLost    = 0;
   pHCIFrame->nReadByteErrorCount = 0;
}

/**
 * -------------
 * Frame Destroy
 * -------------
 * See header file
 **/
NFC_HAL_INTERNAL void PHCIFrameDestroy(
         tHCIFrameInstance* pHCIFrame )
{
   static_PHCIFrameCleanContextLists( pHCIFrame );
}

/**
 * ---------------
 * Frame Pre Reset
 * ---------------
 * See header file
 **/
NFC_HAL_INTERNAL void PHCIFramePreReset(
         tNALBindingContext*   pBindingContext)
{
   tHCIFrameInstance* pHCIFrame = PNALContextGetHCIFrameInstance( pBindingContext );

   /* clean context lists */
   static_PHCIFrameCleanContextLists( pHCIFrame );

   /* State is DISCONNECTED */
   static_PHCIFrameSetStateDisconnected( pHCIFrame );

   /* Flush DFC Queue */
   PNALDFCFlush( pBindingContext, P_DFC_TYPE_HCI_FRAME );

   PSHDLCPreReset( pBindingContext );
}

/**
 * -------------
 * Frame Connect
 * -------------
 * See header file
 **/
NFC_HAL_INTERNAL void PHCIFrameConnect(
         tNALBindingContext*                   pBindingContext,
         tPNALGenericCompletion* pCallbackFunction,
         void*                       pCallbackParameter,
         tPNALGenericCompletion* pReconnectIndicationFunction,
         void*                       pReconnectIndicationCallbackParameter)
{
   tHCIFrameInstance* pHCIFrame = PNALContextGetHCIFrameInstance( pBindingContext );

   /* Check hci state is not connected */
   CNALDebugAssert( ( static_PHCIFrameIsStateConnected( pHCIFrame) == W_FALSE ));

   pHCIFrame->pCallbackFunction  = pCallbackFunction;
   pHCIFrame->pCallbackParameter = pCallbackParameter;
   pHCIFrame->pReconnectIndicationFunction = pReconnectIndicationFunction;
   pHCIFrame->pReconnectIndicationCallbackParameter = pReconnectIndicationCallbackParameter;

   PSHDLCConnect(
      pBindingContext,
      static_PHCIFrameSHDLCConnectCompleted,  null,
      static_PHCIFrameSHDLCResetIndication,   null,
      static_PHCIFrameSHDLCWriteAcknowledged, null,
      static_PHCIFrameSHDLCDataReady,         null);
}

/**
 *   ===================================
 *   Frame WRITE
 *   ===================================
 **/

/**
 * ---------------------------
 * Frame Write Prepare Context
 * ---------------------------
 * See header file
 **/
NFC_HAL_INTERNAL void PHCIFrameWritePrepareContext(
         tNALBindingContext*                 pBindingContext,
         tHCIFrameWriteContext*    pWriteContext,
         uint8_t                   nPipeIdentifier,
         uint32_t                  nLength,
         tPHCIFrameWriteCompleted* pCallbackFunction,
         void*                     pCallbackParameter)
{
   pWriteContext->nPipeIdentifier    = nPipeIdentifier;
   pWriteContext->nLength            = nLength;
   pWriteContext->nCounter           = 0;
   pWriteContext->pCallbackFunction  = pCallbackFunction;
   pWriteContext->pCallbackParameter = pCallbackParameter;
   pWriteContext->pNext              = null;
}

/**
 * -----------
 * Frame Write
 * -----------
 * See header file
 **/
NFC_HAL_INTERNAL void PHCIFrameWrite(
         tNALBindingContext*              pBindingContext,
         tHCIFrameWriteContext* pWriteContext,
         uint8_t*               pBuffer)
{
   pWriteContext->pBuffer = pBuffer;

   /* add context element to the end of context list */
   static_PHCIFrameAddWriteContextToList( pWriteContext, &PNALContextGetHCIFrameInstance( pBindingContext )->pWriteContextListHead );

   /* Start Write */
   static_PHCIFrameWriteMachine( pBindingContext, 0 );
}

/**
 * -----------------
 * Frame Write Sream
 * -----------------
 * See header file
 **/
NFC_HAL_INTERNAL void PHCIFrameWriteStream(
         tNALBindingContext*              pBindingContext,
         tHCIFrameWriteContext* pWriteContext,
         tPHCIFrameGetData*     pGetDataCallbackFunction,
         void*                  pCallbackParameter )
{
   /* Null pointeur Buffer means Stream Buffer */
   pWriteContext->pBuffer = null;

   pWriteContext->pSreamCallbackFunction   = pGetDataCallbackFunction;
   pWriteContext->pStreamCallbackParameter = pCallbackParameter;

   /* add context element to the end of context list */
   static_PHCIFrameAddWriteContextToList(
      pWriteContext,
      &PNALContextGetHCIFrameInstance( pBindingContext )->pWriteContextListHead );

   /* Start Write */
   static_PHCIFrameWriteMachine( pBindingContext, 0 );
}

/**
 *   ===================================
 *   Frame READ
 *   ===================================
 **/

/**
 * -----------------
 * Frame Read Stream
 * -----------------
 * See header file
 **/
NFC_HAL_INTERNAL void PHCIFrameReadStream(
         tNALBindingContext*                   pBindingContext,
         tHCIFrameReadContext*       pReadContext,
         uint8_t                     nPipeIdentifier,
         tPHCIFrameReadDataReceived* pCallbackFunction,
         void*                       pCallbackParameter )
{
   tHCIFrameInstance* pHCIFrame     = PNALContextGetHCIFrameInstance( pBindingContext );

   pReadContext->nPipeIdentifier    = nPipeIdentifier;
   pReadContext->nBufferLength      = 0;
   pReadContext->nCounter           = 0;
   pReadContext->pCallbackFunction  = (tPHCIFrameReadCompleted*)pCallbackFunction;
   pReadContext->pCallbackParameter = pCallbackParameter;
   pReadContext->pNext              = null;

   pReadContext->pBuffer = null;

   /* add context element to the end of context list */
   static_PHCIFrameAddReadContextToList( &pHCIFrame->pReadContextListHead, pReadContext );

   /* The read state machine static_PHCIFrameReadMachine() will be invoked upon data recetion */
}

/**
 * ------------------------
 * Frame Cancel Read Stream
 * ------------------------
 * See header file
 **/
NFC_HAL_INTERNAL void PHCIFrameCancelReadStream(
         tNALBindingContext*             pBindingContext,
         tHCIFrameReadContext* pReadContext )
{
   static_PHCIFrameRemoveReadContextFromList(
      pReadContext,
      &PNALContextGetHCIFrameInstance( pBindingContext )->pReadContextListHead);
}

/**
 *   ===================================
 *   Frame STATISTICS
 *   ===================================
 **/

/**
 * --------------------
 * Frame GET Statistics
 * --------------------
 * See header file
 **/

NFC_HAL_INTERNAL void PHCIFrameGetStatistics(
         tNALBindingContext* pBindingContext,
         uint32_t* pnOSI5ReadMessageLost,
         uint32_t* pnOSI5ReadByteErrorCount )
{
   tHCIFrameInstance* pHCIFrame = PNALContextGetHCIFrameInstance( pBindingContext );

   *pnOSI5ReadMessageLost    = pHCIFrame->nReadMessageLost;
   *pnOSI5ReadByteErrorCount = pHCIFrame->nReadByteErrorCount;
}

/**
 * ----------------------
 * Frame RESET Statistics
 * ----------------------
 * See header file
 **/
NFC_HAL_INTERNAL void PHCIFrameResetStatistics(
         tNALBindingContext*   pBindingContext)
{
   tHCIFrameInstance* pHCIFrame = PNALContextGetHCIFrameInstance( pBindingContext );

   pHCIFrame->nReadMessageLost    = 0;
   pHCIFrame->nReadByteErrorCount = 0;
}

/**
 *   ===================================
 *   M I S C.
 *   ===================================
 **/

/**
 * Add Read Context to List
 **/
static void static_PHCIFrameAddReadContextToList(
         tHCIFrameReadContext** pListHead,
         tHCIFrameReadContext* pReadContext)
{
   if ( *pListHead == null )
   {
      *pListHead = pReadContext;
      return;
   }

   while (  (*pListHead)->pNext != null  )
   {
      pListHead = &(*pListHead)->pNext;
   }
   (*pListHead)->pNext = pReadContext;
}

/**
 * Find Read_Context From List Head
 **/
static tHCIFrameReadContext* static_PHCIFrameFindReadContextFromList(
         uint8_t               nPipeId,
         tHCIFrameReadContext* pReadContext )
{
   while ( pReadContext != null )
   {
      if ( nPipeId == pReadContext->nPipeIdentifier )
      {
         return pReadContext;
      }
      /* seek to next */
      pReadContext = pReadContext->pNext;
   }
   return null;
}

/**
 * Remove Read_Context from List
 **/
static void static_PHCIFrameRemoveReadContextFromList(
         tHCIFrameReadContext*  pReadContext,
         tHCIFrameReadContext** pListHead)
{
   CNALDebugAssert(pReadContext != null);

   if(*pListHead != null)
   {
      if (  *pListHead == pReadContext  )
      {
         *pListHead = pReadContext->pNext;
      }
      else
      {
         while ( (*pListHead)->pNext != pReadContext )
         {
            pListHead = &(*pListHead)->pNext;
         }
         (*pListHead)->pNext = pReadContext->pNext;
      }
   }
}

/**
 * Add Write Context to List
 **/
static void static_PHCIFrameAddWriteContextToList(
         tHCIFrameWriteContext*  pWriteContext,
         tHCIFrameWriteContext** pListHead)
{
   if ( *pListHead == null )
   {
      *pListHead = pWriteContext;
      return;
   }

   while (  (*pListHead)->pNext != null  )
   {
      pListHead = &(*pListHead)->pNext;
   }
   (*pListHead)->pNext = pWriteContext;
}

/**
 * Clean Context Lists
 **/
static void static_PHCIFrameCleanContextLists(
         tHCIFrameInstance* pHCIFrame)
{
   /* Clean up of the context list */
   pHCIFrame->pWriteContextListHead = null;
   pHCIFrame->pSentWriteContextListHead = null;
   pHCIFrame->pReadContextListHead =  null;
}

/**
 * Set State Disconnected
 **/
static void static_PHCIFrameSetStateDisconnected(
         tHCIFrameInstance* pHCIFrame)
{
   pHCIFrame->nState = HCI_FRAME_STATE_DISCONNECTED;
}

/**
 * Set State Connected
 **/
static void static_PHCIFrameSetStateConnected(
         tHCIFrameInstance* pHCIFrame)
{
   pHCIFrame->nState = HCI_FRAME_STATE_CONNECTED;
}

/**
 * Is State is Connected ?
 **/
static bool_t static_PHCIFrameIsStateConnected(
         tHCIFrameInstance* pHCIFrame)
{
   return ( pHCIFrame->nState == HCI_FRAME_STATE_CONNECTED ) ? W_TRUE : W_FALSE;
}
