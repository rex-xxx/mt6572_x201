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

#define P_MODULE  P_MODULE_DEC( HAL )

#include <errno.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <sys/time.h>
#include <sys/types.h>

#include <arpa/inet.h>
#include <semaphore.h>  /* Semaphore */
#include "nal_porting_os.h"
#include "nal_porting_hal.h"
#include "linux_porting_hal.h"

#include "nfc_hal.h"
#include "interface.h"
#include "nal_porting_srv_admin.h"
#include "nal_porting_srv_693.h"
#include "nal_porting_srv_43a.h"
#include "nal_porting_srv_p2p_i.h"
#include "nal_porting_srv_43b.h"
#include "nal_porting_srv_p2p_t.h"
#include "nal_porting_srv_43a_4.h"
#include "nal_porting_srv_felica.h"
#include "nal_porting_srv_type1.h"



#define 	TIMER_SET		'S'
#define	    TIMER_CANCEL	'C'
#define 	EVENT_QUIT		'Q'
#define	    EVENT_KICK		'K'
#define	    EVENT_DATA		'D'

#define Check_Dev_Alive_Interval   3000 //3s

bool_t WT_ResentDetectCmd;

extern tNALBinding * const g_pNALBinding;

/* The NFC controller instance */

tNALInstance g_sNFCCInstance;


/* Retuns the pointer on the single instance */
static __inline tNALInstance* static_PNALGetInstance( void )
{
    return &g_sNFCCInstance;
}

/* Retuns the pointer on the single instance */
static __inline tNALMsrComInstance* static_PComGetInstance( void )
{
    return &g_sNFCCInstance.sComInstance;
}

/* Retuns the pointer on the single instance */
static __inline tNALTimerInstance* static_PTimerGetInstance( void )
{
    return &g_sNFCCInstance.sTimerInstance;
}


/*******************************************************************************

  Timer Functions.

*******************************************************************************/
/* See Functional Specifications Document */
tNALTimerInstance * CNALTimerCreate( void* pPortingConfig )
{
    tNALTimerInstance * pTimer = static_PTimerGetInstance();

    if( pTimer != NULL )
    {
        pTimer->nTimerValue = 0L;
        pTimer->bIsExpired = W_FALSE;
        pTimer->bIsInitialized = W_TRUE;
    }

    return pTimer;
}


/* See Functional Specifications Document */
void CNALTimerDestroy(tNALTimerInstance * pTimer )
{
    if( pTimer != NULL )
    {
        CNALDebugAssert( pTimer->bIsInitialized != W_FALSE );
        pTimer->bIsInitialized = W_FALSE;
    }
}

/* See Functional Specifications Document */
uint32_t CNALTimerGetCurrentTime( tNALTimerInstance* pTimer )
{
    uint32_t nCurrentTime = 0;

    CNALDebugAssert(pTimer != NULL);

    if( pTimer != NULL )
    {
        struct timespec tv;

        CNALDebugAssert( pTimer->bIsInitialized != W_FALSE );

        if (clock_gettime(CLOCK_MONOTONIC, &tv) == 0)
        {
            nCurrentTime = tv.tv_sec * 1000 + tv.tv_nsec / 1000000;
        }
        else
        {
            PNALDebugError("CNALTimerGetCurrentTime : clock_gettime() failed %d", errno);
        }
    }

    return nCurrentTime;
}

/* See Functional Specifications Document */
void CNALTimerSet( tNALTimerInstance* pTimer, uint32_t nTimeoutMs )
{
    CNALDebugAssert(pTimer != NULL);
    uint8_t buff[1];

    if( pTimer != NULL )
    {
        CNALDebugAssert( pTimer->bIsInitialized != W_FALSE );

        pTimer->nTimerValue = CNALTimerGetCurrentTime(pTimer) + nTimeoutMs;
        pTimer->bIsExpired = W_FALSE;

        /* write into the fd to force wakeup of the receive thread */
        buff[0] = TIMER_SET;
        write(g_sNFCCInstance.aWakeUpSockets[0], buff, 1);
    }
}

/* See Functional Specifications Document */
bool_t CNALTimerIsTimerElapsed( tNALTimerInstance* pTimer )
{
    bool_t bIsExpired = W_FALSE;

    CNALDebugAssert(pTimer != NULL);

    if( pTimer != NULL )
    {
        CNALDebugAssert( pTimer->bIsInitialized != W_FALSE );

        bIsExpired = pTimer->bIsExpired;
        pTimer->bIsExpired = W_FALSE;
    }

    return bIsExpired;
}

/* See Functional Specifications Document */
void CNALTimerCancel( tNALTimerInstance* pTimer )
{
    CNALDebugAssert(pTimer != NULL);
    uint8_t buff[1];

    if( pTimer != NULL )
    {
        CNALDebugAssert( pTimer->bIsInitialized != W_FALSE );
        pTimer->nTimerValue = 0L;
        pTimer->bIsExpired = W_FALSE;

        buff[0] = TIMER_CANCEL;
        write(g_sNFCCInstance.aWakeUpSockets[0], buff, 1);
    }
}


/*
 * Marks the current timer as expired.
 */
static void static_CNALTimerSetExpired( tNALTimerInstance* pTimer)
{
    CNALDebugAssert(pTimer != NULL);

    if (pTimer != NULL)
    {
        CNALDebugAssert( pTimer->bIsInitialized != W_FALSE );

        pTimer->nTimerValue = 0L;
        pTimer->bIsExpired = W_TRUE;
    }
}


