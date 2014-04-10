/*
 * Copyright (c) 2007-2011 Inside Secure, All Rights Reserved.
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
   Contains the SHDLC Frame implementation.
*******************************************************************************/
#define P_MODULE  P_MODULE_DEC( S_FRM )

#include "nfc_hal_binding.h"

/* The read state machine status */
#define P_SHDLC_FRAME_READ_STATUS_IDLE               0
#define P_SHDLC_FRAME_READ_STATUS_WAITING_HEADER     1
#define P_SHDLC_FRAME_READ_STATUS_WAITING_BYTE       2
#define P_SHDLC_FRAME_READ_STATUS_WAITING_ESC_BYTE   3
#define P_SHDLC_FRAME_READ_STATUS_RESET_PENDING      4

/* The special byte values */
#define P_SHDLC_STX_HEADER    0x02
#define P_SHDLC_ETX_TRAILER   0x03
#define P_SHDLC_DLE_ESCAPE    0x10

/* See header file */
NFC_HAL_INTERNAL void PSHDLCFrameCreate(
         tSHDLCFrameInstance* pSHDLCFrame,
         tNALComInstance* pComPort,
         uint32_t nFrameType )
{
   CNALDebugAssert(pSHDLCFrame != null);
   CNALMemoryFill(pSHDLCFrame, 0, sizeof(tSHDLCFrameInstance));

   pSHDLCFrame->nReadStatus = P_SHDLC_FRAME_READ_STATUS_IDLE;

   pSHDLCFrame->pComPort = pComPort;
   pSHDLCFrame->nFrameType = nFrameType;

   if((pSHDLCFrame->nFrameType == P_COM_TYPE_NFCC_SHDLC_RXTX)
   || (pSHDLCFrame->nFrameType == P_COM_TYPE_NFC_HAL_SHDLC_RXTX))
   {
      pSHDLCFrame->nReadBufferSize = P_SHDLC_FRAME_READ_BUFFER_SIZE;
      if(pSHDLCFrame->nReadBufferSize > P_SHDLC_FRAME_MAX_BUFFER_SIZE)
      {
         pSHDLCFrame->nReadBufferSize = P_SHDLC_FRAME_MAX_BUFFER_SIZE;
      }
   }
   else
   {
      pSHDLCFrame->nReadBufferSize = P_SHDLC_FRAME_MAX_SIZE + 2;
   }
}

/* See header file */
NFC_HAL_INTERNAL void PSHDLCFrameDestroy(
         tSHDLCFrameInstance* pSHDLCFrame )
{
   if(pSHDLCFrame != null)
   {
      CNALMemoryFill(pSHDLCFrame, 0, sizeof(tSHDLCFrameInstance));
      pSHDLCFrame->nReadStatus = P_SHDLC_FRAME_READ_STATUS_IDLE;
   }
}

/* See header file */
NFC_HAL_INTERNAL void PSHDLCFramePreReset(
         tNALBindingContext* pBindingContext )
{
   tSHDLCFrameInstance* pSHDLCFrame = PNALContextGetSHDLCFrame(pBindingContext);
   tNALComInstance* pComPort;
   uint32_t nFrameType;
   uint32_t nReadBufferSize;
   uint32_t nReceptionCounter;

   CNALDebugAssert(pSHDLCFrame != null);

   /* Preserve the port and the type of protocol */
   pComPort = pSHDLCFrame->pComPort;
   nFrameType = pSHDLCFrame->nFrameType;
   nReadBufferSize = pSHDLCFrame->nReadBufferSize;

   /* Preserve the reception counter */
   nReceptionCounter = pSHDLCFrame->nSHDLCFrameReceptionCounter;

   PNALDFCFlush(pBindingContext, P_DFC_TYPE_SHDLC_FRAME);

   CNALMemoryFill(pSHDLCFrame, 0, sizeof(tSHDLCFrameInstance));

   /* restore the port and the type of protocol */
   pSHDLCFrame->pComPort = pComPort;
   pSHDLCFrame->nFrameType = nFrameType;
   pSHDLCFrame->nReadBufferSize = nReadBufferSize;

   /* restore the reception counter */
   pSHDLCFrame->nSHDLCFrameReceptionCounter = nReceptionCounter;

   pSHDLCFrame->nReadStatus = P_SHDLC_FRAME_READ_STATUS_RESET_PENDING;
}

