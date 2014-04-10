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

#define P_MODULE  P_MODULE_DEC( SHDLC )

#include "nfc_hal_binding.h"

/* Comment this line to activate the piggy backing */
/* #define P_SHDLC_NO_PIGGY_BACKING */

/* SHDLC Timeout values in ms */
#define TIMER_T1_TIMEOUT                     200
#define TIMER_T2_TIMEOUT                     50
#define TIMER_T3_TIMEOUT                     50
#define TIMER_T4_TIMEOUT                     5

#define TIMER_T6_TIMEOUT                     2

/* Connect state-machine status types */
#define CONNECT_STATUS_INIT                  0
#define CONNECT_STATUS_SEND_RESET_PENDING    1
#define CONNECT_STATUS_RESET_SENT            2
#define CONNECT_STATUS_SEND_UA_PENDING       3
#define CONNECT_STATUS_UA_SENT               4
#define CONNECT_STATUS_RESET_PENDING         5

/* The frame values and masks */
#define FRAME_NONE            0x00  /* Meaning: "no frame" */
#define FRAME_UA              (0xE0 | 0x06)
#define FRAME_RST             (0xE0 | 0x19)
#define FRAME_RR_MASK         (0xC0 | 0x00)
#define FRAME_RNR_MASK        (0xC0 | 0x10)
#define FRAME_REJ_MASK        (0xC0 | 0x08)

/* The capability flags */
#define FLAG_CAPABILITY_SREJ_SUPPORTED    0x02

/* The default window size defined in the standard */
#define DEFAULT_WINDOW_SIZE         4

#if ((P_SHDLC_MAX_WINDOW_SIZE * (PSHDLC_PAYLOAD_MAX_SIZE + 1)) > P_SHDLC_READ_BUFFER_SIZE)
#error The read buffer should be able to contain the full window size
#endif

#define READ_CTRL_FRAME_NONE    0  /* meaning: no control frame to send */
#define READ_CTRL_FRAME_RNR     FRAME_RNR_MASK  /* with a S-RNR frame */
#define READ_CTRL_FRAME_ACK     FRAME_RR_MASK   /* with a S-RR frame or a I-frame */
#define READ_CTRL_FRAME_REJ     FRAME_REJ_MASK  /* with a S-REJ frame */
#define READ_CTRL_FRAME_UA      FRAME_UA        /* with a U-UA frame */
#define READ_CTRL_FRAME_RST     FRAME_RST

/* Forward declarations */
static void static_PSHDLCConnectTimerCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter);

static void static_PSHDLCWritePiggyBackTimerCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter);

static void static_PSHDLCRRFrameAlreadySentTimer(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter);

static void static_PSHDLCConnectSendFrame(
         tNALBindingContext* pBindingContext,
         uint8_t nFrame );

static void static_PSHDLCConnectReadCompleted(
         tNALBindingContext* pBindingContext,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nSHDLCFrameReceptionCounter);

static void static_PSHDLCReadReadCompleted(
         tNALBindingContext* pBindingContext,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nSHDLCFrameReceptionCounter);

static bool_t static_PSHDLCWriteReceptionCtrlFrame(
         tNALBindingContext* pBindingContext,
         uint8_t nCtrlFrame,
         uint32_t nSHDLCFrameReceptionCounter);

static void static_PSHDLCWriteWriteFrameCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter);

static void static_PSHDLCWriteTransmit(
         tNALBindingContext* pBindingContext,
         tSHDLCInstance* pSHDLCStack,
         bool_t bForceAck);

#ifdef P_NAL_TRACE_ACTIVE
static void static_PSHDLCWriteTraceStatus(
                  tSHDLCInstance* pSHDLCStack,
                  const char* pMessage)
{
   PNALDebugTrace(pMessage);
   PNALDebugTrace("     nWriteNextToSendFrameId  = 0x%08X (%d)",
      pSHDLCStack->nWriteNextToSendFrameId, pSHDLCStack->nWriteNextToSendFrameId & 0x07);
   PNALDebugTrace("     nWriteNextWindowFrameId  = 0x%08X (%d)",
      pSHDLCStack->nWriteNextWindowFrameId, pSHDLCStack->nWriteNextWindowFrameId & 0x07);
   PNALDebugTrace("     nWriteLastAckSentFrameId = 0x%08X (%d)",
      pSHDLCStack->nWriteLastAckSentFrameId, pSHDLCStack->nWriteLastAckSentFrameId & 0x07);
   PNALDebugTrace("     nWindowSize            = %d", (int) pSHDLCStack->nWindowSize);
   PNALDebugTrace("     bIsWriteFramePending   = %d", (int) pSHDLCStack->bIsWriteFramePending);
   PNALDebugTrace("     bIsWriteOtherSideReady = %d", (int) pSHDLCStack->bIsWriteOtherSideReady);
   PNALDebugTrace("     bIsWriteBufferFull     = %d", (int) pSHDLCStack->bIsWriteBufferFull);

   CNALDebugAssert( pSHDLCStack->nWriteNextToSendFrameId >= pSHDLCStack->nWriteNextWindowFrameId);
   CNALDebugAssert( pSHDLCStack->nWriteNextWindowFrameId > pSHDLCStack->nWriteLastAckSentFrameId);
   CNALDebugAssert( (uint32_t)(pSHDLCStack->nWriteNextToSendFrameId - pSHDLCStack->nWriteLastAckSentFrameId) <=
                 (uint32_t)(pSHDLCStack->nWindowSize + 1) );
}
static char* static_PSHDLCReadGetNextCtrlFrame(uint8_t nCtrlFrame)
{
   switch(nCtrlFrame)
   {
   case READ_CTRL_FRAME_NONE:
      return "NONE";
   case READ_CTRL_FRAME_RNR:
      return "RNR";
   case READ_CTRL_FRAME_ACK:
      return "ACK";
   case READ_CTRL_FRAME_REJ:
      return "REJ";
   case READ_CTRL_FRAME_UA:
      return "UA";
   default:
      return "????????";
   }
}
static void static_PSHDLCReadTraceStatus(
                  tSHDLCInstance* pSHDLCStack,
                  const char* pMessage)
{
   PNALDebugTrace(pMessage);
   PNALDebugTrace("     nReadNextPositionToRead      = 0x%08X (%d)",
      pSHDLCStack->nReadNextPositionToRead, pSHDLCStack->nReadNextPositionToRead & 0x07);
   PNALDebugTrace("     nReadNextToReceivedFrameId   = 0x%08X (%d)",
      pSHDLCStack->nReadNextToReceivedFrameId, pSHDLCStack->nReadNextToReceivedFrameId & 0x07);
   PNALDebugTrace("     nReadLastAcknowledgedFrameId = 0x%08X (%d)",
      pSHDLCStack->nReadLastAcknowledgedFrameId, pSHDLCStack->nReadLastAcknowledgedFrameId & 0x07);
   PNALDebugTrace("     nWindowSize          = %d", pSHDLCStack->nWindowSize);
   PNALDebugTrace("     bIsReadBufferFull    = %d", (int) pSHDLCStack->bIsReadBufferFull);
   PNALDebugTrace("     nNextCtrlFrameToSend = %s",
      static_PSHDLCReadGetNextCtrlFrame(pSHDLCStack->nNextCtrlFrameToSend));
}
#else /* #ifdef P_NAL_TRACE_ACTIVE */
#define static_PSHDLCWriteTraceStatus(pSHDLCStack, pMessage) while(0) {}
#define static_PSHDLCReadTraceStatus(pSHDLCStack, pMessage) while(0) {}
#endif /* #ifdef P_NAL_TRACE_ACTIVE */