/*******************************************************************************

  Communication Port Functions

*******************************************************************************/
static int ms_HalDispatcher(tNALMsrComInstance *dev, unsigned char *outbuf, int len)
{
    int retval = len;
    int nResult=0;
    uint8_t cmd = EVENT_DATA;
    //int sendlen =0 ;

    PNALDebugLog("ms_HalDispatcher()\n");
	/* remove to standy mode = 0x00 event
    if (dev->bStandbyMode == W_TRUE)
    {
        PNALDebugLog("ms_HalDispatcher(): bStandbyMode=W_TRUE\n");
        dev->bStandbyMode = W_FALSE;
        MS_PowerOn();
        dev->card_detect_mask = dev->temp_card_detect_mask;
        dev->reader_detect_mask = dev->temp_reader_detect_mask;
        ms_card_detection(dev);
    }
    */
    dev->curSvcCode = outbuf[0];
    dev->curCmdCode = outbuf[1];

    PNALDebugLog("dev->curSvcCode:0x%02X\n", dev->curSvcCode);

    switch( dev->curSvcCode ){   //service code
    case NAL_SERVICE_ADMIN:
		retval = ms_open_nfc_admin_disp( dev, outbuf, len);
         break;

    case NAL_SERVICE_READER_14_A_3:
        retval = ms_open_nfc_43a_disp(dev, outbuf, len);
        break;

    case NAL_SERVICE_READER_15_3:
        retval = ms_open_nfc_693_disp(dev, outbuf, len);
        break;

    case NAL_SERVICE_P2P_INITIATOR:
        retval = ms_open_nfc_initiator_disp(dev, outbuf, len);
        break;

    case NAL_SERVICE_P2P_TARGET:
		retval = ms_open_nfc_p2p_target_disp(dev, outbuf, len);
        break;

    case NAL_SERVICE_READER_14_A_4:
        retval = ms_open_nfc_43a_4_disp(dev, outbuf, len);
        break;

    case NAL_SERVICE_READER_FELICA:
        retval = ms_open_nfc_felica_disp(dev, outbuf, len);
        break;
	case NAL_SERVICE_READER_TYPE_1:
        retval = ms_open_nfc_type1_disp(dev, outbuf, len);
        break;

    case NAL_SERVICE_READER_14_B_4:
        retval = ms_open_nfc_43b_disp(dev, outbuf, len);
        break;
		
    case NAL_SERVICE_READER_14_B_3:
    case NAL_SERVICE_READER_15_2:
    case NAL_SERVICE_READER_B_PRIME:
    case NAL_SERVICE_READER_KOVIO:
    case NAL_SERVICE_CARD_14_A_4:
    case NAL_SERVICE_CARD_14_B_4:    
    case NAL_SERVICE_SECURE_ELEMENT:
        dev->rx_buffer[0] = dev->curSvcCode;
        //dev->rx_buffer[1] = NAL_RES_FEATURE_NOT_SUPPORTED;
        dev->rx_buffer[1] = NAL_RES_OK;
        dev->nb_available_bytes = 2;
        break;

    case NAL_SERVICE_UICC:
		retval = ms_open_nfc_uicc_disp(dev, outbuf, len);
		break;

    default:
        dev->rx_buffer[0] = NAL_SERVICE_ADMIN;
        dev->rx_buffer[1] = NAL_RES_FEATURE_NOT_SUPPORTED;
        dev->nb_available_bytes = 2;
        break;
    }
    if( dev->nb_available_bytes > 0){
        nResult = write(dev->aDataComSockets[1], &cmd, 1); //notify pDriverThread when data is ready
    }
    return retval;
}




static void * staticDispatcherThread(void *arg)
{
#define MAX_RECV_BUF_LEN    300 //256

    uint8_t recvbuf[MAX_RECV_BUF_LEN];
    int recvlen;
    int status = 0;
    tNALMsrComInstance* pComPort = static_PComGetInstance();


    PNALDebugLog("staticDispatcherThread [start]");
    recvlen = recv(pComPort->aDataComSockets[1], recvbuf, MAX_RECV_BUF_LEN, 0);
    while( pComPort->shutdown == W_FALSE)
    {
        PNALDebugLog("staticDispatcherThread recvlen =%d", recvlen);
        PNALDebugLog("staticDispatcherThread recvbuf =");
        PNALDebugLogBuffer(recvbuf, recvlen);
        CNALSyncEnterCriticalSection(&pComPort->mutex);
        status = ms_HalDispatcher(pComPort, recvbuf, recvlen);
        CNALSyncLeaveCriticalSection(&pComPort->mutex);

        if( status >= 0){
        }else{
            PNALDebugError("staticDispatcherThread: dispatcher error:%d\n", status);
        }
        recvlen = recv(pComPort->aDataComSockets[1], recvbuf, MAX_RECV_BUF_LEN, 0);
        //every time when 'recv' is done, check the shutdown flag before dispatching jobs.
    }
    PNALDebugLog("staticDispatcherThread [end]");
    return NULL;

}