/* See header file */
NFC_HAL_INTERNAL void PSHDLCFramePostReset(
         tNALBindingContext* pBindingContext )
{
   tSHDLCFrameInstance* pSHDLCFrame = PNALContextGetSHDLCFrame(pBindingContext);
   CNALDebugAssert(pSHDLCFrame != null);
   CNALDebugAssert(pSHDLCFrame->nReadStatus == P_SHDLC_FRAME_READ_STATUS_RESET_PENDING);

   pSHDLCFrame->nReadStatus = P_SHDLC_FRAME_READ_STATUS_IDLE;
}

/**
 * Implementes the write state machine.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  bDirectCall  Perform a direct call.
 **/
static void static_PSHDLCFrameWriteLoop(
         tNALBindingContext* pBindingContext,
         bool_t bDirectCall )
{
   tSHDLCFrameInstance* pSHDLCFrame = PNALContextGetSHDLCFrame(pBindingContext);
   uint8_t nLengthWritten;
   uint8_t nLengthToWrite;

   CNALDebugAssert(pSHDLCFrame != null);
   CNALDebugAssert(pSHDLCFrame->nWriteLength > pSHDLCFrame->nWritePosition);

   nLengthToWrite = pSHDLCFrame->nWriteLength - pSHDLCFrame->nWritePosition;

   if((nLengthWritten = (uint8_t)CNALComWriteBytes( pSHDLCFrame->pComPort,
      &pSHDLCFrame->aWriteFrameBuffer[pSHDLCFrame->nWritePosition],
      nLengthToWrite)) != 0)
   {
      CNALDebugAssert(nLengthWritten <= nLengthToWrite);

      pSHDLCFrame->nWritePosition = (uint8_t)(pSHDLCFrame->nWritePosition + nLengthWritten);

      if(pSHDLCFrame->nWritePosition == pSHDLCFrame->nWriteLength)
      {
         tPNALGenericCompletion* pWriteCallbackFunction = pSHDLCFrame->pWriteCallbackFunction;
         void* pWriteCallbackParameter = pSHDLCFrame->pWriteCallbackParameter;

         pSHDLCFrame->nWritePosition = 0;
         pSHDLCFrame->nWriteLength = 0;
         pSHDLCFrame->pWriteCallbackFunction = null;
         pSHDLCFrame->pWriteCallbackParameter = null;

         PNALDebugTrace("Write Completed");

         if(bDirectCall)
         {
            pWriteCallbackFunction(pBindingContext, pWriteCallbackParameter);
         }
         else
         {
            PNALDFCPost1(pBindingContext, P_DFC_TYPE_SHDLC_FRAME,
               pWriteCallbackFunction, pWriteCallbackParameter);
         }
      }
   }
}

void PSHDLCLogBuffer(
         tNALBindingContext* pBindingContext,
         uint8_t* pBuffer,
         uint32_t nLength,
         bool_t bFromNFCC);

/**
 * Implements the read state machine for RXTX.
 *
 * @param[in]  pBindingContext  The context
 *
 * @param[in]  pSHDLCFrame  The SHDLC Frame instance.
 *
 * @param[in]  bDirectCall  The direct call indicator.
 **/