/**
 * Resets the state-machine state variables.
 *
 * @param[in]  pSHDLCStack  The stack instance.
 *
 * @param[in]  nWindowSize  The window size.
 **/
static P_NAL_INLINE void static_PSHDLCConnectState(
         tSHDLCInstance* pSHDLCStack,
         uint8_t nWindowSize )
{
   pSHDLCStack->nWindowSize = nWindowSize;

   /* Reset the connectiion */
   pSHDLCStack->nReadNextToReceivedFrameId = 0x20;
   pSHDLCStack->nReadLastAcknowledgedFrameId = 0x1F;
   pSHDLCStack->nReadT1Timeout = (TIMER_T1_TIMEOUT * nWindowSize)>>2;
   pSHDLCStack->nWriteLastAckSentFrameId = 0x1F;
   pSHDLCStack->nWriteNextToSendFrameId = 0x20;
   pSHDLCStack->nWriteNextWindowFrameId = 0x20;

   pSHDLCStack->bIsWriteOtherSideReady = W_TRUE;
}

/**
 * Shifts the counter values to avoid an overflow.
 *
 * @param[in]  pSHDLCStack  The stack instance.
 **/
static void static_PSHDLCConnectAvoidCounterSpin(
         tSHDLCInstance* pSHDLCStack )
{
   pSHDLCStack->nReadNextToReceivedFrameId += 0x20;
   pSHDLCStack->nReadLastAcknowledgedFrameId += 0x20;
   pSHDLCStack->nWriteLastAckSentFrameId += 0x20;
   pSHDLCStack->nWriteNextToSendFrameId += 0x20;
   pSHDLCStack->nWriteNextWindowFrameId += 0x20;
}

/**
 * Fills the SHDLC instance structure.
 *
 * See the function PSHDLCCreate.
 **/
static void static_PSHDLCCreate(
         tSHDLCInstance* pSHDLCStack )
{
   CNALDebugAssert(pSHDLCStack != null);

   CNALMemoryFill(pSHDLCStack, 0, sizeof(tSHDLCInstance));

   pSHDLCStack->nNextCtrlFrameToSend = READ_CTRL_FRAME_NONE;

   static_PSHDLCConnectState(pSHDLCStack, P_SHDLC_MAX_WINDOW_SIZE);

   /* Reset state-machine */
   pSHDLCStack->nConnectStatus = CONNECT_STATUS_INIT;
   pSHDLCStack->nConnectNextFrameToSend = FRAME_NONE;

   /* Read state-machine */
   pSHDLCStack->bIsReadBufferFull = W_FALSE;

   /* Write state-machine */
   pSHDLCStack->bIsWriteFramePending = W_FALSE;
   pSHDLCStack->bIsWriteBufferFull = W_FALSE;
   pSHDLCStack->bIsRRFrameAlreadySent = W_FALSE;
}

/* See header file */
NFC_HAL_INTERNAL void PSHDLCCreate(
         tSHDLCInstance* pSHDLCStack )
{
   static_PSHDLCCreate(pSHDLCStack );
}

/* See header file */
NFC_HAL_INTERNAL void PSHDLCDestroy(
         tSHDLCInstance* pSHDLCStack )
{
   if(pSHDLCStack != null)
   {
      CNALMemoryFill(pSHDLCStack, 0, sizeof(tSHDLCInstance));
   }
}

/* See header file */
NFC_HAL_INTERNAL void PSHDLCPreReset(
         tNALBindingContext* pBindingContext )
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);

   CNALDebugAssert(pSHDLCStack != null );

   PSHDLCFramePreReset(pBindingContext);
   PNALMultiTimerCancel( pBindingContext, TIMER_T1_SHDLC_ACK );
   PNALMultiTimerCancel( pBindingContext, TIMER_T2_SHDLC_RESEND );
   PNALMultiTimerCancel( pBindingContext, TIMER_T3_SHDLC_RST );
   PNALMultiTimerCancel( pBindingContext, TIMER_T4_SHDLC_PIGGYBACK );

   static_PSHDLCCreate(pSHDLCStack );

   pSHDLCStack->nConnectStatus = CONNECT_STATUS_RESET_PENDING;
}

/**
 * Receives the read completed event for the reset state-machine.
 **/
static void static_PSHDLCGenericReadCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nSHDLCFrameReceptionCounter)
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);

   CNALDebugAssert(pSHDLCStack != null );
   CNALDebugAssert(nLength >= 1);
   CNALDebugAssert(nLength <= P_SHDLC_FRAME_MAX_SIZE);

   if(pSHDLCStack->nConnectStatus == CONNECT_STATUS_UA_SENT)
   {
      static_PSHDLCReadReadCompleted( pBindingContext, pBuffer, nLength, nSHDLCFrameReceptionCounter);
   }
   else
   {
      static_PSHDLCConnectReadCompleted( pBindingContext, pBuffer, nLength, nSHDLCFrameReceptionCounter);
   }

   PSHDLCFrameRead(
         pBindingContext,
         pSHDLCStack->aReceptionBuffer,
         &static_PSHDLCGenericReadCompleted,
         null );
}

/**
 * Checks for the reception of a RST frame.
 *
 * If the buffer contains a valid RST frame, the connection is reset
 * and the function returns W_TRUE.
 *
 * @param[in]  pSHDLCStack  The stack instance.
 *
 * @param[in]  pBuffer  The reception buffer.
 *
 * @param[in]  nLength  The length in bytes of the data in the buffer.
 *
 * @return  W_TRUE if the buffer contains a valid RST frame, W_FALSE otherwise.
 **/
static bool_t static_PSHDLCConnectCheckRSTFrame(
         tSHDLCInstance* pSHDLCStack,
         uint8_t* pBuffer,
         uint32_t nLength)
{
   CNALDebugAssert(pSHDLCStack != null );
   CNALDebugAssert(nLength >= 1);

   if((nLength <= 3) && (pBuffer[0] == FRAME_RST))
   {
      uint8_t nWindowSize = DEFAULT_WINDOW_SIZE;

      if(nLength == 3)
      {
         uint8_t nFlags = pBuffer[2];
         if((nFlags & 0xFC) != 0)
         {
            /* Error in the frame format */
            return W_FALSE;
         }
         else if((nFlags & FLAG_CAPABILITY_SREJ_SUPPORTED) != 0)
         {
            /* SREJ is not supported */
            return W_FALSE;
         }
      }

      if(nLength >= 2)
      {
         nWindowSize = pBuffer[1];
         if(nWindowSize == 0)
         {
            /* Error in the frame format */
            return W_FALSE;
         }
      }

      if(nWindowSize <= P_SHDLC_MAX_WINDOW_SIZE)
      {
         static_PSHDLCConnectState(pSHDLCStack, nWindowSize);
         return W_TRUE;
      }
   }

   return W_FALSE;
}

/**
 * Receives the read completed event for the reset state-machine.
 **/