static void * staticDeviceIOThread(void *arg)
{
    int recvlen = 0, nResult = 0, cmd = EVENT_DATA;
    tNALMsrComInstance* pComPort = static_PComGetInstance();
    WaitCallBack    *cbwait;

    PNALDebugLog("staticDeviceIOThread [start]");
    while(1){
        PNALDebugLog("staticDeviceIOThread [wait ant_rx_sem]");
        sem_wait(&pComPort->ant_rx_sem);    //wakeup upon InterfaceSend or when shutting down
        PNALDebugLog("staticDeviceIOThread [start recv...]");
        if( pComPort->shutdown == W_TRUE )
            break;


        //////   Waiting section   ////////////////
        CNALSyncEnterCriticalSection(&pComPort->mutex);
        if( pComPort->WaitingPeriod){
            usleep(pComPort->WaitingPeriod);
            pComPort->WaitingPeriod = 0;
            cbwait = pComPort->cbWait;
            pComPort->cbWait = NULL;
            if( cbwait ){
                cbwait(pComPort);
            }
            CNALSyncLeaveCriticalSection(&pComPort->mutex);
            continue;
        } else {
            CNALSyncLeaveCriticalSection(&pComPort->mutex);
        }


        if( pComPort->ant_recv_started == W_FALSE){    //transeive not started yet
            PNALDebugLog( "[IO] Waked-up, but transeive not started");
            continue;
        }

        /////   IO section   ////////////////////////
        CNALSyncEnterCriticalSection(&pComPort->mutex);
        //start transeiving
     
        recvlen = ms_interfaceTranseive(0xFFFFFFFF, pComPort->ant_recv_buffer, 2500);
        if( recvlen >=2 && Checksum_is_correct(pComPort->ant_recv_buffer, recvlen-2) ){
            PNALDebugLog( "[IO]recv checksum ok, len:%d\n", recvlen-2);
            recvlen -= 2;
        }else{
            PNALDebugLog( "[IO]err recvlen = %d\n", recvlen);
        }
        PNALDebugLog("staticDeviceIOThread recvlen =%d", recvlen);

        if( recvlen > 0 ){
            pComPort->ant_recv_started = W_FALSE;
			if (pComPort->recvDetectMask == W_TRUE)
		    {
                PNALDebugLog("staticDeviceIOThread recvlchangeDetectMask = TRUE");
				ms_card_detection(pComPort);
		    }
			else
			{
                pComPort->cbRecv(pComPort, pComPort->ant_recv_buffer, recvlen);		
                pComPort->p_rx_data = pComPort->rx_buffer;
	            PNALDebugLog( "[IO]pComPort->nb_available_bytes = %d\n", pComPort->nb_available_bytes);

	            if( pComPort->nb_available_bytes > 0){  //data ready, inform main thread
	                nResult = write(pComPort->aDataComSockets[1], &cmd, 1);
	            }				
	            PNALDebugLog( "[IO]write done");
			}
        }
        else{   //error case.
            PNALDebugError("staticDeviceIOThread recv error %d", recvlen);
            pComPort->ant_recv_started = W_FALSE;
            pComPort->cbRecv(pComPort, pComPort->ant_recv_buffer, recvlen);
            pComPort->p_rx_data = pComPort->rx_buffer;
            PNALDebugLog( "[IO]pComPort->nb_available_bytes = %d\n", pComPort->nb_available_bytes);
            //PNALDebugLog( "[IO]go to detect...");
            //if( pComPort->ant_recv_started == W_FALSE)
            	//ms_card_detection(pComPort);

        }
        CNALSyncLeaveCriticalSection(&pComPort->mutex);

        if (pComPort->bStandbyMode == W_TRUE)
        {
            PNALDebugLog( "[IO]bStandbyMode=W_TRUE");
            MS_PowerOff();
        }

        //custom_dev->ant_recv_started = W_FALSE;

    }
    PNALDebugLog("staticDeviceIOThread [end]");

    return NULL;
}

static void * staticDevIRQThread(void *arg)
{
    int irqSts = 0;
	tNALMsrComInstance* pComPort = static_PComGetInstance();
	PNALDebugLog("staticDevIRQThread [start]");
	while(1)
    {        
        PNALDebugLog("staticDevIRQThread [wait IRQ ant_irq_sem]");
		sem_wait(&pComPort->ant_irq_sem);
		pComPort->irq_semi_cnt--;
		PNALDebugLog("staticDevIRQThread [IRQ SEM Count= %d]", pComPort->irq_semi_cnt);			
		pComPort->getQueueStarted = W_TRUE;  
		PNALDebugLog("staticDevIRQThread [check IRQ start, getQueueStarted =%d]", pComPort->getQueueStarted);
        irqSts = ms_chk_dev_IRQ();
		pComPort->getQueueStarted = W_FALSE;
		PNALDebugLog("staticDevIRQThread [check IRQ result, getQueueStarted =%d]", pComPort->getQueueStarted);
		Stop_Chk_Reader_Alive(pComPort);
		if ((irqSts == NFC_IRQ_STS_ABORT) || (pComPort->bStandbyMode) || (pComPort->clearIRQFlag == W_TRUE))
		{
		    PNALDebugLog("staticDevIRQThread [Clear IRQ, don't getinf: IRQSts=%d]", irqSts);
			pComPort->clearIRQFlag = W_FALSE;
			continue;
		}
	    if (irqSts == NFC_IRQ_STS_RAISE){
			PNALDebugLog("staticDevIRQThread [check IRQ OK, do getinfo]");
			ms_detection_polling_getinfo(pComPort);
		}
		PNALDebugLog("staticDevIRQThread [check IRQ end]");
    }
    return NULL;
}