static void static_PSHDLCFrameReadLoopRXTX(
         tNALBindingContext* pBindingContext,
         tSHDLCFrameInstance* pSHDLCFrame,
         bool_t bDirectCall )
{
   bool_t bError = W_FALSE;
   uint8_t nStatus;
   uint8_t nPosition;
   uint8_t nByteValue;
   uint32_t nByteCount;
   uint8_t* pReadBuffer;
   uint32_t nReadBufferCount;
   uint32_t nSHDLCFrameReceptionCounterToCall = 0;

   tPSHDLCFrameReadCompleted* pReadCallbackFunctionToCall = null;
   void* pReadCallbackParameterToCall = null;
   uint8_t* pReadBufferToCall = null;
   uint8_t nPositionToCall = 0;

   CNALDebugAssert(pSHDLCFrame != null);
   CNALDebugAssert(pSHDLCFrame->nReadStatus != P_SHDLC_FRAME_READ_STATUS_IDLE);

   nStatus = pSHDLCFrame->nReadStatus;
   nPosition = pSHDLCFrame->nReadPosition;
   nByteCount = 0;
   nReadBufferCount = pSHDLCFrame->nReadFrameBufferLength;
   pReadBuffer = pSHDLCFrame->aReadFrameBuffer;

   while(nStatus != P_SHDLC_FRAME_READ_STATUS_IDLE)
   {
      if(nReadBufferCount == 0)
      {
         pReadBuffer = pSHDLCFrame->aReadFrameBuffer;
         if( (nReadBufferCount = CNALComReadBytes(
            pSHDLCFrame->pComPort, pReadBuffer, pSHDLCFrame->nReadBufferSize)) == 0)
         {
            break;
         }
      }

      CNALDebugAssert(nReadBufferCount <= pSHDLCFrame->nReadBufferSize);

      nByteValue = *pReadBuffer++;
      nReadBufferCount--;
      nByteCount++;

      switch(nStatus)
      {
      case P_SHDLC_FRAME_READ_STATUS_WAITING_HEADER:
         CNALDebugAssert( nPosition == 0 );
         if( nByteValue != P_SHDLC_STX_HEADER )
         {
            /* Skipping non-header byte to re-synchronized */
            bError = W_TRUE;
         }
         else
         {
            nStatus = P_SHDLC_FRAME_READ_STATUS_WAITING_BYTE;
         }
         break;
      case P_SHDLC_FRAME_READ_STATUS_WAITING_BYTE:
         if( nByteValue == P_SHDLC_DLE_ESCAPE )
         {
            nStatus = P_SHDLC_FRAME_READ_STATUS_WAITING_ESC_BYTE;
         }
         else if( nByteValue == P_SHDLC_STX_HEADER )
         {
            /* Error detected, skip the frame */
            bError = W_TRUE;
         }
         else if( nByteValue == P_SHDLC_ETX_TRAILER )
         {
            uint8_t nCRC;

            if(nPosition < 2)
            {
               /* To few data, error skip the frame */
               bError = W_TRUE;
               break;
            }
            nCRC = pSHDLCFrame->pReadBuffer[--nPosition];
            pSHDLCFrame->nReadCRC ^= nCRC; /* Undo the last CRC */
            if( pSHDLCFrame->nReadCRC != nCRC)
            {
               /* CRC error, error skip the frame */
               bError = W_TRUE;
               break;
            }
            nStatus = P_SHDLC_FRAME_READ_STATUS_IDLE;

#ifdef P_NAL_TRACE_ACTIVE
#ifdef P_SHDLC_TRACE

            PSHDLCLogBuffer(pBindingContext, pSHDLCFrame->pReadBuffer, nPosition, W_TRUE);

#endif /* #ifdef P_SHDLC_TRACE */
#endif /* #ifdef P_NAL_TRACE_ACTIVE */

            pReadCallbackFunctionToCall = pSHDLCFrame->pReadCallbackFunction;
            pReadCallbackParameterToCall = pSHDLCFrame->pReadCallbackParameter;
            pReadBufferToCall = pSHDLCFrame->pReadBuffer;
            nPositionToCall = nPosition;

            /* Increment the counter but avoid a zero value */
            if((nSHDLCFrameReceptionCounterToCall = pSHDLCFrame->nSHDLCFrameReceptionCounter) == 0)
            {
               nSHDLCFrameReceptionCounterToCall = 1;
            }
            pSHDLCFrame->nSHDLCFrameReceptionCounter = nSHDLCFrameReceptionCounterToCall + 1;

            nPosition = 0;
            pSHDLCFrame->nReadCRC = 0;
            pSHDLCFrame->pReadBuffer = null;
            pSHDLCFrame->pReadCallbackFunction = null;
            pSHDLCFrame->pReadCallbackParameter = null;
         }
         else
         {
            if(nPosition == P_SHDLC_FRAME_MAX_SIZE + 1)
            {
               /* Error: too many bytes in the frame, skip it */
               bError = W_TRUE;
            }
            else
            {
               pSHDLCFrame->pReadBuffer[nPosition++] = nByteValue;
               pSHDLCFrame->nReadCRC ^= nByteValue;
            }
         }
         break;
      case P_SHDLC_FRAME_READ_STATUS_WAITING_ESC_BYTE:
         if((nByteValue != P_SHDLC_DLE_ESCAPE)
         && (nByteValue != P_SHDLC_STX_HEADER)
         && (nByteValue != P_SHDLC_ETX_TRAILER))
         {
            /* Error: Only special bytes should be escaped, skip the frame */
            bError = W_TRUE;
         }
         else
         {
            if(nPosition == P_SHDLC_FRAME_MAX_SIZE + 1)
            {
               /* Error: too many bytes in the frame, skip it */
               bError = W_TRUE;
            }
            else
            {
               pSHDLCFrame->pReadBuffer[nPosition++] = nByteValue;
               pSHDLCFrame->nReadCRC ^= nByteValue;

               nStatus = P_SHDLC_FRAME_READ_STATUS_WAITING_BYTE;
            }
         }
         break;

      default:
         PNALDebugError("static_PSHDLCFrameReadLoopRXTX: Wrong state");
         CNALDebugAssert(W_FALSE);
         return;
      } /* switch() */

      if(bError)
      {
         PNALDebugWarning("static_PSHDLCFrameReadLoopRXTX: Frame error detected");
         nPosition = 0;
         bError = W_FALSE;

         pSHDLCFrame->nReadCRC = 0;

         if( nByteValue == P_SHDLC_STX_HEADER )
         {
            nStatus = P_SHDLC_FRAME_READ_STATUS_WAITING_BYTE;
            nByteCount--;
         }
         else
         {
            nStatus = P_SHDLC_FRAME_READ_STATUS_WAITING_HEADER;
         }

         pSHDLCFrame->nOSI2FrameReadByteErrorCount += nByteCount;
         pSHDLCFrame->nOSI2FrameReadByteTotalCount += nByteCount;

         if( nByteValue == P_SHDLC_STX_HEADER )
         {
            nByteCount = 1;
         }
         else
         {
            nByteCount = 0;
         }
      }
   } /* while() */

   pSHDLCFrame->nOSI2FrameReadByteTotalCount += nByteCount;
   pSHDLCFrame->nReadStatus = nStatus;
   pSHDLCFrame->nReadPosition = nPosition;

   pSHDLCFrame->nReadFrameBufferLength = nReadBufferCount;
   if(nReadBufferCount != 0)
   {
      CNALMemoryMove(&pSHDLCFrame->aReadFrameBuffer, pReadBuffer, nReadBufferCount);
   }

   if(pReadCallbackFunctionToCall != null)
   {
      if(bDirectCall)
      {
         CNALDebugAssert(nSHDLCFrameReceptionCounterToCall != 0);

         pReadCallbackFunctionToCall(pBindingContext,
            pReadCallbackParameterToCall,
            pReadBufferToCall, nPositionToCall,
            nSHDLCFrameReceptionCounterToCall);
      }
      else
      {
         PNALDFCPost4( pBindingContext, P_DFC_TYPE_SHDLC_FRAME,
            pReadCallbackFunctionToCall,
            pReadCallbackParameterToCall,
            pReadBufferToCall, nPositionToCall,
            nSHDLCFrameReceptionCounterToCall);
      }
   }
}

