/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2012
*
*  BY OPENING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
*  THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
*  RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON
*  AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
*  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
*  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
*  NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
*  SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
*  SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK ONLY TO SUCH
*  THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
*  NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S
*  SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
*
*  BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
*  LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
*  AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
*  OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY BUYER TO
*  MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE. 
*
*  THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE
*  WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
*  LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
*  RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
*  THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
*
*****************************************************************************/

/*******************************************************************************
 * Filename:
 * ---------
 *  mtk_nfc_osal.c
 *
 * Project:
 * --------
 *
 * Description:
 * ------------
 *
 * Author:
 * -------
 *  Hiki Chen, ext 25281, hiki.chen@mediatek.com, 2012-05-10
 * 
 *******************************************************************************/
/***************************************************************************** 
 * Include
 *****************************************************************************/ 
#ifdef WIN32
#include <windows.h>
#include <assert.h>
#endif
#include "mtk_nfc_sys_type.h"
//#include "mtk_nfc_status.h"
#include "mtk_nfc_sys.h"

#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>  /* UNIX standard function definitions */
#include <fcntl.h>   /* File control definitions */
#include <errno.h>   /* Error number definitions */
#include <termios.h> /* POSIX terminal control definitions */
#include <signal.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/wait.h>
#include <sys/ipc.h>
#include <sys/time.h>
#include <sys/timeb.h>
#include <sys/ioctl.h>

#include <utils/Log.h> // For Debug



/***************************************************************************** 
 * Define
 *****************************************************************************/
#define WM_MTK_NFC_TASK  (WM_USER)
#define LOG_TAG "NFC-MW"


/***************************************************************************** 
 * Data Structure
 *****************************************************************************/
 
#ifdef WIN32
typedef struct
{
    CRITICAL_SECTION    cs;
    BOOL                is_used;    // 1 = used; 0 = unused    
    UINT32              timer_id;   //timer's id returned from SetTimer()
    ppCallBck_t         timer_expiry_callback;
    VOID                *timer_expiry_context;
} nfc_timer_table_struct;
#endif
/***************************************************************************** 
 * Extern Area
 *****************************************************************************/ 
//extern DWORD g_dwNfcRxHdlrThreadId;
//extern DWORD g_dwNfcMainThreadId;

int gconn_fd_tmp = (-1);

int gInterfaceHandle;



/***************************************************************************** 
 * GLobal Variable
 *****************************************************************************/ 
static pthread_mutex_t g_hMutex[MTK_NFC_MUTEX_MAX_NUM];

#define MTK_NFC_MSG_RING_SIZE 128

MTK_NFC_MSG_RING_BUF nfc_main_msg_ring_body;
MTK_NFC_MSG_RING_BUF * nfc_main_msg_ring = NULL;
MTK_NFC_MSG_T * nfc_main_msg_ring_buffer[MTK_NFC_MSG_RING_SIZE]; //pointer array
INT32 nfc_main_msg_cnt;


MTK_NFC_MSG_RING_BUF nfc_service_msg_ring_body;
MTK_NFC_MSG_RING_BUF * nfc_service_msg_ring = NULL;
MTK_NFC_MSG_T * nfc_service_msg_ring_buffer[MTK_NFC_MSG_RING_SIZE]; //pointer array
INT32 nfc_service_msg_cnt;


/***************************************************************************** 
 * Function
 *****************************************************************************/ 
    
NFCSTATUS mtk_nfc_sys_i2c_read(UINT8* data, UINT16 len);
    
NFCSTATUS mtk_nfc_sys_i2c_write(UINT8* data, UINT16 len);



/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_mem_alloc
 * DESCRIPTION
 *  Allocate a block of memory
 * PARAMETERS
 *  size [IN] the length of the whole memory to be allocated
 * RETURNS
 *  On success, return the pointer to the allocated memory
 * NULL (0) if failed
 *****************************************************************************/ 
VOID *
mtk_nfc_sys_mem_alloc (
    UINT32 u4Size
)
{
    void *pMem = NULL;

    //ALOGD("MALLOC,%d,",u4Size);
    if (u4Size != 0)
    {
        //ALOGD("MALLOC,address,%x,",pMem);
        pMem = malloc(u4Size);
        //ALOGD("MALLOC,address2,%x,",pMem);
    }
    return pMem;

}

/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_mem_free
 * DESCRIPTION
 *  Release unused memory
 * PARAMETERS
 *  pMem        [IN] the freed memory address
 * RETURNS
 *  NONE
 *****************************************************************************/ 