int Stop_Chk_Reader_Alive(tNALMsrComInstance* pComPort)
{
    PNALDebugLog("[Stop_Chk_Reader_Alive]..");
	pComPort->chkReaderAlive = W_FALSE;		
	pComPort->chkReaderAliveTimer = 0;
    return 0;	
}

int Start_IRQ_Detect(tNALMsrComInstance* pComPort)
{
    PNALDebugLog("[Start_IRQ_Thread]..");
    PNALDebugLog("[Start_IRQ_Thread]getQueueStarted =%d", pComPort->getQueueStarted);
    if (pComPort->getQueueStarted == W_FALSE)
    {
		pComPort->irq_semi_cnt++;
		PNALDebugLog("[Start_IRQ_Thread]IRQ SEM Count= %d", pComPort->irq_semi_cnt);
		sem_post(&pComPort->ant_irq_sem);   //wakeup irq thread           
    }
	else
	{
	    PNALDebugLog("[Start_IRQ_Thread]IRQ already started");
	}

#if (WatchDog_Enable == 1)
	{
        PNALDebugLog("[Start_IRQ_Thread]Enable ChkReaderAliveThread");
		WT_ResentDetectCmd= W_FALSE;	
		pComPort->chkReaderAliveTimer = 0;
		pComPort->chkReaderAlive = W_TRUE;
	}
#endif
    return 0;	
}

int Stop_IRQ_Detect(tNALMsrComInstance* pComPort)
{
    PNALDebugLog("[Stop_IRQ_Thread]..");
	PNALDebugLog("[Stop_IRQ_Thread]getQueueStarted =%d", pComPort->getQueueStarted);
	
#if (WatchDog_Enable == 1)
	Stop_Chk_Reader_Alive(pComPort);
#endif

	if (pComPort->getQueueStarted)
    {
        PNALDebugLog("[Stop_IRQ_Detect]Clear IRQ first");
		pComPort->clearIRQFlag = W_TRUE;
		//pComPort->getQueueStarted = W_FALSE;  
		ms_IRQ_abort();                
    }    
	else
	{
	    PNALDebugLog("[Stop_IRQ_Detect]IRQ is already stop");
	}
    return 0;	
}



int ms_chk_reader_alive_callback(tNALMsrComInstance  *dev, unsigned char *inbuf, int len)
{
    unsigned char extern_mask = 0x00;
    PNALDebugLog("[ms_chk_reader_alive_callback], len = %d\n", len);
	if (len <= 0)
	{
        PNALDebugError("[ms_chk_reader_alive_callback]len <= 0");
	}
	else
	{
        PNALDebugLogBuffer(inbuf, len);
	}
	if (len != 23)
	{
        PNALDebugLog("[ms_chk_reader_alive_callback]I2C Crash, Reset I2C\n");
		I2C_Reset();
        PNALDebugLog("[ms_chk_reader_alive_callback]I2C Crash, WatchDog Resend Detect Cmd\n");		
        ms_card_detection(dev);
		return 0;
	}
	
	if (dev->reader_detect_mask & NAL_PROTOCOL_READER_P2P_INITIATOR)
 	{
 	    extern_mask|= DrvPL_P2P_Initiator_424;  //I_424
 	}      
 	if (dev->card_detect_mask & NAL_PROTOCOL_CARD_P2P_TARGET)
 	{
 	    extern_mask|= DrvPL_P2P_Target_424;  //T_424
 	}	
	
	if (WT_ResentDetectCmd)
	{
		PNALDebugLog("[ms_chk_reader_alive_callback]WatchDog Resend Detect Cmd\n");
        ms_card_detection(dev);
		WT_ResentDetectCmd = W_FALSE;        		
	}
	else if(dev->getIRQFlag || inbuf[5] == 0x03)
	{
        PNALDebugLog("[ms_chk_reader_alive_callback]Get IRQ Info\n");
        Stop_Chk_Reader_Alive(dev);
		ms_detection_polling_getinfo(dev);
		dev->getIRQFlag= W_FALSE;
	}
#if (WatchDog_Check_Detect_Mask == 1)	
	else if ((inbuf[6]!= (dev->reader_detect_mask & 0x007F))
		  || (inbuf[7]!= ((dev->reader_detect_mask >> 8) & 0x00FF))
		  || (inbuf[9]!= ((dev->card_detect_mask >> 8) & 0x00FF))
		  || (inbuf[10]!= extern_mask)
		  )
	{
        PNALDebugLog("[ms_chk_reader_alive_callback]Detect Mask is change\n");
		if ((inbuf[18] != 0x01) || (inbuf[19] != 0x01) || (inbuf[20] != 0x01))
	    {
            PNALDebugLog("[ms_chk_reader_alive_callback]SWP Status error, DO I2C_Reset\n");
			I2C_Reset();
	    }
		PNALDebugLog("[ms_chk_reader_alive_callback]Detect Mask is change, Resend detect cmd\n");
		ms_card_detection(dev);
	}	
#endif	
	return 0;
}	