/**
 * Implements the read state machine for Direct mode.
 *
 * @param[in]  pBindingContext  The context
 *
 * @param[in]  pSHDLCFrame  The SHDLC Frame instance.
 *
 * @param[in]  bDirectCall  The direct call indicator.
 **/
static void static_PSHDLCFrameReadLoopDirect(
         tNALBindingContext* pBindingContext,
         tSHDLCFrameInstance* pSHDLCFrame,
         bool_t bDirectCall )
{
   uint32_t nByteCount = 0;
   uint8_t* pReadBuffer;
   void* pReadCallbackParameterToCall = null;
   uint32_t nSHDLCFrameReceptionCounterToCall = 0;
   tPSHDLCFrameReadCompleted* pReadCallbackFunctionToCall = null;

   CNALDebugAssert(pSHDLCFrame != null);
   CNALDebugAssert(pSHDLCFrame->nReadStatus == P_SHDLC_FRAME_READ_STATUS_WAITING_HEADER);

   pReadBuffer = pSHDLCFrame->pReadBuffer;

   if( (nByteCount = CNALComReadBytes(
      pSHDLCFrame->pComPort, pReadBuffer, pSHDLCFrame->nReadBufferSize)) == 0)
   {
      /* no data availble */
      return;
   }

   pReadCallbackFunctionToCall = pSHDLCFrame->pReadCallbackFunction;
   pReadCallbackParameterToCall = pSHDLCFrame->pReadCallbackParameter;

   /*return to the idle stat*/
   pSHDLCFrame->nReadStatus = P_SHDLC_FRAME_READ_STATUS_IDLE;
   pSHDLCFrame->nOSI2FrameReadByteTotalCount += nByteCount;
   pSHDLCFrame->pReadBuffer = null;
   pSHDLCFrame->pReadCallbackFunction = null;
   pSHDLCFrame->pReadCallbackParameter = null;

   if((nSHDLCFrameReceptionCounterToCall = pSHDLCFrame->nSHDLCFrameReceptionCounter) == 0)
   {
      nSHDLCFrameReceptionCounterToCall = 1;
   }
   pSHDLCFrame->nSHDLCFrameReceptionCounter = nSHDLCFrameReceptionCounterToCall + 1;

   if(pReadCallbackFunctionToCall != null)
   {
      if(bDirectCall)
      {
         CNALDebugAssert(nSHDLCFrameReceptionCounterToCall != 0);

         pReadCallbackFunctionToCall(pBindingContext,
            pReadCallbackParameterToCall,
            pReadBuffer, nByteCount,
            nSHDLCFrameReceptionCounterToCall);
      }
      else
      {
         CNALDebugAssert(nSHDLCFrameReceptionCounterToCall != 0);
         PNALDFCPost4( pBindingContext, P_DFC_TYPE_SHDLC_FRAME,
            pReadCallbackFunctionToCall,
            pReadCallbackParameterToCall,
            pReadBuffer, nByteCount,
             nSHDLCFrameReceptionCounterToCall);
      }
   }
}