VOID 
mtk_nfc_sys_mem_free (
    VOID *pMem
)
{
   if (pMem != NULL)
   {
       //ALOGD("FREE,address,%x,",pMem);
       free(pMem);       
       //ALOGD("FREE,address2,%x,",pMem);
       pMem=NULL;
   }
    return;
}

/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_mutex_inialize
 * DESCRIPTION
 *  Create a mutex object
 * PARAMETERS
 *  mutex_id    [IN] mutex index used by NFC library
 * RETURNS
 *  MTK_NFC_SUCCESS
 *****************************************************************************/ 
INT32 
mtk_nfc_sys_mutex_inialize (
    void
)
{
    INT8 index;
    // - TBD
    for (index = 0; index < MTK_NFC_MUTEX_MAX_NUM; index++)
    {
        pthread_mutex_init(&g_hMutex[index], NULL);
    }

    return MTK_NFC_SUCCESS;
}


/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_mutex_create
 * DESCRIPTION
 *  Create a mutex object
 * PARAMETERS
 *  mutex_id    [IN] mutex index used by NFC library
 * RETURNS
 *  MTK_NFC_SUCCESS
 *****************************************************************************/ 
INT32 
mtk_nfc_sys_mutex_create (
    MTK_NFC_MUTEX_E mutex_id
)
{
    // - TBD
    if (mutex_id >= MTK_NFC_MUTEX_MAX_NUM)
    {
        return MTK_NFC_ERROR;
    }

    pthread_mutex_init(&g_hMutex[mutex_id], NULL);
    return MTK_NFC_SUCCESS;
}

/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_mutex_take
 * DESCRIPTION
 *  Request ownership of a mutex and if it's not available now, then block the
 *  thread execution
 * PARAMETERS
 *  mutex_id    [IN] mutex index used by NFC library
 * RETURNS
 *  MTK_NFC_SUCCESS
 *****************************************************************************/ 
INT32 
mtk_nfc_sys_mutex_take (
    MTK_NFC_MUTEX_E mutex_id
)
{
    // - TBD
    pthread_mutex_lock(&g_hMutex[mutex_id]);
    return MTK_NFC_SUCCESS;
}

/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_mutex_give
 * DESCRIPTION
 *  Release a mutex ownership
 * PARAMETERS
 *  mutex_id    [IN] mutex index used by NFC library
 * RETURNS
 *  MTK_NFC_SUCCESS
 *****************************************************************************/ 
INT32 
mtk_nfc_sys_mutex_give (
    MTK_NFC_MUTEX_E mutex_id
)
{
    // - TBD
    pthread_mutex_unlock(&g_hMutex[mutex_id]);
    return MTK_NFC_SUCCESS;
}

/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_mutex_destory
 * DESCRIPTION
 *  Destory a mutex object
 * PARAMETERS
 *  mutex_id    [IN] mutex index used by NFC library
 * RETURNS
 *  MTK_NFC_SUCCESS
 *****************************************************************************/ 
INT32 
mtk_nfc_sys_mutex_destory (
    MTK_NFC_MUTEX_E mutex_id
)
{
    // - TBD
    
    if (pthread_mutex_destroy(&g_hMutex[mutex_id]))
    {
        return MTK_NFC_ERROR;
    }
    return MTK_NFC_SUCCESS;
}

/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_msg_alloc
 * DESCRIPTION
 *  Allocate a block of memory for message
 * PARAMETERS
 *  u2Size      [IN] the length of the whole MTK_NFC_MSG structure
 * RETURNS
 *  Pinter to the created message if successed
 *  NULL (0) if failed
 *****************************************************************************/ 
MTK_NFC_MSG_T *
mtk_nfc_sys_msg_alloc (
    UINT16 u2Size
)
{
    return mtk_nfc_sys_mem_alloc(u2Size);
}

/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_msg_inialize
 * DESCRIPTION
 *  Send a message to a task
 * PARAMETERS
 *  task_id     [IN] target task id
 *  msg         [IN] the send message
 * RETURNS
 *  MTK_NFC_SUCCESS
 *****************************************************************************/ 
