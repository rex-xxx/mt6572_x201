/*
 * Copyright (c) 2009-2010 Inside Secure, All Rights Reserved.
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

  This header file contains the functions to implement when porting Open NFC
  on the device. For the porting, refer the porting reference manual.

*******************************************************************************/

#ifndef __NAL_PORTING_HAL_H
#define __NAL_PORTING_HAL_H
#include <semaphore.h>  /* Semaphore */
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
#define P_COM_TYPE_NFC_HAL_MSR3110_UART    10
#define P_COM_TYPE_NFC_HAL_MSR3110_SPI     11
#define P_COM_TYPE_NFC_HAL_MSR3110_I2C     12

/*******************************************************************************
  Reset Constants
*******************************************************************************/

#define P_RESET_BOOT       0
#define P_RESET_WAKEUP     1

/*******************************************************************************
  Creation functions
*******************************************************************************/
struct __tNALMsrComInstance;
typedef struct __tNALMsrComInstance tNALMsrComInstance;
typedef  int RecvCallBack( tNALMsrComInstance *,unsigned char *, int);
typedef  int WaitCallBack( tNALMsrComInstance *);

extern bool_t WT_ResentDetectCmd;


#define WRITE_QUEUE_SIZE  5
#define P_MAX_RX_BUFFER_SIZE  256
#define WatchDog_Enable 1 //Watch dog flag
#define WatchDog_Check_Detect_Mask 0 //Watch dog: check detect mask  flag


typedef struct __tWriteBufInstance
{
    uint16_t wri_buf_len;
    uint8_t wri_buf[P_MAX_RX_BUFFER_SIZE];
}tWriteBufInstance;

struct __tNALMsrComInstance
    {
        //void    * pNFCCConnection;      /* the connection, returned by CC Client */

        //uint8_t aRXBuffer[P_MAX_RX_BUFFER_SIZE];
        //uint32_t nRXDataLength;
        //uint8_t* pRXData;

        bool_t      bInitialResetDone;   /* first CNALResetNFCController() call  done ? */
        uint32_t  nResetPending;         /* number of CNALResetNFCController() calls waiting for confirmation */

        int       aDataComSockets[2];            /* sockets used to data comm */
        pthread_t hDispatcherThread;
        //pthread_t hDetectionThread;
        pthread_t hDeviceIOThread;
        pthread_t hDevIRQThread;    
        pthread_t hDevAliveChkThread;
		
        P_NAL_SYNC_CS  mutex;

        uint8_t     rx_buffer[P_MAX_RX_BUFFER_SIZE];
		uint8_t     temp_rx_buffer[P_MAX_RX_BUFFER_SIZE];
        uint8_t *   p_rx_data;
        int         nb_available_bytes;

        //sem_t  	detect_sem;
        sem_t  	ant_rx_sem;
        sem_t  	ant_rx_done_sem;

		sem_t  	ant_irq_sem;  
        /////////////////////////////////////////////////
        ////// structures for internal communication ////
        /////////////////////////////////////////////////
        unsigned short  card_detect_mask;
        unsigned short  reader_detect_mask;
        unsigned char   curSvcCode;
        unsigned char   curCmdCode;
        uint8_t         ant_send_buffer[P_MAX_RX_BUFFER_SIZE];
        uint8_t         ant_recv_buffer[P_MAX_RX_BUFFER_SIZE];
        bool_t            ant_recv_started;
        bool_t            shutdown;

        RecvCallBack    *cbRecv;


        int             WaitingPeriod;
        WaitCallBack    *cbWait;

        int             device_handle;
        //jimmy add: write queue
        uint8_t wri_que_cur_idx;
        uint8_t wri_que_next_idx;
        uint8_t wri_que_total_cnt;
        tWriteBufInstance write_queue[WRITE_QUEUE_SIZE];
        bool_t      bStandbyMode;
        unsigned short  temp_card_detect_mask;
        unsigned short  temp_reader_detect_mask;

		int ComReadWriteCtrl;

		bool_t getQueueStarted;
		bool_t recvDetectMask;
		bool_t sendDetectCmdDone;
		bool_t clearIRQFlag;

		//for check reader alive var
		bool_t chkReaderAlive;
		int    chkReaderAliveTimer;
        bool_t getIRQFlag;
		
		//for target use
		bool_t sendTargetSddEvt;
		uint8_t    temp_target_buffer[P_MAX_RX_BUFFER_SIZE];
		uint8_t    temp_target_buf_len;

		uint8_t irq_semi_cnt;
		uint8_t swp_reset_cnt;

		unsigned short  card_detect_poily;
        unsigned short  reader_detect_poily;
		unsigned short  temp_card_detect_poily;
        unsigned short  temp_reader_detect_poily;
    } ;



struct __tNALInstance
{
    /* The timer instance  */

    struct __tNALTimerInstance
    {
        uint32_t nTimerValue;       /* The current timer expiration value, 0 if no timer is pending */
        bool_t       bIsExpired;        /* The expiration flag */
        bool_t       bIsInitialized;    /* The initalization flag */

    } sTimerInstance;

    /* The com instance */


#define P_COM_RESET_FLAG  0x00
#define P_COM_DATA_FLAG   0x01



    tNALMsrComInstance sComInstance;

    int aWakeUpSockets[2];            /* sockets used to force select() exit */

    pthread_t hProcessDriverEventsThread;

} ;


//struct __tNALInstance;

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
//struct __tNALTimerInstance;

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

tNALMsrComInstance* CNALComCreate(
         void* pPortingConfig,
         uint32_t* pnType );

void CNALComDestroy(
         tNALMsrComInstance* pComPort );


uint32_t CNALComReadBytes(
         tNALMsrComInstance* pComPort,
         uint8_t* pReadBuffer,
         uint32_t nBufferLength);


uint32_t CNALComWriteBytes(
         tNALMsrComInstance* pComPort,
         uint8_t* pBuffer,
         uint32_t nBufferLength );

void CNALWriteDataFinish(
    tNALMsrComInstance* pComPort);

bool_t SetEventAvailableBytes(
    tNALMsrComInstance* pComPort,
    int nByteLen);

int Reset_Write_Queue(tNALMsrComInstance* pComPort);

int Stop_Chk_Reader_Alive(tNALMsrComInstance* pComPort);

int Start_IRQ_Detect(tNALMsrComInstance* pComPort);

int Stop_IRQ_Detect(tNALMsrComInstance* pComPort);




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