/**
 * Implements the read state machine for I2C.
 *
 * @param[in]  pBindingContext  The context
 *
 * @param[in]  pSHDLCFrame  The SHDLC Frame instance.
 *
 * @param[in]  bDirectCall  The direct call indicator.
 **/
static void static_PSHDLCFrameReadLoopI2C(
         tNALBindingContext* pBindingContext,
         tSHDLCFrameInstance* pSHDLCFrame,
         bool_t bDirectCall )
{
   uint32_t nReadBufferCount, nPosition;
   uint32_t nByteCount = 0;
   uint8_t* pReadBuffer;
   void* pReadCallbackParameterToCall = null;
   uint32_t nSHDLCFrameReceptionCounterToCall = 0;
   tPSHDLCFrameReadCompleted* pReadCallbackFunctionToCall = null;

   CNALDebugAssert(pSHDLCFrame != null);
   CNALDebugAssert(pSHDLCFrame->nReadStatus == P_SHDLC_FRAME_READ_STATUS_WAITING_HEADER);

   pReadBuffer = pSHDLCFrame->aReadFrameBuffer;

   do
   {
      /* Assumption: With I2C, the function CNALComReadBytes() returns one and only one complete frame */
      if( (nReadBufferCount = CNALComReadBytes(
         pSHDLCFrame->pComPort, pReadBuffer, pSHDLCFrame->nReadBufferSize)) == 0)
      {
         /* no data availble */
         break;
      }

      /* Skipping bytes to re-synchronized */
      if((nReadBufferCount >= 2) && (nReadBufferCount <= pSHDLCFrame->nReadBufferSize))
      {
         nByteCount = nReadBufferCount - 2;

         /* Check the size */
         if((pReadBuffer[0] == nByteCount) && (nByteCount <= P_SHDLC_FRAME_MAX_SIZE))
         {
            uint8_t nLRC = pReadBuffer[0]; /* include "LEN" in the CRC calculation */

            for(nPosition = 0; nPosition < nByteCount; nPosition++)
            {
               nLRC ^= (pSHDLCFrame->pReadBuffer[nPosition] = pReadBuffer[nPosition + 1]);
            }

            /* Check the LRC */
            if(pReadBuffer[nByteCount + 1] == nLRC)
            {
               pSHDLCFrame->nReadStatus = P_SHDLC_FRAME_READ_STATUS_IDLE;

               pReadCallbackFunctionToCall = pSHDLCFrame->pReadCallbackFunction;
               pReadCallbackParameterToCall = pSHDLCFrame->pReadCallbackParameter;
               pReadBuffer = pSHDLCFrame->pReadBuffer;

#ifdef P_NAL_TRACE_ACTIVE
#ifdef P_SHDLC_TRACE

            PSHDLCLogBuffer(pBindingContext, pReadBuffer, nByteCount, W_TRUE);

#endif /* #ifdef P_SHDLC_TRACE */
#endif /* #ifdef P_NAL_TRACE_ACTIVE */

               /* Increment the counter but avoid a zero value */
               if((nSHDLCFrameReceptionCounterToCall = pSHDLCFrame->nSHDLCFrameReceptionCounter) == 0)
               {
                  nSHDLCFrameReceptionCounterToCall = 1;
               }
               pSHDLCFrame->nSHDLCFrameReceptionCounter = nSHDLCFrameReceptionCounterToCall + 1;

               pSHDLCFrame->pReadBuffer = null;
               pSHDLCFrame->pReadCallbackFunction = null;
               pSHDLCFrame->pReadCallbackParameter = null;

               CNALDebugAssert(pReadCallbackFunctionToCall != null);
            }
         }
      }

      pSHDLCFrame->nOSI2FrameReadByteTotalCount += nByteCount;

      if(pReadCallbackFunctionToCall == null)
      {
         PNALDebugWarning("static_PSHDLCFrameReadLoopI2C: Frame error detected");
         pSHDLCFrame->nOSI2FrameReadByteErrorCount += nByteCount;
      }
   }
   while(pReadCallbackFunctionToCall == null);

   if(pReadCallbackFunctionToCall != null)
   {
      if(bDirectCall)
      {
         CNALDebugAssert(nSHDLCFrameReceptionCounterToCall != 0);

         pReadCallbackFunctionToCall(pBindingContext,
            pReadCallbackParameterToCall,
            pReadBuffer, nByteCount,
            nSHDLCFrameReceptionCounterToCall);
      }
      else
      {
         PNALDFCPost4( pBindingContext, P_DFC_TYPE_SHDLC_FRAME,
            pReadCallbackFunctionToCall,
            pReadCallbackParameterToCall,
            pReadBuffer, nByteCount,
            nSHDLCFrameReceptionCounterToCall);
      }
   }
}