INT32 
mtk_nfc_sys_msg_inialize (
    void
)
{
    //For Main Message
    nfc_main_msg_ring = &nfc_main_msg_ring_body;
    nfc_main_msg_ring->start_buffer = &nfc_main_msg_ring_buffer[0];
    nfc_main_msg_ring->end_buffer = &nfc_main_msg_ring_buffer[MTK_NFC_MSG_RING_SIZE-1];
    nfc_main_msg_ring->next_write = nfc_main_msg_ring->start_buffer;
    nfc_main_msg_ring->next_read = nfc_main_msg_ring->start_buffer;
    nfc_main_msg_cnt = 0;

    //For Service Message
    nfc_service_msg_ring = &nfc_service_msg_ring_body;
    nfc_service_msg_ring->start_buffer = &nfc_service_msg_ring_buffer[0];
    nfc_service_msg_ring->end_buffer = &nfc_service_msg_ring_buffer[MTK_NFC_MSG_RING_SIZE-1];
    nfc_service_msg_ring->next_write = nfc_service_msg_ring->start_buffer;
    nfc_service_msg_ring->next_read = nfc_service_msg_ring->start_buffer;
    nfc_service_msg_cnt = 0;

    return MTK_NFC_SUCCESS;
}


/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_msg_send
 * DESCRIPTION
 *  Send a message to a task
 * PARAMETERS
 *  task_id     [IN] target task id
 *  msg         [IN] the send message
 * RETURNS
 *  MTK_NFC_SUCCESS
 *****************************************************************************/ 
INT32 
mtk_nfc_sys_msg_send (
    MTK_NFC_TASKID_E task_id, 
    const MTK_NFC_MSG_T *msg
)
{
    #if 1//def DEBUG_LOG
    ALOGD("Send message type:%d\n",task_id);
    #endif
  
    if (msg == NULL)
    {
       // MNL_DEBUG_OUTPUT(MDBG_MSG, DBG_ERR,"MNLMsgS", "NULL", "");
        return MTK_NFC_ERROR;
    }

    if (MTK_NFC_TASKID_MAIN == task_id)
    {
        mtk_nfc_sys_mutex_take(MTK_MUTEX_MSG_Q);
        /*buffer full*/
        if(nfc_main_msg_cnt == MTK_NFC_MSG_RING_SIZE)
        {
            mtk_nfc_sys_mutex_give(MTK_MUTEX_MSG_Q);
            //MNL_DEBUG_OUTPUT(MDBG_MSG, DBG_ERR,"MNLMsgS", "FULL", "");
            return MTK_NFC_ERROR;
        }
        
        if ( nfc_main_msg_ring != NULL)
        {
            *(nfc_main_msg_ring->next_write) = (MTK_NFC_MSG_T*)msg;
        
            nfc_main_msg_ring->next_write++;
        
            // Wrap check the input circular buffer
            if ( nfc_main_msg_ring->next_write > nfc_main_msg_ring->end_buffer )
            {
                nfc_main_msg_ring->next_write = nfc_main_msg_ring->start_buffer;
            }
        
            mtk_nfc_sys_mutex_take(MTK_MUTEX_MSG_CNT);
            nfc_main_msg_cnt++;
            mtk_nfc_sys_mutex_give(MTK_MUTEX_MSG_CNT);
            mtk_nfc_sys_mutex_give(MTK_MUTEX_MSG_Q);
            
            return MTK_NFC_SUCCESS;
        }
        else
        {
            mtk_nfc_sys_mutex_give(MTK_MUTEX_MSG_Q);
            //MNL_DEBUG_OUTPUT(MDBG_MSG, DBG_ERR,"MNLBufS", "NULL", "");
            return MTK_NFC_ERROR;
        }

    }
    else if (MTK_NFC_TASKID_SERVICE == task_id)
    {
        mtk_nfc_sys_mutex_take(MTK_MUTEX_SERVICE_MSG_Q);
        /*buffer full*/
        if(nfc_service_msg_cnt == MTK_NFC_MSG_RING_SIZE)
        {
            mtk_nfc_sys_mutex_give(MTK_MUTEX_SERVICE_MSG_Q);
            //MNL_DEBUG_OUTPUT(MDBG_MSG, DBG_ERR,"MNLMsgS", "FULL", "");
            #if 1//def DEBUG_LOG
            ALOGD("Send message to service, full\n");
            #endif
            return MTK_NFC_ERROR;
        }
        
        if ( nfc_service_msg_ring != NULL)
        {
            *(nfc_service_msg_ring->next_write) = (MTK_NFC_MSG_T*)msg;
        
            nfc_service_msg_ring->next_write++;
        
            // Wrap check the input circular buffer
            if ( nfc_service_msg_ring->next_write > nfc_service_msg_ring->end_buffer )
            {
                nfc_service_msg_ring->next_write = nfc_service_msg_ring->start_buffer;
            }
        
            mtk_nfc_sys_mutex_take(MTK_MUTEX_SERVICE_MSG_CNT);
            nfc_service_msg_cnt++;
            mtk_nfc_sys_mutex_give(MTK_MUTEX_SERVICE_MSG_CNT);
            mtk_nfc_sys_mutex_give(MTK_MUTEX_SERVICE_MSG_Q);
            #if 1//def DEBUG_LOG
            ALOGD("Send message to service, success\n");
            #endif
            
            return MTK_NFC_SUCCESS;
        }
        else
        {
            mtk_nfc_sys_mutex_give(MTK_MUTEX_SERVICE_MSG_Q);
            #if 1//def DEBUG_LOG
            ALOGD("Send message to service, fail, null\n");
            #endif
            //MNL_DEBUG_OUTPUT(MDBG_MSG, DBG_ERR,"MNLBufS", "NULL", "");
            return MTK_NFC_ERROR;
        }

    }
    else if (MTK_NFC_TASKID_SOCKET == task_id)
    {
        int32_t ret;
        //printf("mtk_nfc_sys_msg_send: gconn_fd_tmp,%d,%d\n",gconn_fd_tmp,(sizeof(MTK_NFC_MSG_T) + msg->length)); 
        //printf("%x,%x\n",msg->length,msg->type);
        
        ret = write(gconn_fd_tmp, msg, (sizeof(MTK_NFC_MSG_T) + msg->length)); 

        #if 1//def DEBUG_LOG
        ALOGD("mtk_nfc_sys_msg_send: ret,%d\n",ret); 
        #endif
        mtk_nfc_sys_mem_free( (VOID*)msg);

        return MTK_NFC_SUCCESS;

    }
    else
    {
        return MTK_NFC_ERROR;
    }

    return MTK_NFC_ERROR;
}