static void static_PSHDLCConnectReadCompleted(
         tNALBindingContext* pBindingContext,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nSHDLCFrameReceptionCounter)
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);
   tSHDLCFrameInstance* pSHDLCFrame = PNALContextGetSHDLCFrame(pBindingContext);
   CNALDebugAssert(pSHDLCStack != null );
   CNALDebugAssert(nLength >= 1);

   if((nLength == 1) && (pBuffer[0] == FRAME_UA))
   {
     /* avoid informing upper layer of the establishment of the SHDLC link if a xmit is still in progress */
     if (pSHDLCFrame->nWriteLength == 0) {
        pSHDLCStack->nConnectStatus = CONNECT_STATUS_UA_SENT;

        PNALMultiTimerCancel( pBindingContext, TIMER_T3_SHDLC_RST );

        PNALDFCPost1( pBindingContext, P_DFC_TYPE_SHDLC,
          pSHDLCStack->pConnectCallbackFunction,
          pSHDLCStack->pConnectCallbackParameter);

        pSHDLCStack->pConnectCallbackFunction = null;
        pSHDLCStack->pConnectCallbackParameter = null;
     }
   }
   else if(static_PSHDLCConnectCheckRSTFrame(pSHDLCStack, pBuffer, nLength))
   {
      /* static_PSHDLCConnectSendFrame(pBindingContext, FRAME_UA); */

      static_PSHDLCConnectSendFrame(pBindingContext, FRAME_RST);
   }
}

/**
 * Receives the write completed event for the reset state-machine.
 **/
static void static_PSHDLCConnectWriteFrameCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter)
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);

   CNALDebugAssert(pSHDLCStack != null );

   /* UA Already recived from NFC Controller ? */
   if( pSHDLCStack->nConnectStatus == CONNECT_STATUS_UA_SENT )
   {
      return;
   }

   CNALDebugAssert( ( pSHDLCStack->nConnectStatus == CONNECT_STATUS_SEND_RESET_PENDING ) ||
        ( pSHDLCStack->nConnectStatus == CONNECT_STATUS_SEND_UA_PENDING ) );

   if( pSHDLCStack->nConnectStatus == CONNECT_STATUS_SEND_RESET_PENDING )
   {
      pSHDLCStack->nConnectStatus = CONNECT_STATUS_RESET_SENT;
   }
   else
   {
      pSHDLCStack->nConnectStatus = CONNECT_STATUS_UA_SENT;
   }

   if(pSHDLCStack->nConnectNextFrameToSend == FRAME_NONE)
   {
      if( pSHDLCStack->nConnectStatus == CONNECT_STATUS_UA_SENT )
      {
         PNALMultiTimerCancel( pBindingContext, TIMER_T3_SHDLC_RST );

         PNALDFCPost1( pBindingContext, P_DFC_TYPE_SHDLC,
            pSHDLCStack->pConnectCallbackFunction,
            pSHDLCStack->pConnectCallbackParameter);

         pSHDLCStack->pConnectCallbackFunction = null;
         pSHDLCStack->pConnectCallbackParameter = null;
      }
   }
   else
   {
      uint8_t nNextFrame = pSHDLCStack->nConnectNextFrameToSend;
      pSHDLCStack->nConnectNextFrameToSend = FRAME_NONE;
      static_PSHDLCConnectSendFrame(pBindingContext, nNextFrame);
   }
}

/**
 * Generic send function for the RST and UA frames
 *
 * @param[in]  pSHDLCStack  The stack instance
 *
 * @param[in]  nFrame  The frame value RST or UA
 **/
static void static_PSHDLCConnectSendFrame(
         tNALBindingContext* pBindingContext,
         uint8_t nFrame )
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);
   uint32_t nLength;

   CNALDebugAssert( pSHDLCStack != null );
   CNALDebugAssert((nFrame == FRAME_RST) || (nFrame == FRAME_UA));

   if ( ( pSHDLCStack->nConnectStatus == CONNECT_STATUS_SEND_RESET_PENDING ) ||
        ( pSHDLCStack->nConnectStatus == CONNECT_STATUS_SEND_UA_PENDING ) )
   {
      /* Prepare a reset frame to be sent */
      pSHDLCStack->nConnectNextFrameToSend = nFrame;
   }
   else
   {
      if ( nFrame == FRAME_RST )
      {
         /* RST is next */
         pSHDLCStack->nConnectStatus = CONNECT_STATUS_SEND_RESET_PENDING;

         /* Set Timer T3 */
         PNALMultiTimerSet( pBindingContext,
            TIMER_T3_SHDLC_RST, TIMER_T3_TIMEOUT,
            &static_PSHDLCConnectTimerCompleted,
            null );

         pSHDLCStack->aSendBuffer[0] = FRAME_RST;

        /* The window size and the capabilities are optional but it cleaner to send
         * them. The other side may not handle the default values.
         * In the final SHDLC specification, SREJ is not supported so we don't
         * need to send the option byte
         */
#ifdef HCI_SWP
         /* Reply 2 always... 1 not suported*/
         pSHDLCStack->aSendBuffer[1] = 2;
#else
         pSHDLCStack->aSendBuffer[1] = pSHDLCStack->nWindowSize;
#endif
         /* pSHDLCStack->aSendBuffer[2] = 0; */

         nLength = 2;
      }
      else
      {
         /* Sending UA Frame */
         pSHDLCStack->nConnectStatus = CONNECT_STATUS_SEND_UA_PENDING;

         pSHDLCStack->aSendBuffer[0] = FRAME_UA;

         nLength = 1;
      }

     PSHDLCFrameWrite(
         pBindingContext,
         pSHDLCStack->aSendBuffer, nLength,
         &static_PSHDLCConnectWriteFrameCompleted,
         null );
   }
}

/**
 * Receives the timer expired event for the reset state-machine.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pCallbackParameter  not used.
 **/
static void static_PSHDLCConnectTimerCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter)
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);

   PSHDLCFramePreReset(pBindingContext);
   PSHDLCFramePostReset(pBindingContext);

   PSHDLCFrameRead(
         pBindingContext,
         pSHDLCStack->aReceptionBuffer,
         &static_PSHDLCGenericReadCompleted,
         null );

   pSHDLCStack->nConnectStatus = CONNECT_STATUS_INIT;
   static_PSHDLCConnectSendFrame( pBindingContext, FRAME_RST );
}