/* See header file */
NFC_HAL_INTERNAL void PSHDLCFrameWrite(
         tNALBindingContext* pBindingContext,
         uint8_t* pBuffer,
         uint32_t nLength,
         tPNALGenericCompletion* pCallbackFunction,
         void* pCallbackParameter )
{
   tSHDLCFrameInstance* pSHDLCFrame = PNALContextGetSHDLCFrame(pBindingContext);
   uint8_t nLRC = 0;
   uint32_t nIndex;
   uint8_t* pWriteBuffer;

   CNALDebugAssert(pSHDLCFrame != null);
   CNALDebugAssert(pBuffer != null);
   CNALDebugAssert(pCallbackFunction != null);
   CNALDebugAssert((nLength >= 1) && (nLength <= P_SHDLC_FRAME_MAX_SIZE));

   CNALDebugAssert(pSHDLCFrame->nWritePosition == 0);
   CNALDebugAssert(pSHDLCFrame->nWriteLength == 0);
   CNALDebugAssert(pSHDLCFrame->pWriteCallbackFunction == null);
   CNALDebugAssert(pSHDLCFrame->pWriteCallbackParameter == null);

   pSHDLCFrame->pWriteCallbackFunction = pCallbackFunction;
   pSHDLCFrame->pWriteCallbackParameter = pCallbackParameter;

   PNALDebugTrace("PSHDLCFrameWrite ( callback-%p( %p) ):", pCallbackFunction, pCallbackParameter);

#ifdef P_NAL_TRACE_ACTIVE
#ifdef P_SHDLC_TRACE

   PSHDLCLogBuffer(pBindingContext, pBuffer, nLength, W_FALSE);

#endif /* #ifdef P_SHDLC_TRACE */
#endif /* #ifdef P_NAL_TRACE_ACTIVE */

   pWriteBuffer = pSHDLCFrame->aWriteFrameBuffer;

   if((pSHDLCFrame->nFrameType == P_COM_TYPE_NFCC_SHDLC_RXTX)
   || (pSHDLCFrame->nFrameType == P_COM_TYPE_NFC_HAL_SHDLC_RXTX))
   {
      *pWriteBuffer++ = P_SHDLC_STX_HEADER;

      for(nIndex = 0; nIndex < nLength; nIndex++)
      {
         uint8_t nValue = pBuffer[nIndex];
         if((nValue == P_SHDLC_STX_HEADER)
         || (nValue == P_SHDLC_ETX_TRAILER)
         || (nValue == P_SHDLC_DLE_ESCAPE))
         {
            *pWriteBuffer++ = P_SHDLC_DLE_ESCAPE;
         }

         *pWriteBuffer++ = nValue;
         nLRC ^= nValue;
      }

      if((nLRC == P_SHDLC_STX_HEADER)
      || (nLRC == P_SHDLC_ETX_TRAILER)
      || (nLRC == P_SHDLC_DLE_ESCAPE))
      {
         *pWriteBuffer++ = P_SHDLC_DLE_ESCAPE;
      }

      *pWriteBuffer++ = nLRC;

      *pWriteBuffer++ = P_SHDLC_ETX_TRAILER;
   }
   else if(pSHDLCFrame->nFrameType == P_COM_TYPE_NFC_HAL_SHDLC_DIRECT)
   {
      CNALMemoryCopy(pWriteBuffer,pBuffer,nLength);
      pWriteBuffer=pWriteBuffer+nLength;
   }
   else{
      *pWriteBuffer++ = (uint8_t)nLength;

      nLRC = (uint8_t)nLength; /* include "LEN" in the CRC calculation */

      for(nIndex = 0; nIndex < nLength; nIndex++)
      {
         uint8_t nValue = pBuffer[nIndex];

         *pWriteBuffer++ = nValue;
         nLRC ^= nValue;
      }

      *pWriteBuffer++ = nLRC;
   }

   pSHDLCFrame->nWriteLength = (uint8_t)(pWriteBuffer - pSHDLCFrame->aWriteFrameBuffer);

   static_PSHDLCFrameWriteLoop(pBindingContext, W_FALSE);
}