/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_msg_recv
 * DESCRIPTION
 *  Recv a message from a task
 * PARAMETERS
 *  task_id     [IN] target task id
 *  msg         [IN] the receive message pointer
 * RETURNS
 *  MTK_NFC_SUCCESS
 *****************************************************************************/ 
INT32 
mtk_nfc_sys_msg_recv (
    MTK_NFC_TASKID_E task_id, 
    MTK_NFC_MSG_T **msg
)
{
    if (msg != NULL)
    {
        if (MTK_NFC_TASKID_MAIN == task_id)
        {
             mtk_nfc_sys_mutex_take(MTK_MUTEX_MSG_Q);
             if ( nfc_main_msg_ring->next_write == nfc_main_msg_ring->next_read )
             {   
                 mtk_nfc_sys_mutex_give(MTK_MUTEX_MSG_Q);
                 return MTK_NFC_ERROR;
             }    
            
             if (*(nfc_main_msg_ring->next_read) == NULL)
             {
                 mtk_nfc_sys_mutex_give(MTK_MUTEX_MSG_Q);
                 return MTK_NFC_ERROR;
             }    
            (*msg) = *(nfc_main_msg_ring->next_read);
            
            
            nfc_main_msg_ring->next_read++;
            
            // Wrap check output circular buffer
            if ( nfc_main_msg_ring->next_read > nfc_main_msg_ring->end_buffer )
            {
                nfc_main_msg_ring->next_read = nfc_main_msg_ring->start_buffer;
            }
            
            mtk_nfc_sys_mutex_take(MTK_MUTEX_MSG_CNT);
            nfc_main_msg_cnt--;
            mtk_nfc_sys_mutex_give(MTK_MUTEX_MSG_CNT);
            mtk_nfc_sys_mutex_give(MTK_MUTEX_MSG_Q);
            
            return MTK_NFC_SUCCESS;
        }
        else if(MTK_NFC_TASKID_SERVICE == task_id)
        {
             mtk_nfc_sys_mutex_take(MTK_MUTEX_SERVICE_MSG_Q);
             if ( nfc_service_msg_ring->next_write == nfc_service_msg_ring->next_read )
             {   
                 mtk_nfc_sys_mutex_give(MTK_MUTEX_SERVICE_MSG_Q);
                 return MTK_NFC_ERROR;
             }    
            
             if (*(nfc_service_msg_ring->next_read) == NULL)
             {
                 mtk_nfc_sys_mutex_give(MTK_MUTEX_SERVICE_MSG_Q);
                 return MTK_NFC_ERROR;
             }    
            (*msg) = *(nfc_service_msg_ring->next_read);
            
            
            nfc_service_msg_ring->next_read++;
            
            // Wrap check output circular buffer
            if ( nfc_service_msg_ring->next_read > nfc_service_msg_ring->end_buffer )
            {
                nfc_service_msg_ring->next_read = nfc_service_msg_ring->start_buffer;
            }
            
            mtk_nfc_sys_mutex_take(MTK_MUTEX_SERVICE_MSG_CNT);
            nfc_service_msg_cnt--;
            mtk_nfc_sys_mutex_give(MTK_MUTEX_SERVICE_MSG_CNT);
            mtk_nfc_sys_mutex_give(MTK_MUTEX_SERVICE_MSG_Q);
            
            return MTK_NFC_SUCCESS;
        }
        else
        {
           return MTK_NFC_ERROR;
        }

   }
   else
   {
       return MTK_NFC_ERROR;
   }
    return MTK_NFC_SUCCESS;
}