static void * staticChkReaderAliveThread(void *arg)
{
    int length = 0;
	tNALMsrComInstance* pComPort = null;
	unsigned char cmd_get_info[] = { 0x4B ,0x06 ,0x02 ,0x01 ,0x03 ,0x02 ,0x03 ,0x00 ,0x5C ,0x00};  
	PNALDebugLog("[staticChkReaderAliveThread] start");
	while(1)
    {        
        pComPort = static_PComGetInstance();
		
        if (pComPort->chkReaderAlive)
        {
            pComPort->chkReaderAliveTimer++;
        }        
		if (pComPort->chkReaderAliveTimer >= Check_Dev_Alive_Interval)
		{
            PNALDebugLog("[staticChkReaderAliveThread] Send Get Info Cmd");
			pComPort->chkReaderAliveTimer = 0;
			memcpy(pComPort->ant_send_buffer, cmd_get_info, sizeof(cmd_get_info));
			    /* Send command to Target */
            length = ms_interfaceSend(pComPort, sizeof(cmd_get_info));
			if (length > 0)
			{
                pComPort->cbRecv = (RecvCallBack *)ms_chk_reader_alive_callback;
			}			
		}
		
		usleep(1000);
    }
    return NULL;
}


static bool_t static_PComReadData(tNALMsrComInstance* pComPort)
{
    int32_t nResult;
    uint8_t event;

    //if( pComPort->nb_available_bytes == 0 )
    //{
        nResult = recv(pComPort->aDataComSockets[0], &event, 1, 0);
        if( nResult < 0 )
        {
            PNALDebugWarning("An error occured during the read operation on the communication port");
            CNALComDestroy(pComPort);
            return W_FALSE;
        }
        else
        {
            if( event != EVENT_DATA ){
                PNALDebugWarning("Wrong event received:%x", event);
                return W_FALSE;
            }
        }
    //}

    return W_TRUE;
}




/* See Functional Specifications Document */
tNALMsrComInstance* CNALComCreate(
    void* pPortingConfig,
    uint32_t* pnType )
{
    tNALMsrComInstance* pComPort = static_PComGetInstance();
    uint32_t nError;
    uint32_t addr;
    pComPort->p_rx_data = pComPort->rx_buffer;
    //sem_init(&pComPort->detect_sem, 0, 0);
    sem_init(&pComPort->ant_rx_sem, 0, 0);
	sem_init(&pComPort->ant_irq_sem, 0, 0);
    //sem_init(&pComPort->ant_rx_done_sem, 0, 0);

	//ms_interfaceInit(0);
	PNALDebugLog("CNALComCreate : ms_interfaceInit(1)");
	ms_interfaceInit(1);
	
    CNALSyncCreateCriticalSection(&pComPort->mutex);
    PNALDebugLog("CNALComCreate : socketpair()");
    if (socketpair(AF_UNIX, SOCK_STREAM, 0, pComPort->aDataComSockets) )
    {
        PNALDebugError("CNALComCreate : socketpair() failed %d", errno);
        return null;
    }
    PNALDebugLog("CNALComCreate : create hDispatcherThread");
    if (pthread_create(&pComPort->hDispatcherThread, NULL, staticDispatcherThread, NULL))
    {
        PNALDebugError("Can not create the event dispatcher thread");
        return null;
    }
    //if (pthread_create(&pComPort->hDetectionThread, NULL, staticDetectionThread, NULL))
    //{
    //    PNALDebugError("Can not create the event detection thread");
    //    return null;
    //}
    PNALDebugLog("CNALComCreate : create hDeviceIOThread");
    if (pthread_create(&pComPort->hDeviceIOThread, NULL, staticDeviceIOThread, NULL))
    {
        PNALDebugError("Can not create the event device-io thread");
        return null;
    }
	//For IRQ Event
	pComPort->getQueueStarted = W_FALSE;
	pComPort->recvDetectMask= W_FALSE;
	pComPort->sendDetectCmdDone = W_FALSE;
	pComPort->clearIRQFlag= W_FALSE;
	pComPort->irq_semi_cnt = 0;
	pComPort->swp_reset_cnt = 0;
	
    if (pthread_create(&pComPort->hDevIRQThread, NULL, staticDevIRQThread, NULL))
    {
        PNALDebugError("Can not create the event device-ioctl thread");
        return null;
    }	
    //For Reader Alive Check
    pComPort->chkReaderAlive= W_FALSE;
    if (pthread_create(&pComPort->hDevAliveChkThread, NULL, staticChkReaderAliveThread, NULL))
    {
        PNALDebugError("Can not create the event device-alive thread");
        return null;
    }	


    pComPort->nb_available_bytes = 0;
    pComPort->bInitialResetDone = W_TRUE;
    pComPort->nResetPending = 0;

    pComPort->wri_que_cur_idx = 0;
    pComPort->wri_que_next_idx = 0;
    pComPort->wri_que_total_cnt = 0;
	pComPort->temp_card_detect_poily = 0;
	pComPort->temp_reader_detect_poily = 0;
    *pnType = P_COM_TYPE_NFC_HAL_MSR3110_I2C;

	pComPort->ComReadWriteCtrl = 0;
	PNALDebugTrace("[CNALComCreate]pComPort->ComReadWriteCtrl: %d", pComPort->ComReadWriteCtrl);

    return pComPort;

return_error:

    *pnType = 0;

    return NULL;
}