/* See header file */
NFC_HAL_INTERNAL void PSHDLCFrameRead(
         tNALBindingContext* pBindingContext,
         uint8_t* pBuffer,
         tPSHDLCFrameReadCompleted* pCallbackFunction,
         void* pCallbackParameter )
{
   tSHDLCFrameInstance* pSHDLCFrame = PNALContextGetSHDLCFrame(pBindingContext);
   CNALDebugAssert(pSHDLCFrame != null);
   CNALDebugAssert(pSHDLCFrame->nReadStatus == P_SHDLC_FRAME_READ_STATUS_IDLE);
   CNALDebugAssert(pBuffer != null);
   CNALDebugAssert(pCallbackFunction != null);

   CNALDebugAssert(pSHDLCFrame->nReadCRC == 0);
   CNALDebugAssert(pSHDLCFrame->nReadPosition == 0);
   CNALDebugAssert(pSHDLCFrame->pReadBuffer == null);
   CNALDebugAssert(pSHDLCFrame->pReadCallbackFunction == null);
   CNALDebugAssert(pSHDLCFrame->pReadCallbackParameter == null);

   pSHDLCFrame->nReadStatus = P_SHDLC_FRAME_READ_STATUS_WAITING_HEADER;

   pSHDLCFrame->pReadBuffer = pBuffer;
   pSHDLCFrame->pReadCallbackFunction = pCallbackFunction;
   pSHDLCFrame->pReadCallbackParameter = pCallbackParameter;

   PNALDebugTrace("PSHDLCFrameRead ( callback-%p( %p) )", pCallbackFunction, pCallbackParameter);

   if((pSHDLCFrame->nFrameType == P_COM_TYPE_NFCC_SHDLC_RXTX)
   || (pSHDLCFrame->nFrameType == P_COM_TYPE_NFC_HAL_SHDLC_RXTX))
   {
      static_PSHDLCFrameReadLoopRXTX(pBindingContext, pSHDLCFrame, W_FALSE);
   }
   else if(pSHDLCFrame->nFrameType == P_COM_TYPE_NFC_HAL_SHDLC_DIRECT)
   {
      static_PSHDLCFrameReadLoopDirect(pBindingContext, pSHDLCFrame, W_FALSE);
   }
   else
   {
      static_PSHDLCFrameReadLoopI2C(pBindingContext, pSHDLCFrame, W_FALSE);
   }

   /* Avoid incoming frame overflow : do not prioritize reception */
   if(pSHDLCFrame->nWriteLength != 0)
   {
      static_PSHDLCFrameWriteLoop(pBindingContext, W_FALSE);
   }
}