/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_msg_free
 * DESCRIPTION
 *  Free a block of memory for message
 * PARAMETERS
 *  msg         [IN] the freed message
 * RETURNS
 *  NONE
 *****************************************************************************/ 
VOID 
mtk_nfc_sys_msg_free (
    MTK_NFC_MSG_T *msg
)
{
	#if 0
    if (msg != NULL)
    {
        mtk_nfc_sys_mem_free(msg);
    }
    #endif
    mtk_nfc_sys_mem_free(msg);
    return;
}


/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_dbg_string
 * DESCRIPTION
 *  Output a given string
 * PARAMETERS
 *  pString     [IN] pointer to buffer content to be displayed
 * RETURNS
 *  NONE
 *****************************************************************************/ 
VOID
mtk_nfc_sys_dbg_string (
    const CH *pString
)
{
    ALOGD("%s", pString);
    #if 0
    printf("%s", pString);
    #endif
    return;
}

/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_dbg_trace
 * DESCRIPTION
 *  Output the traced raw data
 * PARAMETERS
 *  pString     [IN] data Data block
 *  length      [IN] size buffer size of the data block
 * RETURNS
 *  NONE
 *****************************************************************************/ 
VOID
mtk_nfc_sys_dbg_trace (
    UINT8   pData[], 
    UINT32  u4Len
)
{
	  #if 0
    UINT32 i;
    
    for (i = 0; i < u4Len; i++)
    {
        printf("%02X,",*(pData+i));
    }
    #endif

    return;
}

/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_timer_create
 * DESCRIPTION
 *  Create a new timer
 * PARAMETERS
 *  NONE
 * RETURNS
 *  a valid timer ID or MTK_NFC_TIMER_INVALID_ID if an error occured
 *****************************************************************************/ 
UINT32 
mtk_nfc_sys_timer_create (
    VOID
)
{
    // - TBD
    
    return MTK_NFC_TIMER_INVALID_ID;
}

/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_timer_start
 * DESCRIPTION
 *  Start a timer
 * PARAMETERS
 *  timer_id    [IN] a valid timer id
 *  period      [IN] expiration time in milliseconds
 *  timer_expiry[IN] callback to be called when timer expires
 *  arg         [IN] callback fucntion parameter
 * RETURNS
 *  NONE
 *****************************************************************************/ 
VOID 
mtk_nfc_sys_timer_start (UINT32 timer_slot, UINT32 period, ppCallBck_t timer_expiry, VOID *arg)
{
    // - TBD
    
    return;
}

/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_timer_stop
 * DESCRIPTION
 *  Start a timer
 * PARAMETERS
 *  timer_id    [IN] a valid timer id
 * RETURNS
 *  NONE
 *****************************************************************************/ 
VOID 
mtk_nfc_sys_timer_stop (
    MTK_NFC_TIMER_E timer_id
)
{
    // - TBD
    
    return;
}

/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_timer_delete
 * DESCRIPTION
 *  Delete a timer
 * PARAMETERS
 *  timer_id    [IN] a valid timer id
 * RETURNS
 *  NONE
 *****************************************************************************/ 
VOID
mtk_nfc_sys_timer_delete (
    MTK_NFC_TIMER_E timer_id
)
{
    // - TBD
    
    return;
}

/***************************************************************************** 
 * Function
 *  mtk_nfc_sys_sleep
 * DESCRIPTION
 *  task sleep funciton
 * PARAMETERS
 *  pString     [IN] data Data block
 *  length      [IN] size buffer size of the data block
 * RETURNS
 *  VOID
 *****************************************************************************/ 