/* See Functional Specifications Document */
void CNALComDestroy(
    tNALMsrComInstance* pComPort )
{
    uint8_t cmd[1];

    if(pComPort != NULL)
    {
        pComPort->shutdown = W_TRUE;
        //send an event to terminate DispatcherThread
        cmd[0] = EVENT_QUIT;
        write(pComPort->aDataComSockets[0], cmd, 1);
        pthread_join(pComPort->hDispatcherThread, NULL);

        sem_post(&pComPort->ant_rx_sem);
        pthread_join(pComPort->hDeviceIOThread, NULL);

		pthread_join(pComPort->hDevIRQThread, NULL);

		pthread_join(pComPort->hDevAliveChkThread, NULL);

        //sem_destroy(&pComPort->detect_sem);
        sem_destroy(&pComPort->ant_rx_sem);		
        sem_destroy(&pComPort->ant_irq_sem);
        //sem_destroy(&pComPort->ant_rx_done_sem);

        close(pComPort->aDataComSockets[0]);
        close(pComPort->aDataComSockets[1]);
        CNALSyncDestroyCriticalSection(&pComPort->mutex);
        ms_interfaceClose();
    }
}


/* See Functional Specifications Document */
uint32_t CNALComReadBytes(
    tNALMsrComInstance* pComPort,
    uint8_t* pReadBuffer,
    uint32_t nBufferLength)
{
    PNALDebugLog("CNALComReadBytes start...");
    PNALDebugLog("CNALComReadBytes:  Input nBufferLength= %d", nBufferLength);
    PNALDebugLog("CNALComReadBytes:  pComPort->nb_available_bytes= %d", pComPort->nb_available_bytes);
    if(pComPort->nb_available_bytes != 0)
    {
        if(nBufferLength <= (uint32_t)pComPort->nb_available_bytes)
        {
            //PNALDebugLog("CNALComCreate [1]");
            CNALMemoryCopy(pReadBuffer, pComPort->p_rx_data, nBufferLength);
            pComPort->nb_available_bytes -= nBufferLength;
            pComPort->p_rx_data += nBufferLength;
        }
        else
        {
            //PNALDebugLog("CNALComCreate [2]");
            //PNALDebugLog("CNALComCreate:  pReadBuffer= %x", pReadBuffer);
            //PNALDebugLog("CNALComCreate:  pComPort->p_rx_data= %x", pComPort->p_rx_data);
            CNALMemoryCopy(pReadBuffer, pComPort->p_rx_data, pComPort->nb_available_bytes);
            nBufferLength = pComPort->nb_available_bytes;
            pComPort->nb_available_bytes = 0;
        }
    }
    else
    {
        //PNALDebugLog("CNALComCreate [3]");
        nBufferLength = 0;
    }
    PNALDebugLog("CNALComReadBytes:  return nBufferLength= %d", nBufferLength);
    PNALDebugLog("CNALComReadBytes end...");
    return nBufferLength;
}

bool_t Check_Detect_Cmd(uint8_t* inbuf, uint32_t buflen)
{
    if ((inbuf[0]==NAL_SERVICE_ADMIN) && (inbuf[1]== NAL_CMD_DETECTION))
    {
        return W_TRUE;
    }
    else
    {
        return W_FALSE;
    }

}

int Reset_Write_Queue(tNALMsrComInstance* pComPort)
{
    PNALDebugLog("[Reset_Write_Queue] start...");
    pComPort->wri_que_next_idx = 0;
    pComPort->wri_que_total_cnt = 0;
    pComPort->wri_que_cur_idx = 0;
    return 0;
}
/* See Functional Specifications Document */
uint32_t CNALComWriteBytes(
    tNALMsrComInstance* pComPort,
    uint8_t* pBuffer,
    uint32_t nBufferLength )
{
    PNALDebugLog("[jim]CNALComWriteBytes start...");
    /*
    if (Check_Detect_Cmd(pBuffer, nBufferLength) == W_TRUE)
    {
        PNALDebugLog("[CNALComWriteBytes]Found detect cmd");
        Reset_Write_Queue(pComPort);
    }
    */
    if ((pComPort->wri_que_next_idx == pComPort->wri_que_cur_idx) & (pComPort->wri_que_total_cnt >= WRITE_QUEUE_SIZE))
    {
        PNALDebugError("CNALComWriteBytes: Write Queue is not enough");
        return 0;
    }

	if( ( pBuffer[1] & 0xF0) == NAL_MESSAGE_TYPE_COMMAND)
	{
		pComPort->ComReadWriteCtrl++;
		PNALDebugTrace("[CNALComWriteBytes]pComPort->ComReadWriteCtrl: %d", pComPort->ComReadWriteCtrl);
	}

    PNALDebugLog("[jimmy]CNALComWriteBytes: wri_que_next_idx=%d", pComPort->wri_que_next_idx);
    PNALDebugLog("[jimmy]CNALComWriteBytes: wri_que_cur_idx=%d", pComPort->wri_que_cur_idx);
    PNALDebugLog("[jimmy]CNALComWriteBytes: wri_que_total_cnt=%d", pComPort->wri_que_total_cnt);
    pComPort->write_queue[pComPort->wri_que_next_idx].wri_buf_len = nBufferLength;
    CNALMemoryCopy(pComPort->write_queue[pComPort->wri_que_next_idx].wri_buf, pBuffer, nBufferLength);
    PNALDebugLog("[jim]CNALComWriteBytes Write Buf len=%d, buf:", pComPort->write_queue[pComPort->wri_que_next_idx].wri_buf_len);
    PNALDebugLogBuffer(pComPort->write_queue[pComPort->wri_que_next_idx].wri_buf,
        pComPort->write_queue[pComPort->wri_que_next_idx].wri_buf_len);
    if (++(pComPort->wri_que_next_idx) >= WRITE_QUEUE_SIZE)
    {
        pComPort->wri_que_next_idx = 0;
    }

    //if( write(pComPort->aDataComSockets[0], pBuffer, nBufferLength) > 0)

    if (pComPort->wri_que_total_cnt != 0)
    {
        pComPort->wri_que_total_cnt++;
        PNALDebugLog("[jim]only write, CNALComWriteBytes end...");
        return 0;
    }
    pComPort->wri_que_total_cnt++;
    if( write(pComPort->aDataComSockets[0], pComPort->write_queue[pComPort->wri_que_cur_idx].wri_buf
        , pComPort->write_queue[pComPort->wri_que_cur_idx].wri_buf_len) > 0)
    {
        PNALDebugLog("[jim]start write, CNALComWriteBytes end...");
        return pComPort->write_queue[pComPort->wri_que_cur_idx].wri_buf_len;
    }
    PNALDebugLog("[jim]CNALComWriteBytes end(return 0)...");
    return 0;
}