/* See header file */
NFC_HAL_INTERNAL void PSHDLCConnect(
         tNALBindingContext* pBindingContext,
         tPSHDLCConnectCompleted* pCallbackFunction,
         void* pCallbackParameter,
         tPSHDLCResetIndication* pResetIndication,
         void* pResetIndicationCallbackParameter,
         tPSHDLCWriteAcknowledged* pWriteAcknowledgedHandler,
         void* pWriteHandlerParameter,
         tPSHDLCDataReady* pDataReadyHandler,
         void* pDataReadyHandlerParameter )
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);
   CNALDebugAssert(pSHDLCStack != null);

   if(pSHDLCStack->nConnectStatus == CONNECT_STATUS_RESET_PENDING )
   {
      PSHDLCFramePostReset(pBindingContext);
      pSHDLCStack->nConnectStatus = CONNECT_STATUS_INIT;
   }

   CNALDebugAssert(pSHDLCStack->nConnectStatus == CONNECT_STATUS_INIT);

   pSHDLCStack->pConnectCallbackFunction = pCallbackFunction;
   pSHDLCStack->pConnectCallbackParameter = pCallbackParameter;
   pSHDLCStack->pResetIndicationFunction = pResetIndication;
   pSHDLCStack->pResetIndicationFunctionParameter = pResetIndicationCallbackParameter;
   pSHDLCStack->pReadHandler = pDataReadyHandler;
   pSHDLCStack->pReadHandlerParameter = pDataReadyHandlerParameter;
   pSHDLCStack->pWriteHandler = pWriteAcknowledgedHandler;
   pSHDLCStack->pWriteHandlerParameter = pWriteHandlerParameter;

   PSHDLCFrameRead(
         pBindingContext,
         pSHDLCStack->aReceptionBuffer,
         &static_PSHDLCGenericReadCompleted,
         null );

   static_PSHDLCConnectSendFrame(pBindingContext, FRAME_RST);
}

/**
 * Computes the modulo of a value.
 *
 * @param[in]  nValue The initial value.
 *
 * @param[in]  nModulo  The modulo value.
 *
 * @return the computed value.
 **/
static P_NAL_INLINE uint32_t static_PSHDLCWriteModulo(uint32_t nValue, uint32_t nModulo )
{
   CNALDebugAssert((nModulo >= 1) && (nModulo <= 4));
   switch(nModulo)
   {
   case 1:
      return 0;
   case 2:
      return nValue & 1;
   case 3:
      return nValue - ((nValue / 3) * 3);
   }
   return nValue & 3;
}

/* See header file */
NFC_HAL_INTERNAL bool_t PSHDLCWrite(
         tNALBindingContext* pBindingContext,
         uint8_t* pFrameBuffer,
         uint32_t nFrameBufferLength )
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);
   uint32_t nNextToSendFrameId, nLastAckSentFrameId;
   uint32_t nFramePosition;
   uint32_t nWindowSize;
   uint8_t* pBuffer;

   static_PSHDLCWriteTraceStatus(pSHDLCStack, "PSHDLCWrite()");
   PNALDebugTraceBuffer(pFrameBuffer, nFrameBufferLength);

   CNALDebugAssert( pSHDLCStack != null );
   CNALDebugAssert( pFrameBuffer != null );
   CNALDebugAssert( nFrameBufferLength <= PSHDLC_PAYLOAD_MAX_SIZE);

   nWindowSize = pSHDLCStack->nWindowSize;
   nNextToSendFrameId = pSHDLCStack->nWriteNextToSendFrameId;
   nLastAckSentFrameId = pSHDLCStack->nWriteLastAckSentFrameId;
   CNALDebugAssert( nNextToSendFrameId > nLastAckSentFrameId);

   if( nNextToSendFrameId - nLastAckSentFrameId - 1 >= pSHDLCStack->nWindowSize)
   {
      /* The window is full, the caller should wait */
      pSHDLCStack->bIsWriteBufferFull = W_TRUE;
      return W_FALSE;
   }

   pSHDLCStack->nWritePayload += nFrameBufferLength;

   nFramePosition = static_PSHDLCWriteModulo(nNextToSendFrameId, nWindowSize);
   pBuffer = pSHDLCStack->aWriteFrameArray[nFramePosition].aData;
   pSHDLCStack->aWriteFrameArray[nFramePosition].nLength = nFrameBufferLength + 1;

   /* Create the I-header : The next to receive Id is left to zero for now */
   *pBuffer++ = (uint8_t)(0x80 | ((nNextToSendFrameId & 0x07) << 3));

   /* Copy the payload */
   CNALMemoryCopy(pBuffer, pFrameBuffer, nFrameBufferLength);

   /* Increment the next to send identifier */
   if((pSHDLCStack->nWriteNextToSendFrameId = nNextToSendFrameId + 1) == 0)
   {
      static_PSHDLCConnectAvoidCounterSpin(pSHDLCStack);
   }

   static_PSHDLCWriteTransmit(pBindingContext, pSHDLCStack, W_FALSE);

   return W_TRUE;
}

/**
 * Checks if there is enough space to receive a frame in the read buffer.
 *
 * @param[in]  pSHDLCStack  The SHDLC stack.
 *
 * @param[in]  nLength  The length in bytes of the frame.
 *
 * @return  W_TRUE if there is enough space, W_FALSE otherwise.
 **/
static P_NAL_INLINE bool_t static_PSHDLCReadCheckFreeSize(
         tSHDLCInstance* pSHDLCStack,
         uint32_t nLength)
{
   /* Adding one byte for the length and 4 byte for the counter */
   return ((nLength + 1 + 4) <= (P_SHDLC_READ_BUFFER_SIZE - pSHDLCStack->nReadBufferDataLength))?W_TRUE:W_FALSE;
}

/* See header file */
NFC_HAL_INTERNAL uint32_t PSHDLCRead(
         tNALBindingContext* pBindingContext,
         uint8_t* pBuffer,
         uint32_t* pnSHDLCFrameReceptionCounter )
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);

   uint32_t nLength = 0;

   CNALDebugAssert( pSHDLCStack != null );
   static_PSHDLCReadTraceStatus(pSHDLCStack, "PSHDLCRead");

   if(pSHDLCStack->nReadBufferDataLength != 0)
   {
      uint32_t nCurrentPosition = pSHDLCStack->nReadNextPositionToRead;
      uint32_t nNextPosition;
      uint32_t nSHDLCFrameReceptionCounter;

      nLength = pSHDLCStack->aReadBuffer[nCurrentPosition++];
      nCurrentPosition &= (P_SHDLC_READ_BUFFER_SIZE - 1);
      CNALDebugAssert(nLength != 0);
      nSHDLCFrameReceptionCounter = pSHDLCStack->aReadBuffer[nCurrentPosition++];
      nCurrentPosition &= (P_SHDLC_READ_BUFFER_SIZE - 1);
      nSHDLCFrameReceptionCounter = nSHDLCFrameReceptionCounter << 8;
      nSHDLCFrameReceptionCounter |= pSHDLCStack->aReadBuffer[nCurrentPosition++];
      nCurrentPosition &= (P_SHDLC_READ_BUFFER_SIZE - 1);
      nSHDLCFrameReceptionCounter = nSHDLCFrameReceptionCounter << 8;
      nSHDLCFrameReceptionCounter |= pSHDLCStack->aReadBuffer[nCurrentPosition++];
      nCurrentPosition &= (P_SHDLC_READ_BUFFER_SIZE - 1);
      nSHDLCFrameReceptionCounter = nSHDLCFrameReceptionCounter << 8;
      nSHDLCFrameReceptionCounter |= pSHDLCStack->aReadBuffer[nCurrentPosition++];
      nCurrentPosition &= (P_SHDLC_READ_BUFFER_SIZE - 1);

      *pnSHDLCFrameReceptionCounter = nSHDLCFrameReceptionCounter;

      nNextPosition = (nCurrentPosition + nLength) & (P_SHDLC_READ_BUFFER_SIZE - 1);

      if( nNextPosition > nCurrentPosition)
      {
         CNALMemoryCopy(pBuffer, &pSHDLCStack->aReadBuffer[nCurrentPosition], nLength);
      }
      else
      {
         uint32_t nChunk = P_SHDLC_READ_BUFFER_SIZE - nCurrentPosition;

         CNALMemoryCopy(pBuffer, &pSHDLCStack->aReadBuffer[nCurrentPosition], nChunk);
         pBuffer += nChunk;
         CNALMemoryCopy(pBuffer, &pSHDLCStack->aReadBuffer[0], nNextPosition);
      }

      pSHDLCStack->nReadNextPositionToRead = nNextPosition;
      pSHDLCStack->nReadBufferDataLength -= nLength + 5;

      if( pSHDLCStack->bIsReadBufferFull )
      {
         if((P_SHDLC_READ_BUFFER_SIZE - pSHDLCStack->nReadBufferDataLength)
            >= P_SHDLC_READ_BUFFER_FREE_TRESHOLD)
         {
            pSHDLCStack->bIsReadBufferFull = W_FALSE;
            pSHDLCStack->nNextCtrlFrameToSend = READ_CTRL_FRAME_ACK;
            static_PSHDLCWriteTransmit(pBindingContext, pSHDLCStack, W_TRUE);
         }
      }
   }

   return nLength;
}

