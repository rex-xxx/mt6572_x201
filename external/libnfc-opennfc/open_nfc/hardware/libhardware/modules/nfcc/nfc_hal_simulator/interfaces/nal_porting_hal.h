/*
 * Copyright (c) 2007-2010 Inside Contactless, All Rights Reserved.
 *
 * Company Confidential Document
 *
 * Picopass, Open NFC, the Open NFC logo, Wave-Me and the Wave-Me logo are trademarks
 * or registered trademarks of Inside Contactless.
 *
 * Other brand, product and company names mentioned herein may be trademarks,
 * registered trademarks or trade names of their respective owners.
 *
 * The information and source code contained herein is the exclusive
 * property of Inside Contactless and may not be disclosed, examined
 * or reproduced in whole or in part without explicit written authorization
 * from the company.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. INSIDE CONTACTLESS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL
 * INSIDE CONTACTLESS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR
 * FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES,
 * HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
 * THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF INSIDE CONTACTLESS
 * HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

/*******************************************************************************

  This header file contains the functions to implement when porting Open NFC
  on the device. For the porting, refer the porting reference manual.

*******************************************************************************/

#ifndef __NAL_PORTING_HAL_H
#define __NAL_PORTING_HAL_H

#include "nal_porting_config.h"
#include "nal_porting_types.h"
#include "nal_porting_inline.h"

/*******************************************************************************
  Communication Constants
*******************************************************************************/

#define P_COM_TYPE_NFCC_SHDLC_RXTX         0
#define P_COM_TYPE_NFCC_SHDLC_I2C          1
#define P_COM_TYPE_NFCC_SHDLC_SPI          2
#define P_COM_TYPE_NFCC_SHDLC_TCPIP        3
#define P_COM_TYPE_NFC_HAL_SHDLC_RXTX      4
#define P_COM_TYPE_NFC_HAL_SHDLC_I2C       5
#define P_COM_TYPE_NFC_HAL_SHDLC_SPI       6
#define P_COM_TYPE_NFC_HAL_SHDLC_TCPIP     7
#define P_COM_TYPE_NFC_HAL_TCPIP           8
#define P_COM_TYPE_NFC_HAL_SHDLC_DIRECT    9

/*******************************************************************************
  Reset Constants
*******************************************************************************/

#define P_RESET_BOOT       0
#define P_RESET_WAKEUP     1

/*******************************************************************************
  Creation functions
*******************************************************************************/

struct __tNALInstance;

typedef struct __tNALInstance tNALInstance;

tNALInstance * CNALPreCreate(
         void * pPortingConfig);

bool_t CNALPostCreate(
         tNALInstance * pNALInstance,
         void * pNALVoidContext);

void CNALPreDestroy(
         tNALInstance * pNALInstance);

void CNALPostDestroy(
         tNALInstance * pNALInstance);

/*******************************************************************************
  Timer Functions
*******************************************************************************/
struct __tNALTimerInstance;

typedef struct __tNALTimerInstance tNALTimerInstance;

tNALTimerInstance* CNALTimerCreate(void* pPortingConfig);

void CNALTimerDestroy(
         tNALTimerInstance* pTimer );

uint32_t CNALTimerGetCurrentTime(
         tNALTimerInstance* pTimer );

void CNALTimerSet(
         tNALTimerInstance* pTimer,
         uint32_t nAbsoluteTime );

bool_t CNALTimerIsTimerElapsed(
         tNALTimerInstance* pTimer );

void CNALTimerCancel(
         tNALTimerInstance* pTimer );

/*******************************************************************************
  Communication Port Functions
*******************************************************************************/
struct __tNALComInstance;

typedef struct __tNALComInstance tNALComInstance;

tNALComInstance* CNALComCreate(
         void* pPortingConfig,
         uint32_t* pnType );

void CNALComDestroy(
         tNALComInstance* pComPort );


uint32_t CNALComReadBytes(
         tNALComInstance* pComPort,
         uint8_t* pReadBuffer,
         uint32_t nBufferLength);


uint32_t CNALComWriteBytes(
         tNALComInstance* pComPort,
         uint8_t* pBuffer,
         uint32_t nBufferLength );


/*******************************************************************************
  Reset Functions
*******************************************************************************/

void CNALResetNFCController(
            void* pPortingConfig,
            uint32_t nResetType );

bool_t CNALResetIsPending(
            void* pPortingConfig );

/*******************************************************************************
  Synchronization functions
*******************************************************************************/

void CNALSyncTriggerEventPump(
            void* pPortingConfig );

#endif /* __NAL_PORTING_HAL_H */