void CNALWriteDataFinish(tNALMsrComInstance* pComPort)
{
    PNALDebugLog("[jimmy]CNALWriteDataFinish start...");
    PNALDebugLog("[jimmy]CNALWriteDataFinish wri_que_total_cnt =%d", pComPort->wri_que_total_cnt);
    if (pComPort->wri_que_total_cnt <= 0)
    {
        return;
    }
    pComPort->wri_que_total_cnt--;
    if (++pComPort->wri_que_cur_idx >= WRITE_QUEUE_SIZE)
    {
        pComPort->wri_que_cur_idx = 0;
    }
    if (pComPort->wri_que_total_cnt > 0)
    {
        PNALDebugLog("[jim]CNALWriteDataFinish send next, wri_que_cur_idx=%d", pComPort->wri_que_cur_idx);
        PNALDebugLog("[jim]CNALWriteDataFinish Write Buf len=%d, buf:", pComPort->write_queue[pComPort->wri_que_cur_idx].wri_buf_len);
        PNALDebugLogBuffer(pComPort->write_queue[pComPort->wri_que_cur_idx].wri_buf
            , pComPort->write_queue[pComPort->wri_que_cur_idx].wri_buf_len);
        if( write(pComPort->aDataComSockets[0], pComPort->write_queue[pComPort->wri_que_cur_idx].wri_buf
            , pComPort->write_queue[pComPort->wri_que_cur_idx].wri_buf_len) > 0)
        {
            PNALDebugLog("[jim]CNALWriteDataFinish, write success");
        }
    }
    PNALDebugLog("[jimmy]CNALWriteDataFinish end...");
    return;
}

/*******************************************************************************

  Reset Functions

*******************************************************************************/

/* See Functional Specifications Document */
void CNALResetNFCController(
    void* pPortingConfig,
    uint32_t nResetType )
{
    tNALMsrComInstance* pComPort = static_PComGetInstance();
    uint8_t nValue = (uint8_t)nResetType;

    PNALDebugLog("*****************  Reseting NFC Controller *********************");

    pComPort->bInitialResetDone = W_TRUE;
#if 0
    pComPort->nResetPending++;

    if(CCClientSendDataEx(pComPort->pNFCCConnection, P_COM_RESET_FLAG, &nValue, 1) == 0)
    {
        PNALDebugWarning("Error returned by CCClientSendData()\n");

        pComPort->bInitialResetDone = W_FALSE;
        pComPort->nResetPending--;
    }
#endif
}

/*
 * Thread dealing with the call of poll function of the NAL binding
 */