VOID 
mtk_nfc_sys_sleep (
    UINT32 u4MilliSeconds
)
{
    int i;
    for (i=0;i<u4MilliSeconds;i++)
        {
    usleep(1000);
        }
	  #if 0
    #ifdef WIN32
    Sleep(u4MilliSeconds);
    #endif
    #endif
    
    return;
}

VOID
mtk_nfc_sys_assert (
    INT32 value
)
{
	  #if 0
    #ifdef WIN32
    assert(value);
    #endif
    #endif
    
    return;
}

#ifdef WIN32 // PHY layer
/* ***************************************************************************
Physical Link Function
    gLinkFunc.init  = mtkNfcDal_uart_init;
    gLinkFunc.open = mtkNfcDal_uart_open;
    gLinkFunc.close = mtkNfcDal_uart_close;
    gLinkFunc.read  = mtkNfcDal_uart_read;
    gLinkFunc.write  = mtkNfcDal_uart_write;
    gLinkFunc.flush  = mtkNfcDal_uart_flush;
    gLinkFunc.reset = mtkNfcDal_chip_reset;    „³ GPIO control for NFC pins
UART
    void mtkNfcDal_uart_init (void);
    NFCSTATUS mtkNfcDal_uart_open (const char* deviceNode, void ** pLinkHandle)
    int mtkNfcDal_uart_read (uint8_t * pBuffer, int nNbBytesToRead);
    int mtkNfcDal_uart_write (uint8_t * pBuffer, int nNbBytesToWrite);
    void mtkNfcDal_uart_flush (void);
    void mtkNfcDal_uart_close (void);
GPIO
    int mtkNfcDal_chip_reset (int level);
/* ************************************************************************ */

extern HANDLE g_hUart;

// UART settings for Windows UART driver
#define NFC_UART_BAUD                   (115200)
#define NFC_UART_BUF_TX                 (1024)
#define NFC_UART_BUF_RX                 (1024)

VOID *
mtk_nfc_sys_uart_init (
    const CH* strDevPortName
)
{
    HANDLE hUARTHandle = INVALID_HANDLE_VALUE;

    hUARTHandle = CreateFile(strDevPortName, GENERIC_READ | GENERIC_WRITE,
                  0, NULL, OPEN_EXISTING, 0, NULL);

    if (INVALID_HANDLE_VALUE != hUARTHandle)
    {
        DCB dcb;
        BOOL fSuccess;

        fSuccess = GetCommState(hUARTHandle, &dcb);
        if (fSuccess)
        {
            dcb.BaudRate = NFC_UART_BAUD;
            dcb.ByteSize = 8;
            dcb.Parity = NOPARITY;
            dcb.StopBits = ONESTOPBIT;
            dcb.fOutxDsrFlow = FALSE;
            dcb.fOutxCtsFlow = FALSE;
            dcb.fDtrControl = DTR_CONTROL_DISABLE;
            dcb.fRtsControl = RTS_CONTROL_ENABLE;
            dcb.fInX = FALSE;           // No Xon/Xoff flow control
            dcb.fOutX = FALSE;
            dcb.fBinary = TRUE;
            dcb.fAbortOnError = FALSE;  // Do not abort reads/writes on error
            dcb.fErrorChar = FALSE;     // Disable error replacement
            dcb.fNull = FALSE;          // Disable null stripping

            fSuccess = SetCommState(hUARTHandle, &dcb);
            
            if (fSuccess)
            {
                COMMTIMEOUTS timeouts;

                // setup device buffer
                SetupComm(hUARTHandle, NFC_UART_BUF_RX, NFC_UART_BUF_TX);

                // setup timeout
                GetCommTimeouts(hUARTHandle, &timeouts);
                timeouts.ReadIntervalTimeout = MAXDWORD;
                timeouts.ReadTotalTimeoutConstant = 0;
                timeouts.ReadTotalTimeoutMultiplier = 0;
                timeouts.WriteTotalTimeoutConstant = 0;
                timeouts.WriteTotalTimeoutMultiplier = 0;
                SetCommTimeouts(hUARTHandle, &timeouts);
            }
        }

        if (!fSuccess)
        {
            CloseHandle(hUARTHandle);
            hUARTHandle = INVALID_HANDLE_VALUE;
        }
    }

    return hUARTHandle;
}