/* Return W_TRUE if the buffer was empty */
static P_NAL_INLINE bool_t static_PSHDLCReadWriteInBuffer(
         tSHDLCInstance* pSHDLCStack,
         uint8_t* pFrameBuffer,
         uint32_t nFrameLength,
         uint32_t nSHDLCFrameReceptionCounter)
{
   uint32_t nDataLength;
   uint32_t nNextPosition;

   CNALDebugAssert(pSHDLCStack != null );
   CNALDebugAssert(nFrameLength > 0);
   CNALDebugAssert(nFrameLength <= 0xFF);

   nDataLength = pSHDLCStack->nReadBufferDataLength;
   nNextPosition = pSHDLCStack->nReadNextPositionToRead + nDataLength;
   nNextPosition &= (P_SHDLC_READ_BUFFER_SIZE - 1);

   pSHDLCStack->aReadBuffer[nNextPosition++] = (uint8_t)nFrameLength;
   nNextPosition &= (P_SHDLC_READ_BUFFER_SIZE - 1);
   pSHDLCStack->aReadBuffer[nNextPosition++] = (uint8_t)((nSHDLCFrameReceptionCounter >> 24) & 0xFF);
   nNextPosition &= (P_SHDLC_READ_BUFFER_SIZE - 1);
   pSHDLCStack->aReadBuffer[nNextPosition++] = (uint8_t)((nSHDLCFrameReceptionCounter >> 16) & 0xFF);
   nNextPosition &= (P_SHDLC_READ_BUFFER_SIZE - 1);
   pSHDLCStack->aReadBuffer[nNextPosition++] = (uint8_t)((nSHDLCFrameReceptionCounter >> 8) & 0xFF);
   nNextPosition &= (P_SHDLC_READ_BUFFER_SIZE - 1);
   pSHDLCStack->aReadBuffer[nNextPosition++] = (uint8_t)(nSHDLCFrameReceptionCounter & 0xFF);
   nNextPosition &= (P_SHDLC_READ_BUFFER_SIZE - 1);

   if(nNextPosition + nFrameLength <= P_SHDLC_READ_BUFFER_SIZE)
   {
      CNALMemoryCopy(
         &pSHDLCStack->aReadBuffer[nNextPosition],
         pFrameBuffer, nFrameLength);
   }
   else
   {
      uint32_t nCopyLength = P_SHDLC_READ_BUFFER_SIZE - nNextPosition;

      CNALMemoryCopy(
         &pSHDLCStack->aReadBuffer[nNextPosition],
         pFrameBuffer, nCopyLength);

      CNALMemoryCopy(
         &pSHDLCStack->aReadBuffer[0],
         pFrameBuffer + nCopyLength, nFrameLength - nCopyLength);
   }

   pSHDLCStack->nReadBufferDataLength = nDataLength + nFrameLength + 5;

   return (nDataLength == 0)?W_TRUE:W_FALSE;
}

static void static_PSHDLCReadTimerCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter)
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);
   CNALDebugAssert(pSHDLCStack != null );

   static_PSHDLCReadTraceStatus(pSHDLCStack, "static_PSHDLCReadTimerCompleted");

   pSHDLCStack->nNextCtrlFrameToSend = READ_CTRL_FRAME_ACK;
   static_PSHDLCWriteTransmit(pBindingContext, pSHDLCStack, W_TRUE);
}

/**
 * Receives the read completed event for the write state-machine.
 **/