/* See header file */
NFC_HAL_INTERNAL void PSHDLCFramePoll(
         tNALBindingContext* pBindingContext )
{
   tSHDLCFrameInstance* pSHDLCFrame = PNALContextGetSHDLCFrame(pBindingContext);
   CNALDebugAssert(pSHDLCFrame != null);

   if(pSHDLCFrame->nReadStatus != P_SHDLC_FRAME_READ_STATUS_RESET_PENDING)
   {
      if(pSHDLCFrame->nReadStatus != P_SHDLC_FRAME_READ_STATUS_IDLE)
      {
         if((pSHDLCFrame->nFrameType == P_COM_TYPE_NFCC_SHDLC_RXTX)
         || (pSHDLCFrame->nFrameType == P_COM_TYPE_NFC_HAL_SHDLC_RXTX))
         {
            static_PSHDLCFrameReadLoopRXTX(pBindingContext, pSHDLCFrame, W_FALSE);
         }
         else if(pSHDLCFrame->nFrameType == P_COM_TYPE_NFC_HAL_SHDLC_DIRECT)
         {
            static_PSHDLCFrameReadLoopDirect(pBindingContext, pSHDLCFrame, W_FALSE);
            return;
         }
         else
         {
            static_PSHDLCFrameReadLoopI2C(pBindingContext, pSHDLCFrame, W_FALSE);
         }
      }

      if(pSHDLCFrame->nWriteLength != 0)
      {
         static_PSHDLCFrameWriteLoop(pBindingContext, W_FALSE);
      }
   }
}

/* See header file */
NFC_HAL_INTERNAL void PSHDLCFrameGetStatistics(
         tNALBindingContext* pBindingContext,
         uint32_t* pnOSI2FrameReadByteErrorCount,
         uint32_t* pnOSI2FrameReadByteTotalCount,
         uint32_t* pnOSI2FrameWriteByteTotalCount)
{
   tSHDLCFrameInstance* pSHDLCFrame = PNALContextGetSHDLCFrame(pBindingContext);
   CNALDebugAssert(pSHDLCFrame != null);

   *pnOSI2FrameReadByteErrorCount = pSHDLCFrame->nOSI2FrameReadByteErrorCount;
   *pnOSI2FrameReadByteTotalCount = pSHDLCFrame->nOSI2FrameReadByteTotalCount;
   *pnOSI2FrameWriteByteTotalCount = pSHDLCFrame->nOSI2FrameWriteByteTotalCount;
}

/* See header file */
NFC_HAL_INTERNAL void PSHDLCFrameResetStatistics(
         tNALBindingContext* pBindingContext)
{
   tSHDLCFrameInstance* pSHDLCFrame = PNALContextGetSHDLCFrame(pBindingContext);
   CNALDebugAssert(pSHDLCFrame != null);

   pSHDLCFrame->nOSI2FrameReadByteErrorCount = 0;
   pSHDLCFrame->nOSI2FrameReadByteTotalCount = 0;
   pSHDLCFrame->nOSI2FrameWriteByteTotalCount = 0;
}