INT32 
mtk_nfc_sys_uart_read (
//    VOID *pLinkHandle,
    UINT8 *pBuffer, 
    UINT32 nNbBytesToRead
)
{
    DWORD dwRead = 0;    

    if (INVALID_HANDLE_VALUE != g_hUart)
    {
        if (ReadFile(g_hUart, pBuffer, nNbBytesToRead, (LPDWORD)&dwRead, NULL))
        {
            // read success - one shot read and return
        }
        else
        {
            //assert(0);
            dwRead = -1;
        }
    }
    else
    {
        mtk_nfc_sys_dbg_string("UART Handle is invalid\r\n");
        dwRead = -2;
    }
    
    return dwRead;
}

INT32
mtk_nfc_sys_uart_write (
//    VOID *pLinkHandle,
    UINT8 *pBuffer, 
    UINT32 nNbBytesToWrite
)
{
    DWORD dwWritten;
    UINT32 u4Offset = 0;

    mtk_nfc_sys_dbg_string("            ---> PHY TX: ");
    mtk_nfc_sys_dbg_trace(pBuffer, nNbBytesToWrite);
    mtk_nfc_sys_dbg_string("\r\n");

    if (INVALID_HANDLE_VALUE != g_hUart)
    {
        while (u4Offset < nNbBytesToWrite)
        {
            if (WriteFile(g_hUart, &pBuffer[u4Offset], nNbBytesToWrite - u4Offset, &dwWritten, NULL))
            {
                // write success - continuely write if the write data is not completed
                u4Offset += dwWritten;
            }
            else
            {
                //assert(0);            
                break;
            }
        }
    }
    else
    {
        mtk_nfc_sys_dbg_string("UART Handle is invalid\r\n");
    }    

    return dwWritten;
}

VOID 
mtk_nfc_sys_uart_flush (
    VOID *pLinkHandle
)
{
    // purge any information in buffer
    PurgeComm(pLinkHandle, PURGE_TXABORT | PURGE_RXABORT | PURGE_TXCLEAR | PURGE_RXCLEAR);
}

// uninit UART
VOID
mtk_nfc_sys_uart_uninit (
    VOID *pLinkHandle
)
{
    if (INVALID_HANDLE_VALUE != pLinkHandle)
    {
        CloseHandle(pLinkHandle);
    }
}
#endif


VOID 
mtk_nfc_sys_dbg_trx_to_file(
    BOOL    fgIsTx, 
    UINT8   pData[], 
    UINT32  u4Len
)
{


}

/*****************************************************************************
 * FUNCTION
 *  mtk_nfc_sys_interface_init
 * DESCRIPTION
 *  Initialize communication interface between DH and NFCC
 * PARAMETERS
 *  strDevPortName      [IN] Device Name
 *  i4Baud              [IN] Baudrate
 * RETURNS
 *  NONE
 *****************************************************************************/
VOID *
mtk_nfc_sys_interface_init (
    const CH* strDevPortName,
    const INT32 i4Baud
)
{
#ifdef WIN32
    return mtk_nfc_sys_uart_init(strDevPortName, i4Baud);
#else
    return mtk_nfc_interface_init(0);
#endif
}

/*****************************************************************************
 * FUNCTION
 *  mtk_nfc_sys_interface_read
 * DESCRIPTION
 *  Read data from NFCC
 * PARAMETERS
 *  pBuffer             [IN] read buffer
 *  nNbBytesToRead      [IN] number of bytes to read
 * RETURNS
 *  number of bytes read
 *****************************************************************************/
INT32 
mtk_nfc_sys_interface_read (
    UINT8 *pBuffer, 
    UINT16 nNbBytesToRead
)
{       
#ifdef WIN32
    return mtk_nfc_sys_uart_read(pBuffer, nNbBytesToRead);
#else // 
   return mtk_nfc_sys_i2c_read(pBuffer, nNbBytesToRead);
#endif
}

/*****************************************************************************
 * FUNCTION
 *  mtk_nfc_sys_interface_write
 * DESCRIPTION
 *  Write data to NFCC
 * PARAMETERS
 *  pBuffer             [IN] write buffer
 *  nNbBytesToWrite     [IN] number of bytes to write
 * RETURNS
 *  number of bytes written
 *****************************************************************************/
INT32
mtk_nfc_sys_interface_write (
    UINT8 *pBuffer, 
    UINT16 nNbBytesToWrite
)
{
#ifdef WIN32
    return mtk_nfc_sys_uart_write(pBuffer, nNbBytesToWrite);
#else // 
   return mtk_nfc_sys_i2c_write(pBuffer, nNbBytesToWrite);
#endif
}