void * PDriverReceiveThread(void * arg)
{
    tNALInstance   * pNALInstance = static_PNALGetInstance();
    tNALMsrComInstance    * pComPort = static_PComGetInstance();
    tNALTimerInstance  * pTimer = static_PTimerGetInstance();

    fd_set             readfds;
    int               data_fd, ctrl_fd, max_fd;

    uint32_t nTimeoutMs;
    struct timeval   tv;
    int res;
    uint8_t buffer[256];

    data_fd = pComPort->aDataComSockets[0];
    ctrl_fd = pNALInstance->aWakeUpSockets[1];


    if (data_fd > ctrl_fd)
    {
        max_fd = data_fd + 1;
    }
    else
    {
        max_fd = ctrl_fd + 1;
    }
    for (;;)
    {
        bool_t bCallPoll = W_FALSE;

        FD_ZERO(&readfds);
        FD_SET(data_fd, &readfds);
        FD_SET(ctrl_fd, &readfds);
        if (pTimer->nTimerValue != 0)
        {
            uint32_t nCurrentTime = CNALTimerGetCurrentTime(pTimer);

            if (pTimer->nTimerValue > nCurrentTime)
            {
                nTimeoutMs = pTimer->nTimerValue - nCurrentTime;
                tv.tv_sec = nTimeoutMs / 1000;
                tv.tv_usec = (nTimeoutMs - tv.tv_sec * 1000) * 1000;
                res = select(max_fd, &readfds, NULL, NULL, &tv);
            }
            else
            {
                res = 0;
            }
        }
        else
        {
            res = select(max_fd, &readfds, NULL, NULL, NULL);
        }
        PNALDebugLog("PDriverReceiveThread : select res :%d\n", res);
        if (res < 0)
        {
            if (errno != EINTR)
            {
                PNALDebugError("PDriverReceiveThread : select failed : errno %d\n", errno);
                pthread_exit((void *) -1);
            }
        }
        else if (res > 0)
        {
            if (FD_ISSET(data_fd, &readfds))
            {
                PNALDebugLog("PDriverReceiveThread : static_PComReadData start");
                /* some data has been received */
                static_PComReadData(pComPort);
                bCallPoll = W_TRUE;
                PNALDebugLog("PDriverReceiveThread : static_PComReadData end");
            }
            else
            {
                /* process data received on the control socket */
                int ret, i;
                PNALDebugLog("FD_ISSET = W_FALSE");
                ret = recv(ctrl_fd, buffer, sizeof(buffer), 0);

                if (((ret < 0) && (errno != EINTR)) || (ret == 0))
                {
                    /* something goes wrong with the control socket, this typically occurs when exitin the service */

                    pthread_exit((void *) -1);
                }


                for (i=0; i<ret; i++)
                {
                    switch (buffer[i])
                    {
                    case EVENT_QUIT :
                        pthread_exit((void *) -1);
                        break;

                    case EVENT_KICK :
                        bCallPoll= W_TRUE;
                        break;
                    }
                }
            }
        }
        else
        {
            /* select returned due to a timeout */
            static_CNALTimerSetExpired(pTimer);
            bCallPoll = W_TRUE;
        }

        if (bCallPoll == W_TRUE)
        {
            do
            {
                PNALDebugLog("PDriverReceiveThread : pPollFunction start");
                g_pNALBinding->pPollFunction(arg);

            }
            while(pComPort->nb_available_bytes != 0);
        }
    }
}



void CNALSyncTriggerEventPump(
    void* pPortingConfig )
{
    tNALInstance * pNALInstance = static_PNALGetInstance();
    uint8_t buff[1];

    buff[0] = EVENT_KICK;
    write(pNALInstance->aWakeUpSockets[0], buff, 1);
}

/* See Functional Specification Document */
tNALInstance * CNALPreCreate(void * pPortingConfig)
{
    tNALInstance * pNALInstance = static_PNALGetInstance();

    CNALMemoryFill(pNALInstance, 0, sizeof(* pNALInstance));

    if (socketpair(AF_UNIX, SOCK_STREAM, 0, pNALInstance->aWakeUpSockets))
    {
        PNALDebugError("CNALPreCreate : socketpair() failed %d", errno);

        return null;
    }

    return pNALInstance;
}


/* See Functional Specification Document */
bool_t CNALPostCreate(tNALInstance * pNALInstance, void * pNALVoidInstance)
{
    if (pthread_create(&pNALInstance->hProcessDriverEventsThread, NULL, PDriverReceiveThread, pNALVoidInstance))
    {
        PNALDebugError("Can not create the event processing thread");
        return W_FALSE;
    }

    return W_TRUE;
}

/* See Functional Specification Document */
void CNALPreDestroy(tNALInstance * pNALInstance)
{
    if (pNALInstance->hProcessDriverEventsThread)
    {
        uint8_t buff[1];

        buff[0] = EVENT_QUIT;
        write(pNALInstance->aWakeUpSockets[0], buff, 1);
        pthread_join(pNALInstance->hProcessDriverEventsThread, NULL);
    }
}


/* See Functional Specification Document */
void CNALPostDestroy(tNALInstance * pNALInstance)
{
    if (pNALInstance)
    {
        close(pNALInstance->aWakeUpSockets[0]);
        close(pNALInstance->aWakeUpSockets[1]);
    }
}

void StopNALEventLoop(void)
{
   ssize_t nResult;

   tNALInstance * pNALInstance = static_PNALGetInstance();

   if (pNALInstance->hProcessDriverEventsThread)
   {
      uint8_t buff[1];

      buff[0] = EVENT_QUIT;
      nResult = write(pNALInstance->aWakeUpSockets[0], buff, 1);
      if (nResult <= 0)
      {
         PNALDebugError("%s: write error %zd", __FUNCTION__, nResult);
      }
      pthread_join(pNALInstance->hProcessDriverEventsThread, NULL);
   }
}


tNALBinding  * GetNALBinding(void)
{

    PNALDebugError("GetNALBinding mstar...");

    return g_pNALBinding;
}

bool_t SetEventAvailableBytes(tNALMsrComInstance* pComPort, int nByteLen)
{
    if (pComPort->ComReadWriteCtrl > 0)
    {
        PNALDebugError("[SetEventAvailableBytes]ComReadWriteCtrl: %d", pComPort->ComReadWriteCtrl);
        PNALDebugError("[SetEventAvailableBytes]Event can't report");
        return W_FALSE;
    }
    memcpy( pComPort->rx_buffer, pComPort->temp_rx_buffer, nByteLen );
    pComPort->nb_available_bytes = nByteLen;
    return W_TRUE;
}

/* EOF */