static void static_PSHDLCReadReadCompleted(
         tNALBindingContext* pBindingContext,
         uint8_t* pBuffer,
         uint32_t nLength,
         uint32_t nSHDLCFrameReceptionCounter)
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);

   uint8_t nCtrlHeader;
   uint32_t nReceivedFrameId;
   uint32_t nNonAcknowledgedFrameNumber;

   bool_t bForceTransmit = W_FALSE;

   static_PSHDLCReadTraceStatus(pSHDLCStack, "static_PSHDLCReadReadCompleted");
   PNALDebugTraceBuffer(pBuffer, nLength);

   CNALDebugAssert(pSHDLCStack != null );
   CNALDebugAssert(nLength >= 1);

   nCtrlHeader = pBuffer[0];

   /* Check if the frame is an I-frame */
   if((nCtrlHeader & 0xC0) != 0x80)
   {
      /* Not an I-Frame */
      if(static_PSHDLCConnectCheckRSTFrame(pSHDLCStack, pBuffer, nLength))
      {
         /* The frame is a valid RST frame */

         pSHDLCStack->bInReestablishment = W_TRUE;

         pSHDLCStack->nNextCtrlFrameToSend = READ_CTRL_FRAME_RST /* READ_CTRL_FRAME_UA; */;
         goto function_return;
      }
      else if(nLength == 1)
      {
         static_PSHDLCWriteReceptionCtrlFrame(pBindingContext, nCtrlHeader, nSHDLCFrameReceptionCounter);
      }
      else
      {
         PNALDebugWarning("Reception of an unknown frame header, drop it");
         pSHDLCStack->nReadFrameLost++;
         pSHDLCStack->nReadByteErrorCount += nLength;
      }
      return;
   }

   /* Get the identifier of the I-Frame */
   nReceivedFrameId = (nCtrlHeader >> 3) & 0x07;

   /* build a RR frame with the same "next to receive identifier" */
   nCtrlHeader &= 0x07;
   nCtrlHeader |= 0xC0;
   /* Send this fake acknowledgememnt piggy-backed with the I-Frame
    * to the the write state machine
    */
   if (static_PSHDLCWriteReceptionCtrlFrame(pBindingContext, nCtrlHeader, nSHDLCFrameReceptionCounter) != W_TRUE)
   {
      PNALDebugWarning("No more processing of this frame...");
      return;
   }

   /* Check if the received identifier matches the expected value */
   if(nReceivedFrameId != (pSHDLCStack->nReadNextToReceivedFrameId & 0x07))
   {
      /* No, at least a frame is missing, reject the frame */
      PNALDebugWarning("The received frame id does not match the expected id, drop it with REJ");
      pSHDLCStack->nNextCtrlFrameToSend = READ_CTRL_FRAME_REJ;
      bForceTransmit = W_TRUE;
      pSHDLCStack->nReadFrameLost++;
      goto function_return;
   }

   /* Compute the length of the I-frame payload */
   nLength--;

   if(nLength != 0)
   {
      /* Check the free size in the buffer */
      if(static_PSHDLCReadCheckFreeSize(pSHDLCStack, nLength) == W_FALSE)
      {
         PNALDebugWarning("Not enough space to store the received data, drop it");
         pSHDLCStack->nReadFrameLost++;
         pSHDLCStack->nReadByteErrorCount += nLength;
         /* Not enough space to store the data */
         pSHDLCStack->nNextCtrlFrameToSend = READ_CTRL_FRAME_RNR;
         pSHDLCStack->bIsReadBufferFull = W_TRUE;

         goto function_return;
      }
   }

   /* Compute the number of non-acknoledged frames */
   nNonAcknowledgedFrameNumber =
      pSHDLCStack->nReadNextToReceivedFrameId -
      pSHDLCStack->nReadLastAcknowledgedFrameId;

   /* If this number is greater than or equals to the window size */
   if( nNonAcknowledgedFrameNumber >= pSHDLCStack->nWindowSize )
   {
      /* Then send an acknowledge */
      pSHDLCStack->nNextCtrlFrameToSend = READ_CTRL_FRAME_ACK;
   }
   else if( nNonAcknowledgedFrameNumber == 1 )
   {
      /* It's the first frame in the window, start the acknowledge timer */
      PNALMultiTimerSet( pBindingContext,
         TIMER_T1_SHDLC_ACK, pSHDLCStack->nReadT1Timeout,
         &static_PSHDLCReadTimerCompleted,
         null );
   }

   pSHDLCStack->nReadNextToReceivedFrameId++;
   if(pSHDLCStack->nReadNextToReceivedFrameId == 0)
   {
      static_PSHDLCConnectAvoidCounterSpin(pSHDLCStack);
   }

   if(nLength != 0)
   {
      /* cancel thr TIMER_T6_SHDLC_RESEND and clear flag if an I-frame
      is received before timer expiration*/
      if(pSHDLCStack->bIsRRFrameAlreadySent == W_TRUE)
      {
         pSHDLCStack->bIsRRFrameAlreadySent = W_FALSE;
         PNALMultiTimerCancel(pBindingContext, TIMER_T6_SHDLC_RESEND);
      }

      /* If the I-frame is HCI chained, force the transmition of the next RR */
      if((pSHDLCStack->aReceptionBuffer[1] & 0x80) == 0)
      {
         bForceTransmit = W_TRUE;
         pSHDLCStack->nRRPiggyBackHeader = pSHDLCStack->aReceptionBuffer[1];
      }
      else
      {
         if((pSHDLCStack->aReceptionBuffer[1] & 0x7F) != pSHDLCStack->nRRPiggyBackHeader)
         {
            if((pSHDLCStack->aReceptionBuffer[2] & 0xC0) == 0x40)
            {
               /* If the HCI frame is an event, force the transmition of the RR */
               bForceTransmit = W_TRUE;
            }
         }
         pSHDLCStack->nRRPiggyBackHeader = 0;
      }

      pSHDLCStack->nReadPayload += nLength;

      if(static_PSHDLCReadWriteInBuffer(
         pSHDLCStack,
         &pSHDLCStack->aReceptionBuffer[1],
         nLength,
         nSHDLCFrameReceptionCounter) != W_FALSE)
      {
         /* The buffer was empty, send the data ready event */
         PNALDFCPost1(
            pBindingContext, P_DFC_TYPE_SHDLC,
            pSHDLCStack->pReadHandler,
            pSHDLCStack->pReadHandlerParameter);
      }
   }

function_return:

   static_PSHDLCWriteTransmit(pBindingContext, pSHDLCStack, bForceTransmit);
}

/**
 * Informs the read state-machine that the write state-machine has sent an acknoledge.
 *
 * @param[in]  pBindingContext  The context.
 **/
static void static_PSHDLCReadAcknowledgeSent(
         tNALBindingContext* pBindingContext)
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);
   CNALDebugAssert(pSHDLCStack != null);

   static_PSHDLCReadTraceStatus(pSHDLCStack, "static_PSHDLCReadAcknowledgeSent");

   pSHDLCStack->nReadLastAcknowledgedFrameId =
      pSHDLCStack->nReadNextToReceivedFrameId - 1;

   PNALMultiTimerCancel(pBindingContext, TIMER_T1_SHDLC_ACK);
}

/**
 * Receives the timer expired event for the write state-machine.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pCallbackParameter  The callback parameter.
 **/
static void static_PSHDLCWriteTimerCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter)
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);
   CNALDebugAssert(pSHDLCStack != null );

   PNALDebugWarning("static_PSHDLCWriteTimerCompleted: Frame not aknowleged before timeout");

   static_PSHDLCWriteTraceStatus(pSHDLCStack, "WriteTimerCompleted()");

   if(pSHDLCStack->nWriteNextWindowFrameId >
      pSHDLCStack->nWriteLastAckSentFrameId + 1)
   {
      PNALDebugWarning("static_PSHDLCWriteTimerCompleted: Frame not aknowleged before timeout : resend");

      /* At least one frame was sent and not acknowledged */
      pSHDLCStack->nWriteNextWindowFrameId--;

      /* Resend this frame */
      static_PSHDLCWriteTransmit(pBindingContext, pSHDLCStack, W_TRUE);
   }
}

/**
 * Forces the transmition of a control frame or of an I-Frame.
 *
 * No write frame should be pending.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  pSHDLCStack  The SHDLC stack.
 *
 * @param[in]  bForceAck Force the ack to be sent.
 **/