/*****************************************************************************
 * FUNCTION
 *  mtk_nfc_sys_interface_flush
 * DESCRIPTION
 *  Flush communication interface
 * PARAMETERS
 *  pLinkHandle         [IN] Link Handle
 * RETURNS
 *  NONE
 *****************************************************************************/
VOID 
mtk_nfc_sys_interface_flush (
    VOID *pLinkHandle
)
{
#ifdef WIN32
    mtk_nfc_sys_uart_flush(pLinkHandle);
#endif
}

/*****************************************************************************
 * FUNCTION
 *  mtk_nfc_sys_interface_uninit
 * DESCRIPTION
 *  mt6605 gpio config
 * PARAMETERS
 *  pLinkHandle         [IN] Link Handle
 * RETURNS
 *  NONE
 *****************************************************************************/
VOID
mtk_nfc_sys_interface_uninit (
    VOID *pLinkHandle
)
{
#ifdef WIN32
    mtk_nfc_sys_uart_uninit(pLinkHandle);
#endif
}


/*****************************************************************************
 * FUNCTION
 *  mtk_nfc_sys_gpio_write
 * DESCRIPTION
 *  mt6605 gpio config
 * PARAMETERS
 *  ePin        [IN] GPIO PIN
 *  eHighLow    [IN] High or How
 * RETURNS
 *  NONE
 *****************************************************************************/
VOID 
mtk_nfc_sys_gpio_write(
    MTK_NFC_GPIO_E ePin, 
    MTK_NFC_PULL_E eHighLow
)
{
   UINT32 result;
   //static int mt6605_dev_unlocked_ioctl(struct file *filp, unsigned int cmd, unsigned long arg)
   //result = unlocked_ioctl(gInterfaceHandle, MTK_NFC_IOCTL_WRITE, ((ePin << 8) | (eHighLow)));
   result = ioctl(gInterfaceHandle, MTK_NFC_IOCTL_WRITE, ((ePin << 8) | (eHighLow)));
   
   if(result != MTK_NFC_SUCCESS)
   {
      ;//ERROR
   }
   
   return;

}

/*****************************************************************************
 * FUNCTION
 *  mtk_nfc_sys_gpio_read
 * DESCRIPTION
 *  mt6605 gpio config
 * PARAMETERS
 *  ePin        [IN] GPIO PIN
 * RETURNS
 *  MTK_NFC_PULL_E
 *****************************************************************************/
MTK_NFC_PULL_E 
mtk_nfc_sys_gpio_read(
    MTK_NFC_GPIO_E ePin
)
{
   MTK_NFC_PULL_E result;
   //static int mt6605_dev_unlocked_ioctl(struct file *filp, unsigned int cmd, unsigned long arg)
   //result = unlocked_ioctl(gInterfaceHandle, MTK_NFC_IOCTL_READ, ePin);
   result = ioctl(gInterfaceHandle, MTK_NFC_IOCTL_READ, ePin);
   
   #if 0
   if(result != MTK_NFC_SUCCESS)
   {
      ;//ERROR
   }
   #endif
   
   return result;

}

NFCSTATUS mtk_nfc_sys_i2c_read(UINT8* data, UINT16 len)
{
    NFCSTATUS result, i;
   result = read(gInterfaceHandle, data, len);   
   return result;
}


NFCSTATUS mtk_nfc_sys_i2c_write(UINT8* data, UINT16 len)
{
    NFCSTATUS result, i;
    #ifdef DEBUG_LOG
    //NFC_LOGD(DBG_MOD_HAL, DBG_TYP_INF, "mtk_nfc_sys_i2c_write :len,0x%x\r\n", len);
    ALOGD("mtk_nfc_sys_i2c_write :len,0x%x\r\n", len);
    #endif
  
    ALOGD("[TX],");
    for( i =0;i < len;i++)
    {
       ALOGD("%02x,",data[i]);
    }
    ALOGD("\n");
     
    result = write(gInterfaceHandle, data, len);
    
    return result;
}
int mtk_nfc_interface_init(int type)
{
   int result = MTK_NFC_SUCCESS;
   char *       pComPort;

   
   gInterfaceHandle = FALSE;
   pComPort = "/dev/mt6605";

   gInterfaceHandle = open(pComPort, O_RDWR | O_NOCTTY);

   //unlocked_ioctl(handle, 0, 1);

   
   
   return (result);

}