static void static_PSHDLCWriteTransmit(
         tNALBindingContext* pBindingContext,
         tSHDLCInstance* pSHDLCStack,
         bool_t bForceAck)
{
   uint32_t nNextWindowFrameId, nNextToSendFrameId;
   bool_t bSendIframe;
   uint8_t nNextCtrlFrameToSend;
   uint32_t nLengthToSend;

#ifdef P_SHDLC_NO_PIGGY_BACKING
   bForceAck = W_TRUE;
#endif

   static_PSHDLCWriteTraceStatus(pSHDLCStack, "static_PSHDLCWriteTransmit()");

   if(pSHDLCStack->bIsWriteFramePending != W_FALSE)
   {
      /* a frame is already being sent */
      return;
   }

   nNextToSendFrameId = pSHDLCStack->nWriteNextToSendFrameId;
   nNextWindowFrameId = pSHDLCStack->nWriteNextWindowFrameId;
   nNextCtrlFrameToSend = pSHDLCStack->nNextCtrlFrameToSend;

   CNALDebugAssert( nNextWindowFrameId <= nNextToSendFrameId );

   bSendIframe = W_FALSE;

   /* Do we have some I-frame to send ? */
   if((nNextWindowFrameId != nNextToSendFrameId) && (pSHDLCStack->bIsWriteOtherSideReady != W_FALSE))
   {
      if((nNextCtrlFrameToSend == READ_CTRL_FRAME_NONE)
      || (nNextCtrlFrameToSend == READ_CTRL_FRAME_ACK))
      {
         bSendIframe = W_TRUE;
      }
   }
   else if(nNextCtrlFrameToSend == READ_CTRL_FRAME_NONE)
   {
      return;
   }

   if(bSendIframe == W_FALSE)
   {
      CNALDebugAssert(nNextCtrlFrameToSend != READ_CTRL_FRAME_NONE);

      if(bForceAck == W_FALSE)
      {
         /* Just set the piggy-backing timer */
         PNALMultiTimerSet( pBindingContext,
            TIMER_T4_SHDLC_PIGGYBACK, TIMER_T4_TIMEOUT,
            &static_PSHDLCWritePiggyBackTimerCompleted,
            null );

         return;
      }
      /* set bIsRRFrameAlreadySent to avoid to send I-frame  before timeout*/
      pSHDLCStack->bIsRRFrameAlreadySent = W_TRUE;
      /*set TIMER_T6_SHDLC_RESEND to send probable I-frame after timeout*/
      PNALMultiTimerSet( pBindingContext,
           TIMER_T6_SHDLC_RESEND, TIMER_T6_TIMEOUT,
           &static_PSHDLCRRFrameAlreadySentTimer,
           null );
      nLengthToSend = 1;
      if ((nNextCtrlFrameToSend != READ_CTRL_FRAME_UA) && (nNextCtrlFrameToSend != READ_CTRL_FRAME_RST))
      {
         nNextCtrlFrameToSend |= (pSHDLCStack->nReadNextToReceivedFrameId) & 0x07;
         static_PSHDLCReadAcknowledgeSent(pBindingContext);
      }

      pSHDLCStack->aSendBuffer[0] = nNextCtrlFrameToSend;

      if (nNextCtrlFrameToSend == READ_CTRL_FRAME_RST)
      {
         pSHDLCStack->aSendBuffer[1] = pSHDLCStack->nWindowSize;
         nLengthToSend = 2;
      }
   }
   else if(pSHDLCStack->bIsRRFrameAlreadySent == W_TRUE)
   {
      /* exit : I-frame will be sent after RR-Frame-Already-Sent timeout*/
      PNALDebugTrace("static_PSHDLCWriteTransmit wait RR-Frame-Already-Sent timeout to send I-frame");
      return;
   }
   else
   {
      uint32_t nPosition = static_PSHDLCWriteModulo(nNextWindowFrameId, pSHDLCStack->nWindowSize);

      nLengthToSend = pSHDLCStack->aWriteFrameArray[nPosition].nLength;

      CNALMemoryCopy(
         pSHDLCStack->aSendBuffer,
         pSHDLCStack->aWriteFrameArray[nPosition].aData,
         nLengthToSend);

      /* Set the guard/transmit timeout */
      PNALMultiTimerSet( pBindingContext,
         TIMER_T2_SHDLC_RESEND, TIMER_T2_TIMEOUT,
         &static_PSHDLCWriteTimerCompleted,
         null );

      pSHDLCStack->aSendBuffer[0] |= (pSHDLCStack->nReadNextToReceivedFrameId) & 0x07;
      static_PSHDLCReadAcknowledgeSent(pBindingContext);

      pSHDLCStack->nWriteNextWindowFrameId = nNextWindowFrameId + 1;
      if( pSHDLCStack->nWriteNextWindowFrameId == 0)
      {
         static_PSHDLCConnectAvoidCounterSpin( pSHDLCStack );
      }
   }

   PSHDLCFrameWrite(
         pBindingContext,
         pSHDLCStack->aSendBuffer, nLengthToSend,
         static_PSHDLCWriteWriteFrameCompleted, null );

   pSHDLCStack->bIsWriteFramePending = W_TRUE;
   pSHDLCStack->nNextCtrlFrameToSend = READ_CTRL_FRAME_NONE;

   PNALMultiTimerCancel( pBindingContext, TIMER_T4_SHDLC_PIGGYBACK );
}

/* The RR-Frame-Already-Sent timer completion handler */
static void static_PSHDLCRRFrameAlreadySentTimer(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter)
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);

   static_PSHDLCReadTraceStatus(pSHDLCStack, "static_PSHDLCRRFrameAlreadySentTimer");
   pSHDLCStack->bIsRRFrameAlreadySent = W_FALSE;
   static_PSHDLCWriteTransmit(pBindingContext, pSHDLCStack, W_TRUE);
}

/* The piggy-back timer completion handler */
static void static_PSHDLCWritePiggyBackTimerCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter)
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);

   static_PSHDLCWriteTransmit(pBindingContext, pSHDLCStack, W_TRUE);
}
/**
 * Receives the write completed event for the write state-machine.
 **/
static void static_PSHDLCWriteWriteFrameCompleted(
         tNALBindingContext* pBindingContext,
         void* pCallbackParameter)
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);

   static_PSHDLCWriteTraceStatus(pSHDLCStack, "static_PSHDLCWriteWriteFrameCompleted()");

   CNALDebugAssert(pSHDLCStack != null );

   pSHDLCStack->bIsWriteFramePending = W_FALSE;

   static_PSHDLCWriteTransmit(pBindingContext, pSHDLCStack, W_FALSE);
}

/**
 * Rebuilds a 32-bit identifier from a 3-bit "next to receive" value.
 *
 * @param[in]  pSHDLCStack  The SHDLC stack.
 *
 * @param[in]  n3bitValue  The 3-bit value to translate.
 *
 * @return  The 32-bit value.
 **/
static uint32_t static_PSHDLCWriteRebuild32BitIdentifier(
         tSHDLCInstance* pSHDLCStack,
         uint8_t n3bitValue )
{
   uint32_t n32bitValue;

   CNALDebugAssert(pSHDLCStack != null);
   n32bitValue = pSHDLCStack->nWriteLastAckSentFrameId;

   CNALDebugAssert(pSHDLCStack->nWriteNextToSendFrameId >= pSHDLCStack->nWriteNextWindowFrameId);
   CNALDebugAssert(pSHDLCStack->nWriteNextWindowFrameId > n32bitValue);



   if( n3bitValue > (n32bitValue & 0x07))
   {
      n32bitValue = (n32bitValue & 0xFFFFFFF8) | n3bitValue;
   }
   else
   {
      n32bitValue = ((n32bitValue + 8) & 0xFFFFFFF8) | n3bitValue;
   }

   return n32bitValue;
}

/**
 * Receives a control frame for the write state machine.
 *
 * @param[in]  pBindingContext  The context.
 *
 * @param[in]  nCtrlFrame  The one byte control frame. This value is not checked yet.
 *
 * @param[in]  nSHDLCFrameReceptionCounter  The reception counter of the frame.
 *
 * @return       W_TRUE if the frame must be processed.
 **/
static bool_t static_PSHDLCWriteReceptionCtrlFrame(
         tNALBindingContext* pBindingContext,
         uint8_t nCtrlFrame,
         uint32_t nSHDLCFrameReceptionCounter)
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);
   uint8_t nMaskedValue = nCtrlFrame & 0xF8;  /* the next to receive to zero */
   uint32_t nNextToReceive;
   bool_t bAlreadyCalled = W_FALSE;
   uint32_t nPosition;

   CNALDebugAssert(pSHDLCStack != null);

   PNALDebugTrace("static_PSHDLCWriteReceptionCtrlFrame( nCtrlFrame=0x%02X )", nCtrlFrame);
   static_PSHDLCWriteTraceStatus(pSHDLCStack, "static_PSHDLCWriteReceptionCtrlFrame()");

   if((nMaskedValue != FRAME_RNR_MASK)
   && (nMaskedValue != FRAME_RR_MASK)
   && (nMaskedValue != FRAME_REJ_MASK))
   {
      if(nCtrlFrame == FRAME_UA)
      {
         if (pSHDLCStack->bInReestablishment != W_FALSE)
         {
            pSHDLCStack->bInReestablishment = W_FALSE;

            PNALDFCPost1(
               pBindingContext, P_DFC_TYPE_SHDLC,
               pSHDLCStack->pResetIndicationFunction,
               pSHDLCStack->pResetIndicationFunctionParameter);
         }
         else
         {
            PNALDebugWarning("Residual UA frame, ignore it");
         }
      }
      else
      {
         PNALDebugWarning("Unknown control frame, ignore it");
      }
      pSHDLCStack->nReadFrameLost++;
      pSHDLCStack->nReadByteErrorCount ++;
      return W_TRUE;
   }

   if(nMaskedValue == FRAME_RNR_MASK)
   {
      pSHDLCStack->bIsWriteOtherSideReady = W_FALSE;
   }
   else
   {
      pSHDLCStack->bIsWriteOtherSideReady = W_TRUE;
   }

   nNextToReceive = static_PSHDLCWriteRebuild32BitIdentifier(pSHDLCStack, nCtrlFrame & 0x07);

   if((nNextToReceive <= pSHDLCStack->nWriteNextWindowFrameId) &&
      (pSHDLCStack->nWriteLastAckSentFrameId < nNextToReceive))
   {
      if(nNextToReceive < pSHDLCStack->nWriteNextWindowFrameId)
      {
         uint32_t nFrameLost = pSHDLCStack->nWriteNextWindowFrameId - nNextToReceive;
         /* In normal case, the  frameToSend is processed after received and analysing
           I-frame and RR-frame.
          In few case the  frameToSend is processed after analysing Received RR-frame
          and before analysing Received I-frame, in such case (i.e FrameLost == 0x01),
          we consider the I-frame analysis is delayed, so no frame is lost*/
         if(nFrameLost == 0x01)
         {
            PNALDebugWarning("I-frame analysis was delayed after frame send process");
            pSHDLCStack->nWriteLastAckSentFrameId = nNextToReceive - 1;
            return W_TRUE;
         }
         PNALDebugWarning("Sent frames are lost (%d frames)", nFrameLost);
         pSHDLCStack->nWriteFrameLost += nFrameLost;

         for(nPosition = nNextToReceive; nPosition < pSHDLCStack->nWriteNextWindowFrameId; nPosition++)
         {
            pSHDLCStack->nWriteByteErrorCount += pSHDLCStack->aWriteFrameArray[
               static_PSHDLCWriteModulo(nPosition, pSHDLCStack->nWindowSize)].nLength;
         }
      }

      /* Parse all the acknowledged frames to send the frame counter of the acknoledgement */
      for(nPosition = pSHDLCStack->nWriteLastAckSentFrameId + 1;
         nPosition < nNextToReceive;
         nPosition++)
      {
         CNALDebugAssert(pSHDLCStack->aWriteFrameArray[
               static_PSHDLCWriteModulo(nPosition, pSHDLCStack->nWindowSize)].nLength >= 2);

         if(((pSHDLCStack->aWriteFrameArray[
               static_PSHDLCWriteModulo(nPosition, pSHDLCStack->nWindowSize)].aData[1]) & 0x80) != 0)
         {
            bAlreadyCalled = W_TRUE;

            PNALDFCPost2( pBindingContext, P_DFC_TYPE_SHDLC,
               pSHDLCStack->pWriteHandler,
               pSHDLCStack->pWriteHandlerParameter,
               PNALUtilConvertUint32ToPointer(nSHDLCFrameReceptionCounter));
         }
      }

      pSHDLCStack->nWriteLastAckSentFrameId = nNextToReceive - 1;
      pSHDLCStack->nWriteNextWindowFrameId = nNextToReceive;

      if(pSHDLCStack->nWriteNextToSendFrameId == nNextToReceive)
      {
         /* All frames are acknlowledged */

         /* Cancel the guard/transmit timeout */
         PNALMultiTimerCancel( pBindingContext, TIMER_T2_SHDLC_RESEND );

         /* Informs the caller if the buffer was full */
         if( pSHDLCStack->bIsWriteBufferFull )
         {
            pSHDLCStack->bIsWriteBufferFull = W_FALSE;

            if(bAlreadyCalled == W_FALSE)
            {
               PNALDFCPost2( pBindingContext, P_DFC_TYPE_SHDLC,
                  pSHDLCStack->pWriteHandler,
                  pSHDLCStack->pWriteHandlerParameter,
                  (void*)0);  /* Zero means: no acknoledgment of a complete message */
            }
         }
      }
      else
      {
         /* Some frames still needs to be send */
         static_PSHDLCWriteTransmit(pBindingContext, pSHDLCStack, W_FALSE);
      }
   }
   else
   {
      PNALDebugWarning("Out of range NR value in the frame, ignore it");
      pSHDLCStack->nReadFrameLost++;
      pSHDLCStack->nReadByteErrorCount ++;

      return W_FALSE;
   }

   return W_TRUE;
}

/* See header file */
NFC_HAL_INTERNAL void PSHDLCGetStatistics(
         tNALBindingContext* pBindingContext,
         uint32_t* pnOSI4WindowSize,
         uint32_t* pnOSI4ReadPayload,
         uint32_t* pnOSI4ReadFrameLost,
         uint32_t* pnOSI4ReadByteErrorCount,
         uint32_t* pnOSI4WritePayload,
         uint32_t* pnOSI4WriteFrameLost,
         uint32_t* pnOSI4WriteByteErrorCount )
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);
   CNALDebugAssert(pSHDLCStack != null);

   *pnOSI4WindowSize = pSHDLCStack->nWindowSize;

   *pnOSI4ReadPayload = pSHDLCStack->nReadPayload;
   *pnOSI4ReadFrameLost = pSHDLCStack->nReadFrameLost;
   *pnOSI4ReadByteErrorCount = pSHDLCStack->nReadByteErrorCount;

   *pnOSI4WritePayload = pSHDLCStack->nWritePayload;
   *pnOSI4WriteFrameLost = pSHDLCStack->nWriteFrameLost;
   *pnOSI4WriteByteErrorCount = pSHDLCStack->nWriteByteErrorCount;
}

/* See header file */
NFC_HAL_INTERNAL void PSHDLCResetStatistics(
         tNALBindingContext* pBindingContext)
{
   tSHDLCInstance* pSHDLCStack = PNALContextGetSHDLCInstance(pBindingContext);
   CNALDebugAssert(pSHDLCStack != null);

   pSHDLCStack->nReadPayload = 0;
   pSHDLCStack->nReadFrameLost = 0;
   pSHDLCStack->nReadByteErrorCount = 0;

   pSHDLCStack->nWritePayload = 0;
   pSHDLCStack->nWriteFrameLost = 0;
   pSHDLCStack->nWriteByteErrorCount = 0;
}
